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

# Switch on Status type
grep -rln --include='*.java' -E 'switch\s*\(.*[sS]tatus' \
  kiteclass/*/src/main/java kitehub/*/src/main/java \
  | head -20
```

Threshold: file with ≥3 cascade lines OR ≥1 switch with ≥3 case branches.

False positives:
- Test fixtures that legitimately walk states (`given().status(SCHEDULED).when()...`)
- Single-shot guards (`if (status == ARCHIVED) return null;`) — not cascades

---

## 3. Primitive Obsession

```bash
# Public method signatures with primitive domain types
grep -rn --include='*.java' \
  -E 'public.*\b(String\s+(color|colour|hex|email|phone|address)|BigDecimal\s+(amount|price))\b' \
  kiteclass/*/src/main/java kitehub/*/src/main/java \
  | grep -v '/test/' \
  | head -30

# Entity fields (require value object instead)
grep -rn --include='*.java' \
  -E '@Column.*\n\s+private\s+String\s+(email|phone|color|hex)\b' \
  -A0 \
  kiteclass/*/src/main/java kitehub/*/src/main/java
```

Manual review:
- DTOs at REST boundary translating from JSON — accepted (annotate as boundary translation)
- JPA entity fields → flag, suggest converter / embedded value object

---

## 4. Leaky Abstraction (Vendor types in domain)

```bash
# Ollama types outside ollama adapter package
grep -rn --include='*.java' \
  -E '\b(Ollama(Request|Response|Context|Client))\b' \
  kiteclass/*/src/main/java kitehub/*/src/main/java \
  | grep -vE '/(adapter|client/external|integration/ollama)/' \
  | head -20

# OpenAI types outside openai adapter
grep -rn --include='*.java' \
  -E '\b(OpenAI|ChatCompletion(Request|Response))\b' \
  kiteclass/*/src/main/java kitehub/*/src/main/java \
  | grep -vE '/(adapter|client/external|integration/openai)/' \
  | head -20
```

False positives:
- Adapter package itself (good — that's where vendor types belong)
- Test mocks using real vendor type to validate adapter (acceptable in `*AdapterTest.java`)

---

## 5. Direct Event Publish (No Outbox)

```bash
# RabbitMQ direct publish outside outbox package
grep -rn --include='*.java' \
  -E '\brabbitTemplate\.(send|convertAndSend)\b' \
  kiteclass/*/src/main/java kitehub/*/src/main/java \
  | grep -vE '/outbox/' \
  | head -20

# Spring Cloud Stream direct
grep -rn --include='*.java' \
  -E '\bstreamBridge\.send\b' \
  kiteclass/*/src/main/java kitehub/*/src/main/java \
  | grep -vE '/outbox/' \
  | head -20

# Kafka direct
grep -rn --include='*.java' \
  -E '\bkafkaTemplate\.send\b' \
  kiteclass/*/src/main/java kitehub/*/src/main/java \
  | grep -vE '/outbox/' \
  | head -20
```

False positives to skip:
- The outbox publisher itself (`OutboxEventPublisher.java` is the legitimate sender)
- In-process Spring `ApplicationEventPublisher` — not cross-service, OK
- Test fixtures (`*Test.java`)

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
