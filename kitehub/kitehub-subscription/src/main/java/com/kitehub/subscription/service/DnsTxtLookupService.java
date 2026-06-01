package com.kitehub.subscription.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * DNS TXT record lookup service using JDK JNDI (com.sun.jndi.dns.DnsContextFactory).
 *
 * <p>Per GAP-812 §Phần A: replaces the previous {@code return false} stub in
 * {@link DomainService#checkDnsTxtRecord(String, String)} with a real DNS TXT
 * resolver. No external dependency required (JNDI is built into the JDK).</p>
 *
 * <p>Convention per industry standard + GAP-812 outside-in audit findings:
 * tenant adds TXT record at {@code _kitehub-verify.{domain}} (preferred — avoids
 * SPF/DKIM collision at apex). Apex {@code {domain}} accepted as fallback.</p>
 *
 * <p>Returns {@code false} (never throws) on lookup failure / missing record /
 * timeout — let the state machine handle (kept PENDING_VERIFY until timeout job
 * flips to FAILED). NameNotFoundException is benign (record simply not added
 * yet by tenant).</p>
 *
 * <p>Extracted from {@link DomainService} for testability — implementations
 * can be mocked in unit tests (avoid hitting real DNS during CI).</p>
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class DnsTxtLookupService {

    /** TXT record subdomain prefix per industry convention. */
    private static final String VERIFY_PREFIX = "_kitehub-verify.";

    /** DNS lookup timeout in milliseconds (configurable via system property). */
    private static final String DNS_TIMEOUT_MS = "5000";

    /**
     * Check if a TXT record matching the expected token exists at either
     * {@code _kitehub-verify.{domain}} (preferred) or {@code {domain}} (fallback apex).
     *
     * @param domain        the custom domain (e.g., "school.example.com")
     * @param expectedToken token to match (e.g., "kitehub-verify=abc123-...")
     * @return true if any TXT record at either host contains the expected token; false otherwise
     */
    public boolean verifyTxtRecord(String domain, String expectedToken) {
        if (domain == null || domain.isBlank() || expectedToken == null || expectedToken.isBlank()) {
            log.debug("DNS TXT verify skipped: domain or token blank");
            return false;
        }

        String[] candidates = { VERIFY_PREFIX + domain, domain };
        for (String host : candidates) {
            List<String> records = lookupTxtRecords(host);
            for (String record : records) {
                if (record.contains(expectedToken)) {
                    log.info("DNS TXT verify SUCCESS: host={}, token matched", host);
                    return true;
                }
            }
            log.debug("DNS TXT lookup at host={} found {} records, no match", host, records.size());
        }

        return false;
    }

    /**
     * Look up all TXT records at the given host via JNDI.
     * Returns empty list (never throws) on lookup failure / no records.
     *
     * <p>Visible for testing — can be overridden in test subclasses to avoid
     * hitting real DNS.</p>
     *
     * @param host fully-qualified hostname to look up (e.g., "_kitehub-verify.school.example.com")
     * @return list of TXT record values (with surrounding quotes stripped), or empty list
     */
    protected List<String> lookupTxtRecords(String host) {
        List<String> results = new ArrayList<>();
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        env.put(Context.PROVIDER_URL, "dns:");
        env.put("com.sun.jndi.dns.timeout.initial", DNS_TIMEOUT_MS);
        env.put("com.sun.jndi.dns.timeout.retries", "1");

        DirContext ctx = null;
        try {
            ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(host, new String[]{"TXT"});
            Attribute txt = attrs.get("TXT");
            if (txt == null) {
                return results;
            }
            NamingEnumeration<?> values = txt.getAll();
            while (values.hasMore()) {
                String value = String.valueOf(values.next());
                // JNDI returns TXT values with surrounding quotes for multi-part records
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                results.add(value);
            }
        } catch (NameNotFoundException e) {
            // Record not added yet by tenant — benign
            log.debug("DNS TXT lookup: host={} not found (record not added yet)", host);
        } catch (Exception e) {
            log.warn("DNS TXT lookup failed for host={}: {}", host, e.getMessage());
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (Exception ignored) {
                    // best-effort close
                }
            }
        }
        return results;
    }
}
