# <Topic> <Action> Runbook — <one-line scope>

<!--
  TEMPLATE — copy file này khi viết runbook mới, rồi xóa block comment này.

  ĐẶT ĐÚNG FOLDER (per deployment-naming-convention.md §2 — hỏi "chạy khi nào? bao lâu một lần?"):
    - account-prep/   : 1 lần / tài khoản vendor (đăng ký AWS / domain / SePay / Zalo / Resend)
    - deploy/         : 1 lần / release (DNS setup, secrets seeding, bootstrap, cutover)
    - operations/     : lặp (incident response, rotation định kỳ, per-alert)
    - operations/runbooks/ : 1 runbook / 1 alert cụ thể

  NAMING (per §3):
    - `<topic>-<action>-runbook.md`  (quy trình có nhánh quyết định)
    - `<topic>-<action>-procedure.md` (quy trình tuyến tính 1 mạch)

  NGÔN NGỮ (per dev-readable-doc-language.md): narrative tiếng Việt + identifier English.
-->

**Audience:** <ai đọc — vd "Solo dev đăng ký vendor X lần đầu">
**Standards:** `release-deploy-standard.md` §3.4 · `dev-readable-doc-language.md` §2 · `deployment-naming-convention.md` §2
**Cross-link upstream:** <prerequisite phải xong trước — vd domain verified, account active>
**Cross-link downstream:** <runbook/code/feature mà runbook này unblock>
**Estimated time:** <~N phút/giờ>
**Last-Updated:** YYYY-MM-DD

---

## TL;DR

<3-5 dòng: runbook này làm gì + cần làm mấy việc chính. Nếu là vendor/infra prep, ghi rõ phần nào là real-user action (KYC/browser) vs phần nào code đã sẵn.>

---

## 1. Trước khi bắt đầu — chuẩn bị

| Cần có | Ghi chú |
|---|---|
| <prerequisite 1> | <chi tiết> |
| <prerequisite 2> | <chi tiết> |

<Nếu có blocker (vd AWS suspended GAP-612) → ghi rõ bước nào defer.>

---

## 2. <Tên bước nhóm 1>

### 2.1 <Bước con>

1. <hành động cụ thể — có thể paste lệnh / URL>
2. <hành động tiếp>

```bash
# Lệnh kèm comment giải thích — kỳ vọng output gì
<command>
```

### 2.2 <Bước con>

...

---

## 3. <Tên bước nhóm 2 — vd cấu hình / set secret>

<Nếu set AWS secret: ghi rõ secret name (kitehub/production/<name>), schema payload, và lệnh put-secret-value HOẶC bước console.>

---

## 4. Verify

1. <bước verify happy-path — kèm kỳ vọng cụ thể: HTTP 200, DB row, log line>
2. <verify side-effect nếu có>

### Sad path (lỗi thường gặp)

| Triệu chứng | Nguyên nhân | Xử lý |
|---|---|---|
| <lỗi> | <root cause> | <fix> |

---

## 5. Liên quan

- Code: `<service/path>` — <class/method liên quan>
- Secret / IaC: `infrastructure/terraform-aws/<file>.tf` · `scripts/fetch-secrets.sh`
- Gap: GAP-NNN · Sister runbook: `<...>-runbook.md`
