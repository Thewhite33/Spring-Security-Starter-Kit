# 🛡️ Spring Boot Security Starter

A clone-and-build authentication boilerplate for Spring Boot 4. Auth is already wired — JWT access + refresh tokens, BCrypt password hashing, per-endpoint rate limiting, and a pluggable provider pattern — so you can start writing your own domain logic on day one instead of rebuilding login for the tenth time.

This is **not** a published library you add as a dependency. You clone this repo, it becomes your project, and you edit it directly.

---

## ✨ What's included

- **JWT auth** — access token + refresh token, issued on login, validated on every protected request via a servlet filter
- **BCrypt password hashing** — no plaintext passwords ever touch the database
- **Rate limiting** — Bucket4j + Caffeine, applied per-IP to `/auth/login` and `/auth/register` independently, with separate limits for each
- **Pluggable auth providers** — add a new login method (API key, Google OAuth, magic link, whatever) by implementing one interface and annotating it `@Component`. No existing code needs to change.
- **No forced migration tool** — Hibernate creates your schema automatically in dev (`ddl-auto: update`). Add Flyway/Liquibase yourself later if you want versioned migrations in production.
- **Clean layering** — controller → service → repository, DTOs and exceptions kept separate, nothing magic

---

## 📋 Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL 14+ (or swap for any JPA-compatible DB)

---

## 🚀 Quick start

**1. Clone it**
```bash
git clone https://github.com/your-org/security-starter.git
cd security-starter
```

**2. Create a database**
```sql
CREATE DATABASE security_starter_db;
```

**3. Configure `application.yml`**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/security_starter_db
    username: postgres
    password: your_db_password
  jpa:
    hibernate:
      ddl-auto: update   # auto-creates tables; swap for Flyway before production

app:
  jwt:
    secret: ${JWT_SECRET:CHANGE-THIS-TO-A-RANDOM-STRING-AT-LEAST-32-CHARS-LONG}
    expiration-ms: 900000            # 15 min access token
    refresh-expiration-ms: 604800000 # 7 day refresh token
  rate-limit:
    login:
      capacity: 5
      refill-tokens: 5
      refill-duration-seconds: 60
    register:
      capacity: 3
      refill-tokens: 3
      refill-duration-seconds: 60
```

Never commit a real JWT secret. Use an environment variable in any shared or deployed environment.

**4. Run it**
```bash
mvn spring-boot:run
```

Hibernate creates the `users` table (and any others your entities define) on first boot. No manual SQL, no migration step needed to get started.

---

## 📚 API reference

### Register
```
POST /auth/register
Content-Type: application/json

{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "SecurePassword123!"
}
```
`201 Created` on success. Rate-limited independently from login (default: 3 attempts/min per IP).

### Login
```
POST /auth/login
Content-Type: application/json

{
  "usernameOrEmail": "john@example.com",
  "password": "SecurePassword123!"
}
```
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```
Rate-limited to 5 attempts/min per IP by default. A 429 response includes `Retry-After`.

### Refresh
```
POST /auth/refresh
Content-Type: application/json

{ "refreshToken": "YOUR_REFRESH_TOKEN" }
```
Returns a new access token. Wire in rotation/revocation yourself if you need single-use refresh tokens.

### Access a protected route
```
GET /api/ping
Authorization: Bearer YOUR_ACCESS_TOKEN
```
`JwtAuthFilter` validates the token and populates the `SecurityContext` before the request reaches your controller — you don't do anything special to make an endpoint "protected" beyond your normal Spring Security config.

---

## 🏗️ Building your app on top of this

**Add your own entities normally:**
```java
@Entity
public class Post {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String title;

    @ManyToOne
    private User author; // reuse the kit's User entity
}
```

**Protect your own endpoints with standard Spring Security:**
```java
@RestController
@RequestMapping("/api/posts")
public class PostController {

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> create(@RequestBody PostRequest req, Authentication auth) {
        String currentUser = auth.getName(); // from the validated JWT
        // your logic
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable UUID id) { /* ... */ }
}
```
Nothing auth-related needs to be touched — `JwtAuthFilter` already runs ahead of every request.

---

## 🔌 Extending the kit

