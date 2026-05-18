---
audience: dev
domain: seed
layer: 3-api-contract
last-updated: 2026-05-18
version: 1.0
---

# Seed Worker — Layer 3: API Contract

**Related rules:** [`rules.md`](./rules.md) BR-SEED-001..BR-SEED-010
**Related use cases:** [`use-cases.md`](./use-cases.md) UC-SEED-001..UC-SEED-003
**Wave/Gap:** GAP-658 Wave 98 Bucket B2

---

## Scope

Domain `seed/` KHÔNG có HTTP REST API endpoint — purely internal Java service consumed bởi seed worker + (future) onboarding checklist service. API contract dưới đây là **Java method contract** của `VietnamSampleDataGenerator` Spring component.

---

## Java API — `VietnamSampleDataGenerator`

**Package:** `com.kitehub.platform.seed`
**Stereotype:** `@Component` (Spring-managed singleton)
**Config dependency:** `seed.locale` (default `vi-VN`)

### Method: `generateStudent()`

```java
public SampleStudent generateStudent()
```

**Returns:** `SampleStudent(String fullName, String gender, String region)`
- `fullName`: Vietnamese full name (e.g., `Trần Thị Hồng`) khi `seed.locale=vi-VN`
- `gender`: `"F"` | `"M"`
- `region`: `"Bắc"` | `"Trung"` | `"Nam"`

**English fallback:** `SampleStudent("Student Sample", "U", "N/A")` khi `seed.locale=en-US`.
**Errors:** `IllegalStateException` nếu CSV resource chưa load (startup failure).

---

### Method: `generateTeacher()`

```java
public SampleTeacher generateTeacher()
```

**Returns:** `SampleTeacher(String fullName, String specialty)`
- `specialty`: enum-like string per BR-SEED-003 (`Anh ngữ` | `Toán` | `Lý` | `Hóa` | etc.)

---

### Method: `generateCenter()`

```java
public SampleCenter generateCenter()
```

**Returns:** `SampleCenter(String name, String shortName, String city)`

---

### Method: `generateClass()`

```java
public SampleClass generateClass()
```

**Returns:** `SampleClass(String name, int gradeLevel, String subject)`
- `gradeLevel`: 0-12 (0 = non-grade-bound như IELTS/TOEIC/N5)

---

### Method: `generateAddress()`

```java
public SampleAddress generateAddress()
```

**Returns:** `SampleAddress(String street, String district, String city)`

---

### Method: `generateSubject()`

```java
public SampleSubject generateSubject()
```

**Returns:** `SampleSubject(String name, String abbreviation)`
- `name`: Vietnamese (`Anh ngữ`)
- `abbreviation`: English code (`EN`)

---

### Method: `formatVND(BigDecimal amount)`

```java
public String formatVND(BigDecimal amount)
```

**Returns:** VND-formatted string per `Locale.forLanguageTag("vi-VN")` `NumberFormat.getCurrencyInstance`.
**Example:** `formatVND(BigDecimal.valueOf(1_500_000))` → `"1.500.000 ₫"` (Java 17+ default) hoặc `"1.500.000đ"` (older JDK).
**Null safety:** Returns `""` cho `null` input.

---

### Method: `formatVNDate(LocalDate date)`

```java
public String formatVNDate(LocalDate date)
```

**Returns:** Vietnamese date format `EEEE, dd/MM/yyyy` (e.g., `"Thứ Năm, 14/05/2026"`).
**Pattern:** `DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))`
**Null safety:** Returns `""` cho `null`.

---

### Method: `formatVNTime(LocalTime time)`

```java
public String formatVNTime(LocalTime time)
```

**Returns:** 24-hour `HH:mm` (e.g., `"09:30"`, `"14:00"`).
**Null safety:** Returns `""` cho `null`.

---

## Configuration

```yaml
# application.yml
seed:
  locale: vi-VN   # default; override 'en-US' for test fixtures
```

Spring property `seed.locale` consumed via `@Value("${seed.locale:vi-VN}")`.

---

## Thread safety

`VietnamSampleDataGenerator` là thread-safe:
- CSV data loaded once tại `@PostConstruct` vào immutable `List<String[]>`
- Random selection dùng `ThreadLocalRandom` (no contention)
- DTOs (`SampleStudent` etc.) là Java records (immutable)

---

## Error codes (Java exceptions)

| Exception | When |
|---|---|
| `IllegalStateException("Failed to load CSV resource: ...")` | CSV resource missing tại startup |
| `IllegalStateException("Sample data not loaded — CSV resource missing")` | `pickRandom()` called trên empty list (defensive) |

---

## Versioning

Major version aligned với platform module version (`kitehub-platform-1.0.0-SNAPSHOT`). Backward-compat policy:
- Adding new `generate*()` methods = non-breaking
- Changing existing return DTO shape = breaking (requires version bump)
- Changing CSV schema = breaking (requires migration of consumers)

---

## Log

- **2026-05-18 (v1.0):** API contract tạo Wave 98 Bucket B2 per GAP-658. Java method contract chỉ — domain không expose HTTP endpoint.
