#!/usr/bin/env python3
"""Generate Vietnamese teacher names CSV (100 rows).

Schema: first_name,last_name,full_name,gender,region,specialty
"""
import csv
import random
from pathlib import Path

random.seed(20260518)

FAMILY_NAMES = [
    "Trần", "Nguyễn", "Lê", "Phạm", "Hoàng", "Huỳnh", "Vũ", "Đỗ", "Bùi", "Phan",
    "Dương", "Đặng", "Ngô", "Lý",
]
F_MIDDLE = ["Thị", "Thị", "Ngọc", "Thúy", "Kim"]
M_MIDDLE = ["Văn", "Văn", "Đức", "Quốc", "Minh"]
F_GIVEN = ["Hồng", "Mai", "Lan", "Hương", "Linh", "Thảo", "Trang", "An", "Bích", "Hà",
           "Hằng", "Huyền", "Loan", "Ngọc", "Nhung", "Phương", "Quỳnh", "Tâm", "Thanh",
           "Thu", "Trâm", "Vân", "Yến", "Diệu"]
M_GIVEN = ["An", "Bình", "Cường", "Dũng", "Đức", "Hải", "Hùng", "Khánh", "Long",
           "Minh", "Nam", "Phong", "Quang", "Sơn", "Tâm", "Tài", "Thắng", "Thành",
           "Tiến", "Toàn", "Trung", "Tuấn", "Việt", "Vũ"]
REGIONS = ["Bắc", "Trung", "Nam"]
SPECIALTIES = ["Anh ngữ", "Toán", "Lý", "Hóa", "Văn", "Sử", "Địa", "Tin học",
               "Sinh học", "Tiếng Nhật", "Tiếng Hàn", "Tiếng Trung"]


def main():
    rows = set()
    while len(rows) < 100:
        gender = random.choice(["F", "M"])
        family = random.choice(FAMILY_NAMES)
        if gender == "F":
            middle = random.choice(F_MIDDLE)
            given = random.choice(F_GIVEN)
        else:
            middle = random.choice(M_MIDDLE)
            given = random.choice(M_GIVEN)
        region = random.choice(REGIONS)
        specialty = random.choice(SPECIALTIES)
        full_name = f"{family} {middle} {given}"
        rows.add((given, family, full_name, gender, region, specialty))

    rows = sorted(rows, key=lambda r: (r[5], r[3], r[1]))[:100]

    out = Path(__file__).resolve().parent.parent.parent / \
        "kitehub" / "kitehub-platform" / "src" / "main" / "resources" / \
        "seed-data" / "vn-friendly" / "teacher-names.csv"
    with open(out, "w", encoding="utf-8-sig", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["first_name", "last_name", "full_name", "gender", "region", "specialty"])
        writer.writerows(rows)
    print(f"Generated {len(rows)} teacher rows → {out}")


if __name__ == "__main__":
    main()
