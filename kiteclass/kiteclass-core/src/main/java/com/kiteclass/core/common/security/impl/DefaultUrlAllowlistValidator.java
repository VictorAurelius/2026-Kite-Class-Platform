package com.kiteclass.core.common.security.impl;

import com.kiteclass.core.common.security.UrlAllowlistValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Config-driven URL allowlist that blocks SSRF vectors (loopback, link-local,
 * private ranges, userinfo) and enforces a per-tenant domain allowlist with a
 * global fallback.
 *
 * <p>Rule summary (per ADR-011 §SSRF):
 * <ol>
 *   <li>Scheme MUST be http or https — everything else is denied.</li>
 *   <li>URL MUST NOT carry userinfo (credentials embedded in URL).</li>
 *   <li>Host MUST NOT resolve into a private/loopback/link-local range.</li>
 *   <li>Tenant-scoped (tenantId != null) → must match
 *       {@code security.url.allowlist.<tenantId>} OR the default allowlist.</li>
 *   <li>Global (tenantId == null) → must match a public-api pattern.</li>
 * </ol>
 *
 * <p>Configuration keys (see application.yml):
 * <pre>
 * security.url.allowlist.default: ["api.partner.com", "*.trusted.org"]
 * security.url.allowlist.public-api-patterns: ["^https://api\\.ollama\\.com/.*"]
 * security.url.allowlist.&lt;tenantId&gt;: [...]
 * </pre>
 *
 * @since 3.24.0 (Wave 4 Sub-PR 4.2)
 */
@Component
public class DefaultUrlAllowlistValidator implements UrlAllowlistValidator {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultUrlAllowlistValidator.class);

    private final Environment env;
    private final List<String> defaultAllowlist;
    private final List<Pattern> publicApiPatterns;

    public DefaultUrlAllowlistValidator(
            Environment env,
            @Value("${security.url.allowlist.default:}") String defaultCsv,
            @Value("${security.url.allowlist.public-api-patterns:}") String publicApiCsv
    ) {
        this.env = env;
        this.defaultAllowlist = splitCsv(defaultCsv);
        this.publicApiPatterns = splitCsv(publicApiCsv).stream()
                .map(Pattern::compile)
                .toList();
    }

    @Override
    public boolean isAllowed(String url, String tenantId) {
        if (url == null || url.isBlank()) {
            return false;
        }

        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException ex) {
            LOG.debug("URL rejected — unparseable: {}", url);
            return false;
        }

        if (uri.getScheme() == null || uri.getHost() == null) {
            return false;
        }
        String scheme = uri.getScheme().toLowerCase();
        if (!(scheme.equals("http") || scheme.equals("https"))) {
            LOG.debug("URL rejected — scheme {} not in [http, https]", scheme);
            return false;
        }
        if (uri.getUserInfo() != null) {
            LOG.debug("URL rejected — userinfo embedded");
            return false;
        }

        String host = uri.getHost().toLowerCase();

        // Literal-IP hosts get resolved + range-checked up front. Hostnames get matched against the
        // configured allowlist first (fast, no DNS). If allowlisted, we still deny if the name
        // resolves to an internal range (to block DNS-rebind attacks that point a whitelisted host
        // at 127.0.0.1). If not allowlisted, we fall through to the per-tenant / public-api gate.
        if (isLiteralIp(host) && isInternalIp(host)) {
            LOG.debug("URL rejected — literal internal IP: {}", host);
            return false;
        }
        if (host.equals("localhost")) {
            LOG.debug("URL rejected — localhost");
            return false;
        }

        // Tenant-scoped path
        if (tenantId != null && !tenantId.isBlank()) {
            List<String> tenantList = tenantAllowlist(tenantId);
            boolean matched = matchesAnyDomain(host, tenantList) || matchesAnyDomain(host, defaultAllowlist);
            if (matched) {
                return !resolvesInternally(host);
            }
            LOG.debug("URL rejected — host {} not in tenant({}) or default allowlist", host, tenantId);
            return false;
        }

        // Null tenantId → only allow if matches public-api pattern list
        for (Pattern p : publicApiPatterns) {
            if (p.matcher(url).matches()) {
                return !resolvesInternally(host);
            }
        }
        LOG.debug("URL rejected — no tenant + no public-api pattern match: {}", url);
        return false;
    }

    private static boolean isLiteralIp(String host) {
        // IPv4 dotted-quad or IPv6 literal (URI.getHost strips the brackets)
        return host.matches("^(\\d{1,3}\\.){3}\\d{1,3}$") || host.contains(":");
    }

    private static boolean isInternalIp(String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            return addr.isAnyLocalAddress()
                    || addr.isLoopbackAddress()
                    || addr.isLinkLocalAddress()
                    || addr.isSiteLocalAddress()
                    || addr.isMulticastAddress()
                    || isPrivateRange(addr.getHostAddress());
        } catch (UnknownHostException ex) {
            return true;
        }
    }

    /**
     * Resolve the hostname; return true if it points at an internal range (DNS-rebind guard).
     * Unresolvable → false (we've already allowlisted it; upstream HTTP call will fail naturally).
     */
    private static boolean resolvesInternally(String host) {
        try {
            InetAddress[] addrs = InetAddress.getAllByName(host);
            for (InetAddress addr : addrs) {
                if (addr.isAnyLocalAddress()
                        || addr.isLoopbackAddress()
                        || addr.isLinkLocalAddress()
                        || addr.isSiteLocalAddress()
                        || addr.isMulticastAddress()
                        || isPrivateRange(addr.getHostAddress())) {
                    return true;
                }
            }
            return false;
        } catch (UnknownHostException ex) {
            LOG.debug("Allowlisted host {} did not resolve; upstream call will fail naturally", host);
            return false;
        }
    }

    private List<String> tenantAllowlist(String tenantId) {
        String raw = env.getProperty("security.url.allowlist." + tenantId, "");
        return splitCsv(raw);
    }

    private static boolean matchesAnyDomain(String host, List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (String pat : patterns) {
            if (matchesDomain(host, pat.toLowerCase().trim())) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesDomain(String host, String pattern) {
        if (pattern.isEmpty()) {
            return false;
        }
        if (pattern.startsWith("*.")) {
            String suffix = pattern.substring(1); // ".trusted.org"
            return host.endsWith(suffix) && !host.equals(suffix.substring(1));
        }
        return host.equals(pattern);
    }

    /** Fallback net-range check for literal IPs InetAddress didn't flag. */
    private static boolean isPrivateRange(String ip) {
        if (ip == null) {
            return false;
        }
        if (ip.equals("::1")) {
            return true;
        }
        // IPv4 ranges
        if (ip.startsWith("10.")
                || ip.startsWith("127.")
                || ip.startsWith("192.168.")
                || ip.startsWith("169.254.")) {
            return true;
        }
        // 172.16.0.0 – 172.31.255.255
        if (ip.startsWith("172.")) {
            String[] parts = ip.split("\\.");
            if (parts.length >= 2) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    if (second >= 16 && second <= 31) {
                        return true;
                    }
                } catch (NumberFormatException ignored) {
                    // not a dotted-quad IPv4 — leave to InetAddress flags
                }
            }
        }
        return false;
    }

    private static List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        for (String token : csv.split(",")) {
            String t = token.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }
}
