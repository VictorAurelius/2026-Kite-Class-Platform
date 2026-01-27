# KiteClass Gateway - Email Service Module

## Overview

**Status:** ✅ IMPLEMENTED (PR 1.5 - 2026-01-27)
**Branch:** feature/gateway
**Location:** `kiteclass/kiteclass-gateway/src/main/java/com/kiteclass/gateway/service/`

The Email Service Module provides email sending capabilities with HTML templates using Spring Boot Mail and Thymeleaf, integrated with the authentication module for password reset functionality.

---

## Architecture

```
gateway/
├── config/
│   └── EmailProperties              # Email configuration properties
├── service/
│   ├── EmailService                 # Email service interface
│   └── impl/
│       └── EmailServiceImpl         # Implementation with JavaMailSender
├── module/auth/
│   ├── entity/
│   │   └── PasswordResetToken       # Password reset token entity
│   ├── repository/
│   │   └── PasswordResetTokenRepository
│   └── service/impl/
│       └── AuthServiceImpl          # Updated with email integration
└── resources/
    └── templates/email/
        ├── password-reset.html      # Password reset email template
        ├── welcome.html             # Welcome email template
        └── account-locked.html      # Account locked notification
```

---

## Email Service Interface

### Methods

```java
public interface EmailService {

    // Send password reset email with token link
    Mono<Void> sendPasswordResetEmail(
        String to,
        String userName,
        String resetToken
    );

    // Send welcome email to new user
    Mono<Void> sendWelcomeEmail(
        String to,
        String userName
    );

    // Send account locked notification
    Mono<Void> sendAccountLockedEmail(
        String to,
        String userName,
        long lockDurationMinutes
    );
}
```

---

## Reactive Pattern Implementation

### Wrapping Blocking JavaMailSender

**Problem:** JavaMailSender is blocking (synchronous)
**Solution:** Wrap in Mono and execute on boundedElastic scheduler

```java
private Mono<Void> sendEmail(
    String to,
    String subject,
    String templateName,
    Map<String, Object> variables
) {
    return Mono.fromRunnable(() -> {
        try {
            // Process Thymeleaf template
            Context context = new Context();
            context.setVariables(variables);
            String html = templateEngine.process("email/" + templateName, context);

            // Create and send email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(emailProperties.getFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    })
    .subscribeOn(Schedulers.boundedElastic()) // Non-blocking execution
    .then();
}
```

**Key Points:**
- `Mono.fromRunnable()` - Wraps blocking operation
- `subscribeOn(Schedulers.boundedElastic())` - Executes on separate thread pool
- `.then()` - Convert to Mono<Void>

---

## Password Reset Flow

### Database Schema

```sql
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP NULL,
    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token);
CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_expires_at ON password_reset_tokens(expires_at);
CREATE INDEX idx_password_reset_tokens_used_at ON password_reset_tokens(used_at);
```

### Flow Diagram

