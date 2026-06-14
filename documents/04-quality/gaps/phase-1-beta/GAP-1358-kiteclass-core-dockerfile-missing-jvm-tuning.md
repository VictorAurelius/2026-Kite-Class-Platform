# GAP-1358: kiteclass-core/gateway Dockerfile thiếu JVM container tuning

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps
**Found:** 2026-06-14 (Performance full audit post wave-p0-closeout-1, sub-check 5.4)
**Affects:** `kiteclass/kiteclass-core/Dockerfile`, kiteclass-gateway (không có Dockerfile riêng)

## Problem

`kiteclass/kiteclass-core/Dockerfile:71` dùng `ENTRYPOINT ["java","-jar","app.jar"]` — KHÔNG có `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`. Cả 6 service kitehub (gateway/subscription/email/branding/admin + base) đều set `ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"`.

kiteclass-core là service Java LỚN nhất + memory-intensive nhất (toàn bộ nghiệp vụ giáo dục). Java 17+ bật `UseContainerSupport` mặc định (cgroup limit được tôn trọng) nhưng `MaxRAMPercentage` default chỉ 25% → heap under-provisioned (chỉ dùng 1/4 RAM container), lệch hẳn fleet 75%. Dưới tải, heap nhỏ → GC pressure cao / OOM risk sớm hơn cần thiết.

## Proposed Fix

Thêm `ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"` + `ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]` vào kiteclass-core Dockerfile (mirror kitehub pattern). Xác nhận kiteclass-gateway build path cũng được tune.

## Acceptance Criteria

- [ ] kiteclass-core Dockerfile có `MaxRAMPercentage=75.0` + `UseContainerSupport`
- [ ] kiteclass-gateway container heap tuned tương tự
- [ ] grep JVM opts: cả 8 service Java đồng nhất

## Related

- Discovered in: 2026-06-14 performance audit (F-006)
- GAP-408 (PARTIAL) — JVM heap dev-profile (scope khác: dev vs prod Dockerfile)
