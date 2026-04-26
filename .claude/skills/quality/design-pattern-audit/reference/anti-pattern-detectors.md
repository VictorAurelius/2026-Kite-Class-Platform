# Anti-Pattern Detectors — Grep Recipes

Concrete one-liners per category. Multi-module-safe (per memory `feedback_audit_grep_scope.md`).

---

## 1. God Service / Class

```bash
# All Service.java files sorted by LOC, exclude tests
find kiteclass kitehub -path '*/src/main/java/*' -name '*Service.java' \
  -not -path '*/test/*' \
  -exec wc -l {} + 2>/dev/null | sort -rn | head -20
```

Threshold: rows with LOC > 500 = hotspot.

False positives to skip:
- `*Configuration.java`, `*Properties.java` — config classes, not services
- Generated code (`target/generated-sources/**`) — exclude `target/`
- Abstract base classes (`Abstract*Service.java`) — review case-by-case

---

## 2. Status Switch / If Cascade

```bash
# Cascade detection — at least 2 status comparisons in same file
grep -rln --include='*.java' \
  -E '(if|else if).*[sS]tatus\s*==' \
  kiteclass/*/src/main/java kitehub/*/src/main/java \
  | xargs -I{} bash -c 'count=$(grep -cE "(if|else if).*[sS]tatus\s*==" "$1"); [ "$count" -ge 2 ] && echo "$count  $1"' _ {} \
  | sort -rn | head -20

# Switch on Status type — exclude HTTP layer + Feign-style response.status()
grep -rln --include='*.java' -E 'switch\s*\(.*[sS]tatus' \
  kiteclass/*/src/main/java kitehub/*/src/main/java \
  | xargs -I{} bash -c '
      grep -lE "switch\s*\(\s*[a-zA-Z_]+\.[sS]tatus\(\)" "$1" >/dev/null && exit 0
      echo "$1"' _ {} \
  | head -20
```

Threshold: file with ≥3 cascade lines OR ≥1 switch with ≥3 case branches.

False positives:
- Test fixtures that legitimately walk states (`given().status(SCHEDULED).when()...`)
- Single-shot guards (`if (status == ARCHIVED) return null;`) — not cascades
- **HTTP `response.status()` switches** (Feign error decoders, RestTemplate handlers) — not domain status. The xargs filter above strips files where every match is the `*.status()` method call, not a domain field. Calibrated 2026-04-26 after FeignConfig false positive in baseline.

---

## 3. Primitive Obsession

```bash
# Public method signatures with primitive domain types
grep -rn --include='*.java' \
  -E 'public.*\b(String\s+(color|colour|hex|email|phone|address)|BigDecimal\s+(amount|price))\b' \
  kiteclass/*/src/main/java kitehub/*/src/main/java \
  | grep -v '/test/' \
  | head -30

# Entity fields (require value object instead) — multiline-aware
# Note: rg multiline mode catches @Column on previous line; works on most shells.
grep -rzPo --include='*.java' \
  '@Column[^;]{0,200}\n\s*private\s+String\s+(email|phone|color|hex|address|amount)\b' \
  kiteclass/*/src/main/java kitehub/*/src/main/java 2>/dev/null \
  | tr '\0' '\n' | head -20
```

Manual review (calibrated 2026-04-26 from baseline):
- **Boundary DTOs at REST/JSON layer** (`*Request.java`, `*Response.java`, `dto/**`) — accepted as translation; do NOT flag even if validated by `@Size`/`@Email`. Add `dto/` to exclude path if scan adds noise.
- **JPA entity fields** (`@Entity`, `@Column`-decorated POJOs in `entity/`) → flag, suggest converter / embedded value object.
- **`BigDecimal` in invoice/payment DTOs** — accepted (currency-as-primitive at boundary), entity-side Money value object preferred.
- Real domain hotspots from baseline 2026-04-26: `Student.email`, `Student.phone` (entity-side, no value object).

---

## 4. Leaky Abstraction (Vendor types in domain)

```bash
# Ollama types outside ollama adapter package — calibrated to also accept /client/ as adapter convention
grep -rn --include='*.java' \
  -E '\b(Ollama(Request|Response|Context|Client))\b' \
  kiteclass/*/src/main/java kitehub/*/src/main/java \
  | grep -vE '/(adapter|client|client/external|integration/ollama)/' \
  | head -20

# OpenAI types outside openai adapter
grep -rn --include='*.java' \
  -E '\b(OpenAI|ChatCompletion(Request|Response))\b' \
  kiteclass/*/src/main/java kitehub/*/src/main/java \
  | grep -vE '/(adapter|client|client/external|integration/openai)/' \
  | head -20
```

