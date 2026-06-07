# Điều tra cross-flow: 5 cross-tenant IDOR gap (G3 production-parity P2 cluster B)

**Ngày:** 2026-06-07
**Phạm vi:** GAP-1015, GAP-1019, GAP-1023, GAP-1025, GAP-1035 (recurrence ≥4 — nghi systemic class)
**Loại:** READ-ONLY investigation — không sửa code
**Người điều tra:** Investigation agent (G3 production-parity)

---

## 0. Phát hiện quan trọng đầu tiên: 5/5 gap đã DONE

Cả 5 gap đều `🟢 DONE`, đóng tại **Wave security-2 Bucket B + C (2026-06-06)** — đã nằm trong `documents/04-quality/gaps/phase-1-beta/closed/`. Đây KHÔNG phải 5 gap còn open chờ fix. Vì vậy nhiệm vụ G3 thực chất là **verify fix đã ship có đúng shared-class + canonical fix + không còn residual IDOR không** (production-parity gate), chứ không phải plan fix from scratch.

Investigation đọc code thực tế (controller + guard + gateway filter) và phát hiện **1 residual cross-tenant IDOR chưa đóng** (InstanceController mutation/enumeration endpoints) — xem §3 + §4.

---

## 1. Bảng per-gap

| Gap | Controller : endpoint | Root cause cụ thể | Service / file | Severity | Trạng thái fix |
|-----|----------------------|-------------------|----------------|----------|----------------|
| **GAP-1015** | `SubscriptionController` `/api/platform/subscriptions/**` (GET, create, upgrade, downgrade, cancel, renew) | Role-gate `@PreAuthorize` chỉ check role, KHÔNG bind `subscription.instanceId` (hoặc `request.instanceId` cho create) với tenant của caller → OWNER bất kỳ thao tác sub tenant khác bằng cách đoán UUID | `kitehub-subscription` `controller/SubscriptionController.java` + `service/SubscriptionService` + `SubscriptionRenewalService` | 🔴 P0 | ✅ Fixed — `TenantOwnershipGuard.requireOwnership(...)` mọi endpoint; `/expiring` đổi sang admin-only |
| **GAP-1019** | `BrandingJobController` (5 endpoint) + `AIBrandingController` (4 endpoint) `/api/platform/branding/**` | Tenant scope xác định qua `X-Instance-Id` do **client tự gửi**; gateway không strip/validate header này → OWNER gửi instance-id tenant khác | `kitehub-branding` `controller/BrandingJobController.java` + `controller/AIBrandingController.java` | 🔴 P0 | ✅ Fixed — `TenantOwnershipGuard.requireInstanceOwnership` (job, header bắt buộc) + `requireInstanceOwnershipIfPresent` (AI, header optional) bind `X-Instance-Id` vs trusted `X-Tenant-Id` |
| **GAP-1023** | `DomainController` `/api/instances/{id}/domain` (initiate/verify/delete/get) | Ban đầu **ZERO `@PreAuthorize`** + path `{id}` (instanceId) không bind ownership → bất kỳ user thao tác/xóa domain instance khác (destructive DELETE) | `kitehub-subscription` `controller/DomainController.java` + `service/DomainService` | 🔴 P0 | ✅ Fixed — `@PreAuthorize(OWNER_AUTHZ)` + `TenantOwnershipGuard.requireOwnership(id, X-Tenant-Id)` cả 4 endpoint |
| **GAP-1025** | `InstanceController` `/api/platform/instances` | **ZERO `@PreAuthorize`** + gateway route chỉ `authenticated()` → user thường list-all instances + soft-delete/purge/extend-trial bất kỳ instance | `kitehub-subscription` `controller/InstanceController.java` + `InstanceService`/`InstancePurgeService`/`TrialService` | 🔴 P0 | ⚠️ Fixed MỘT PHẦN — chỉ gate `listInstances`/`listInstancesByCursor`/`deleteInstance`/`purgeInstance`/`extendTrial` (admin-only). **Còn residual** — xem §3 |
| **GAP-1035** | `BrandingController` `/api/v1/settings/branding` (PUT, /logo, /favicon) | **ZERO `@PreAuthorize`** → STAFF/TEACHER (non-OWNER) ghi đè branding own-tenant (defacement) | `kiteclass-core` `module/settings/controller/BrandingController.java` + `BrandingService` | 🟠 P1 | ✅ Fixed — `@PreAuthorize("hasAnyRole('ADMIN','OWNER')")` PUT + logo + favicon |

