# Skill: Enums & Constants

Định nghĩa tất cả Enums, Constants, và String values cho KiteClass Platform.

## Mô tả

Tài liệu định nghĩa chuẩn cho:
- Java Enums với mô tả và mapping
- TypeScript string literals và const objects
- Constants cho configuration
- Message templates
- Validation patterns

## Trigger phrases

- "enum definition"
- "constants"
- "status values"
- "định nghĩa enum"
- "string literals"

---

## User & Authentication Enums

### UserRole

```java
// Java
public enum UserRole {
    OWNER("Chủ trung tâm", "Full access to all features"),
    ADMIN("Quản trị viên", "Manage users, classes, billing"),
    TEACHER("Giáo viên", "Manage assigned classes, attendance"),
    STAFF("Nhân viên", "Limited access based on permissions"),
    PARENT("Phụ huynh", "View children's information");

    private final String displayNameVi;
    private final String description;

    // Constructor, getters...
}
```

```typescript
// TypeScript
export const UserRole = {
  OWNER: 'OWNER',
  ADMIN: 'ADMIN',
  TEACHER: 'TEACHER',
  STAFF: 'STAFF',
  PARENT: 'PARENT',
} as const;

export type UserRole = (typeof UserRole)[keyof typeof UserRole];

export const UserRoleLabels: Record<UserRole, string> = {
  OWNER: 'Chủ trung tâm',
  ADMIN: 'Quản trị viên',
  TEACHER: 'Giáo viên',
  STAFF: 'Nhân viên',
  PARENT: 'Phụ huynh',
};
```

### UserStatus

```java
public enum UserStatus {
    ACTIVE("Đang hoạt động"),
    INACTIVE("Không hoạt động"),
    PENDING("Chờ kích hoạt"),
    LOCKED("Đã khóa"),
    DELETED("Đã xóa");

    private final String displayName;
}
```

```typescript
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'PENDING' | 'LOCKED' | 'DELETED';
```

### AuthProvider

```java
public enum AuthProvider {
    LOCAL("Email/Password"),
    GOOGLE("Google OAuth"),
    ZALO("Zalo OAuth");

    private final String displayName;
}
```

---

## Student Enums

### StudentStatus

```java
public enum StudentStatus {
    ACTIVE("Đang học", "Currently enrolled in classes"),
    INACTIVE("Tạm nghỉ", "Temporarily not attending"),
    GRADUATED("Đã tốt nghiệp", "Completed all courses"),
    DROPPED("Đã nghỉ học", "Left the center"),
    PENDING("Chờ xác nhận", "Registration pending");

    private final String displayNameVi;
    private final String description;

    // Color mapping for UI
    public String getColorClass() {
        return switch (this) {
            case ACTIVE -> "bg-green-100 text-green-800";
            case INACTIVE -> "bg-yellow-100 text-yellow-800";
            case GRADUATED -> "bg-blue-100 text-blue-800";
            case DROPPED -> "bg-gray-100 text-gray-800";
            case PENDING -> "bg-orange-100 text-orange-800";
        };
    }
}
```

```typescript
export const StudentStatus = {
  ACTIVE: 'ACTIVE',
  INACTIVE: 'INACTIVE',
  GRADUATED: 'GRADUATED',
  DROPPED: 'DROPPED',
  PENDING: 'PENDING',
} as const;

export type StudentStatus = (typeof StudentStatus)[keyof typeof StudentStatus];

export const StudentStatusConfig: Record<StudentStatus, {
  label: string;
  color: string;
  bgColor: string;
}> = {
  ACTIVE: { label: 'Đang học', color: 'text-green-800', bgColor: 'bg-green-100' },
  INACTIVE: { label: 'Tạm nghỉ', color: 'text-yellow-800', bgColor: 'bg-yellow-100' },
  GRADUATED: { label: 'Đã tốt nghiệp', color: 'text-blue-800', bgColor: 'bg-blue-100' },
  DROPPED: { label: 'Đã nghỉ học', color: 'text-gray-800', bgColor: 'bg-gray-100' },
  PENDING: { label: 'Chờ xác nhận', color: 'text-orange-800', bgColor: 'bg-orange-100' },
};
```

