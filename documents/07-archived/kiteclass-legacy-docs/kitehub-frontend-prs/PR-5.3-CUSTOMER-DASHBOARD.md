# PR 5.3: Customer Dashboard & Instances

## Overview
Customer portal with instance management, dashboard overview, and reusable UI components for authenticated users.

## Features

### 1. Customer Dashboard (`/(customer)/dashboard/page.tsx`)
Main landing page after OWNER login.

#### Components
- **Welcome Header**: Personalized greeting with user name/email
- **Instance Grid**: Card-based layout of user's instances
- **Loading State**: Spinner during data fetch
- **Error State**: Retry functionality on API errors
- **Empty State**: CTA to create first instance

#### Instance Card Display
Each instance card shows:
- **Organization Name**: Primary identifier
- **Status Badge**: TRIAL/ACTIVE/SUSPENDED/EXPIRED
- **Subdomain**: `{subdomain}.kiteclass.com`
- **Tier**: FREE/BASIC/PREMIUM/ENTERPRISE
- **Created Date**: Formatted in Vietnamese locale
- **Trial Expiry**: Countdown for TRIAL instances

#### Grid Layout
- Mobile: 1 column
- Tablet (sm): 2 columns
- Desktop (lg): 3 columns

#### Click Behavior
Cards are links to `/instances/{id}` for detailed view.

### 2. Instance Detail Page (`/(customer)/instances/[id]/page.tsx`)
Detailed view of a single instance (to be implemented).

**Planned Features:**
- Instance settings
- Subscription management
- Usage analytics
- Team members
- Branding assets

## React Query Hooks (`hooks/use-instances.ts`)

### useOwnerInstances(ownerId)
Fetches all instances owned by a user.

```typescript
const { data: instances, isLoading, error, refetch } = useOwnerInstances(user?.id);
```

**Features:**
- Query key: `['instances', 'owner', ownerId]`
- Enabled only when `ownerId` exists
- Returns `Instance[]` array
- Auto-caches for 5 minutes (React Query default)

**Use Cases:**
- Dashboard instance list
- Instance count for limits

### useInstance(id)
Fetches a single instance by ID.

```typescript
const { data: instance, isLoading } = useInstance(instanceId);
```

**Features:**
- Query key: `['instances', id]`
- Enabled only when `id` exists
- Returns single `Instance` object

**Use Cases:**
- Instance detail page
- Settings page
- Edit forms

### useTrialStatus(instanceId)
Fetches trial status for an instance.

```typescript
const { data: trialStatus } = useTrialStatus(instanceId);
```

**Features:**
- Query key: `['instances', instanceId, 'trial-status']`
- Returns `TrialStatus` with days remaining
- Enabled only when `instanceId` exists

**Use Cases:**
- Trial countdown timer
- Upgrade prompts
- Expiry warnings

## Common Components

### 1. StatusBadge (`components/common/StatusBadge.tsx`)
Displays instance status with appropriate color.

```typescript
<StatusBadge status={instance.status} />
```

**Variants:**
- **TRIAL**: Blue badge
- **ACTIVE**: Green badge
- **SUSPENDED**: Orange badge
- **EXPIRED**: Red badge

**Features:**
- Automatic color mapping
- Uppercase text
- Vietnamese labels

### 2. LoadingSpinner (`components/common/LoadingSpinner.tsx`)
Reusable loading indicator.

```typescript
<LoadingSpinner className="mt-12" />
```

**Features:**
- Tailwind animated spinner
- Customizable size via className
- Centered by default

### 3. ErrorAlert (`components/common/ErrorAlert.tsx`)
Error display with retry option.

```typescript
<ErrorAlert
  message="Không thể tải danh sách instance"
  onRetry={() => refetch()}
/>
```

**Features:**
- Red error styling
- Optional retry button
- Vietnamese error messages
- Dismissible (future)

### 4. EmptyState (`components/common/EmptyState.tsx`)
Empty state with CTA action.

```typescript
<EmptyState
  title="Chưa có instance nào"
  description="Tạo instance KiteClass đầu tiên"
  action={{
    label: 'Tạo instance mới',
    onClick: () => router.push('/register'),
  }}
/>
```

**Features:**
- Icon placeholder (future)
- Title + description
- Primary action button
- Centered layout

### 5. TrialCountdown (`components/common/TrialCountdown.tsx`)
Trial expiry countdown timer.

```typescript
<TrialCountdown trialEndDate={instance.trialEndDate} />
```

**Features:**
- Real-time countdown
- Days/hours/minutes display
- Urgent state (< 3 days)
- Vietnamese labels

## Type Definitions

### Instance Type (`types/instance.ts`)

```typescript
export interface Instance {
  id: number;
  organizationName: string;
  subdomain: string;
  tier: 'FREE' | 'BASIC' | 'PREMIUM' | 'ENTERPRISE';
  status: 'TRIAL' | 'ACTIVE' | 'SUSPENDED' | 'EXPIRED';
  ownerId: number;
  createdAt: string;
  trialEndDate?: string;
}

export interface TrialStatus {
  isActive: boolean;
  daysRemaining: number;
  trialEndDate: string;
}
```

### API Response Type (`types/api.ts`)

```typescript
export interface ApiResponse<T> {
  data: T;
  message?: string;
  timestamp: string;
}
```

## API Integration

### Endpoints Used
```typescript
endpoints.instances.byOwner(ownerId)
// GET /api/instances/owner/{ownerId}

endpoints.instances.byId(id)
// GET /api/instances/{id}

endpoints.instances.trialStatus(instanceId)
// GET /api/instances/{instanceId}/trial-status
```

