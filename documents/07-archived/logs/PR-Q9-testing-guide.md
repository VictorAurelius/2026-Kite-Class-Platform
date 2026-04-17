# PR-Q9: AI Branding with Ollama - Testing Guide

## Automated Tests

### Backend Tests (Java)

**Location:** `kitehub/kitehub-branding/src/test/java/`

```bash
# Run all tests
cd kitehub
./mvnw -f kitehub-branding/pom.xml test

# Run specific tests
./mvnw -f kitehub-branding/pom.xml test -Dtest=ColorUtilsTest
./mvnw -f kitehub-branding/pom.xml test -Dtest=ThemeGenerationServiceTest
```

**Test Coverage:**
- ✅ ColorUtils: 10 tests - HSL color transformations (lighten/darken)
- ✅ ThemeGenerationService: 8 tests - Theme config generation from LogoAnalysis
- ✅ OllamaClient: 4 tests - Color type validation and JSON serialization

**Total: 22 tests**

### Frontend Tests (TypeScript/Vitest)

**Location:** `kitehub/kitehub-frontend/src/`

```bash
# Run all tests
cd kitehub/kitehub-frontend
npm test -- --run

# Run specific tests
npm test -- --run ThemePreviewCard
npm test -- --run use-theme-generation
```

**Test Coverage:**
- ✅ ThemePreviewCard: 7 tests - Component rendering with mock data
- ✅ useThemeGeneration: 5 tests - API calls, state management, error handling

**Total: 12 tests**

---

## Manual Integration Testing

### Prerequisites

1. **Ollama Infrastructure Running:**
   ```bash
   ./scripts/up.sh --profile ai-local
   docker ps | grep ollama  # Should show healthy
   ```

2. **Models Pulled:**
   ```bash
   docker exec kitehub-ollama ollama list
   # Should show: llama3.1:8b, llava:13b
   ```

3. **Services Running:**
   ```bash
   docker-compose -f docker-compose.kitehub.yml ps
   # All services should be "Up" or "healthy"
   ```

### Test Flow: End-to-End

#### 1. Logo Upload & Analysis

**URL:** `http://localhost:3001/branding/wizard`

**Steps:**
1. Upload a logo image (PNG/JPG)
2. Wait for AI analysis (~10-30s)
3. Verify analysis results:
   - ✅ Single color values (not arrays)
   - ✅ `primaryColor`, `secondaryColor`, `accentColor` present
   - ✅ Theme enum: MODERN, CLASSIC, PLAYFUL, or MINIMAL
   - ✅ Brand personality traits array

**API Call:**
```http
POST /api/platform/branding/ai/analyze-logo
{
  "logoUrl": "https://...",
  "organizationName": "Test Org"
}
```

**Expected Response:**
```json
{
  "primaryColor": "#2196F3",
  "secondaryColor": "#FF5722",
  "accentColor": "#4CAF50",
  "theme": "MODERN",
  "typography": "Clean Sans-serif",
  "targetAudience": "...",
  "brandPersonality": ["Professional", "Friendly"]
}
```

#### 2. Theme Config Generation

**Step:** In wizard, proceed to Review step (step 4)

**Expected Behavior:**
- Theme generation automatically triggers
- Loading spinner shows "Đang tạo theme configuration..."
- Theme Preview Card appears with:
  - ✅ Color palette (Primary, Secondary, Accent variants 50-900)
  - ✅ Neutral grays (50-900)
  - ✅ Semantic colors (Success, Warning, Error, Info)
  - ✅ Typography preview (heading/body fonts)
  - ✅ Spacing & Layout info

**API Call:**
```http
POST /api/platform/branding/ai/generate-theme
{
  "primaryColor": "#2196F3",
  "secondaryColor": "#FF5722",
  "accentColor": "#4CAF50",
  "theme": "MODERN",
  ...
}
```