### Gender

```java
public enum Gender {
    MALE("Nam"),
    FEMALE("Nữ"),
    OTHER("Khác");

    private final String displayName;
}
```

---

## Class Enums

### ClassStatus

```java
public enum ClassStatus {
    DRAFT("Nháp", "Class is being configured"),
    SCHEDULED("Đã lên lịch", "Class scheduled but not started"),
    IN_PROGRESS("Đang diễn ra", "Class currently active"),
    COMPLETED("Đã hoàn thành", "Class ended normally"),
    CANCELLED("Đã hủy", "Class cancelled");

    private final String displayNameVi;
    private final String description;
}
```

```typescript
export type ClassStatus = 'DRAFT' | 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export const ClassStatusConfig = {
  DRAFT: { label: 'Nháp', icon: 'FileEdit', color: 'gray' },
  SCHEDULED: { label: 'Đã lên lịch', icon: 'Calendar', color: 'blue' },
  IN_PROGRESS: { label: 'Đang diễn ra', icon: 'Play', color: 'green' },
  COMPLETED: { label: 'Đã hoàn thành', icon: 'CheckCircle', color: 'purple' },
  CANCELLED: { label: 'Đã hủy', icon: 'XCircle', color: 'red' },
} as const;
```

### DayOfWeek

```java
public enum DayOfWeek {
    MONDAY("Thứ Hai", "T2", 1),
    TUESDAY("Thứ Ba", "T3", 2),
    WEDNESDAY("Thứ Tư", "T4", 3),
    THURSDAY("Thứ Năm", "T5", 4),
    FRIDAY("Thứ Sáu", "T6", 5),
    SATURDAY("Thứ Bảy", "T7", 6),
    SUNDAY("Chủ Nhật", "CN", 7);

    private final String displayNameVi;
    private final String shortName;
    private final int order;
}
```

```typescript
export const DayOfWeek = {
  MONDAY: { value: 'MONDAY', label: 'Thứ Hai', short: 'T2', order: 1 },
  TUESDAY: { value: 'TUESDAY', label: 'Thứ Ba', short: 'T3', order: 2 },
  WEDNESDAY: { value: 'WEDNESDAY', label: 'Thứ Tư', short: 'T4', order: 3 },
  THURSDAY: { value: 'THURSDAY', label: 'Thứ Năm', short: 'T5', order: 4 },
  FRIDAY: { value: 'FRIDAY', label: 'Thứ Sáu', short: 'T6', order: 5 },
  SATURDAY: { value: 'SATURDAY', label: 'Thứ Bảy', short: 'T7', order: 6 },
  SUNDAY: { value: 'SUNDAY', label: 'Chủ Nhật', short: 'CN', order: 7 },
} as const;
```

### SessionStatus

```java
public enum SessionStatus {
    SCHEDULED("Đã lên lịch"),
    COMPLETED("Đã hoàn thành"),
    CANCELLED("Đã hủy"),
    MAKEUP("Học bù");

    private final String displayName;
}
```

---

## Attendance Enums

### AttendanceStatus

```java
public enum AttendanceStatus {
    PRESENT("Có mặt", "P", "bg-green-500"),
    ABSENT("Vắng", "V", "bg-red-500"),
    LATE("Đi trễ", "T", "bg-yellow-500"),
    EXCUSED("Có phép", "CP", "bg-blue-500"),
    MAKEUP("Học bù", "HB", "bg-purple-500");

    private final String displayNameVi;
    private final String shortCode;
    private final String colorClass;

    // Points deduction for gamification
    public int getPointsDeduction() {
        return switch (this) {
            case PRESENT -> 0;
            case LATE -> -5;
            case EXCUSED -> 0;
            case ABSENT -> -10;
            case MAKEUP -> 0;
        };
    }
}
```

