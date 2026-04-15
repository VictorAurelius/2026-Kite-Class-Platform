# GAP-041: Security Hardening — Injection Attacks (SVG XSS, SSRF, CSRF)

**Status:** 🟢 DONE (Wave 4 Sub-PR 4.2, merged 2026-04-14; JsoupSvgSanitizer + DefaultUrlAllowlistValidator (DNS-rebind guard) + DoubleSubmitCsrfTokenProvider (fail-loud on weak secret). 38 XSS/SSRF/CSRF scenarios covered.)
**Priority:** 🔴 P0 (security critical)
**Domain:** Security
**Detected:** 2026-04-14 (simulation: cross-cutting × C5)

## Problem

GAP-018 (content safety) chỉ cover **content moderation** (NSFW). Không cover **injection attacks**:

- ❌ SVG upload: SVG có thể chứa `<script>` → XSS khi render
- ❌ SSRF: AI analyze uploaded URL → attacker provide `http://internal-admin:8080/...` → probe internal network
- ❌ CSRF: wizard form submits không có CSRF token
- ❌ File upload: không virus scan, size limit lỏng
- ❌ Prompt injection: user input trong prompt → hijack AI behavior
- ❌ Rate limit bypass via multiple accounts
- ❌ Tenant isolation bypass (filter injection)

## Proposed Fix

### 1. SVG Sanitization

```java
public String sanitizeSvg(String svgContent) {
  // Use DOMPurify-like sanitizer for SVG
  var clean = SvgSanitizer.sanitize(svgContent, SafeMode.STRICT);
  // Strip: <script>, event handlers (onclick, onerror, ...)
  // Strip: external URLs (xlink:href pointing externally)
  // Strip: CSS with expressions
  return clean;
}
```

Or: reject SVG uploads, convert server-side to PNG.

### 2. SSRF Protection

```java
public URL validateUploadUrl(String userProvidedUrl) {
  URL url = new URL(userProvidedUrl);

  // Block internal IPs
  InetAddress addr = InetAddress.getByName(url.getHost());
  if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()
      || addr.isLinkLocalAddress() || addr.isSiteLocalAddress()) {
    throw new SecurityException("Internal URLs not allowed");
  }

  // Allowlist schemes
  if (!Set.of("http", "https").contains(url.getProtocol())) throw ...;

  // Allowlist ports
  if (!Set.of(80, 443).contains(url.getPort())) throw ...;

  return url;
}
```

### 3. CSRF Tokens

Spring Security CSRF for wizard form submits:
```java
http.csrf(csrf -> csrf.csrfTokenRepository(
  CookieCsrfTokenRepository.withHttpOnlyFalse()
));
```

FE include token in requests.

### 4. File Upload Security

```java
public class UploadValidator {
  MAX_SIZE: 5MB
  ALLOWED_TYPES: image/png, image/jpeg, image/webp, image/svg+xml
  MIN_DIMENSIONS: 100x100
  MAX_DIMENSIONS: 4096x4096

  public void validate(MultipartFile file) {
    if (file.getSize() > MAX_SIZE) throw new SizeException();

    String mime = detectMimeTypeFromContent(file); // Not trust Content-Type header
    if (!ALLOWED_TYPES.contains(mime)) throw new TypeException();

    BufferedImage img = ImageIO.read(file.getInputStream());
    if (img.getWidth() < MIN_DIM || img.getWidth() > MAX_DIM) throw ...;

    // Virus scan
    if (clamAV.isInfected(file)) throw new VirusException();
  }
}
```

### 5. Prompt Injection Protection

User input sanitized trước khi vào AI prompt:
```java
public String sanitizePromptInput(String userInput) {
  // Strip: instruction-like sequences
  var forbidden = List.of(
    "ignore previous", "disregard above", "new instructions",
    "system:", "assistant:", "you are now"
  );
  for (var f : forbidden) {
    if (userInput.toLowerCase().contains(f)) {
      throw new PromptInjectionException();
    }
  }
  return sanitizeHtml(userInput).trim().substring(0, 500); // Length limit
}
```

Use structured prompts với clear delimiters:
```
<SYSTEM>You generate banner descriptions.</SYSTEM>
<USER_INPUT>${sanitized_user_input}</USER_INPUT>
```

### 6. Rate Limit Bypass Protection

- Rate limit per tenant (not per user) — prevent multi-account abuse
- IP-based rate limit cho public endpoints
- CAPTCHA cho signup (prevent automated tenant creation)
- Email domain check (block disposable emails)

### 7. Tenant Isolation Enforcement

- Hibernate filter mandatory (can't disable)
- Integration test: try access other tenant data → 403
- SQL audit: flag queries missing tenant filter
- JPA `@Where` annotation on all entities

### 8. Security Audit Log

```java
@Entity
public class SecurityAuditLog {
  Long id;
  String userId;
  String action;         // UPLOAD, DELETE, IMPERSONATE, etc.
  String resourceType;
  String resourceId;
  String ipAddress;
  String userAgent;
  Boolean success;
  Timestamp occurredAt;
}
```

### 9. Penetration Testing

Quarterly pentest:
- SSRF probing
- SVG XSS attempts
- CSRF bypass
- Tenant isolation bypass
- Prompt injection

## Acceptance Criteria

- [ ] SVG sanitizer implemented or SVG uploads rejected
- [ ] SSRF validator cho URL inputs
- [ ] CSRF tokens enabled cho wizard
- [ ] File upload validation (size/type/virus/dimensions)
- [ ] Prompt injection protection
- [ ] Tenant isolation integration tests
- [ ] Security audit log
- [ ] Quarterly pentest scheduled
- [ ] OWASP Top 10 compliance review
- [ ] Bug bounty program (optional)

## Dependencies

- Spring Security config
- ClamAV or similar virus scanner
- GAP-018 (content safety) — complementary

## Log

- 2026-04-14 — Security hardening gap identified via systematic C5 review
