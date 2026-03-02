# Manual Testing Guide - PR 3.8 Attendance Frontend

**Date:** 2026-03-01
**Module:** Attendance Management (Frontend + Backend Integration)

---

## Prerequisites

### 1. Required Services

✅ **PostgreSQL** - Database running on port 5432
✅ **Redis** - Cache running on port 6379 (optional)
✅ **Backend** - Spring Boot app on port 8081
✅ **Frontend** - Next.js app on port 3000

### 2. Check Services

```bash
# Check PostgreSQL
psql -h localhost -U postgres -d kiteclass -c "SELECT 1;"

# Check Redis (optional)
redis-cli ping

# Check ports
netstat -an | grep -E "5432|6379|8081|3000"
```

---

## Step 1: Start Backend

### Option A: Using Maven Wrapper (Recommended)

```bash
# Terminal 1: Start backend
cd kiteclass/kiteclass-core
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

**Expected output:**
```
...
Started KiteClassCoreApplication in X.XXX seconds
```

### Option B: Using Docker Compose

```bash
cd kiteclass
docker-compose -f docker-compose.dev.yml up kiteclass-core
```

### Verify Backend is Running

```bash
# Check health endpoint
curl http://localhost:8081/actuator/health

# Expected: {"status":"UP"}
```

---

## Step 2: Start Frontend

```bash
# Terminal 2: Start frontend
cd kiteclass/kiteclass-frontend
npm run dev
```

**Expected output:**
```
✓ Ready in X.Xs
○ Local:   http://localhost:3000
```

### Verify Frontend is Running

```bash
# Open browser to:
http://localhost:3000
```

---

## Step 3: Prepare Test Data

### 3.1. Create Test Student

```bash
curl -X POST http://localhost:8081/api/v1/students \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "name": "Nguyễn Văn A",
    "email": "nguyenvana@example.com",
    "phone": "0901234567",
    "dateOfBirth": "2005-01-15",
    "gender": "MALE"
  }'
```

### 3.2. Create Test Course

```bash
curl -X POST http://localhost:8081/api/v1/courses \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "name": "Toán Lớp 10",
    "description": "Khóa học Toán cơ bản",
    "subject": "MATH",
    "level": "BEGINNER",
    "duration": 60,
    "price": 1000000
  }'
```

### 3.3. Create Test Class

```bash
curl -X POST http://localhost:8081/api/v1/courses/1/classes \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "name": "Lớp Toán 10A",
    "maxStudents": 30,
    "locationType": "IN_PERSON",
    "startDate": "2026-03-10",
    "endDate": "2026-06-30"
  }'
```

### 3.4. Enroll Student

```bash
curl -X POST http://localhost:8081/api/v1/enrollments \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "studentId": 1,
    "classId": 1,
    "tuitionAmount": 1000000,
    "discountPercent": 0,
    "finalAmount": 1000000
  }'
```

### 3.5. Update Enrollment Status to ACTIVE

```bash
curl -X PUT http://localhost:8081/api/v1/enrollments/1/status \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "status": "ACTIVE"
  }'
```

---

## Step 4: Test Attendance API (Backend)

### 4.1. Mark Single Attendance

```bash
curl -X POST http://localhost:8081/api/v1/attendance \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "enrollmentId": 1,
    "sessionId": 1,
    "status": "PRESENT",
    "notes": "Đúng giờ"
  }'
```

**Expected Response (201 Created):**
```json
{
  "id": 1,
  "enrollmentId": 1,
  "studentName": "Nguyễn Văn A",
  "sessionId": 1,
  "status": "PRESENT",
  "pointsAwarded": 0,
  "notes": "Đúng giờ",
  ...
}
```

### 4.2. Mark Bulk Attendance

```bash
curl -X POST http://localhost:8081/api/v1/attendance/bulk \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "sessionId": 1,
    "records": [
      {"enrollmentId": 1, "status": "PRESENT"},
      {"enrollmentId": 2, "status": "ABSENT", "notes": "Ốm"},
      {"enrollmentId": 3, "status": "LATE"}
    ]
  }'
```

### 4.3. Get Session Attendance

```bash
curl http://localhost:8081/api/v1/attendance/session/1 \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000"
```

### 4.4. Get Student Statistics

```bash
curl http://localhost:8081/api/v1/attendance/stats/student/1 \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000"
```

**Expected Response:**
```json
{
  "targetId": 1,
  "targetType": "STUDENT",
  "totalSessions": 5,
  "presentCount": 4,
  "absentCount": 1,
  "lateCount": 0,
  "excusedCount": 0,
  "attendanceRate": 80.0
}
```

### 4.5. Update Attendance Status

```bash
curl -X PUT http://localhost:8081/api/v1/attendance/1 \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "status": "EXCUSED",
    "notes": "Có giấy phép"
  }'
