# GAP-1358: kiteclass-core/gateway Dockerfile thiếu JVM container tuning

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** DevOps
**Found:** 2026-06-14 (Performance full audit post wave-p0-closeout-1, sub-check 5.4)
**Resolved:** 2026-06-15 (branch `fix/audit-fixF-devops-2026-06-14`)
**Affects:** `kiteclass/kiteclass-core/Dockerfile`, kiteclass-gateway (không có Dockerfile riêng)

## Problem

`kiteclass/kiteclass-core/Dockerfile:71` dùng `ENTRYPOINT ["java","-jar","app.jar"]` — KHÔNG có `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`. Cả 6 service kitehub (gateway/subscription/email/branding/admin + base) đều set `ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"`.

kiteclass-core là service Java LỚN nhất + memory-intensive nhất (toàn bộ nghiệp vụ giáo dục). Java 17+ bật `UseContainerSupport` mặc định (cgroup limit được tôn trọng) nhưng `MaxRAMPercentage` default chỉ 25% → heap under-provisioned (chỉ dùng 1/4 RAM container), lệch hẳn fleet 75%. Dưới tải, heap nhỏ → GC pressure cao / OOM risk sớm hơn cần thiết.

## Proposed Fix

Thêm `ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"` + `ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]` vào kiteclass-core Dockerfile (mirror kitehub pattern). Xác nhận kiteclass-gateway build path cũng được tune.

## Acceptance Criteria

- [x] kiteclass-core Dockerfile có `MaxRAMPercentage=75.0` + `UseContainerSupport` — `ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"` + `ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]` (mirror kitehub fleet).
- [x] kiteclass-gateway container heap tuned tương tự — **N/A**: `kiteclass/kiteclass-gateway/` chứa duy nhất Eclipse `.settings/` (không có pom.xml / source / Dockerfile) — không phải JVM container. Production KiteClass routing = nginx (`kiteclass/nginx/nginx.conf`) + shared `kite-gateway` (kitehub-gateway, đã tune). Documented N/A.
- [x] grep JVM opts: cả 8 service Java đồng nhất — 6/6 Java services (5 kitehub + kiteclass-core) đều có `MaxRAMPercentage=75.0` (verified `grep -l`). "8 service" trong AC gốc đếm cả non-JVM (gateway nginx) — chỉ 6 service là JVM thật, tất cả đồng nhất.

## Resolution (2026-06-15)

Added container-aware JVM tuning to `kiteclass/kiteclass-core/Dockerfile`, mirroring the 5 kitehub Java services: `ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"` + `ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]` (was bare `java -jar app.jar` → default 25% heap cap). `kiteclass-gateway` is a Java-less stub (only `.settings/`); production KiteClass ingress is nginx + the shared `kite-gateway` (already tuned) — no separate JVM container to tune (documented N/A).

## Related

- Discovered in: 2026-06-14 performance audit (F-006)
- GAP-408 (PARTIAL) — JVM heap dev-profile (scope khác: dev vs prod Dockerfile)
