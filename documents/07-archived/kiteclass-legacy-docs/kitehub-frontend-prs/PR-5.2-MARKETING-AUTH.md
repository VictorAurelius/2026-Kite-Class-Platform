# PR 5.2: Marketing Pages & Auth Forms

## Overview
Public-facing marketing pages and authentication flows including landing page, pricing, login, and registration.

## Features

### 1. Landing Page (`/(public)/page.tsx`)
Professional marketing page with Vietnamese content.

#### Sections
- **Hero Section**: Value proposition with CTA buttons
- **Features Grid**: 6 key features with emoji icons
- **Stats Section**: Social proof (500+ centers, 50K+ students)
- **CTA Section**: Free trial signup

#### Key Features Highlighted
- 👨‍🎓 Quản lý học viên - Student management
- 👨‍🏫 Quản lý giảng viên - Teacher management
- 📚 Khóa học & Lớp học - Courses & Classes
- ✅ Điểm danh tự động - Auto attendance
- 💳 Thanh toán & Hóa đơn - Payments & Invoicing
- 🎨 Landing page AI - AI branding

#### CTAs
- Primary: "Dùng thử miễn phí 14 ngày" → `/register`
- Secondary: "Xem bảng giá" → `/pricing`

### 2. Pricing Page (`/(public)/pricing/page.tsx`)
Tier comparison and plan selection.

#### Pricing Tiers
1. **FREE** - Miễn phí
   - 50 students max
   - 2 teachers max
   - 1GB storage
   - Basic features

2. **BASIC** - 199,000đ/tháng
   - 200 students
   - 5 teachers
   - 5GB storage
   - Full features

3. **PREMIUM** - 399,000đ/tháng
   - 500 students
   - Unlimited teachers
   - 20GB storage
   - Priority support

4. **ENTERPRISE** - Contact sales
   - Unlimited everything
   - Custom features
   - Dedicated support

#### Features
- Annual discount (2 months free)
- Vietnamese currency (VND)
- Contact sales for Enterprise
- Clear feature comparison

### 3. Login Page (`/(auth)/login/page.tsx`)
Secure authentication with form validation.

#### Features
- **Email/Password Auth**: Standard login flow
- **Form Validation**: Zod schema with React Hook Form
- **Error Handling**: User-friendly Vietnamese messages
- **Loading States**: Disabled button during submission
- **Auto Redirect**: ADMIN → `/admin`, OWNER → `/dashboard`
- **Register Link**: Signup CTA for new users

#### Validation Rules
```typescript
{
  email: string().email('Email không hợp lệ'),
  password: string().min(6, 'Mật khẩu phải có ít nhất 6 ký tự'),
}
```

#### User Flow
1. User enters email and password
2. Form validates on submit
3. API call to `/api/auth/login`
4. Store user + tokens in Zustand + localStorage
5. Redirect based on role (ADMIN vs OWNER)

### 4. Register Page (`/(auth)/register/page.tsx`)
New user signup with validation.

#### Form Fields
- **Name**: Minimum 2 characters
- **Email**: Valid email format
- **Password**: Minimum 6 characters
- **Confirm Password**: Must match password

#### Features
- **Password Confirmation**: Zod refine validation
- **Role Selection**: Auto-assigned as OWNER for new signups
- **Auto Login**: Logs in after successful registration
- **Error Handling**: Displays server errors (e.g., email exists)
- **Login Link**: CTA for existing users

#### Validation Schema
```typescript
registerSchema
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Mật khẩu xác nhận không khớp',
    path: ['confirmPassword'],
  })
```

#### User Flow
1. User fills registration form
2. Password confirmation validated
3. API call to `/api/auth/register`
4. Auto-login with returned tokens
5. Redirect to `/dashboard`

## Layouts

### Public Layout (`(public)/layout.tsx`)
- **Header**: Logo, navigation links
- **Footer**: Copyright, social links
- **No Auth Required**: Accessible to all visitors

### Auth Layout (`(auth)/layout.tsx`)
- **Centered Card**: Clean, focused UI
- **No Header/Footer**: Minimal distractions
- **Responsive**: Mobile-friendly forms

## Components

### Form Components
Built with native HTML + Tailwind for simplicity:
- Text inputs with validation
- Submit buttons with loading states
- Error messages below fields
- Links for navigation

### No Custom Components
Intentionally lightweight to demonstrate basic patterns before introducing Shadcn components.

## API Integration

### Endpoints Used
```typescript
endpoints.auth.login    // POST /api/auth/login
endpoints.auth.register // POST /api/auth/register
```