---

## 2. Shared-class verdict

### 2.1 KHÔNG phải tất cả cùng 1 class — chia 2 nhóm

**Nhóm A — Cross-tenant IDOR thực sự (horizontal, OWASP A01)** — 4 gap: GAP-1015, GAP-1019, GAP-1023, + phần residual GAP-1025.

Pattern chung duy nhất:
> Platform route (`/api/platform/**`, `/api/instances/{id}/**`) nhận resource id (path variable / body field / client header `X-Instance-Id`) nhưng **không verify id đó thuộc tenant của caller**. Role-gate `@PreAuthorize` (nếu có) chỉ chặn vertical (sai role), KHÔNG chặn horizontal (đúng role nhưng sai tenant).

Tất cả 4 đều dựa trên cùng 1 nền: gateway `TenantHeaderGuardFilter` (GAP-814) inject trusted `X-Tenant-Id` từ JWT `tenantId` claim trên mọi non-public route + global `RemoveRequestHeader=X-Tenant-Id` strip client value trước. Đã verify filter này tồn tại + đúng (`kitehub-gateway/.../filter/TenantHeaderGuardFilter.java`, Order `LOWEST_PRECEDENCE-1`). Đây là điều kiện tiên quyết khiến canonical fix khả thi.

**Nhóm B — Vertical privilege escalation (missing role gate, A01 khác sub-class)** — 1 gap: GAP-1035.

`kiteclass-core` BrandingController nằm trên subdomain route, tenant isolation đã do `TenantContext` + RLS (BaseEntity tenant filter) lo — `brandingService.updateBranding()` ghi vào tenant hiện tại từ context, KHÔNG nhận id từ client. Vì vậy đây KHÔNG phải cross-tenant IDOR mà là **missing role gate trong cùng tenant** (STAFF được quyền của OWNER). Fix đúng = chỉ thêm `@PreAuthorize`, KHÔNG cần ownership guard. Sister đúng pattern: `BrandingVersionController` đã có gate.

### 2.2 Pattern chung Nhóm A = "load/act by client-supplied id without tenant predicate"

| Biến thể | Gap | Cơ chế bind |
|----------|-----|-------------|
| id = path var `{id}` = instanceId trực tiếp | GAP-1023 domain, GAP-1015 `/instance/{id}/active` | `requireOwnership(pathId, tenantHeader)` |
| id = resource id → phải load resource lấy instanceId | GAP-1015 subscription `/{id}` (pre-load `getSubscription` rồi guard TRƯỚC mutation) | `requireOwnership(resource.getInstanceId(), tenantHeader)` |
| id = body field | GAP-1015 create (`request.getInstanceId()`) | `requireOwnership(request.getInstanceId(), tenantHeader)` |
| id = client header `X-Instance-Id` | GAP-1019 branding | `requireInstanceOwnership(headerInstanceId, tenantHeader)` |

---

## 3. RESIDUAL cross-tenant IDOR chưa đóng (G3 finding mới — NÊN file gap)

`InstanceController` (`kitehub-subscription`) GAP-1025 chỉ gate các endpoint admin/destructive. Còn các endpoint sau **vẫn ZERO `@PreAuthorize` + ZERO ownership binding** (confirmed đọc file `controller/InstanceController.java`):

| Endpoint | Dòng | Loại | Rủi ro residual |
|----------|------|------|-----------------|
| `PUT /api/platform/instances/{id}` (`updateInstancePut`) | 176-183 | **MUTATION** | Bất kỳ authenticated user **sửa field instance bất kỳ** cross-tenant — IDOR write chưa đóng |
| `PATCH /api/platform/instances/{id}` (`updateInstance`) | 185-192 | **MUTATION** | Như trên |
| `GET /api/platform/instances/owner/{ownerId}` (`getInstancesByOwner`) | 163-167 | READ | Enumerate instance của owner bất kỳ bằng đoán ownerId |
| `GET /api/platform/instances/{id}` (`getInstanceById`) | 139-143 | READ | Đọc metadata instance bất kỳ |
| `GET /api/platform/instances/subdomain/{subdomain}` | 151-155 | READ | Đọc theo subdomain |
| `GET /api/platform/instances/{id}/trial-status` | 213-217 | READ | Đọc trial status instance bất kỳ |
| `POST /api/platform/instances` + `/register` | 114-131 | PROVISION | Self-service, không cần ownership (đúng) |

