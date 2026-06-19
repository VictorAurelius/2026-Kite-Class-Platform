# GAP-1294: Comment sai "deprecated @MockitoBean" trong test slices kc-core

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Backend (test doc accuracy)
**Found:** 2026-06-14 (trong khi đóng GAP-1278)
**Affects:** `kiteclass/kiteclass-core/src/test/.../course/controller/CourseControllerTest.java`, `.../student/controller/StudentControllerTest.java` (và có thể slices khác)

## Problem

`CourseControllerTest` + `StudentControllerTest` có comment "Uses @TestConfiguration
to provide mock beans instead of **deprecated @MockitoBean**" / "Replaces deprecated
@MockitoBean annotation". Điều này SAI: annotation bị deprecate là `@MockBean`
(`org.springframework.boot.test.mock.mockito.MockBean`), KHÔNG phải `@MockitoBean`
(`org.springframework.test.context.bean.override.mockito.MockitoBean`) — `@MockitoBean`
chính là bản thay thế non-deprecated (Spring Framework 6.2 / Spring Boot 3.4+).

Comment sai propagate một convention nhầm: né `@MockitoBean` (bản đúng) để dùng
`@TestConfiguration @Bean Mockito.mock(...)`. Convention `@Bean`-mock đó lại đúng là
nguyên nhân class bug GAP-1278 (mock đi qua `populateBean` → persistence
post-processor) — nên comment sai không chỉ về câu chữ mà còn đẩy người đọc về
hướng dễ tái phát bug.

## Proposed Fix

Sửa comment cho đúng (`@MockBean` mới là deprecated). KHÔNG bắt buộc đổi sang
`@MockitoBean` ở các slice không có vấn đề persistence (giữ `@Bean`-mock OK cho bean
không có `@PersistenceContext`); chỉ cần xoá phát biểu sai "deprecated @MockitoBean".

## Acceptance Criteria

- [ ] Comment "deprecated @MockitoBean" được sửa/loại trong các test slice liên quan
- [ ] (tuỳ chọn) Ghi chú ngắn khi nào dùng `@MockitoBean` vs `@Bean`-mock (bean có `@PersistenceContext` inherited → `@MockitoBean`)

## Related

- Discovered in: GAP-1278 closure
- GAP-1278 — `@MockitoBean` là fix đúng cho mock `AuthorizationBean` trong `@WebMvcTest`