```
┌──────────────────────────────────────────────────────────────┐
│              PASSWORD RESET FLOW                              │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  User                  Gateway                 Database       │
│   │                       │                       │           │
│   │─POST forgot-password─>│                       │           │
│   │  {email}              │                       │           │
│   │                       │──findByEmail────────> │           │
│   │                       │ <──User───────────────│           │
│   │                       │                       │           │
│   │                       │──Generate UUID────────│           │
│   │                       │──Delete old tokens──> │           │
│   │                       │──Save new token─────> │           │
│   │                       │                       │           │
│   │                       │──sendEmail()────────> │           │
│   │                       │  (async, non-blocking)│           │
│   │                       │                       │           │
│   │<─200 OK───────────────│                       │           │
│   │  "Check your email"   │                       │           │
│   │                       │                       │           │
│   │───Receives email──────│                       │           │
│   │   with reset link     │                       │           │
│   │                       │                       │           │
│   │─POST reset-password──>│                       │           │
│   │  {token, newPassword} │                       │           │
│   │                       │──Find token─────────> │           │
│   │                       │ <─Token───────────────│           │
│   │                       │──Validate (not expired,           │
│   │                       │   not used)           │           │
│   │                       │──Update password────> │           │
│   │                       │──Mark token used────> │           │
│   │                       │──Delete refresh tokens>│           │
│   │                       │                       │           │
│   │<─200 OK───────────────│                       │           │
│   │  "Password reset"     │                       │           │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

### Forgot Password Implementation

```java
@Override
@Transactional
public Mono<Void> forgotPassword(ForgotPasswordRequest request) {
    return userRepository.findByEmailAndDeletedFalse(request.email())
        .flatMap(user -> {
            // Check account is active
            if (user.getStatus() != UserStatus.ACTIVE) {
                return Mono.empty(); // Silent fail for security
            }

            // Generate UUID token
            String resetToken = UUID.randomUUID().toString();

            // Create token entity
            PasswordResetToken tokenEntity = PasswordResetToken.builder()
                .token(resetToken)
                .userId(user.getId())
                .expiresAt(Instant.now().plusMillis(
                    emailProperties.getResetTokenExpiration()
                ))
                .createdAt(Instant.now())
                .build();

            // Delete old tokens and save new one
            return passwordResetTokenRepository.deleteByUserId(user.getId())
                .then(passwordResetTokenRepository.save(tokenEntity))
                .flatMap(savedToken -> {
                    // Send email (non-blocking)
                    return emailService.sendPasswordResetEmail(
                        user.getEmail(),
                        user.getName(),
                        resetToken
                    );
                });
        })
        .then() // Always return success (don't reveal if email exists)
        .switchIfEmpty(Mono.empty());
}
```

### Reset Password Implementation

```java
@Override
@Transactional
public Mono<Void> resetPassword(ResetPasswordRequest request) {
    return passwordResetTokenRepository.findByToken(request.token())
        .switchIfEmpty(Mono.error(new BusinessException(
            MessageCodes.PASSWORD_RESET_TOKEN_INVALID,
            HttpStatus.BAD_REQUEST
        )))
        .flatMap(token -> {
            // Validate token
            if (token.isExpired()) {
                return passwordResetTokenRepository.delete(token)
                    .then(Mono.error(new BusinessException(
                        MessageCodes.PASSWORD_RESET_TOKEN_EXPIRED,
                        HttpStatus.BAD_REQUEST
                    )));
            }

            if (token.isUsed()) {
                return Mono.error(new BusinessException(
                    MessageCodes.PASSWORD_RESET_TOKEN_USED,
                    HttpStatus.BAD_REQUEST
                ));
            }

            // Get user and update password
            return userRepository.findById(token.getUserId())
                .flatMap(user -> {
                    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
                    user.setFailedLoginAttempts(0);
                    user.setLockedUntil(null);

                    return userRepository.save(user)
                        .then(Mono.defer(() -> {
                            // Mark token as used
                            token.setUsedAt(Instant.now());
                            return passwordResetTokenRepository.save(token);
                        }))
                        .then(Mono.defer(() -> {
                            // Invalidate all refresh tokens for security
                            return refreshTokenRepository.deleteByUserId(user.getId());
                        }));
                });
        })
        .then();
}
```

---

## Email Templates (Thymeleaf)

### Template Variables

#### password-reset.html
```java
variables.put("userName", userName);
variables.put("resetLink", baseUrl + "/reset-password?token=" + token);
variables.put("expirationMinutes", expiration / 60000);
```

#### welcome.html
```java
variables.put("userName", userName);
variables.put("loginUrl", baseUrl + "/login");
```

#### account-locked.html
```java
variables.put("userName", userName);
variables.put("lockDurationMinutes", duration);
variables.put("supportEmail", "support@kiteclass.com");
```

### Template Features
- Responsive HTML design
- Vietnamese language
- Call-to-action buttons
- Copy-paste link fallback
- Security warnings
- Brand styling

---

## Configuration

### application.yml

```yaml
spring:
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true