Closure note GAP-1025 nói rõ: *"Ungated reads (getInstanceById/getInstanceBySubdomain) + provisioning (create/register) left as-is (single-instance ownership binding = Bucket B IDOR scope)"* — tức là CỐ Ý để lại. NHƯNG note này KHÔNG đề cập 2 endpoint **MUTATION** `PUT`/`PATCH /{id}` và `GET /owner/{ownerId}` enumeration. Đây là khoảng hở thật:

- `PUT`/`PATCH /{id}` là **write cross-tenant** — nghiêm trọng hơn ungated read, đáng lẽ phải bind ownership (như subscription) hoặc gate admin-only.
- `GET /owner/{ownerId}` là enumeration cross-owner — cùng class với list-all đã bị gate, nhưng theo ownerId thay vì list-all.

→ **Khuyến nghị: file 1 gap P1 mới** (vd `GAP-NNN-instance-controller-residual-idor`) cho cluster này. Đây chính là biểu hiện "recurrence ≥4 systemic" — guard đã áp cho 4 controller nhưng InstanceController còn sót các endpoint non-admin.

---

## 4. Fix recommendation

### 4.1 Canonical fix pattern — per gap (đã ship, verify-only ở G3)

| Gap | Canonical pattern (đã áp) | G3 verify cần làm |
|-----|---------------------------|-------------------|
| GAP-1015 | `requireOwnership(instanceId, X-Tenant-Id)`; mutation pre-load resource để guard TRƯỚC khi mutate | Walk OWNER A vs sub OWNER B → 403; admin → 200; create cross-instance → 403 |
| GAP-1019 | `requireInstanceOwnership` (header bắt buộc) / `IfPresent` (optional) bind `X-Instance-Id` vs `X-Tenant-Id` | Walk forge X-Instance-Id → 403; own → 201; admin bypass |
| GAP-1023 | `requireOwnership(pathId, X-Tenant-Id)` (path id = instanceId) | Walk OWNER A xóa domain B → 403 (verify never delete) |
| GAP-1025 | `@PreAuthorize` admin-only list/delete/purge/extend | Walk owner list-all → 403; **+ kiểm PUT/PATCH residual (§3)** |
| GAP-1035 | `@PreAuthorize("hasAnyRole('ADMIN','OWNER')")` (RLS lo tenant scope) | Walk STAFF PUT branding → 403; OWNER → 200 |

### 4.2 Có META systemic fix không?

**Một phần — KHÔNG hoàn toàn shared được.**

1. **`TenantOwnershipGuard` đang BỊ DUPLICATE 2 bản** — `kitehub-subscription/.../security/TenantOwnershipGuard.java` và `kitehub-branding/.../security/TenantOwnershipGuard.java`. Logic gần như y hệt (`isPlatformAdmin()` + so trusted header). Vì là 2 Maven module riêng nên đây là copy-paste, KHÔNG phải shared class thực. Đây là code-duplication tech-debt — không thể gộp thành 1 class trừ khi tạo shared library module (vượt scope P2). Chấp nhận được vì logic nhỏ + ổn định, nhưng nên ghi nhận: bất kỳ thay đổi guard logic phải sync cả 2 bản (`cross-flow-bug-class-sweep.md` áp dụng).

2. **Aspect/filter systemic KHÔNG khả thi cho cluster này** vì điểm bind khác nhau từng endpoint (path var vs body field vs client header vs phải pre-load resource). Một `@PreAuthorize` SpEL chung hay 1 aspect không cover được trường hợp "load subscription để lấy instanceId rồi mới so" (GAP-1015 mutation). Đành phải per-controller — đó là lý do guard là static helper gọi tường minh mỗi endpoint, không phải interceptor tự động.

3. **kiteclass-core (GAP-1035) KHÔNG dùng guard** — tenant scope đã do RLS/TenantContext lo, chỉ cần role gate. Không hợp nhất vào cùng cơ chế với nhóm A.

**Kết luận META:** systemic ý nghĩa nhất = (a) đóng residual InstanceController §3, (b) thêm reviewer-rule "platform route nhận id từ client phải bind ownership", (c) sync-discipline cho 2 bản guard. KHÔNG có 1 aspect đóng tất cả.

