# Skill: Escape Fix Loop (Thoát vòng lặp sửa lỗi)

Khi Claude rơi vào vòng lặp sửa lỗi liên tục mà không hội tụ, cần nhận diện và thoát ra.

## Trigger phrases

- "vòng lặp"
- "sửa mãi không xong"
- "fix loop"
- "lặp đi lặp lại"

---

## Dấu hiệu nhận biết vòng lặp

1. **3+ commits liên tiếp cùng vấn đề** - VD: fix null safety 5 lần
2. **Mỗi fix tạo ra lỗi mới** - Fix A gây lỗi B, fix B gây lỗi C
3. **Quay lại pattern cũ** - Đã thử giải pháp X, lại quay về X
4. **Code ngày càng phức tạp** - Thêm utility, wrapper, helper mà vẫn lỗi
5. **User phản hồi "vẫn chưa hết"** - Sau 3+ lần fix cùng loại lỗi

---

## Quy trình thoát vòng lặp

### Bước 1: DỪNG LẠI - Không fix thêm

```
⚠️ PHÁT HIỆN VÒNG LẶP
- Số lần fix: [X] commits
- Vấn đề: [mô tả]
- Pattern: Fix A → Lỗi B → Fix B → Lỗi C → ...
```

### Bước 2: XÁC ĐỊNH ĐIỂM REVERT

Tìm commit TRƯỚC KHI bắt đầu series fix lỗi:
```bash
git log --oneline -20
# Tìm commit cuối cùng TRƯỚC KHI bắt đầu fix loop
```

### Bước 3: REVERT CODE

```bash
# Revert về commit trước khi bắt đầu fix loop
git reset --hard <commit-hash>
```

### Bước 4: PHÂN TÍCH ROOT CAUSE

Trả lời các câu hỏi:
1. **Vấn đề gốc là gì?** (không phải symptom)
2. **Tại sao các fix trước không hoạt động?**
3. **Có cách tiếp cận khác không?**

### Bước 5: CẬP NHẬT SKILL/PROMPT

Thêm vào skill liên quan:
```markdown
## ⚠️ Cảnh báo - Tránh vòng lặp

**Vấn đề đã gặp:** [mô tả]
**Giải pháp SAI đã thử:** [liệt kê]
**Giải pháp ĐÚNG:** [mô tả]
```

### Bước 6: THỰC HIỆN LẠI VỚI APPROACH MỚI

---

## Ví dụ: Null Safety Loop

### Vấn đề gốc
Eclipse null analysis quá strict, không tương thích với Java/Spring conventions.

### Giải pháp SAI đã thử
1. Thêm `@NonNull` annotations → Lombok không hỗ trợ
2. Xóa Lombok, viết manual getters → Builder không có `@NonNull`
3. Xóa Builder → `Objects.requireNonNull()` không có `@NonNull` return
4. Tạo NullSafetyUtils → Local variables vẫn không tin
5. Annotated local variables → Vẫn còn warnings...

### Giải pháp ĐÚNG
**KHÔNG sử dụng Eclipse null analysis cho project này.**

Thay vào đó:
- Dùng `@Nullable` annotations cho documentation only
- Dùng `Objects.requireNonNull()` cho runtime checks
- Disable Eclipse null analysis warnings
- Hoặc chuyển sang IntelliJ với nullable analysis ít strict hơn

**Thêm vào `.vscode/settings.json`:**
```json
{
  "java.compile.nullAnalysis.mode": "disabled"
}
```

---

## Template cho User

Khi phát hiện vòng lặp, hỏi User:

```
Tôi đã phát hiện vòng lặp sửa lỗi:
- Đã thử [X] giải pháp cho [vấn đề]
- Mỗi fix tạo ra lỗi mới

Đề xuất:
1. Revert về commit [hash] (trước khi bắt đầu fix)
2. [Giải pháp thay thế - VD: disable feature, dùng tool khác]

User có muốn tiếp tục theo hướng này không?
```

---

## Actions

### Khi nhận diện vòng lặp
1. DỪNG ngay, không thêm code
2. Liệt kê các giải pháp đã thử
3. Đề xuất revert + approach mới
4. Chờ User confirm trước khi thực hiện

### Sau khi thoát vòng lặp
1. Cập nhật skill liên quan với "Cảnh báo tránh vòng lặp"
2. Document giải pháp đúng
3. Commit với message rõ ràng về việc revert và lý do