### Add a new login method (e.g. Google OAuth)
Implement `AuthProvider`, register it as a bean — the registry picks it up with zero other changes:

```java
@Component
@RequiredArgsConstructor
public class GoogleAuthProvider implements AuthProvider {

    @Override
    public boolean supports(LoginRequest request) {
        return request.getGoogleIdToken() != null; // add this field to LoginRequest
    }

    @Override
    public AuthResult authenticate(LoginRequest request) {
        // validate token with Google, find-or-create the User, return AuthResult
    }
}
```
`AuthProviderRegistry` autowires `List<AuthProvider>` and dispatches to whichever `supports()` returns true — your defaults and any new providers coexist without conflict.

### Scale rate limiting across multiple instances
The default `InMemoryRateLimiter` (Bucket4j buckets cached with Caffeine, evicted after 10 min idle) works for a single instance. For multiple instances behind a load balancer:
1. Add `bucket4j-redis` and `spring-boot-starter-data-redis`.
2. Implement `RedisRateLimiter` against the same `RateLimiter` interface.
3. Swap which bean is active via a property-driven `@ConditionalOnProperty`, or just delete the in-memory one — it's your code now.

> **Behind a reverse proxy?** `RateLimitFilter` currently keys on `request.getRemoteAddr()`, which returns the proxy's IP, not the client's, once you're behind nginx/an ALB. Read `X-Forwarded-For` instead once you deploy behind one, and only trust it if your proxy is configured to strip client-supplied values first — otherwise it's trivially spoofable.

### Add password reset / email verification
Not built in by default. Pattern to follow: create a `PasswordResetToken` entity alongside `User` (same shape as a refresh token — value + expiry + used flag), add `/auth/forgot-password` and `/auth/reset-password` endpoints to `AuthController`, and use `JavaMailSender` to send the link.

### Switching databases
Nothing here is Postgres-specific except the driver dependency and JDBC URL. Swap the `postgresql` dependency for `mysql-connector-j`, `h2`, etc., and update `spring.datasource.url` accordingly — the entities and repositories are plain JPA.

---

## ⚙️ Configuration reference

| Property | Default | Meaning |
|---|---|---|
| `app.jwt.secret` | *(required)* | Signing key for JWTs — 32+ random characters, from an env var |
| `app.jwt.expiration-ms` | `900000` | Access token lifetime (15 min) |
| `app.jwt.refresh-expiration-ms` | `604800000` | Refresh token lifetime (7 days) |
| `app.rate-limit.login.capacity` | `5` | Max login attempts per window, per IP |
| `app.rate-limit.login.refill-duration-seconds` | `60` | Window length for login attempts |
| `app.rate-limit.register.capacity` | `3` | Max registration attempts per window, per IP |
| `spring.jpa.hibernate.ddl-auto` | `update` | Auto-creates/updates schema — replace with Flyway before production |

---

## ✅ Before you deploy this

- [ ] Replace the JWT secret with a real, random value injected via environment variable — never commit it
- [ ] Replace `ddl-auto: update` with Flyway or Liquibase for controlled schema migrations
- [ ] Put this behind HTTPS; make sure your proxy forwards `X-Forwarded-For` and that `RateLimitFilter` reads it instead of `getRemoteAddr()`
- [ ] Lock down CORS in `SecurityConfig` to your actual frontend origin(s)
- [ ] Tune token expiry to your risk tolerance — 15 min access tokens is a reasonable default, not a rule
- [ ] Add `spring-boot-starter-actuator` if you want metrics on failed logins / rate-limit hits

---

## 🐛 Troubleshooting

| Symptom | Likely cause |
|---|---|
| App won't boot, JPA errors on startup | Check `spring.datasource.*` credentials and that the DB actually exists |
| `429` immediately on first login attempt | You already burned the bucket testing — wait out `refill-duration-seconds`, or lower nothing and just wait |
| `401` on every request to a protected route | Missing or malformed `Authorization: Bearer <token>` header — check for typos, not extra whitespace |
| `401` right after a refresh call succeeded | You're still sending the *old* access token — use the new one from the refresh response |

---

## 📄 License

MIT — use, modify, and ship it in personal or commercial projects without asking.