```typescript
export type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'LATE' | 'EXCUSED' | 'MAKEUP';

export const AttendanceStatusConfig: Record<AttendanceStatus, {
  label: string;
  short: string;
  color: string;
  bgColor: string;
  points: number;
}> = {
  PRESENT: { label: 'Có mặt', short: 'P', color: 'white', bgColor: 'bg-green-500', points: 10 },
  ABSENT: { label: 'Vắng', short: 'V', color: 'white', bgColor: 'bg-red-500', points: -10 },
  LATE: { label: 'Đi trễ', short: 'T', color: 'white', bgColor: 'bg-yellow-500', points: -5 },
  EXCUSED: { label: 'Có phép', short: 'CP', color: 'white', bgColor: 'bg-blue-500', points: 0 },
  MAKEUP: { label: 'Học bù', short: 'HB', color: 'white', bgColor: 'bg-purple-500', points: 5 },
};
```

---

## Billing Enums

### InvoiceStatus

```java
public enum InvoiceStatus {
    DRAFT("Nháp", false),
    SENT("Đã gửi", false),
    PAID("Đã thanh toán", true),
    PARTIAL("Thanh toán một phần", false),
    OVERDUE("Quá hạn", false),
    CANCELLED("Đã hủy", true);

    private final String displayNameVi;
    private final boolean isFinal;  // Cannot change after this status

    public boolean canEdit() {
        return this == DRAFT;
    }

    public boolean canPay() {
        return this == SENT || this == PARTIAL || this == OVERDUE;
    }

    public boolean canCancel() {
        return this == DRAFT || this == SENT;
    }
}
```

```typescript
export type InvoiceStatus = 'DRAFT' | 'SENT' | 'PAID' | 'PARTIAL' | 'OVERDUE' | 'CANCELLED';

export const InvoiceStatusConfig = {
  DRAFT: { label: 'Nháp', color: 'gray', canEdit: true, canPay: false, canCancel: true },
  SENT: { label: 'Đã gửi', color: 'blue', canEdit: false, canPay: true, canCancel: true },
  PAID: { label: 'Đã thanh toán', color: 'green', canEdit: false, canPay: false, canCancel: false },
  PARTIAL: { label: 'Thanh toán một phần', color: 'yellow', canEdit: false, canPay: true, canCancel: false },
  OVERDUE: { label: 'Quá hạn', color: 'red', canEdit: false, canPay: true, canCancel: true },
  CANCELLED: { label: 'Đã hủy', color: 'gray', canEdit: false, canPay: false, canCancel: false },
} as const;
```

### PaymentMethod

```java
public enum PaymentMethod {
    CASH("Tiền mặt", "cash", true),
    BANK_TRANSFER("Chuyển khoản", "bank", true),
    MOMO("Ví MoMo", "momo", true),
    VNPAY("VNPay QR", "vnpay", true),
    ZALOPAY("ZaloPay", "zalopay", true),
    CREDIT_CARD("Thẻ tín dụng", "card", false);  // Not available in VN

    private final String displayNameVi;
    private final String code;
    private final boolean availableInVN;
}
```

### PaymentStatus

```java
public enum PaymentStatus {
    PENDING("Chờ xử lý"),
    PROCESSING("Đang xử lý"),
    COMPLETED("Thành công"),
    FAILED("Thất bại"),
    REFUNDED("Đã hoàn tiền");

    private final String displayName;
}
```

---

## Gamification Enums

### PointActionType

```java
public enum PointActionType {
    // Positive actions
    ATTENDANCE_PRESENT("Điểm danh đúng giờ", 10),
    ATTENDANCE_EARLY("Đến sớm", 5),
    HOMEWORK_COMPLETE("Hoàn thành bài tập", 15),
    HOMEWORK_EXCELLENT("Bài tập xuất sắc", 25),
    QUIZ_PASS("Đạt bài kiểm tra", 20),
    QUIZ_PERFECT("Điểm tuyệt đối", 50),
    PARTICIPATION("Phát biểu tích cực", 5),
    HELP_CLASSMATE("Giúp đỡ bạn học", 10),

    // Negative actions
    ATTENDANCE_LATE("Đi trễ", -5),
    ATTENDANCE_ABSENT("Vắng không phép", -10),
    HOMEWORK_MISSING("Thiếu bài tập", -10),
    BEHAVIOR_WARNING("Cảnh cáo hành vi", -15);

    private final String description;
    private final int points;
}
```

