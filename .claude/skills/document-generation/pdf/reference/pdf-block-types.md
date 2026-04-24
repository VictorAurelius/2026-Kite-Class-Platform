# PDF Block Types — Reusable Layout Primitives

Compose a PDF page by stacking these blocks. Each block has a purpose + the HTML/CSS idiom that survives OpenHTMLtoPDF's renderer.

| # | Block | Purpose | Key CSS |
|:-:|-------|---------|---------|
| 1 | **Title bar** | Page-top title (H1 + subtitle) | `h1 { text-align: center; font-size: 18pt; }` |
| 2 | **Meta table** | Key-value block (invoice meta, recipient info) | `table.meta td.label { font-weight: 700; width: 35%; }` |
| 3 | **Line items** | Bordered item table (invoice, order) | `table.items { border-collapse: collapse; } th, td { border: 1px solid #999; }` |
| 4 | **Totals box** | Right-aligned totals with grand-total rule | `.totals { width: 40%; margin-left: auto; } tr.grand td { border-top: 2px solid #000; }` |
| 5 | **Signature block** | Two-column signer rows (tenant / counterparty) | `display: table; width: 100%; .sign-col { display: table-cell; width: 50%; }` |
| 6 | **Address block** | Multi-line postal address | `.address { line-height: 1.4; white-space: pre-line; }` |
| 7 | **Brand header** | Logo + tenant name + brand color bar | `<img> + tinted background band via background-color` |
| 8 | **Footer strip** | Page-foot attribution, legal, page number | `@page { @bottom-center { content: counter(page); } }` |
| 9 | **Barcode placeholder** | Area reserved for barcode/QR | `<div class="barcode">...</div>`, image injected at render time |
| 10 | **Stamp slot** | Reserved empty area for hand-applied stamps | `.stamp { width: 4cm; height: 4cm; border: 1px dashed #999; }` |
| 11 | **VAT summary** | VN-specific VAT break-out table (Circular 78) | 4-row table: subtotal, VAT rate, VAT amount, total |
| 12 | **Page break** | Explicit new page | `.page-break { page-break-before: always; }` |
| 13 | **Rule line** | Section separator | `<hr />` or `border-top: 1px solid #000;` |
| 14 | **Pull quote** | Emphasized standalone text | `font-size: 14pt; font-style: italic; border-left: 4px solid #brand;` |
| 15 | **KPI tile row** | 2×2 or 3×1 metric tile layout | CSS grid emulation via table rows (CSS grid only partial in OpenHTMLtoPDF) |
| 16 | **Photo + caption** | Single image with text below | `figure { text-align: center; } figcaption { font-size: 9pt; color: #666; }` |
| 17 | **Timeline row** | Horizontal milestones | Table row with date cells; line via `border-top` on td |
| 18 | **Terms list** | Numbered / bulleted policy items | `ol.terms li { margin-bottom: 6pt; }` |
| 19 | **Disclaimer footer** | Fine-print legal note | `p.disclaimer { font-size: 8pt; color: #666; }` |
| 20 | **Watermark overlay** | Draft/sample watermark | Rendered as absolute-positioned rotated text with low opacity |

**Gotchas to respect when composing blocks:**

- OpenHTMLtoPDF supports a **subset** of CSS 2.1 + a few CSS 3 properties; no flex, limited grid. Use tables for structured layout.
- Font family must map to a registered TTF (see SKILL.md gotchas).
- Avoid HTML entities outside XHTML core — use Unicode directly.