email:
  from: ${EMAIL_FROM:KiteClass <noreply@kiteclass.com>}
  base-url: ${APP_BASE_URL:http://localhost:3000}
  reset-token-expiration: ${EMAIL_RESET_TOKEN_EXPIRATION:3600000}
```

### EmailProperties

```java
@Configuration
@ConfigurationProperties(prefix = "email")
public class EmailProperties {
    private String from;
    private String baseUrl;
    private Long resetTokenExpiration;
}
```

### Gmail Setup

1. Enable 2-Factor Authentication
2. Generate App Password (not regular password)
3. Set environment variables:
   ```bash
   MAIL_USERNAME=your-email@gmail.com
   MAIL_PASSWORD=your-16-char-app-password
   ```

---

## Security Features

### Token Security
- **UUID Generation:** Unpredictable, cryptographically secure
- **Expiration:** 1 hour default (configurable)
- **One-Time Use:** `used_at` timestamp prevents reuse
- **Database Storage:** Can be invalidated server-side

### Email Privacy
```java
.then() // Always return success
.switchIfEmpty(Mono.empty()); // Don't reveal if email exists
```
**Why:** Prevent email enumeration attacks

### Session Invalidation
```java
// Delete all refresh tokens after password reset
refreshTokenRepository.deleteByUserId(user.getId())
```
**Why:** Force re-login on all devices for security

---

## REST API Endpoints

### POST /api/v1/auth/forgot-password

**Request:**
```json
{
  "email": "user@example.com"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Nếu email tồn tại, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu"
}
```

### POST /api/v1/auth/reset-password

**Request:**
```json
{
  "token": "uuid-token-from-email",
  "newPassword": "NewPassword@456"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Đặt lại mật khẩu thành công"
}
```

---

## Error Handling

### Error Messages (i18n)

```properties
# messages.properties
error.password_reset.token_invalid=Liên kết đặt lại mật khẩu không hợp lệ
error.password_reset.token_expired=Liên kết đặt lại mật khẩu đã hết hạn
error.password_reset.token_used=Liên kết đặt lại mật khẩu đã được sử dụng
```

### Email Send Failures

```java
.onErrorResume(e -> {
    log.error("Failed to send email", e);
    return Mono.empty(); // Don't fail the request
})
```

**Strategy:** Log error but return success to user (prevent DOS)

---

## Testing

### Unit Tests (EmailServiceTest)

```java
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {
    @Mock JavaMailSender mailSender;
    @Mock TemplateEngine templateEngine;
    @Mock(lenient = true) EmailProperties emailProperties;

    @Test
    void shouldSendPasswordResetEmail() {
        // Test with mocked JavaMailSender
    }
}
```

**Coverage:** 5 tests, all passing ✅

### Integration Tests (PasswordResetIntegrationTest)

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class PasswordResetIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void shouldResetPasswordWithValidToken() {
        // Test full flow with real database
    }
}
```

**Coverage:** 8 tests ✅

---

## Usage Examples

### Sending Password Reset Email

```java
@Autowired
private EmailService emailService;

public Mono<Void> sendResetEmail(User user, String token) {
    return emailService.sendPasswordResetEmail(
        user.getEmail(),
        user.getName(),
        token
    );
}
```

### Manual Testing

```bash
# Request password reset
curl -X POST http://localhost:8080/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email":"owner@kiteclass.local"}'

# Reset password
curl -X POST http://localhost:8080/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{"token":"<token>","newPassword":"NewPass@123"}'
```

---

## Future Enhancements

1. **Email Verification**
   - Verify email on user registration
   - Email verification tokens table
   - Prevent login if not verified

2. **Email Templates**
   - More templates (invoice, notification, etc.)
   - Template customization per tenant
   - Multi-language support

3. **Email Queue**
   - Async processing with message queue
   - Retry logic for failed sends
   - Batch email sending

4. **Email Tracking**
   - Open tracking
   - Click tracking
   - Delivery status webhooks

---

## Related Skills

- [Auth Module](auth-module.md) - Password reset integration
- [Testing Guide](testing-guide.md) - Unit & integration tests
- [API Design](api-design.md) - REST endpoint conventions
- [Database Design](database-design.md) - Token storage schema

---

## Troubleshooting

### Email Not Sending

1. **Check SMTP credentials**
   ```bash
   echo $MAIL_USERNAME
   echo $MAIL_PASSWORD
   ```

2. **Check logs**
   ```
   Failed to send email to: user@example.com
   ```

3. **Test SMTP connection**
   ```bash
   telnet smtp.gmail.com 587
   ```

### Token Expired

- Default: 1 hour
- Configure: `email.reset-token-expiration`
- User must request new token

### Token Already Used

- One-time use only
- Must request new token
- Check `used_at` timestamp

---

**Generated:** 2026-01-27
**Author:** Claude Sonnet 4.5 + VictorAurelius
**Version:** PR 1.5 Complete
