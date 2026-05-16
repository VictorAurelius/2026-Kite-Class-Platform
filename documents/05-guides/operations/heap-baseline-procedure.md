# Quy trình Heap Baseline cho JVM Services — Pre/Post Spring Boot Bump

**Cập nhật lần cuối:** 2026-05-16
**Sở hữu:** Ops team
**Áp dụng:** Mọi `kitehub-*` + `kiteclass-*` Java service (Spring Boot 3.5.x)
**Liên quan:** [`GAP-440`](../../04-quality/gaps/GAP-440-spring-boot-dep-bump-before-prod.md) · [`GAP-451`](../../04-quality/gaps/GAP-451-spring-boot-3-5-x-no-newer-patch-await-upstream.md) · Wave 86 Bucket B

---

## 1. Khi nào chạy

Runbook này PHẢI chạy trong các trường hợp sau:

| Trigger | Lý do |
|---|---|
| **Trước Spring Boot major bump** (vd 3.5.x → 3.6.x) | Major version có thể đổi GC defaults, classloader behavior, native lib bundling |
| **Trước Spring Boot minor bump** (vd 3.5.14 → 3.6.0) | Minor có thể bundle tomcat-embed / netty / jackson version mới — native memory impact |
| **Trước Spring Boot patch bump** (vd 3.5.14 → 3.5.16) | Patch hiếm khi đổi memory profile nhưng vẫn cần xác minh |
| **Sau khi áp dụng bump trên staging** | So sánh delta với baseline để phát hiện regression sớm |
| **Quarterly hygiene** | Phát hiện classloader leak / native lib growth do dependency creep |
| **Sau khi bật JVM tuning flag mới** | Vd `-XX:MaxRAMPercentage`, `-XX:+UseZGC` |

Wave 86 Bucket B (re-scope) context: Spring Boot 3.5.14 hiện là latest 3.5.x patch. Bump thực tế deferred đến `GAP-451` (chờ upstream 3.5.15+). Runbook này chuẩn bị sẵn để Ops chạy ngay khi 3.5.15+ ship.

---

## 2. Tool cần thiết

- **`jcmd`** (đi kèm JDK 17+) — chạy lệnh diagnostic vào JVM đang chạy
- **JVM flag `-XX:NativeMemoryTracking=summary`** — bật Native Memory Tracking (NMT). Bắt buộc đặt từ startup; KHÔNG enable runtime được
- **`jstat`** (optional) — đo GC stats theo thời gian
- **Container exec capability** — `docker exec -it kite-{service}` hoặc `kubectl exec -it pod/{service}` để chạy `jcmd` từ trong container

NMT overhead: 5-10% memory + 1% CPU. Bật trong production OK vì giá trị diagnostic vượt overhead.

---

## 3. Quy trình 7 bước

### Bước 1: Enable NMT trên service target

Edit `application-production.yml` hoặc Helm `values.yaml` cho service:

```yaml
# Helm values.yaml cho kitehub-subscription
extraEnv:
  - name: JAVA_TOOL_OPTIONS
    value: "-XX:NativeMemoryTracking=summary -XX:+UnlockDiagnosticVMOptions"
```

Hoặc Docker Compose:

```yaml
# docker-compose.kitehub.yml
services:
  kitehub-subscription:
    environment:
      JAVA_TOOL_OPTIONS: "-XX:NativeMemoryTracking=summary"
```

Restart service. Verify NMT enabled:

```bash
docker exec kitehub-subscription jcmd 1 VM.native_memory summary | head -5
# Kỳ vọng output: "Native Memory Tracking: ..."
# Nếu thấy "Native memory tracking is not enabled" → flag chưa được apply, kiểm tra lại
```

### Bước 2: Capture pre-bump baseline (TRƯỚC khi bump)

Lấy snapshot trạng thái JVM hiện tại (Spring Boot 3.5.14):

```bash
# Tìm Java PID trong container
docker exec kitehub-subscription jcmd
# Output: 1   /app/app.jar  (PID 1, đại diện cho Java process)

# Capture baseline
docker exec kitehub-subscription jcmd 1 VM.native_memory baseline
# Kỳ vọng: "Baseline succeeded"

# Lưu summary lúc baseline
docker exec kitehub-subscription jcmd 1 VM.native_memory summary \
  > /tmp/heap-baseline-pre-bump-$(date +%Y%m%d-%H%M).txt
```

### Bước 3: Chạy workload điển hình

Để baseline có ý nghĩa, JVM phải đã warmed up + chạy workload thực tế. Trong staging:

```bash
# Chạy smoke test workload điển hình
bash scripts/smoke-test.sh --service kitehub-subscription --duration 5m

# Hoặc trigger load test
bash scripts/load-test.sh --rps 50 --duration 10m
```

Tối thiểu 5-10 phút workload trước khi capture post-workload snapshot.

### Bước 4: Capture post-workload snapshot (vẫn chạy 3.5.14)

```bash
docker exec kitehub-subscription jcmd 1 VM.native_memory summary.diff \
  > /tmp/heap-post-workload-pre-bump-$(date +%Y%m%d-%H%M).txt
```