**Expected Response:**
```json
{
  "colors": {
    "primary": {
      "shade50": "#E3F2FD",
      "shade500": "#2196F3",
      "shade900": "#0D47A1"
    },
    ...
  },
  "typography": {
    "fontFamilyHeading": "'Inter', ...",
    "fontSizes": { ... },
    ...
  },
  "spacing": { ... },
  "layout": { ... }
}
```

#### 3. Theme Storage in KiteClass

**API Endpoint:** `PUT /api/v1/settings/branding`

**Test:**
```bash
curl -X PUT http://localhost:8088/api/v1/settings/branding \
  -H "Content-Type: application/json" \
  -d '{
    "displayName": "Test Org",
    "primaryColor": "#2196F3",
    "secondaryColor": "#FF5722",
    "accentColor": "#4CAF50",
    "themeConfigJson": "{\"colors\":{...}}"
  }'
```

**Verify Storage:**
```bash
# Check database
docker exec kitehub-postgres psql -U kitehub -d kiteclass_shared \
  -c "SELECT theme_config_json FROM branding LIMIT 1;"
```

**Verify Retrieval:**
```http
GET /api/v1/settings/branding/theme
```

---

## Edge Cases to Test

### 1. Invalid Colors
- Upload logo with unusual colors (very dark, very light)
- Verify color transformations don't break (no #GGGGGG)

### 2. API Errors
- Stop Ollama: `docker stop kitehub-ollama`
- Upload logo → Should see error message
- Restart: `docker start kitehub-ollama`

### 3. Theme Variants
Test all 4 theme types produce different configs:
- MODERN → Sans-serif heading, 4px spacing unit
- CLASSIC → Serif heading, 4px spacing unit
- PLAYFUL → Sans-serif heading, larger border radius
- MINIMAL → Sans-serif heading, 8px spacing unit

### 4. Color Variant Generation
- Verify shade50 is lightest, shade900 is darkest
- Verify shade500 matches base color (or very close)
- Verify all shades are valid hex codes

---

## Performance Benchmarks

### AI Processing Times (approximate)

- **Logo Analysis:** 10-30s (depends on Ollama model warm-up)
- **Theme Generation:** <1s (pure computation, no AI)
- **Theme Config API:** <100ms (JSON serialization)

### Resource Usage

- **Ollama Models:**
  - llama3.1:8b: ~4.9 GB disk
  - llava:13b: ~8.0 GB disk
- **Memory:** ~2-4 GB during inference

---

## Troubleshooting

### Ollama Not Responding

```bash
# Check health
curl http://localhost:11434/api/tags

# Check logs
docker logs kitehub-ollama --tail 50

# Restart
docker-compose -f docker-compose.kitehub.yml --profile ai-local restart kitehub-ollama
```

### Theme Generation Fails

**Common Issues:**
1. Backend not running → Start `kitehub-branding` service
2. Invalid LogoAnalysis JSON → Check field names match (primaryColor, not primaryColors)
3. Color transformation error → Check ColorUtils handles edge cases

### Frontend Not Showing Preview

**Debug Steps:**
1. Open browser DevTools → Network tab
2. Check POST `/api/platform/branding/ai/generate-theme` request
3. Verify response is valid ThemeConfig JSON
4. Check Console for React errors

---

## Success Criteria

✅ All automated tests pass (22 backend + 12 frontend = 34 tests)
✅ Logo upload → analysis works end-to-end
✅ Theme preview shows complete config (colors, typography, spacing, layout)
✅ Theme config saves to KiteClass database
✅ Theme config retrieves via API
✅ No console errors in browser
✅ No exceptions in backend logs

---

## Related Documentation

- Backend API: `kitehub/kitehub-branding/src/main/java/com/kitehub/branding/controller/AIBrandingController.java`
- Frontend Hook: `kitehub/kitehub-frontend/src/hooks/use-theme-generation.ts`
- Theme Types: `kitehub/kitehub-frontend/src/types/theme.ts`
- Color Utils: `kitehub/kitehub-branding/src/main/java/com/kitehub/branding/util/ColorUtils.java`