### 4.3 Fix decomposition cho P2 fix wave (nếu mở wave verify + residual)

Group theo service/file để worktree-parallel KHÔNG conflict:

| Agent | Service / file (disjoint) | Scope | Test | G3 walk-verify |
|-------|---------------------------|-------|------|----------------|
| **Agent 1** | `kitehub-subscription` `InstanceController.java` (+ `InstanceService` updateInstance) | **Đóng residual §3**: bind `requireOwnership` cho PUT/PATCH `/{id}` + `GET /owner/{ownerId}` (hoặc gate admin tùy ngữ nghĩa); cân nhắc gate read `getInstanceById` | Thêm nested test trong `InstanceApiContractTest` (owner update cross-instance → 403; admin → 200) | Walk owner PUT instance B → 403 |
| **Agent 2** | `kitehub-subscription` `SubscriptionController` + `DomainController` (cùng module, guard chung) | **Verify-only** GAP-1015 + GAP-1023 (re-walk, không sửa) | Chạy lại `SubscriptionTenantOwnershipTest` (đã cover GAP-1015 + GAP-1023 nested) | Walk sub + domain cross-tenant → 403 |
| **Agent 3** | `kitehub-branding` `BrandingJobController` + `AIBrandingController` + `TenantOwnershipGuard` (branding bản) | **Verify-only** GAP-1019 + đánh giá `requireInstanceOwnershipIfPresent` omit-header edge | `BrandingTenantOwnershipTest` + `TenantOwnershipGuardTest` | Walk forge X-Instance-Id → 403 |
| **Agent 4** | `kiteclass-core` `module/settings/controller/BrandingController.java` | **Verify-only** GAP-1035 (nhóm B, khác class) | `BrandingControllerTest` (lưu ý dùng TestSecurityConfig permit-all → cần live re-walk cho 403) | Walk STAFF PUT branding → 403 |

**Lý do disjoint:** Agent 1+2 cùng đụng `kitehub-subscription` nhưng **file controller khác nhau** (InstanceController vs Subscription/Domain) → nếu Agent 1 sửa InstanceController + Agent 2 chỉ verify (không sửa) thì không conflict; nếu cả 2 đều sửa cùng module nên gộp 1 agent để tránh merge contention trên cùng `pom.xml`/`SubscriptionTenantOwnershipTest`. Agent 3 (branding) + Agent 4 (kiteclass-core) hoàn toàn tách module → song song an toàn.

**Đề xuất tối thiểu cho P2 wave:** chỉ Agent 1 cần sửa code (residual §3); 3 agent còn lại là G3 production-parity re-walk verify (theo `pre-handoff-self-test-completeness.md` §3 post-fix re-walk). Nếu coordinator chỉ muốn verify (không fix residual ngay) → tách §3 thành gap riêng cho wave sau.

---

## 5. Tóm tắt cho coordinator

1. **5/5 gap đã DONE** (Wave security-2 Bucket B+C, 2026-06-06) — không phải open. Nhiệm vụ thực = G3 verify + tìm residual.
2. **Shared-class: 4/5 cùng class** (cross-tenant IDOR horizontal — GAP-1015/1019/1023/1025). **GAP-1035 khác class** (vertical privilege escalation, kiteclass-core dùng RLS không phải guard).
3. **Canonical fix = `TenantOwnershipGuard` static helper** gọi tường minh mỗi endpoint (KHÔNG aspect tự động được — điểm bind khác nhau). Guard bị **duplicate 2 bản** (subscription + branding, 2 module riêng) — tech-debt sync, không gộp được trong P2 scope.
4. **Nền tảng vững:** gateway `TenantHeaderGuardFilter` (GAP-814) inject trusted `X-Tenant-Id` + strip client value — đã verify đúng.
5. **🔴 RESIDUAL CHƯA ĐÓNG (G3 finding):** `InstanceController` `PUT/PATCH /{id}` (mutation cross-tenant write) + `GET /owner/{ownerId}` (enumeration) vẫn ZERO authz + ZERO ownership binding. GAP-1025 closure cố ý bỏ qua reads/provisioning nhưng KHÔNG đề cập 2 mutation endpoint này → **nên file gap P1 mới**.
6. **Decomposition:** 4 agent disjoint theo module; chỉ Agent 1 (InstanceController residual) cần sửa code, 3 agent còn lại re-walk verify.