`summary.diff` show delta so với baseline (Bước 2). Đây là baseline "warmed up" của 3.5.14 — đại diện trạng thái production.

### Bước 5: Apply Spring Boot bump + redeploy

Per `release-deploy-standard.md` §3.3 (MINOR bump) hoặc §3.4 (MAJOR bump). Sau khi service mới ship trên staging:

```bash
# Verify service version đã bump
curl -s http://staging.kitehub.me/actuator/info | jq '.build.version'
# Kỳ vọng: phiên bản mới (vd 3.5.16)
```

### Bước 6: Capture post-bump snapshot (cùng workload Bước 3)

Chạy LẠI cùng workload Bước 3 trên version mới:

```bash
# Reset baseline trên service mới
docker exec kitehub-subscription jcmd 1 VM.native_memory baseline

# Chạy workload (same script, same duration)
bash scripts/smoke-test.sh --service kitehub-subscription --duration 5m

# Capture post-workload trên version mới
docker exec kitehub-subscription jcmd 1 VM.native_memory summary.diff \
  > /tmp/heap-post-workload-post-bump-$(date +%Y%m%d-%H%M).txt
```

### Bước 7: Diff pre-bump vs post-bump

So sánh 2 file `summary.diff` (Bước 4 + Bước 6):

```bash
diff /tmp/heap-post-workload-pre-bump-*.txt \
     /tmp/heap-post-workload-post-bump-*.txt
```

Tập trung vào các section sau:

| Section NMT | Ý nghĩa | Acceptable delta |
|---|---|---|
| **Java Heap** | Heap chính, GC quản lý | ±5% acceptable |
| **Class** | Classloader metadata | ±5% acceptable; >10% = classloader leak suspect |
| **Thread** | Stack memory của threads | ±5% acceptable; spike >20% = thread leak |
| **Code Cache** | JIT-compiled code | +0-15% normal sau bump (mới warm up) |
| **GC** | GC infrastructure | ±5% acceptable |
| **Compiler** | JIT compiler workspace | ±10% acceptable |
| **Internal** | Misc JVM internals | ±5% acceptable |
| **Native Memory Tracking** | NMT overhead | ±1MB tolerance |

---

## 4. Alert threshold + escalation

| Delta | Hành động |
|---|---|
| **Non-heap delta ≤ 10%** | ✅ PASS — bump không gây regression đáng kể. Document baseline mới cho lần sau |
| **Non-heap delta 10-20%** | ⚠️ INVESTIGATE — file follow-up gap. Khả năng cause: config drift, classloader leak, native lib version mới bundle thêm |
| **Non-heap delta > 20%** | 🚨 ROLLBACK candidate — không deploy production cho đến khi root cause xác định. Escalate tới tech lead |
| **Heap delta > 50%** | 🚨 ROLLBACK ngay — likely có memory leak hoặc default heap setting đổi. Compare GC log để confirm |

Lưu artifact (`/tmp/heap-*` files) vào `documents/04-quality/audits/heap-baseline/YYYY-MM-DD-<service>-bump.md` để có lịch sử trend.

---

## 5. Owner + cadence

| Owner | Trách nhiệm |
|---|---|
| **Ops team** | Chạy procedure sau mỗi production deploy. Lưu artifact vào audits folder |
| **Tech lead** | Review threshold breach (>10% delta). Approve/reject deploy continuation |
| **Dev team** | Provide workload script (`smoke-test.sh` per service) để Ops chạy được reproducible |

**Cadence khuyến nghị:**
- Pre/post mỗi Spring Boot patch bump (PATCH): MANDATORY
- Pre/post mỗi Spring Boot minor/major bump: MANDATORY
- Quarterly trên top 3 services theo traffic: hygiene check
- Sau mỗi JVM flag change: MANDATORY

---

## 6. Cross-link

- `release-deploy-standard.md` §3 — checklist deploy per-bump-type. Heap baseline là 1 evidence cho "deploy không gây regression"
- `pre-launch-infra-hardening-checklist.md` — JVM tuning Cat 5 ops readiness
- [`GAP-440`](../../04-quality/gaps/GAP-440-spring-boot-dep-bump-before-prod.md) — Spring Boot dep bump tracking
- [`GAP-451`](../../04-quality/gaps/GAP-451-spring-boot-3-5-x-no-newer-patch-await-upstream.md) — upstream wait cho 3.5.15+
- Wave 86 Bucket B — re-scope shipped test scaffold + heap procedure doc against 3.5.14 baseline
- Spring Boot bump retest checklist: `BulkImportAsyncBaselineTest` + `WebhookIdempotencyReplayBaselineTest` trong `kitehub-subscription/src/test/java/com/kitehub/subscription/baseline/`

---

## 7. Lịch sử thay đổi

- **2026-05-16:** Procedure tạo mới trong Wave 86 Bucket B (re-scope) — chuẩn bị sẵn cho GAP-451 Spring Boot upstream bump. Pin baseline 3.5.14. Owner Ops team.
