# Role Hierarchy — API Contract

## POST /api/v1/roles
**Request:**
```json
{
  "name": "DEPT_HEAD",
  "description": "Head of academic department",
  "level": 4,
  "parentId": 3,
  "permissionIds": [1, 2, 5]
}
```
**Response 201:** Role

## GET /api/v1/roles/{id}/children
Returns direct children in hierarchy.

## POST /api/v1/users/{userId}/roles
**Request:** `{ "roleId": 5 }`
Assign role.

## DELETE /api/v1/users/{userId}/roles/{roleId}
Revoke role.

## GET /api/v1/users/{userId}/permissions
Returns user's effective permission names (union of all roles).

## GET /api/v1/users/{userId}/has-permission?name=USER_MANAGE
Returns `{ "granted": true }`.

## POST /api/v1/roles/{id}/permissions
Grant permission to role.
**Request:** `{ "permissionId": 12 }`

## Log
- 2026-04-14 — Initial API
