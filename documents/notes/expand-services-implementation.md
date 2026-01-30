# EXPAND SERVICES IMPLEMENTATION NOTES

**Version:** 1.0
**Created:** 2026-01-30
**Purpose:** Implementation notes cho ENGAGEMENT, MEDIA, PREMIUM features

**Tham chiếu:**
- `system-architecture-v3-final.md` PHẦN 6B (Feature Detection)
- `architecture-clarification-qa.md` PART 1 (Pricing Tiers)
- `backend-implementation-plan-v2.md` Phase 3 (Feature Detection)

---

## MỤC LỤC

1. [Tổng quan Expand Services](#tổng-quan-expand-services)
2. [ENGAGEMENT Package](#engagement-package)
3. [MEDIA Package](#media-package)
4. [PREMIUM Package](#premium-package)
5. [Feature Flag Patterns](#feature-flag-patterns)
6. [Upgrade Flow UX](#upgrade-flow-ux)
7. [Testing Checklist](#testing-checklist)

---

# TỔNG QUAN EXPAND SERVICES

## Pricing Tiers & Features

```
┌─────────────────────────────────────────────────────────┐
│  PRICING TIERS                                          │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  BASIC (499k VND/month)                                │
│  ✅ 50 students, 10 courses                             │
│  ✅ Classes & scheduling                                │
│  ✅ Basic dashboard                                     │
│  ❌ No attendance tracking                              │
│  ❌ No media upload                                     │
│  ❌ No analytics                                        │
│  ⚠️ Watermark on public pages                           │
│                                                         │
│  STANDARD (999k VND/month)                             │
│  ✅ 200 students, 50 courses                            │
│  ✅ All BASIC features                                  │
│  ✅ ENGAGEMENT: Attendance, grades, progress            │
│  ✅ MEDIA: Image/video upload (5GB storage)             │
│  ❌ No advanced analytics                               │
│  ⚠️ Watermark on public pages                           │
│                                                         │
│  PREMIUM (1,499k VND/month)                            │
│  ✅ Unlimited students & courses                        │
│  ✅ All STANDARD features                               │
│  ✅ PREMIUM: Advanced analytics, reports                │
│  ✅ Custom domain (mydomain.com)                        │
│  ✅ AI branding with manual override                    │
│  ✅ Priority support                                    │
│  ✅ No watermark                                        │
└─────────────────────────────────────────────────────────┘
```

## Feature Matrix

| Feature | BASIC | STANDARD | PREMIUM |
|---------|-------|----------|---------|
| Max Students | 50 | 200 | ∞ |
| Max Courses | 10 | 50 | ∞ |
| Attendance Tracking | ❌ | ✅ | ✅ |
| Grade Management | ❌ | ✅ | ✅ |
| Progress Tracking | ❌ | ✅ | ✅ |
| Parent Portal | ❌ | ✅ | ✅ |
| Media Upload | ❌ | ✅ (5GB) | ✅ (20GB) |
| Image in Lessons | ❌ | ✅ | ✅ |
| Video Embedding | ❌ | ✅ | ✅ |
| File Attachments | ❌ | ✅ | ✅ |
| Basic Dashboard | ✅ | ✅ | ✅ |
| Advanced Analytics | ❌ | ❌ | ✅ |
| Custom Reports | ❌ | ❌ | ✅ |
| Export to Excel/PDF | ✅ | ✅ | ✅ |
| Custom Domain | ❌ | ❌ | ✅ |
| AI Branding | ❌ | ❌ | ✅ |
| Manual Override | ❌ | ❌ | ✅ |
| Watermark | ⚠️ Yes | ⚠️ Yes | ✅ No |
| Priority Support | ❌ | ❌ | ✅ |

---

# ENGAGEMENT PACKAGE

**Included in:** STANDARD, PREMIUM
**Required tier:** STANDARD or higher

## Features

### 1. Attendance Tracking

```java
// Backend: Attendance Entity
@Entity
@Table(name = "attendance_records")
public class AttendanceRecord extends BaseEntity {

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId; // Multi-tenant isolation

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "class_session_id")
    private ClassSession classSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status; // PRESENT, ABSENT, LATE, EXCUSED

    @Column(length = 500)
    private String notes;

    private LocalDateTime recordedAt;
}

// Backend: Attendance Service
@Service
public class AttendanceService {

    private final FeatureDetectionService featureService;

    /**
     * Record attendance (requires ENGAGEMENT)
     */
    @Transactional
    public AttendanceRecord recordAttendance(
        UUID instanceId,
        UUID studentId,
        UUID classSessionId,
        AttendanceStatus status
    ) {
        // ⭐ Feature check
        featureService.requireFeature(instanceId, "ENGAGEMENT");

        // Create record
        AttendanceRecord record = new AttendanceRecord();
        record.setInstanceId(instanceId);
        record.setStudentId(studentId);
        record.setClassSessionId(classSessionId);
        record.setStatus(status);
        record.setRecordedAt(LocalDateTime.now());

        return attendanceRepo.save(record);
    }
}

// Backend: API Controller
@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    /**
     * POST /api/v1/attendance
     */
    @PostMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('CENTER_ADMIN')")
    public ResponseEntity<AttendanceDTO> recordAttendance(
        @RequestHeader("X-Instance-Id") UUID instanceId,
        @RequestBody @Valid AttendanceRequest request
    ) {
        // Feature check happens in service layer
        AttendanceRecord record = attendanceService.recordAttendance(
            instanceId,
            request.getStudentId(),
            request.getClassSessionId(),
            request.getStatus()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(AttendanceDTO.from(record));
    }
}
```

```tsx
// Frontend: Attendance Page
'use client';

import { useFeatureDetection } from '@/hooks/use-feature-detection';
import { UpgradePrompt } from '@/components/upgrade-prompt';

export default function AttendancePage() {
  const { hasFeature, tier } = useFeatureDetection();

  // ⭐ Feature check
  if (!hasFeature('ENGAGEMENT')) {
    return (
      <UpgradePrompt
        feature="Attendance Tracking"
        currentTier={tier}
        requiredTier="STANDARD"
        description="Theo dõi điểm danh học viên, gửi thông báo cho phụ huynh"
        benefits={[
          'Điểm danh trực tuyến',
          'Báo cáo tỷ lệ tham dự',
          'Thông báo vắng mặt tự động',
          'Export báo cáo Excel'
        ]}
      />
    );
  }

  // Render attendance UI
  return (
    <div>
      <AttendanceTable />
      <AttendanceForm />
    </div>
  );
}
```

### 2. Grade Management

```java
// Backend: Grade Entity
@Entity
@Table(name = "grades")
public class Grade extends BaseEntity {

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @ManyToOne
    private Student student;

    @ManyToOne
    private Course course;

    @Column(length = 100)
    private String assignmentName;

    @Column(precision = 5, scale = 2)
    private BigDecimal score; // e.g., 85.50

    @Column(precision = 5, scale = 2)
    private BigDecimal maxScore; // e.g., 100.00

    @Column(length = 1000)
    private String feedback;
}

// Backend: Grade Service với feature check
@Service
public class GradeService {

    public Grade createGrade(UUID instanceId, GradeCreateRequest request) {
        // ⭐ Require ENGAGEMENT
        featureService.requireFeature(instanceId, "ENGAGEMENT");

        // Create grade...
    }
}
```

```tsx
// Frontend: Grade UI
export default function GradesPage() {
  const { hasFeature } = useFeatureDetection();

  if (!hasFeature('ENGAGEMENT')) {
    return <UpgradePrompt feature="Grade Management" requiredTier="STANDARD" />;
  }

  return <GradeManager />;
}
```

### 3. Progress Tracking

```java
// Backend: Progress Entity
@Entity
@Table(name = "student_progress")
public class StudentProgress extends BaseEntity {

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @ManyToOne
    private Student student;

    @ManyToOne
    private Course course;

    @Column
    private Integer completedLessons;

    @Column
    private Integer totalLessons;

    @Column(precision = 5, scale = 2)
    private BigDecimal completionPercentage;

    @Column(precision = 5, scale = 2)
    private BigDecimal averageScore;
}

// Feature check pattern (same as above)
```

### 4. Parent Portal Access

```java
// Backend: Parent can view attendance, grades, progress
@RestController
@RequestMapping("/api/v1/parent")
public class ParentPortalController {

    @GetMapping("/students/{studentId}/attendance")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<List<AttendanceDTO>> getStudentAttendance(
        @RequestHeader("X-Instance-Id") UUID instanceId,
        @PathVariable UUID studentId
    ) {
        // ⭐ Feature check
        featureService.requireFeature(instanceId, "ENGAGEMENT");

        // Return attendance records...
    }
}
```

---

# MEDIA PACKAGE

**Included in:** STANDARD, PREMIUM
**Required tier:** STANDARD or higher

## Features

### 1. Image Upload in Lessons

```java
// Backend: Lesson Entity
@Entity
@Table(name = "lessons")
public class Lesson extends BaseEntity {

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ElementCollection
    @CollectionTable(name = "lesson_images")
    private List<String> imageUrls; // CDN URLs

    @Column(name = "video_url", length = 500)
    private String videoUrl; // YouTube, Vimeo embed URL

    @ElementCollection
    @CollectionTable(name = "lesson_attachments")
    private List<LessonAttachment> attachments;
}

@Embeddable
public class LessonAttachment {
    private String fileName;
    private String fileUrl;
    private Long fileSizeBytes;
    private String fileType; // PDF, DOCX, etc.
}

// Backend: Media Upload Service
@Service
public class MediaUploadService {

    private final S3StorageService s3Service;
    private final FeatureDetectionService featureService;

    /**
     * Upload image (requires MEDIA feature)
     */
    public String uploadImage(
        UUID instanceId,
        MultipartFile imageFile
    ) {
        // ⭐ Feature check
        featureService.requireFeature(instanceId, "MEDIA");

        // Check storage limit
        long currentStorageUsed = getStorageUsed(instanceId);
        long storageLimit = getStorageLimit(instanceId);

        if (currentStorageUsed + imageFile.getSize() > storageLimit) {
            throw new StorageLimitExceededException(
                String.format(
                    "Storage limit exceeded: %d MB / %d MB. Please upgrade to PREMIUM.",
                    currentStorageUsed / (1024 * 1024),
                    storageLimit / (1024 * 1024)
                )
            );
        }

        // Validate image
        validateImage(imageFile);

        // Upload to S3
        String s3Key = String.format(
            "instances/%s/lessons/%s",
            instanceId,
            UUID.randomUUID() + getFileExtension(imageFile)
        );

        String cdnUrl = s3Service.upload(imageFile, s3Key);

        // Track storage usage
        trackStorageUsage(instanceId, imageFile.getSize());

        return cdnUrl;
    }

    /**
     * Get storage limit based on tier
     */
    private long getStorageLimit(UUID instanceId) {
        InstanceConfig config = featureService.getInstanceConfig(instanceId);
        PricingTier tier = config.getTier();

        return switch (tier) {
            case BASIC -> 0L; // No media upload
            case STANDARD -> 5L * 1024 * 1024 * 1024; // 5GB
            case PREMIUM -> 20L * 1024 * 1024 * 1024; // 20GB
        };
    }

    private void validateImage(MultipartFile file) {
        // Check file type
        String contentType = file.getContentType();
        if (!contentType.startsWith("image/")) {
            throw new ValidationException("File must be an image");
        }

        // Check file size (max 10MB per image)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new ValidationException("Image size must not exceed 10MB");
        }

        // Check dimensions (optional)
        // BufferedImage img = ImageIO.read(file.getInputStream());
        // if (img.getWidth() > 4000 || img.getHeight() > 4000) {
        //     throw new ValidationException("Image dimensions too large");
        // }
    }
}

// Backend: API
@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    @PostMapping("/upload/image")
    @PreAuthorize("hasRole('TEACHER') or hasRole('CENTER_ADMIN')")
    public ResponseEntity<MediaUploadResponse> uploadImage(
        @RequestHeader("X-Instance-Id") UUID instanceId,
        @RequestParam("image") MultipartFile imageFile
    ) {
        String cdnUrl = mediaUploadService.uploadImage(instanceId, imageFile);

        MediaUploadResponse response = MediaUploadResponse.builder()
            .url(cdnUrl)
            .fileName(imageFile.getOriginalFilename())
            .fileSize(imageFile.getSize())
            .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get storage usage
     */
    @GetMapping("/storage/usage")
    public ResponseEntity<StorageUsageResponse> getStorageUsage(
        @RequestHeader("X-Instance-Id") UUID instanceId
    ) {
        long used = mediaUploadService.getStorageUsed(instanceId);
        long limit = mediaUploadService.getStorageLimit(instanceId);

        StorageUsageResponse response = StorageUsageResponse.builder()
            .usedBytes(used)
            .limitBytes(limit)
            .usedMB(used / (1024 * 1024))
            .limitMB(limit / (1024 * 1024))
            .usagePercentage((int) ((used * 100) / limit))
            .build();

        return ResponseEntity.ok(response);
    }
}
```

```tsx
// Frontend: Image Upload Component
'use client';

import { useFeatureDetection } from '@/hooks/use-feature-detection';
import { useStorage } from '@/hooks/use-storage';
import { UpgradePrompt } from '@/components/upgrade-prompt';

export function ImageUploadButton() {
  const { hasFeature } = useFeatureDetection();
  const { storageUsed, storageLimit, usagePercentage } = useStorage();
  const [uploading, setUploading] = useState(false);

  // ⭐ Feature check
  if (!hasFeature('MEDIA')) {
    return (
      <Button onClick={() => showUpgradeDialog('MEDIA')} variant="outline">
        <Upload className="mr-2 h-4 w-4" />
        Upload Image (Requires STANDARD)
      </Button>
    );
  }

  const handleUpload = async (file: File) => {
    // Check storage limit BEFORE upload
    if (file.size + storageUsed > storageLimit) {
      toast.error(
        `Storage limit exceeded: ${usagePercentage}% used. ` +
        `Please upgrade to PREMIUM for 20GB storage.`
      );
      return;
    }

    setUploading(true);

    try {
      const formData = new FormData();
      formData.append('image', file);

      const response = await fetch('/api/v1/media/upload/image', {
        method: 'POST',
        body: formData
      });

      const data = await response.json();
      toast.success('Image uploaded successfully!');
      onImageUploaded(data.url);

    } catch (error) {
      toast.error('Upload failed: ' + error.message);
    } finally {
      setUploading(false);
    }
  };

  return (
    <div>
      <Button onClick={() => fileInput.click()} disabled={uploading}>
        <Upload className="mr-2 h-4 w-4" />
        {uploading ? 'Uploading...' : 'Upload Image'}
      </Button>

      {/* Storage usage indicator */}
      <div className="mt-2 text-sm text-muted-foreground">
        Storage: {storageUsed / (1024 * 1024)} MB / {storageLimit / (1024 * 1024)} MB
        ({usagePercentage}%)
      </div>

      {usagePercentage > 80 && (
        <Alert variant="warning" className="mt-2">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>
            Storage almost full ({usagePercentage}%).
            <Button variant="link" onClick={() => upgradeDialog('PREMIUM')}>
              Upgrade to PREMIUM
            </Button> for 20GB storage.
          </AlertDescription>
        </Alert>
      )}
    </div>
  );
}
```

### 2. Video Embedding (YouTube, Vimeo)

```java
// Backend: Just store embed URL (no upload, no storage limit)
@Service
public class VideoEmbedService {

    public void addVideoToLesson(
        UUID instanceId,
        UUID lessonId,
        String videoUrl
    ) {
        // ⭐ Feature check
        featureService.requireFeature(instanceId, "MEDIA");

        // Validate video URL (YouTube or Vimeo)
        if (!isValidVideoUrl(videoUrl)) {
            throw new ValidationException(
                "Invalid video URL. Only YouTube and Vimeo supported."
            );
        }

        // Update lesson
        Lesson lesson = lessonRepo.findById(lessonId)
            .orElseThrow(() -> new NotFoundException("Lesson not found"));

        lesson.setVideoUrl(videoUrl);
        lessonRepo.save(lesson);
    }

    private boolean isValidVideoUrl(String url) {
        return url.contains("youtube.com") ||
               url.contains("youtu.be") ||
               url.contains("vimeo.com");
    }
}
```

```tsx
// Frontend: Video Embed
export function VideoEmbedInput() {
  const { hasFeature } = useFeatureDetection();

  if (!hasFeature('MEDIA')) {
    return <UpgradePrompt feature="Video Embedding" requiredTier="STANDARD" />;
  }

  return (
    <div>
      <Input
        placeholder="Paste YouTube or Vimeo URL"
        onChange={(e) => validateAndSetVideoUrl(e.target.value)}
      />
      {videoUrl && (
        <div className="mt-4">
          <iframe
            src={getEmbedUrl(videoUrl)}
            width="100%"
            height="400"
            frameBorder="0"
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
            allowFullScreen
          />
        </div>
      )}
    </div>
  );
}
```

### 3. File Attachments (PDF, DOCX)

```java
// Backend: File upload (counts toward storage limit)
@Service
public class FileAttachmentService {

    public LessonAttachment uploadAttachment(
        UUID instanceId,
        MultipartFile file
    ) {
        // ⭐ Feature check
        featureService.requireFeature(instanceId, "MEDIA");

        // Check storage limit
        checkStorageLimit(instanceId, file.getSize());

        // Validate file type
        String contentType = file.getContentType();
        List<String> allowedTypes = List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );

        if (!allowedTypes.contains(contentType)) {
            throw new ValidationException(
                "File type not allowed. Allowed: PDF, DOCX, XLSX"
            );
        }

        // Validate file size (max 50MB)
        if (file.getSize() > 50 * 1024 * 1024) {
            throw new ValidationException("File size must not exceed 50MB");
        }

        // Upload to S3
        String s3Key = String.format(
            "instances/%s/attachments/%s",
            instanceId,
            UUID.randomUUID() + getFileExtension(file)
        );

        String cdnUrl = s3Service.upload(file, s3Key);

        // Create attachment record
        LessonAttachment attachment = new LessonAttachment();
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFileUrl(cdnUrl);
        attachment.setFileSizeBytes(file.getSize());
        attachment.setFileType(getFileType(contentType));

        // Track storage
        trackStorageUsage(instanceId, file.getSize());

        return attachment;
    }
}
```

---

# PREMIUM PACKAGE

**Included in:** PREMIUM only
**Required tier:** PREMIUM

## Features

### 1. Advanced Analytics & Reports

```java
// Backend: Analytics Service (PREMIUM only)
@Service
public class AdvancedAnalyticsService {

    public AnalyticsDashboardDTO getAnalyticsDashboard(UUID instanceId) {
        // ⭐ Feature check
        featureService.requireFeature(instanceId, "PREMIUM");

        // Calculate advanced metrics
        return AnalyticsDashboardDTO.builder()
            .enrollmentTrends(calculateEnrollmentTrends(instanceId))
            .retentionRate(calculateRetentionRate(instanceId))
            .revenueByMonth(calculateRevenueByMonth(instanceId))
            .studentLifetimeValue(calculateLTV(instanceId))
            .coursePerformance(analyzeCoursePerformance(instanceId))
            .teacherEffectiveness(analyzeTeacherMetrics(instanceId))
            .predictiveInsights(generatePredictions(instanceId))
            .build();
    }
}
```

```tsx
// Frontend: Analytics Dashboard (PREMIUM only)
export default function AnalyticsDashboardPage() {
  const { hasFeature, tier } = useFeatureDetection();

  if (!hasFeature('PREMIUM')) {
    return (
      <UpgradePrompt
        feature="Advanced Analytics"
        currentTier={tier}
        requiredTier="PREMIUM"
        description="Phân tích chuyên sâu, dự đoán xu hướng, báo cáo tùy chỉnh"
        benefits={[
          'Enrollment trends & predictions',
          'Student retention analysis',
          'Revenue forecasting',
          'Teacher effectiveness metrics',
          'Custom report builder',
          'Export to Excel/PDF'
        ]}
      />
    );
  }

  return <AdvancedAnalyticsDashboard />;
}
```

### 2. Custom Domain

```java
// Backend: Custom Domain Service
@Service
public class CustomDomainService {

    public void setCustomDomain(UUID instanceId, String customDomain) {
        // ⭐ Require PREMIUM
        InstanceConfig config = featureService.getInstanceConfig(instanceId);
        if (config.getTier() != PricingTier.PREMIUM) {
            throw new FeatureNotAvailableException("PREMIUM", "PREMIUM");
        }

        // Validate domain
        if (!isValidDomain(customDomain)) {
            throw new ValidationException("Invalid domain format");
        }

        // Check DNS CNAME record
        boolean dnsValid = verifyDNSRecord(customDomain, "kiteclass.com");
        if (!dnsValid) {
            throw new DNSVerificationException(
                "Please add CNAME record: " + customDomain + " → kiteclass.com"
            );
        }

        // Update instance
        Instance instance = instanceRepo.findById(instanceId)
            .orElseThrow(() -> new NotFoundException("Instance not found"));

        instance.setCustomDomain(customDomain);
        instanceRepo.save(instance);

        // Provision SSL certificate (Let's Encrypt)
        sslService.provisionCertificate(customDomain);

        log.info("Custom domain configured: {} → {}", customDomain, instance.getSubdomain());
    }
}
```

```tsx
// Frontend: Custom Domain Setup (PREMIUM only)
export function CustomDomainSettings() {
  const { tier } = useFeatureDetection();

  if (tier !== 'PREMIUM') {
    return (
      <Card>
        <CardHeader>
          <CardTitle>🔒 Custom Domain</CardTitle>
          <CardDescription>Available on PREMIUM plan</CardDescription>
        </CardHeader>
        <CardContent>
          <Alert>
            <Lock className="h-4 w-4" />
            <AlertTitle>PREMIUM Feature</AlertTitle>
            <AlertDescription>
              Use your own domain (e.g., mydomain.com) instead of subdomain.
              <Button onClick={handleUpgrade} className="ml-2">
                Upgrade to PREMIUM
              </Button>
            </AlertDescription>
          </Alert>
        </CardContent>
      </Card>
    );
  }

  return <CustomDomainForm />;
}
```

### 3. Watermark Removal

```java
// Backend: Watermark configuration
@Service
public class WatermarkService {

    public boolean shouldShowWatermark(UUID instanceId) {
        InstanceConfig config = featureService.getInstanceConfig(instanceId);

        // Only PREMIUM tier removes watermark
        return config.getTier() != PricingTier.PREMIUM;
    }
}

// API
@GetMapping("/api/v1/instance/{instanceId}/watermark")
public ResponseEntity<WatermarkConfigDTO> getWatermarkConfig(
    @PathVariable UUID instanceId
) {
    boolean showWatermark = watermarkService.shouldShowWatermark(instanceId);

    WatermarkConfigDTO dto = WatermarkConfigDTO.builder()
        .showWatermark(showWatermark)
        .watermarkText("Powered by KiteClass")
        .watermarkPosition("bottom-right")
        .build();

    return ResponseEntity.ok(dto);
}
```

```tsx
// Frontend: Watermark Component
export function Watermark() {
  const { tier } = useFeatureDetection();
  const { watermarkConfig } = useWatermark();

  // PREMIUM users don't see watermark
  if (tier === 'PREMIUM' || !watermarkConfig.showWatermark) {
    return null;
  }

  return (
    <div className="fixed bottom-4 right-4 text-xs text-muted-foreground bg-background/80 px-2 py-1 rounded">
      {watermarkConfig.watermarkText}
    </div>
  );
}

// Usage in layout
export default function PublicLayout({ children }) {
  return (
    <div>
      {children}
      <Watermark />
    </div>
  );
}
```

---

# FEATURE FLAG PATTERNS

## Pattern 1: Service Layer Check

```java
@Service
public class MyService {

    public void doSomething(UUID instanceId) {
        // Check feature at service layer
        featureService.requireFeature(instanceId, "ENGAGEMENT");

        // Proceed with business logic...
    }
}
```

## Pattern 2: Controller Annotation (Custom)

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresFeature {
    String value();
}

// Usage
@PostMapping("/attendance")
@RequiresFeature("ENGAGEMENT")
public ResponseEntity<?> recordAttendance(...) {
    // No manual check needed, handled by aspect
}

// Aspect
@Aspect
@Component
public class FeatureCheckAspect {

    @Around("@annotation(requiresFeature)")
    public Object checkFeature(
        ProceedingJoinPoint joinPoint,
        RequiresFeature requiresFeature
    ) throws Throwable {

        UUID instanceId = extractInstanceId(joinPoint);
        String feature = requiresFeature.value();

        featureService.requireFeature(instanceId, feature);

        return joinPoint.proceed();
    }
}
```

## Pattern 3: Frontend Hook

```tsx
// hooks/use-feature-detection.ts
export function useFeatureDetection() {
  const { data: config } = useSWR('/api/v1/instance/config');

  return {
    tier: config?.tier,
    features: config?.features || {},
    limitations: config?.limitations || {},

    hasFeature: (feature: string) => {
      return config?.features?.[feature] === true;
    },

    isWithinLimit: (limitName: string, currentValue: number) => {
      const limit = config?.limitations?.[limitName];
      return currentValue < limit;
    }
  };
}

// Usage
function MyComponent() {
  const { hasFeature, tier } = useFeatureDetection();

  if (!hasFeature('ENGAGEMENT')) {
    return <UpgradePrompt requiredTier="STANDARD" />;
  }

  return <ActualContent />;
}
```

---

# UPGRADE FLOW UX

## Upgrade Prompt Component

```tsx
// components/upgrade-prompt.tsx
interface UpgradePromptProps {
  feature: string;
  currentTier: string;
  requiredTier: string;
  description: string;
  benefits?: string[];
}

export function UpgradePrompt({
  feature,
  currentTier,
  requiredTier,
  description,
  benefits = []
}: UpgradePromptProps) {
  const router = useRouter();

  const handleUpgrade = () => {
    router.push(`/subscription/upgrade?tier=${requiredTier}`);
  };

  return (
    <div className="flex items-center justify-center min-h-[400px]">
      <Card className="max-w-2xl">
        <CardHeader>
          <div className="flex items-center gap-3">
            <Lock className="h-8 w-8 text-muted-foreground" />
            <div>
              <CardTitle>{feature}</CardTitle>
              <CardDescription>
                Requires {requiredTier} plan or higher
              </CardDescription>
            </div>
          </div>
        </CardHeader>

        <CardContent>
          <p className="text-muted-foreground mb-4">{description}</p>

          {benefits.length > 0 && (
            <div className="mb-6">
              <h4 className="font-semibold mb-2">Benefits:</h4>
              <ul className="space-y-1">
                {benefits.map((benefit, index) => (
                  <li key={index} className="flex items-center gap-2">
                    <Check className="h-4 w-4 text-green-600" />
                    <span>{benefit}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}

          <div className="flex items-center justify-between bg-muted p-4 rounded-lg">
            <div>
              <div className="text-sm text-muted-foreground">Current plan</div>
              <div className="font-semibold">{currentTier}</div>
            </div>
            <ArrowRight className="h-5 w-5 text-muted-foreground" />
            <div>
              <div className="text-sm text-muted-foreground">Required plan</div>
              <div className="font-semibold text-primary">{requiredTier}</div>
            </div>
          </div>

          <Button onClick={handleUpgrade} className="w-full mt-6" size="lg">
            Upgrade to {requiredTier}
          </Button>

          <p className="text-xs text-muted-foreground text-center mt-4">
            You can upgrade anytime. Changes take effect immediately.
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
```

---

# TESTING CHECKLIST

## Feature Detection Tests

```java
// Backend: Feature detection tests
@SpringBootTest
class FeatureDetectionServiceTest {

    @Test
    void basicTier_shouldNotHaveEngagement() {
        UUID instanceId = createTestInstance(PricingTier.BASIC);

        boolean hasEngagement = featureService.hasFeature(instanceId, "ENGAGEMENT");

        assertFalse(hasEngagement);
    }

    @Test
    void standardTier_shouldHaveEngagement() {
        UUID instanceId = createTestInstance(PricingTier.STANDARD);

        boolean hasEngagement = featureService.hasFeature(instanceId, "ENGAGEMENT");

        assertTrue(hasEngagement);
    }

    @Test
    void basicTier_shouldThrowExceptionOnEngagementFeature() {
        UUID instanceId = createTestInstance(PricingTier.BASIC);

        assertThrows(FeatureNotAvailableException.class, () -> {
            featureService.requireFeature(instanceId, "ENGAGEMENT");
        });
    }
}

// Storage limit tests
@Test
void standardTier_shouldHave5GBStorageLimit() {
    UUID instanceId = createTestInstance(PricingTier.STANDARD);

    long limit = mediaService.getStorageLimit(instanceId);

    assertEquals(5L * 1024 * 1024 * 1024, limit); // 5GB
}

@Test
void uploadImage_shouldFailWhenStorageLimitExceeded() {
    UUID instanceId = createTestInstance(PricingTier.STANDARD);

    // Use up storage
    useStorage(instanceId, 5L * 1024 * 1024 * 1024);

    // Try to upload (should fail)
    assertThrows(StorageLimitExceededException.class, () -> {
        mediaService.uploadImage(instanceId, createTestImageFile());
    });
}
```

```tsx
// Frontend: Feature UI tests
describe('Attendance Page', () => {
  it('should show upgrade prompt for BASIC tier', async () => {
    mockFeatureDetection({ tier: 'BASIC', features: {} });

    render(<AttendancePage />);

    expect(screen.getByText(/Requires STANDARD plan/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Upgrade to STANDARD/i })).toBeInTheDocument();
  });

  it('should show attendance UI for STANDARD tier', async () => {
    mockFeatureDetection({
      tier: 'STANDARD',
      features: { engagement: true }
    });

    render(<AttendancePage />);

    expect(screen.getByText(/Attendance Table/i)).toBeInTheDocument();
    expect(screen.queryByText(/Requires STANDARD/i)).not.toBeInTheDocument();
  });
});

// Storage limit tests
describe('Image Upload', () => {
  it('should show warning when storage is 80% full', () => {
    mockStorage({
      used: 4 * 1024 * 1024 * 1024, // 4GB
      limit: 5 * 1024 * 1024 * 1024  // 5GB
    });

    render(<ImageUploadButton />);

    expect(screen.getByText(/Storage almost full/i)).toBeInTheDocument();
  });

  it('should prevent upload when storage is full', async () => {
    mockStorage({
      used: 5 * 1024 * 1024 * 1024,
      limit: 5 * 1024 * 1024 * 1024
    });

    render(<ImageUploadButton />);

    const fileInput = screen.getByLabelText(/upload/i);
    const file = new File(['content'], 'test.jpg', { type: 'image/jpeg' });

    fireEvent.change(fileInput, { target: { files: [file] } });

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith(
        expect.stringContaining('Storage limit exceeded')
      );
    });
  });
});
```

## Checklist

### Backend
- [ ] Feature flag system deployed
- [ ] Tier-based access working (BASIC, STANDARD, PREMIUM)
- [ ] Storage limits enforced (0GB, 5GB, 20GB)
- [ ] Attendance API requires ENGAGEMENT
- [ ] Grade API requires ENGAGEMENT
- [ ] Media upload requires MEDIA
- [ ] Analytics API requires PREMIUM
- [ ] Custom domain requires PREMIUM
- [ ] Watermark removed for PREMIUM only
- [ ] Exception messages helpful (show required tier)

### Frontend
- [ ] useFeatureDetection hook working
- [ ] Upgrade prompts display correctly
- [ ] Storage usage indicator shows correct values
- [ ] Upload blocked when storage limit exceeded
- [ ] Watermark visible (BASIC, STANDARD)
- [ ] Watermark hidden (PREMIUM)
- [ ] All tier transitions tested (BASIC→STANDARD, STANDARD→PREMIUM)
- [ ] Upgrade flow working end-to-end

### Integration
- [ ] Feature changes propagate immediately after upgrade
- [ ] Cache invalidation working (instanceConfig cache)
- [ ] Storage tracking accurate
- [ ] Trial → Paid upgrade preserves data
- [ ] Downgrade scenarios handled gracefully

---

# SUMMARY

**Expand Services:**
1. ✅ ENGAGEMENT (STANDARD+): Attendance, Grades, Progress, Parent Portal
2. ✅ MEDIA (STANDARD+): Image/Video upload, File attachments, Storage limits
3. ✅ PREMIUM (PREMIUM only): Analytics, Custom domain, Watermark removal

**Implementation complete when:**
- All feature checks in place
- Storage limits enforced
- Upgrade prompts working
- Tests passing (backend + frontend)
- Documentation complete

**Ready for production!** 🚀
