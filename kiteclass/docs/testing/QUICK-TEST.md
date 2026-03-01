# Quick Test Guide - Attendance Frontend

## ⚠️ Môi Trường Hiện Tại

**Status:**
- ✅ PostgreSQL: Running (Docker)
- ✅ Redis: Running (Docker)
- ⏳ Backend: Starting... (đang chờ)
- ⏳ Frontend: Chưa start

---

## Bước 1: Đợi Backend Ready

```bash
# Check backend health (đợi đến khi thấy "UP")
curl http://localhost:8081/actuator/health

# Expected: {"status":"UP"}
```

**Nếu backend không start được**, đọc lỗi trong `/tmp/backend.log`:
```bash
tail -100 /tmp/backend.log
```

---

## Bước 2: Tạo Test Data

### 2.1. Tạo Student
```bash
curl -X POST http://localhost:8081/api/v1/students \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "name": "Nguyễn Văn A",
    "email": "nguyenvana@test.com",
    "phone": "0901234567"
  }'
```

### 2.2. Tạo Course
```bash
curl -X POST http://localhost:8081/api/v1/courses \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "name": "Toán 10",
    "subject": "MATH",
    "level": "BEGINNER",
    "duration": 60,
    "price": 1000000
  }'
```

### 2.3. Tạo Class
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

### 2.4. Enroll Student (QUAN TRỌNG!)
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

### 2.5. Kích hoạt Enrollment
```bash
curl -X PUT http://localhost:8081/api/v1/enrollments/1/status \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{"status": "ACTIVE"}'
```

---

## Bước 3: Test Attendance API

### 3.1. Mark Single Attendance
```bash
curl -X POST http://localhost:8081/api/v1/attendance \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "enrollmentId": 1,
    "sessionId": 1,
    "status": "PRESENT"
  }' | jq '.'
```

**Expected:** Status 201, attendance record returned

### 3.2. Get Session Attendance
```bash
curl http://localhost:8081/api/v1/attendance/session/1 \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" | jq '.'
```

### 3.3. Get Student Stats
```bash
curl http://localhost:8081/api/v1/attendance/stats/student/1 \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" | jq '.'
```

---

## Bước 4: Start Frontend

```bash
# Terminal mới
cd kiteclass/kiteclass-frontend
npm run dev
```

**Open browser:** http://localhost:3000

---

## Bước 5: Test UI

### 5.1. Navigate to Attendance
1. Click "Điểm danh" trong sidebar
2. Sẽ thấy overview page

### 5.2. Take Attendance
1. Click "Điểm danh" cho lớp "Toán 10A"
2. URL sẽ là: `/classes/1/attendance`
3. Sẽ thấy danh sách học viên
4. Chọn status và save

---

## Troubleshooting

### Backend không start
```bash
# Xem log
tail -f /tmp/backend.log

# Restart
pkill -f spring-boot
cd kiteclass/kiteclass-core
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

### Frontend lỗi CORS
- Backend phải chạy trên port 8081
- Frontend phải chạy trên port 3000

### Không thấy data
- Chạy lại scripts tạo test data (Step 2)
- Check database:
```bash
docker exec -it kiteclass-postgres psql -U kiteclass -d kiteclass
\dt
SELECT * FROM students;
```

---

## Quick Commands

```bash
# Check all services
docker ps | grep kiteclass
curl http://localhost:8081/actuator/health
curl http://localhost:3000

# View logs
tail -f /tmp/backend.log
tail -f /tmp/frontend.log

# Restart everything
docker restart kiteclass-postgres kiteclass-redis
pkill -f spring-boot
pkill -f next-dev
```

---

**Status Dashboard:**
- PostgreSQL: http://localhost:5432
- Redis: http://localhost:6379
- Backend: http://localhost:8081/actuator/health
- Frontend: http://localhost:3000
- API Docs: http://localhost:8081/swagger-ui.html