### BadgeType

```java
public enum BadgeType {
    ATTENDANCE_STREAK("Chuyên cần", "attendance", "5 buổi liên tiếp không vắng"),
    PERFECT_WEEK("Tuần hoàn hảo", "star", "Điểm danh đủ 1 tuần"),
    HOMEWORK_HERO("Anh hùng bài tập", "book", "10 bài tập liên tiếp"),
    TOP_SCORER("Học sinh giỏi", "trophy", "Top 3 điểm cao nhất lớp"),
    HELPER("Người giúp đỡ", "heart", "Giúp 5 bạn học");

    private final String displayName;
    private final String icon;
    private final String requirement;
}
```

### RewardStatus

```java
public enum RewardStatus {
    AVAILABLE("Có sẵn"),
    OUT_OF_STOCK("Hết hàng"),
    REDEEMED("Đã đổi"),
    EXPIRED("Hết hạn");

    private final String displayName;
}
```

---

## Notification Enums

### NotificationType

```java
public enum NotificationType {
    // System
    SYSTEM_ANNOUNCEMENT("Thông báo hệ thống", "bell"),

    // Attendance
    ATTENDANCE_REMINDER("Nhắc nhở điểm danh", "clock"),
    ATTENDANCE_MARKED("Đã điểm danh", "check"),
    ATTENDANCE_ABSENT("Thông báo vắng mặt", "alert"),

    // Billing
    INVOICE_CREATED("Hóa đơn mới", "file-text"),
    INVOICE_DUE_SOON("Hóa đơn sắp đến hạn", "calendar"),
    INVOICE_OVERDUE("Hóa đơn quá hạn", "alert-triangle"),
    PAYMENT_RECEIVED("Đã nhận thanh toán", "check-circle"),

    // Class
    CLASS_REMINDER("Nhắc nhở lịch học", "calendar"),
    CLASS_CANCELLED("Hủy buổi học", "x-circle"),
    CLASS_MAKEUP("Thông báo học bù", "refresh");

    private final String displayName;
    private final String icon;
}
```

### NotificationChannel

```java
public enum NotificationChannel {
    IN_APP("Trong ứng dụng"),
    EMAIL("Email"),
    SMS("Tin nhắn SMS"),
    ZALO("Zalo OA"),
    PUSH("Push notification");

    private final String displayName;
}
```

---

## KiteHub Enums (SaaS Platform)

### SubscriptionPlan

```java
public enum SubscriptionPlan {
    FREE("Miễn phí", 0, 50, 2, false),
    STARTER("Starter", 500000, 200, 5, true),
    PRO("Pro", 1500000, 1000, 20, true),
    ENTERPRISE("Enterprise", -1, -1, -1, true);  // Custom pricing

    private final String displayName;
    private final long priceVND;        // Monthly price
    private final int maxStudents;       // -1 = unlimited
    private final int maxTeachers;
    private final boolean customDomain;
}
```

```typescript
export type SubscriptionPlan = 'FREE' | 'STARTER' | 'PRO' | 'ENTERPRISE';

export const PlanConfig: Record<SubscriptionPlan, {
  name: string;
  price: number;
  maxStudents: number;
  maxTeachers: number;
  features: string[];
}> = {
  FREE: {
    name: 'Miễn phí',
    price: 0,
    maxStudents: 50,
    maxTeachers: 2,
    features: ['Quản lý học viên cơ bản', 'Điểm danh', '3 theme miễn phí'],
  },
  STARTER: {
    name: 'Starter',
    price: 500000,
    maxStudents: 200,
    maxTeachers: 5,
    features: ['Tất cả tính năng Free', 'Billing & Invoice', 'Báo cáo nâng cao'],
  },
  PRO: {
    name: 'Pro',
    price: 1500000,
    maxStudents: 1000,
    maxTeachers: 20,
    features: ['Tất cả tính năng Starter', 'Gamification', 'API Access', 'Theme Pro'],
  },
  ENTERPRISE: {
    name: 'Enterprise',
    price: -1,
    maxStudents: -1,
    maxTeachers: -1,
    features: ['Tất cả tính năng Pro', 'Custom domain', 'White-label', 'SLA 99.9%'],
  },
};
```