### Request Format
**Login:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Register:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123"
}
```

### Response Format
```json
{
  "data": {
    "user": {
      "id": 1,
      "email": "user@example.com",
      "name": "John Doe",
      "role": "OWNER"
    },
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
  }
}
```

## State Management

### Auth Flow
1. **Submit Form** → Call API
2. **Success Response** → Extract user + tokens
3. **Store in Zustand** → `setAuth(user, accessToken, refreshToken)`
4. **Store in LocalStorage** → Persist tokens
5. **Redirect** → Navigate to dashboard

### Token Storage
- **Zustand Store**: In-memory state for current session
- **LocalStorage**: Persists across page refreshes
- **Both Updated**: Ensures sync between store and storage

## Styling

### Design System
- **Tailwind Utility Classes**: No custom CSS
- **Responsive Grid**: Mobile-first approach
- **Consistent Spacing**: py-20 for sections, gap-4 for grids
- **Muted Colors**: text-muted-foreground for secondary text
- **Primary CTA**: bg-primary with hover states

### Color Semantics
- **Primary**: Main brand color for CTAs
- **Muted**: Secondary text and backgrounds
- **Destructive**: Error messages
- **Card**: Form containers with shadow

### Typography
- **Hero**: text-4xl sm:text-6xl (responsive)
- **Section Headers**: text-3xl font-bold
- **Body**: text-lg for hero, text-sm for forms
- **Muted**: text-muted-foreground for descriptions

## Vietnamese Content

### Marketing Copy
- **Tagline**: "Quản lý trung tâm giáo dục thông minh hơn"
- **Value Prop**: "Tạo và quản lý hệ thống KiteClass"
- **CTA**: "Dùng thử miễn phí 14 ngày"

### Form Labels
- Email, Mật khẩu, Tên
- Đăng nhập, Đăng ký
- Chưa có tài khoản? / Đã có tài khoản?

### Error Messages
- "Email không hợp lệ"
- "Mật khẩu phải có ít nhất 6 ký tự"
- "Mật khẩu xác nhận không khớp"
- "Email hoặc mật khẩu không đúng"

## Routing

### Public Routes
- `/` - Landing page
- `/pricing` - Pricing comparison

### Auth Routes
- `/login` - Login form
- `/register` - Registration form

### Protected Routes (Post-Auth)
- `/dashboard` - OWNER dashboard
- `/admin` - ADMIN dashboard

## Validation

### Client-Side
- **Zod Schemas**: Type-safe validation
- **React Hook Form**: Form state management
- **Instant Feedback**: Error messages on blur/submit

### Server-Side
- Backend validates all requests
- Returns error messages in Vietnamese
- 400 for validation errors, 401 for auth failures

## Security

### Password Requirements
- Minimum 6 characters (expandable to 8-12 later)
- No exposed passwords in state/logs
- Password inputs type="password"

### Token Handling
- JWT tokens stored securely
- No sensitive data in URL params
- Auto-refresh on 401 (handled by API client)

### CSRF Protection
- Not implemented yet (future enhancement)
- Currently using JWT Bearer tokens

## User Experience

### Loading States
```typescript
const [loading, setLoading] = useState(false);

<button disabled={loading}>
  {loading ? 'Đang đăng nhập...' : 'Đăng nhập'}
</button>
```

### Error Display
- Error banner at top of form
- Field-specific errors below inputs
- Vietnamese error messages
- Auto-clear on new submit

### Success Flow
- No success toast (direct redirect)
- Seamless transition to dashboard
- No loading page (instant navigation)

## Responsive Design

### Breakpoints
- **Mobile**: 1 column grid
- **Tablet (sm)**: 2 column grid
- **Desktop (lg)**: 3 column grid

### Hero Section
- Hero text: text-4xl → text-6xl on sm+
- Button stack: Stacks vertically on mobile

### Form Width
- Auth forms: max-w-md centered
- Full width on mobile, fixed on desktop

## Testing Checklist

- [x] Landing page displays correctly
- [x] Pricing page shows all 4 tiers
- [x] Login form validates email format
- [x] Login form validates password length
- [x] Login API call succeeds
- [x] Tokens stored in localStorage
- [x] User redirects based on role
- [x] Register form validates all fields
- [x] Password confirmation works
- [x] Register API creates user
- [x] Auto-login after registration
- [x] Error messages in Vietnamese
- [x] Loading states prevent double-submit
- [x] Mobile responsive layouts

## Performance

### Code Splitting
- Next.js automatically splits pages
- (public) and (auth) are separate bundles
- Client-side navigation (no full page reload)

### Image Optimization
- No images yet (emoji icons only)
- Future: Next.js Image component for logos/screenshots

### Bundle Size
- Minimal dependencies
- No heavy libraries on marketing pages
- Auth pages include react-hook-form + zod (~15KB gzipped)

## SEO (Future Enhancement)

### Not Implemented Yet
- [ ] Meta tags for landing page
- [ ] Open Graph tags
- [ ] Sitemap.xml
- [ ] Robots.txt
- [ ] Structured data (JSON-LD)

### Quick Wins
```tsx
// Future: Add to page.tsx
export const metadata = {
  title: 'KiteHub - Quản lý trung tâm giáo dục',
  description: '...',
};
```

## Accessibility

### Current Implementation
- Semantic HTML (form, label, input)
- Focus styles (focus:ring-2)
- Button disabled states
- Error messages associated with inputs

### Future Enhancements
- [ ] ARIA labels
- [ ] Keyboard navigation
- [ ] Screen reader testing
- [ ] Focus trap in modals

## Future Enhancements

### Marketing
- [ ] Testimonials section
- [ ] Feature deep-dive pages
- [ ] Blog/Resources
- [ ] Case studies

### Auth
- [ ] Social login (Google, Facebook)
- [ ] Forgot password flow
- [ ] Email verification
- [ ] Two-factor authentication
- [ ] Password strength meter
- [ ] Remember me checkbox

### UX
- [ ] Loading skeletons
- [ ] Success toasts
- [ ] Form autofill detection
- [ ] Password visibility toggle

## Related PRs
- **PR 5.1**: Project Setup & Infrastructure (API client, auth store)
- **PR 5.3**: Customer Dashboard & Instances (post-login experience)
- **PR 5.4**: Subscription & Billing Management (payment flows)