```

---

## Step 5: Test Frontend UI

### 5.1. Login (if authentication enabled)

```
1. Navigate to: http://localhost:3000
2. Login with test credentials
```

### 5.2. Navigate to Attendance

```
1. Click "Điểm danh" in sidebar
2. Should see Attendance Overview page
```

**Expected:**
- 4 statistics cards
- Table of active classes
- "Điểm danh" button for each class

### 5.3. Take Attendance

```
1. Click "Điểm danh" button for a class
2. Should navigate to: /classes/1/attendance
```

**Expected:**
- List of students from active enrollments
- Status dropdown for each student (5 options)
- Notes textarea
- Statistics cards (total, present, absent, late, excused)
- "Đánh dấu tất cả có mặt" button
- "Lưu điểm danh" button

### 5.4. Mark Attendance

**Test Case 1: Mark All Present**
```
1. Click "Đánh dấu tất cả có mặt"
2. All students should show status "Có mặt" (green)
3. Statistics should update: Present = total students
4. Click "Lưu điểm danh"
5. Should see success toast
6. Should navigate back to /classes/1
```

**Test Case 2: Mixed Statuses**
```
1. Student 1: PRESENT
2. Student 2: ABSENT (add note: "Ốm")
3. Student 3: LATE (add note: "Kẹt xe")
4. Statistics should update in real-time
5. Click "Lưu điểm danh"
6. Should see success toast
7. Verify in backend: GET /api/v1/attendance/session/1
```

### 5.5. View Attendance History

```
1. Navigate to: /attendance
2. Should see overview with statistics
3. Click on a class
4. Should see attendance details
```

### 5.6. View Reports (Placeholder)

```
1. Navigate to: /attendance/reports
2. Should see "Coming Soon" message
```

---

## Step 6: Test Error Scenarios

### 6.1. Backend Error: Enrollment Not Found

```bash
curl -X POST http://localhost:8081/api/v1/attendance \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "enrollmentId": 99999,
    "sessionId": 1,
    "status": "PRESENT"
  }'
```

**Expected:** 404 Not Found

### 6.2. Backend Error: Duplicate Attendance

```bash
# Mark attendance twice
curl -X POST http://localhost:8081/api/v1/attendance \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "enrollmentId": 1,
    "sessionId": 1,
    "status": "PRESENT"
  }'

# Second time should fail
curl -X POST http://localhost:8081/api/v1/attendance \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "enrollmentId": 1,
    "sessionId": 1,
    "status": "PRESENT"
  }'
```

**Expected:** 400 Bad Request - "Attendance already marked"

### 6.3. Frontend Error: Network Failure

```
1. Stop backend server
2. Try to mark attendance in UI
3. Should see error toast with message
4. Should NOT navigate away
```

---

## Step 7: Automated Test Script

Run the automated test script:

```bash
cd /mnt/f/nam4/doan/2026-Kite-Class-Platform
./test-attendance-api.sh
```

This will test all attendance endpoints sequentially.

---

## Expected Results Checklist

### Backend API ✅
- [ ] Health check returns 200
- [ ] Mark single attendance returns 201
- [ ] Mark bulk attendance returns 201
- [ ] Get attendance by session returns 200
- [ ] Get student stats returns 200
- [ ] Update attendance returns 200
- [ ] Delete attendance returns 204
- [ ] Duplicate attendance returns 400
- [ ] Invalid enrollment returns 404

### Frontend UI ✅
- [ ] Sidebar shows "Điểm danh" menu
- [ ] Overview page displays statistics
- [ ] Classes table shows active classes
- [ ] Take attendance page displays student roster
- [ ] Status dropdowns have 5 options
- [ ] "Mark all present" button works
- [ ] Statistics update in real-time
- [ ] Save button triggers API call
- [ ] Success toast appears
- [ ] Navigate back to class detail
- [ ] Error toast appears on failure

### Integration ✅
- [ ] Frontend → Backend API calls work
- [ ] Data persists to database
- [ ] Points calculation correct
- [ ] Multi-tenant isolation works
- [ ] Cache invalidation works

---

## Troubleshooting

### Backend won't start

**Problem:** Port 8081 already in use
```bash
# Find process using port 8081
lsof -i :8081
# Kill process
kill -9 <PID>
```

**Problem:** Database connection error
```bash
# Check PostgreSQL is running
docker ps | grep postgres
# Or
service postgresql status
```

**Problem:** Migration errors
```bash
# Reset database (CAUTION: deletes all data)
psql -U postgres -d kiteclass -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
# Restart backend (migrations will run)
```

### Frontend won't start

**Problem:** Port 3000 already in use
```bash
# Find and kill process
lsof -i :3000
kill -9 <PID>
```

**Problem:** Module not found
```bash
cd kiteclass/kiteclass-frontend
npm install
```

### API returns 404

**Problem:** Endpoint not found
- Check backend logs for routing errors
- Verify controller is registered
- Check API base URL in frontend (should be http://localhost:8081)

### Frontend shows empty data

**Problem:** No test data
- Run test data creation scripts (Step 3)
- Verify data in database:
  ```sql
  SELECT * FROM students;
  SELECT * FROM classes;
  SELECT * FROM enrollments;
  ```

---

## Quick Start (All-in-One)

```bash
# Terminal 1: Start PostgreSQL (if using Docker)
docker run -d --name postgres -p 5432:5432 -e POSTGRES_PASSWORD=postgres postgres:15

# Terminal 2: Start Backend
cd kiteclass/kiteclass-core
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"

# Terminal 3: Start Frontend
cd kiteclass/kiteclass-frontend
npm run dev

# Terminal 4: Run tests
cd /mnt/f/nam4/doan/2026-Kite-Class-Platform
./test-attendance-api.sh

# Open browser
http://localhost:3000
```

---

## Screenshots Checklist

During testing, capture screenshots of:
- [ ] Attendance Overview page
- [ ] Take Attendance page (empty state)
- [ ] Take Attendance page (with data)
- [ ] Status dropdown with 5 options
- [ ] Statistics cards updating
- [ ] Success toast notification
- [ ] Error toast notification
- [ ] Reports placeholder page

---

**Testing completed by:** _________________
**Date:** _________________
**Results:** ✅ PASS / ❌ FAIL
**Notes:** _________________