### InstanceStatus

```java
public enum InstanceStatus {
    PROVISIONING("Đang khởi tạo"),
    ACTIVE("Đang hoạt động"),
    SUSPENDED("Tạm ngưng"),
    TERMINATED("Đã chấm dứt");

    private final String displayName;
}
```

### ThemeTier

```java
public enum ThemeTier {
    FREE("Miễn phí"),
    PRO("Pro"),
    ENTERPRISE("Enterprise");

    private final String displayName;
}
```

---

## Application Constants

### Pagination

```java
public final class PaginationConstants {
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int MIN_PAGE_SIZE = 1;
    public static final String DEFAULT_SORT_FIELD = "createdAt";
    public static final String DEFAULT_SORT_DIRECTION = "DESC";

    private PaginationConstants() {}
}
```

```typescript
export const PAGINATION = {
  DEFAULT_PAGE_SIZE: 20,
  MAX_PAGE_SIZE: 100,
  MIN_PAGE_SIZE: 1,
  DEFAULT_SORT_FIELD: 'createdAt',
  DEFAULT_SORT_DIRECTION: 'desc',
} as const;
```

### Validation

```java
public final class ValidationConstants {
    // Lengths
    public static final int NAME_MIN_LENGTH = 2;
    public static final int NAME_MAX_LENGTH = 100;
    public static final int EMAIL_MAX_LENGTH = 255;
    public static final int PHONE_LENGTH = 10;
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 128;
    public static final int NOTE_MAX_LENGTH = 1000;

    // Patterns
    public static final String PHONE_PATTERN = "^0[0-9]{9}$";
    public static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    private ValidationConstants() {}
}
```

```typescript
export const VALIDATION = {
  NAME_MIN_LENGTH: 2,
  NAME_MAX_LENGTH: 100,
  EMAIL_MAX_LENGTH: 255,
  PHONE_LENGTH: 10,
  PASSWORD_MIN_LENGTH: 8,
  PASSWORD_MAX_LENGTH: 128,
  NOTE_MAX_LENGTH: 1000,

  PHONE_PATTERN: /^0[0-9]{9}$/,
  EMAIL_PATTERN: /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/,
} as const;
```

### File Upload

```java
public final class FileConstants {
    // Image
    public static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;  // 5MB
    public static final String[] ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/png", "image/webp"};
    public static final String[] ALLOWED_IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp"};

    // Document
    public static final long MAX_DOCUMENT_SIZE = 10 * 1024 * 1024;  // 10MB
    public static final String[] ALLOWED_DOC_TYPES = {"application/pdf", "application/vnd.ms-excel"};

    // Avatar
    public static final int AVATAR_MAX_WIDTH = 500;
    public static final int AVATAR_MAX_HEIGHT = 500;

    private FileConstants() {}
}
```

### Time & Date

```java
public final class DateTimeConstants {
    public static final String DATE_FORMAT = "dd/MM/yyyy";
    public static final String TIME_FORMAT = "HH:mm";
    public static final String DATETIME_FORMAT = "dd/MM/yyyy HH:mm";
    public static final String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    public static final String TIMEZONE_VN = "Asia/Ho_Chi_Minh";

    private DateTimeConstants() {}
}
```

