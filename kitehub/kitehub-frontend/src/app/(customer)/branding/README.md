# AI Branding Portal

## Overview

The AI Branding Portal is a feature that allows KiteHub customers to generate complete branding assets from a logo upload using AI. It provides a 4-step wizard interface for creating professional branding materials.

## Features

### 1. AI-Powered Logo Analysis
- Upload logo (PNG, JPG, WEBP, max 10MB)
- Automatic color extraction (primary, secondary, accent)
- Theme detection (Modern, Classic, Playful, Minimal)
- Brand personality analysis

### 2. Customizable Branding
- Adjust extracted colors with color picker
- Select preferred theme style
- Review brand personality traits

### 3. Automated Asset Generation
- Profile images (optimized for social media)
- Hero images (for landing pages)
- Logos (various sizes and formats)
- Banners (for headers and ads)
- OG Images (for social sharing)

### 4. Asset Management
- View all generated assets
- Filter by asset type
- Search functionality
- Download individual assets
- Preview in full size

## User Flow

### Step 1: Upload Logo
1. Navigate to `/branding/wizard`
2. Upload logo via drag & drop or file picker
3. File validation (type and size)
4. Preview uploaded logo
5. Click "Continue" to analyze

### Step 2: Analyze & Customize
1. View AI analysis results:
   - Primary, secondary, and accent colors
   - Detected theme style
   - Brand personality traits
2. Customize colors using color pickers
3. Select preferred theme style
4. Click "Confirm and Generate"

### Step 3: Generate Assets
1. Real-time progress tracking (0-100%)
2. Current step display
3. Auto-polling every 2 seconds
4. Progress bar with step indicators
5. Automatic advancement on completion

### Step 4: Review & Publish
1. Preview all generated assets
2. Download individual assets
3. View assets in full size (new tab)
4. Publish to apply to instance
5. Return to dashboard

## Pages

### `/branding` - Dashboard
- Overview statistics
- Recent assets gallery
- Quick access to create new branding
- Status cards

### `/branding/wizard` - Wizard
- 4-step branding creation process
- Step indicator
- Contextual navigation

### `/branding/assets` - Asset Management
- Full asset grid view
- Search and filter functionality
- Download and preview actions

## Components

### Wizard Steps
- **UploadStep**: File upload with drag & drop
- **AnalyzeStep**: AI results + customization
- **GenerateStep**: Real-time progress tracking
- **ReviewStep**: Asset gallery + publish

### UI Components
Located in `@/components/branding/`:
- All wizard step components
- Reuses `StepIndicator` from billing

## Hooks

Located in `@/hooks/use-branding.ts`:

### Queries
- `useAssets(instanceId)` - List all assets for instance
- `useBrandingJob(jobId)` - Get job status with auto-polling
- `useJobAssets(jobId)` - Get assets for specific job

### Mutations
- `useUploadAsset()` - Upload logo/asset
- `useAnalyzeLogo()` - Analyze logo colors/theme
- `useCreateBrandingJob()` - Create generation job
- `useGenerateContent()` - Generate marketing copy

## API Endpoints

All endpoints use the `/api/platform/branding` prefix:

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/assets/{instanceId}/{type}` | POST | Upload asset (multipart/form-data) |
| `/ai/analyze-logo` | POST | Analyze logo colors and theme |
| `/jobs` | POST | Create branding generation job |
| `/jobs/{id}` | GET | Get job status (auto-polled) |
| `/jobs/{id}/assets` | GET | Get generated assets for job |
| `/assets/{instanceId}` | GET | List all assets for instance |
| `/content/generate` | POST | Generate marketing content |

## Types

Located in `@/types/branding.ts`:

```typescript
interface LogoAnalysis {
  primaryColor: string;
  secondaryColor: string;
  accentColor: string;
  theme: 'MODERN' | 'CLASSIC' | 'PLAYFUL' | 'MINIMAL';
  brandPersonality: string[];
}

interface BrandingJob {
  id: string; // UUID
  instanceId: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  progress: number; // 0-100
  currentStep: string;
  analysis: LogoAnalysis;
  createdAt: string;
  completedAt?: string;
}

interface BrandingAsset {
  id: string;
  instanceId: string;
  type: 'PROFILE' | 'HERO' | 'LOGO' | 'BANNER' | 'OG_IMAGE';
  url: string;
  s3Key: string;
  createdAt: string;
}

interface MarketingContent {
  title: string;
  subtitle: string;
  tagline: string;
  aboutUs: string;
}
```

## Technical Details

### Auto-Polling
- Jobs are polled every 2 seconds while status is `PROCESSING`
- Polling stops when status becomes `COMPLETED` or `FAILED`
- Uses React Query's `refetchInterval` with conditional logic
- Background polling disabled to save resources

### File Upload
- Uses `FormData` with `multipart/form-data` header
- Client-side validation:
  - File type: PNG, JPG, WEBP only
  - File size: Max 10MB
- Drag & drop support with visual feedback

### State Management
- Wizard state managed with `useState`
- Step transitions: 1 → 2 → 3 → 4
- State includes: step, logoUrl, analysis, jobId
- Auto-advancement on job completion

### Error Handling
- Toast notifications for user feedback
- Try-catch blocks for API calls
- Validation before API requests
- Loading states for all async operations

## Dependencies

- **UI**: `@radix-ui/*` components via `@/components/ui`
- **Icons**: `lucide-react`
- **Toasts**: `sonner`
- **Data Fetching**: `@tanstack/react-query`
- **HTTP Client**: `axios`
- **Routing**: `next/navigation`

## Testing Checklist

- [ ] Logo upload with drag & drop
- [ ] File type validation (reject PDFs, etc.)
- [ ] File size validation (max 10MB)
- [ ] AI analysis returns valid colors
- [ ] Color picker customization saves
- [ ] Theme selection works
- [ ] Job creation triggers successfully
- [ ] Auto-polling updates every 2s
- [ ] Progress bar reflects 0-100%
- [ ] Job stops polling when COMPLETED
- [ ] Asset gallery displays all types
- [ ] Download asset works
- [ ] Preview in new tab works
- [ ] Search filtering works
- [ ] Type filtering works
- [ ] Mobile responsive layouts
- [ ] Error handling on upload failures
- [ ] Error handling on API failures

## Future Enhancements

- Multiple logo upload support
- Batch asset download (ZIP)
- Asset versioning
- Brand guidelines PDF generation
- Color palette export
- Font recommendations
- A/B testing for generated assets
- Custom asset templates
- Integration with marketing tools

## Related PRs

- PR 5.1: Project setup and infrastructure
- PR 5.2: Marketing pages and auth forms
- PR 5.3: Customer dashboard and instances
- PR 5.4: Subscription & billing management
- **PR 5.5: AI Branding Portal** (current)

## Support

For issues or questions:
1. Check the API response errors
2. Verify instance ID is valid
3. Check browser console for errors
4. Ensure file meets requirements
5. Contact support team

---

**Version**: 1.0.0
**Last Updated**: March 2026
**Maintained By**: KiteHub Frontend Team
