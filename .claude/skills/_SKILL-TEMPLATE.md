# Skill Scaffold Template (companion to `skill-conventions.md`)

> Template tham khảo để tạo skill mới — KHÔNG phải skill thật (không có `name:` frontmatter ở file này để CI `check-skill-conventions.sh` không đếm vào drift). Quy ước chính thức: [`.claude/rules/skill-conventions.md`](../rules/skill-conventions.md). Index: [`_README-skills-index.md`](_README-skills-index.md).

## Nguyên tắc (per `skill-conventions.md`)

- **Skill là FOLDER, không phải file** — entry point `SKILL.md` (<100 dòng); detail/table/example → `reference/`; scripts → `scripts/`.
  - Ngoại lệ: skill rất đơn giản (1 bước) có thể là single `.md` với frontmatter.
- **Progressive disclosure 3 lớp:** metadata (name+description, ~100 token, load lúc startup) → SKILL.md body (200-500 token, khi activate) → reference (khi cần).
- **`description` = trigger condition CHO MODEL** (không phải cho người): liệt kê khi nào dùng — "Dùng khi user nói '...', '...', hoặc khi <điều kiện>".
- **Đừng dạy điều Claude đã biết** — chỉ viết project-specific knowledge + gotchas.

## Cấu trúc folder

```
.claude/skills/<category>/<skill-name>/
├── SKILL.md              ← Entry point (<100 dòng)
├── reference/            ← Detail tables, examples, rubric (load on-demand)
│   └── <topic>.md
├── scripts/              ← Executable helpers (nếu có)
└── data/                 ← Configs, fixtures (nếu có)
```

## Scaffold `SKILL.md` (copy ra `<skill-name>/SKILL.md`)

```
---
name: <skill-name>                     # = tên folder, kebab-case
description: "Dùng khi user nói '<từ khóa 1>', '<từ khóa 2>', hoặc khi <điều kiện trigger cụ thể>. <1 câu skill làm gì>."
user-invocable: true                   # true nếu user gọi qua /<skill-name>; bỏ nếu chỉ model auto-trigger
---

# /<skill-name> — <Tên ngắn>

<1-2 câu: skill này làm gì + khi nào dùng.>

## Quy trình

1. <Bước 1 — hành động cụ thể>
2. <Bước 2>
3. <Output: skill trả về gì>

## Tham chiếu

- Detail / rubric / examples → `reference/<topic>.md`
- Rule liên quan: `<rule>.md`

## Gotchas

- <Cạm bẫy project-specific 1 — điều dễ sai>
- <Gotcha 2>
```

## Sau khi tạo skill (Enforcement Parity)

1. Thêm 1 dòng vào [`_README-skills-index.md`](_README-skills-index.md) (per `skill-conventions.md` — CI `check-skill-conventions.sh` drift-check đếm SKILL.md vs index rows).
2. Nếu skill là output review-standard mới → thêm row `output-review-mandate.md` §3.
3. `SKILL.md` body giữ <100 dòng; chuyển detail sang `reference/`.
