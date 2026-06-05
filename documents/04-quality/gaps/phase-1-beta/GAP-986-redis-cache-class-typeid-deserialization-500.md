# GAP-986: Redis cache deserialization 500 — getCourseById/getTeacherById thiếu `@class` type id

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend (cache config)
**Found:** 2026-06-05 (Wave security-1 G2 live re-walk — discovery)
**Affects:** `CourseServiceImpl.getCourseById` + `TeacherServiceImpl.getTeacherById` (`@Cacheable` value `courses`/`teachers`), Redis cache serializer config

## Problem

Sau rebuild kiteclass-core, GET courses/10 + teachers/10 trả **HTTP 500** cho MỌI tenant (kể cả owner): `SerializationException: Could not resolve subtype of [java.lang.Object]: missing type id property '@class'`. Cache entries cũ trong Redis được serialize bằng config Jackson KHÁC (thiếu default-typing `@class`) so với deserializer hiện tại → cache-config/serializer drift. Pre-existing (GAP-983 đã ghi "500 pre-existing user-U confound"). Workaround G2: flush key `courses*`/`teachers*`. Phụ phát hiện: key `courses::126eaa8c...:10` (khanh cache course 10 của sky) = **leak residue** cached trước khi fix GAP-983.

## Proposed Fix

Chuẩn hóa Redis cache serializer (GenericJackson2JsonRedisSerializer với activateDefaultTyping nhất quán) + cache version/namespace bump khi serializer đổi để tránh đọc entry stale; cân nhắc cache eviction on deploy.

## Acceptance Criteria
- [ ] getCourseById/getTeacherById không 500 sau redeploy (stale cache không crash)
- [ ] Serializer config nhất quán write↔read (round-trip IT)

## Related
- Discovered in: Wave security-1 G2 re-walk 2026-06-05
- Parent confound: [[GAP-983]]