### Response Format
```json
{
  "data": [
    {
      "id": 1,
      "organizationName": "English Center ABC",
      "subdomain": "abc-center",
      "tier": "FREE",
      "status": "TRIAL",
      "ownerId": 123,
      "createdAt": "2026-03-15T10:30:00Z",
      "trialEndDate": "2026-03-29T10:30:00Z"
    }
  ],
  "timestamp": "2026-03-15T16:47:58Z"
}
```

## Routing

### Protected Routes
All routes under `(customer)` require authentication:
- `/dashboard` - Main dashboard
- `/instances/{id}` - Instance detail
- `/billing` - Subscription management
- `/settings` - Account settings

### Layout
Uses `DashboardLayout` with:
- Sidebar navigation
- User menu
- Logout button

## State Management

### Auth State (Zustand)
Dashboard reads current user from auth store:

```typescript
const { user } = useAuthStore();
// user.id → fetch instances
// user.name → display greeting
// user.role → check permissions
```

### Server State (React Query)
Instances fetched with React Query:
- Auto-caching (reduces API calls)
- Background refetching (keeps data fresh)
- Loading/error states (automatic)

## User Flows

### First-Time User
1. Login → redirect to `/dashboard`
2. Empty state displayed
3. Click "Tạo instance mới"
4. Redirected to registration/setup flow

### Returning User
1. Login → redirect to `/dashboard`
2. Instance grid loads
3. View instance cards
4. Click instance → navigate to detail page

### Trial User
1. Dashboard shows TRIAL badges
2. Trial countdown visible
3. Upgrade prompts (future)
4. Auto-suspend on expiry (backend)

## Styling

### Card Design
```tsx
<div className="rounded-lg border bg-card p-6 shadow-sm hover:shadow-md">
  {/* Instance content */}
</div>
```

**Features:**
- Rounded corners
- Subtle shadow
- Hover elevation
- Padding for content

### Status Badges
```tsx
<span className="rounded bg-blue-100 px-2 py-0.5 text-xs text-blue-700">
  TRIAL
</span>
```

**Color Mapping:**
- Blue: TRIAL (informational)
- Green: ACTIVE (success)
- Orange: SUSPENDED (warning)
- Red: EXPIRED (error)

### Grid Responsiveness
```tsx
<div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
  {/* Cards */}
</div>
```

## Utilities (`lib/utils.ts`)

### formatDate(dateString)
Formats ISO date to Vietnamese locale.

```typescript
formatDate('2026-03-15T10:30:00Z')
// → "15/03/2026"
```

**Implementation:**
```typescript
export function formatDate(date: string): string {
  return new Date(date).toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
}
```

## Error Handling

### API Errors
```typescript
if (error) {
  return (
    <ErrorAlert
      message="Không thể tải danh sách instance"
      onRetry={() => refetch()}
    />
  );
}
```

**Handled Cases:**
- Network errors
- 401 Unauthorized (auto-redirects to login)
- 403 Forbidden
- 500 Server errors

### Loading States
```typescript
if (isLoading) {
  return <LoadingSpinner className="mt-12" />;
}
```

### Empty States
```typescript
if (instances?.length === 0) {
  return <EmptyState {...props} />;
}
```

## Performance

### React Query Optimizations
- **Stale Time**: 5 minutes (default)
- **Cache Time**: 10 minutes (default)
- **Refetch on Window Focus**: Yes (stays fresh)
- **Deduplicated Requests**: Auto-handled

### Code Splitting
- Next.js automatically splits routes
- Dashboard bundle separate from auth
- Components lazy-loaded (future)

## Accessibility

### Current Implementation
- Semantic HTML (h1, p, div)
- Focus states (focus:ring-2)
- Keyboard navigation (link cards)

### Future Enhancements
- [ ] ARIA labels for status badges
- [ ] Skip to content link
- [ ] Screen reader announcements
- [ ] High contrast mode

## Testing Checklist

- [x] Dashboard loads for authenticated user
- [x] Instance grid displays correctly
- [x] Status badges show correct colors
- [x] Loading spinner appears during fetch
- [x] Error alert displays on API failure
- [x] Empty state shows for new users
- [x] Trial countdown displays for TRIAL instances
- [x] Click instance navigates to detail
- [x] Responsive grid on mobile/tablet/desktop
- [x] Vietnamese date formatting

## Future Enhancements

### Dashboard
- [ ] Quick stats (total students, active courses)
- [ ] Recent activity feed
- [ ] Announcements/notifications
- [ ] Shortcuts to common actions

### Instance Management
- [ ] Create instance flow
- [ ] Edit instance settings
- [ ] Transfer ownership
- [ ] Delete instance (with confirmation)
- [ ] Instance analytics

### Trial Management
- [ ] Trial extension requests
- [ ] Upgrade prompts
- [ ] Feature comparison
- [ ] Upgrade flow integration

### Components
- [ ] DataTable for advanced filtering
- [ ] Charts for analytics
- [ ] Modal dialogs
- [ ] Toast notifications

## Related PRs
- **PR 5.1**: Project Setup & Infrastructure (API client, auth store, layouts)
- **PR 5.2**: Marketing Pages & Auth Forms (login flow before dashboard)
- **PR 5.4**: Subscription & Billing Management (upgrade/downgrade flows)

## Dependencies

All components use existing dependencies from PR 5.1:
- `@tanstack/react-query` - Server state
- `zustand` - Client state
- `tailwindcss` - Styling
- No new packages added

## Migration Notes

### From Mock Data
If previously using mock data:
```typescript
// Before
const instances = mockInstances;

// After
const { data: instances } = useOwnerInstances(user?.id);
```

### Type Changes
Ensure backend returns:
- `id` as number (not string)
- `tier` as enum string
- `status` as enum string
- `createdAt` as ISO 8601 string