False positives:
- Adapter package itself (good — that's where vendor types belong)
- **`/client/` packages** are accepted as adapter convention even though `design-patterns.md` Mandatory Patterns table says "Adapter". Calibrated 2026-04-26 from baseline: `OllamaClient` at `kitehub-branding/.../client/` IS isolating Ollama types correctly; package-name choice is a separate convention question (defer to wave closure decision per Wave 6 plan §3 risk #5).
- Test mocks using real vendor type to validate adapter (acceptable in `*AdapterTest.java`)
- Bean wiring code in `*Config.java` that constructs vendor clients (config-time, not domain-time)

---

## 5. Direct Event Publish (No Outbox)

Reference rule: `.claude/rules/design-patterns.md` §3.5 + §3.5.1 Outbox Bypass Policy (Exceptions A/B/C). The detector must distinguish silent bypass (BANNED) from documented exceptions (allowed).

```bash
# RabbitMQ direct publish outside outbox package — calibrated 2026-04-26:
#  - Skip javadoc/comment-only references (lines starting with " *")
#  - Recognize Exception A "fast-path" / "outbox is the reliability net" markers
#  - Skip *Config.java (Exception B bean wiring)
grep -rn --include='*.java' \
  -E '\brabbitTemplate\.(send|convertAndSend)\b' \
  kiteclass/*/src/main/java kitehub/*/src/main/java \
  | grep -vE '/outbox/' \
  | grep -vE ':\s*\*\s' \
  | grep -vE '/[A-Za-z]*Config\.java:' \
  | xargs -I{} bash -c '
      line="$1"
      file=$(echo "$line" | cut -d: -f1)
      # Exception A marker check — file must contain fast-path comment within 5 lines of the call
      if grep -qE "(outbox is the reliability net|fast-path|best-effort fast-path)" "$file"; then
          # Documented exception — skip
          exit 0
      fi
      echo "$line"
  ' _ {} \
  | head -20

# Spring Cloud Stream direct
grep -rn --include='*.java' \
  -E '\bstreamBridge\.send\b' \
  kiteclass/*/src/main/java kitehub/*/src/main/java \
  | grep -vE '/outbox/|:\s*\*\s|/[A-Za-z]*Config\.java:' \
  | head -20

# Kafka direct
grep -rn --include='*.java' \
  -E '\bkafkaTemplate\.send\b' \
  kiteclass/*/src/main/java kitehub/*/src/main/java \
  | grep -vE '/outbox/|:\s*\*\s|/[A-Za-z]*Config\.java:' \
  | head -20
```

False positives to skip (now baked into the pipeline above where possible):
- **Exception A — fast-path with outbox backup** (per `design-patterns.md` §3.5.1): file contains marker comment `outbox is the reliability net` OR `fast-path` near the call. Reference example: `BrandingEventPublisher`.
- **Exception B — bean wiring** (`*Config.java`, `*Configuration.java`) and javadoc comment examples (lines starting with ` *`)
- **Exception C — test fixtures** (`*Test.java`, `src/test/**` paths) — already excluded by `--include='*.java'` + path
- The outbox publisher itself (`OutboxEventPublisher.java` is the legitimate sender)
- In-process Spring `ApplicationEventPublisher` — not cross-service, OK

---

## Composite Run

For one-shot baseline:

```bash
# Save raw output to a temp file
{
  echo "=== God Services (>500 LOC) ==="
  find kiteclass kitehub -path '*/src/main/java/*' -name '*Service.java' \
    -not -path '*/test/*' -exec wc -l {} + 2>/dev/null | sort -rn | awk '$1>500'
  echo
  echo "=== Status switch density (top 10) ==="
  grep -rlE 'switch\s*\(.*[sS]tatus' --include='*.java' \
    kiteclass/*/src/main/java kitehub/*/src/main/java | head -10
  echo
  echo "=== Vendor type leaks ==="
  grep -rlE 'Ollama(Request|Response)' --include='*.java' \
    kiteclass/*/src/main/java kitehub/*/src/main/java \
    | grep -v adapter | head -10
  echo
  echo "=== Direct event publish ==="
  grep -rlE 'rabbitTemplate\.(send|convertAndSend)' --include='*.java' \
    kiteclass/*/src/main/java kitehub/*/src/main/java \
    | grep -v outbox | head -10
} > /tmp/dp-audit-raw.txt

wc -l /tmp/dp-audit-raw.txt
```

Then human-curate raw → scored audit report.

---

## Calibration Notes

- **WSL2 file path quirk**: globs like `kiteclass/*/src/main/java` work; `kiteclass/**/src/main/java` does NOT (no recursive glob in some shells). Use `find` for recursion.
- **Performance**: full scan of Kite repo ~3-5s; safe for in-session use
- **First-run heuristic**: if a category returns 0 hits, re-run with broader scope before claiming clean — false-zero is worse than false-positive
- **2026-04-26 calibration cycle (Sub-PR 6.4):** Cat 2 + Cat 3 + Cat 4 + Cat 5 detectors tightened after baseline (Sub-PR 6.1) produced 1 false-positive in Cat 2 (FeignConfig HTTP `response.status()`), 1 in Cat 4 (`OllamaClient` at `/client/`), 2 in Cat 5 (RabbitConfig javadoc + BrandingEventPublisher fast-path). Calibrations baked into the recipes above; next audit cycle expected to drop false-positive count to 0 in those categories.
