---
audience: dev
title: Prompt sinh ảnh demo landing — Sky Education
created: 2026-05-29
status: working-note
---

# Prompt sinh ảnh demo landing — Sky Education

Dùng trong giao diện **ChatGPT (Plus)** — image-gen GPT-4o / DALL·E. ChatGPT Plus không kèm API free, nên sinh ảnh qua UI rồi tải về đưa Claude seed + wire FE.

Palette Sky Education: navy `#1B4965` · cam `#E8590C` · hổ phách `#FFB703` · tagline "Chắp cánh tương lai Anh ngữ".

---

## 1. Ảnh chân dung giáo viên (Đỗ Lan Khánh — môn Pháp Luật và Đời Sống)

**Cách dùng:** upload `documents/asset.png` rồi dán prompt:

```
Edit the attached photo into a clean, professional teacher headshot for a website
"Our Teachers" card.
- Keep the woman's face, identity, hairstyle and likeness exactly the same — do not
  change her facial features, age, or expression.
- Replace the busy background with a soft, slightly blurred warm neutral studio
  background (subtle navy-to-amber gradient is welcome).
- Professional soft studio lighting, natural skin tones, friendly approachable look.
- Square 1:1 framing, head and shoulders centered, output at least 1024x1024,
  photorealistic.
- No text, no logo, no watermark.
```

⚠️ Sau khi tạo: **soi kỹ mặt có còn giống cô không** — image-gen đôi khi vẽ lại làm méo nhận diện. Nếu lệch → bảo nó: `keep the exact same face, only change the background and lighting`.

> Lưu ý: nếu chỉ cần "sạch nền + chuyên nghiệp" mà giữ đúng mặt, đôi khi giữ ảnh crop hiện tại (`teacher-do-lan-khanh.webp`) hoặc chỉ nhờ thay nền an toàn hơn là regenerate cả mặt.

---

## 2. Hero banner Sky Education

**Cách dùng:** text-to-image, không cần upload:

```
Create a wide cinematic hero banner (aspect ratio 16:9) for an English language
learning center called "Sky Education".
- Modern bright classroom, diverse Vietnamese students learning English, warm
  welcoming atmosphere.
- Color palette: deep navy blue (#1B4965), vibrant orange (#E8590C), amber (#FFB703)
  highlights.
- Clean professional editorial photography, soft natural daylight, shallow depth of
  field.
- No text, no watermark, no logo. High resolution.
```

---

## Spec khi tải về đưa Claude

| Asset | Yêu cầu | Đặt tại |
|---|---|---|
| Ảnh GV | Vuông ≥1024×1024, PNG/JPG/WebP, không chữ | `documents/teacher-khanh.png` |
| Hero | Ngang ~16:9 (≥1600px rộng), PNG/JPG, **không chữ** | `documents/hero-sky.png` |

Tải xong → báo Claude → seed (static `public/demo/sky/`) + wire FE (hero → HeroSection, teacher → TeachersSection).

---

## Lý do không dùng API tự sinh (state-check 2026-05-29)

- **OpenAI API**: image-gen chỉ tài khoản trả phí có credit; ChatGPT Plus ≠ API access.
- **Gemini/Imagen API**: free tier `limit: 0` cho image gen (`gemini-*-flash-image` RESOURCE_EXHAUSTED); Imagen 4 paid-only. Cần bật billing.
- **Pollinations (FLUX-schnell, free, keyless)**: dùng được nhưng chất lượng chưa đạt chuẩn (đã thử).
- **AI Branding nội bộ**: `OpenAIClient.generateImage` (DALL-E 3) có code nhưng processor stub (`hero = logoUrl`) + mock mode mặc định; bản thật defer Phase 2 (GAP-003).

→ Phương án hiện tại: sinh ảnh qua **ChatGPT Plus UI** (bytes do user tạo) + Claude seed/wire. Không wire DALL-E vào processor (giữ Phase 1).