```typescript
export const DATE_FORMAT = {
  DATE: 'dd/MM/yyyy',
  TIME: 'HH:mm',
  DATETIME: 'dd/MM/yyyy HH:mm',
  ISO: "yyyy-MM-dd'T'HH:mm:ss'Z'",
  DISPLAY_DATE: 'dd MMMM, yyyy',
  DISPLAY_DATETIME: 'dd MMMM, yyyy HH:mm',
} as const;

export const TIMEZONE_VN = 'Asia/Ho_Chi_Minh';
```

---

## Message Templates

### Notification Messages

```java
public final class NotificationMessages {
    // Attendance
    public static final String ATTENDANCE_REMINDER = "Nhắc nhở: Lớp {className} sẽ bắt đầu lúc {time}";
    public static final String ATTENDANCE_MARKED = "Con bạn {studentName} đã được điểm danh: {status}";
    public static final String ATTENDANCE_ABSENT = "Thông báo: {studentName} vắng mặt buổi học ngày {date}";

    // Invoice
    public static final String INVOICE_CREATED = "Hóa đơn mới #{invoiceNo} - Số tiền: {amount}";
    public static final String INVOICE_DUE = "Hóa đơn #{invoiceNo} sẽ đến hạn vào ngày {dueDate}";
    public static final String PAYMENT_SUCCESS = "Đã nhận thanh toán {amount} cho hóa đơn #{invoiceNo}";

    private NotificationMessages() {}
}
```

### Validation Messages

```java
public final class ValidationMessages {
    public static final String REQUIRED = "{field} là bắt buộc";
    public static final String MIN_LENGTH = "{field} phải có ít nhất {min} ký tự";
    public static final String MAX_LENGTH = "{field} không được vượt quá {max} ký tự";
    public static final String INVALID_EMAIL = "Email không hợp lệ";
    public static final String INVALID_PHONE = "Số điện thoại không hợp lệ (phải có 10 số, bắt đầu bằng 0)";
    public static final String INVALID_DATE = "Ngày không hợp lệ";
    public static final String FUTURE_DATE = "{field} phải là ngày trong tương lai";
    public static final String PAST_DATE = "{field} phải là ngày trong quá khứ";

    private ValidationMessages() {}
}
```

```typescript
export const VALIDATION_MESSAGES = {
  required: (field: string) => `${field} là bắt buộc`,
  minLength: (field: string, min: number) => `${field} phải có ít nhất ${min} ký tự`,
  maxLength: (field: string, max: number) => `${field} không được vượt quá ${max} ký tự`,
  invalidEmail: 'Email không hợp lệ',
  invalidPhone: 'Số điện thoại không hợp lệ (phải có 10 số, bắt đầu bằng 0)',
  invalidDate: 'Ngày không hợp lệ',
  futureDate: (field: string) => `${field} phải là ngày trong tương lai`,
  pastDate: (field: string) => `${field} phải là ngày trong quá khứ`,
} as const;
```

---

## API Query Params

```typescript
// Common filter params
export const FILTER_PARAMS = {
  STATUS: 'status',
  SEARCH: 'search',
  DATE_FROM: 'dateFrom',
  DATE_TO: 'dateTo',
  CLASS_ID: 'classId',
  STUDENT_ID: 'studentId',
  TEACHER_ID: 'teacherId',
} as const;

// Sort options
export const SORT_OPTIONS = {
  CREATED_AT_DESC: 'createdAt,desc',
  CREATED_AT_ASC: 'createdAt,asc',
  NAME_ASC: 'name,asc',
  NAME_DESC: 'name,desc',
  DUE_DATE_ASC: 'dueDate,asc',
  AMOUNT_DESC: 'amount,desc',
} as const;
```

## Actions

### Thêm enum mới
1. Định nghĩa trong Java với displayName và metadata
2. Tạo TypeScript type và config object tương ứng
3. Cập nhật tài liệu này
4. Thêm migration nếu cần (cho database enum)

### Sử dụng enum trong code
```java
// Java - Lấy display name
StudentStatus.ACTIVE.getDisplayNameVi()  // "Đang học"

// TypeScript - Lấy config
StudentStatusConfig[status].label        // "Đang học"
StudentStatusConfig[status].bgColor      // "bg-green-100"
```
