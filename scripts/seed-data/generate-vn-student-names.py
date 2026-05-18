#!/usr/bin/env python3
"""
Generate Vietnamese student names CSV for kitehub-platform seed-data.

Output: kitehub/kitehub-platform/src/main/resources/seed-data/vn-friendly/student-names.csv

Schema: first_name,last_name,full_name,gender,region

Diversity:
- 10 common family names (Trần/Nguyễn/Lê/Phạm/Hoàng/Huỳnh/Vũ/Đỗ/Bùi/Phan)
- Middle names balanced by gender (Thị for F, Văn for M, mixed for both)
- Regional split: Bắc/Trung/Nam ~ 1/3 each
- Mix of given names (40+ unique each gender)

Total: 300 rows (must be ≥300 distinct per GAP-658 AC).
"""
import csv
import random
from pathlib import Path

# Set seed for reproducibility
random.seed(20260518)

FAMILY_NAMES = [
    "Trần", "Nguyễn", "Lê", "Phạm", "Hoàng",
    "Huỳnh", "Vũ", "Đỗ", "Bùi", "Phan",
    "Dương", "Đặng", "Ngô", "Lý", "Đào",
]

# Female middle names (more diverse)
F_MIDDLE_NAMES = ["Thị", "Thị", "Thị", "Ngọc", "Diệu", "Thúy", "Kim", "Thanh", "Mỹ"]

# Male middle names
M_MIDDLE_NAMES = ["Văn", "Văn", "Văn", "Đức", "Quốc", "Minh", "Anh", "Hữu", "Xuân"]

# Female given names (Vietnamese with diacritics)
F_GIVEN_NAMES = [
    "Hồng", "Mai", "Lan", "Hoa", "Hương", "Linh", "Thảo", "Trang", "Vy", "An",
    "Bích", "Châu", "Diệu", "Giang", "Hà", "Hằng", "Hạnh", "Huyền", "Kim", "Lệ",
    "Loan", "Mỹ", "Nga", "Ngọc", "Nhung", "Oanh", "Phương", "Quỳnh", "Tâm", "Thanh",
    "Thu", "Thủy", "Trâm", "Tú", "Uyên", "Vân", "Yến", "Anh", "Dung", "My",
    "Ý", "Như", "Nhi", "Hiền", "Trinh", "Tiên", "Tuyết", "Xuân",
]

# Male given names
M_GIVEN_NAMES = [
    "An", "Bình", "Cường", "Dũng", "Đức", "Đạt", "Giang", "Hải", "Hiếu", "Hùng",
    "Khải", "Khang", "Khánh", "Kiên", "Long", "Mạnh", "Minh", "Nam", "Phong", "Phúc",
    "Quang", "Quân", "Sang", "Sơn", "Tâm", "Tài", "Thắng", "Thành", "Thiện", "Tiến",
    "Toàn", "Trí", "Trung", "Tuấn", "Tùng", "Việt", "Vinh", "Vũ", "Bảo", "Đăng",
    "Hoàng", "Khoa", "Lâm", "Phát", "Quý", "Thái", "Thiên", "Vĩ",
]

# Regions
REGIONS = ["Bắc", "Trung", "Nam"]


def generate_rows(count: int) -> list[tuple[str, str, str, str, str]]:
    """Generate (first_name, last_name, full_name, gender, region) tuples."""
    rows = set()  # Use set to ensure diversity until we hit count
    attempts = 0
    max_attempts = count * 10

    while len(rows) < count and attempts < max_attempts:
        attempts += 1
        gender = random.choice(["F", "M"])
        family = random.choice(FAMILY_NAMES)
        if gender == "F":
            middle = random.choice(F_MIDDLE_NAMES)
            given = random.choice(F_GIVEN_NAMES)
        else:
            middle = random.choice(M_MIDDLE_NAMES)
            given = random.choice(M_GIVEN_NAMES)
        region = random.choice(REGIONS)
        full_name = f"{family} {middle} {given}"
        # first_name = given, last_name = family (Vietnamese convention)
        rows.add((given, family, full_name, gender, region))

    return sorted(rows, key=lambda r: (r[3], r[1], r[0]))  # sort gender, family, given


def main():
    out = Path(__file__).resolve().parent.parent.parent / \
        "kitehub" / "kitehub-platform" / "src" / "main" / "resources" / \
        "seed-data" / "vn-friendly" / "student-names.csv"
    out.parent.mkdir(parents=True, exist_ok=True)

    rows = generate_rows(305)  # generate a few extra to ensure ≥300 unique

    # Truncate to 300
    rows = rows[:300]
    assert len(rows) == 300, f"Expected 300 rows, got {len(rows)}"

    # Verify diversity: ≥80 unique full_names (sanity check)
    unique_names = {r[2] for r in rows}
    assert len(unique_names) >= 250, f"Insufficient diversity: only {len(unique_names)} unique names"

    # Write with UTF-8 BOM
    with open(out, "w", encoding="utf-8-sig", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["first_name", "last_name", "full_name", "gender", "region"])
        writer.writerows(rows)

    print(f"Generated {len(rows)} rows ({len(unique_names)} unique full_names) → {out}")


if __name__ == "__main__":
    main()
