# Role Hierarchy — Use Cases

### UC-ROLE-01: Create Custom Role
- **Actor:** Tenant Owner / Admin
- **Steps:**
  1. Admin inputs: name, description, level (1-10), optional parent, permissions
  2. System: validate name unique, level range
  3. System: resolve parent (if any)
  4. System: create Role với permissions attached

### UC-ROLE-02: Assign Role to User
- **Actor:** Admin
- **Steps:**
  1. Admin selects user + role
  2. System: check if assignment exists (idempotent)
  3. System: create UserRole
- **Postcondition:** User có role, effective permissions = union of all role permissions

### UC-ROLE-03: Grant Permission to Role
- **Actor:** Tenant Owner
- **Steps:**
  1. Select role + permission from catalog
  2. System: add to role.permissions
- **Postcondition:** All users với role có permission mới

### UC-ROLE-04: Check User Permission (Runtime)
- **Actor:** System (authorization check)
- **Steps:**
  1. Request comes in với userId
  2. System: query union of permissions across user's roles
  3. System: check against required permission
- **Result:** allow/deny

### UC-ROLE-05: Build Org Chart (Future UI)
- **Actor:** Admin
- **Steps:**
  1. Query all roles + build tree via parent references
  2. Display hierarchical tree

## Log
- 2026-04-14 — Initial UCs
