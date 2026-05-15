"""
Unit tests for ec2-cost-report Lambda handler — digest formatting only.

We do NOT mock boto3 here (network-dependent calls live behind clients);
this file focuses on the deterministic pure-function pieces:
  - recommend(cpu, mem) — recommendation labels
  - format_digest(...)  — HTML/subject shape

Run locally:
    cd infrastructure/terraform-aws/lambdas/ec2-cost-report
    pip install -r requirements.txt pytest
    pytest test_handler.py -v
"""

from __future__ import annotations

import os
from decimal import Decimal

# Set env vars required by handler module import-time
os.environ.setdefault("SNS_TOPIC_ARN", "arn:aws:sns:ap-southeast-1:000000000000:test")
os.environ.setdefault("AWS_REGION_CE", "us-east-1")
os.environ.setdefault("PROJECT_NAME", "kitehub")
os.environ.setdefault("ENVIRONMENT", "test")

import handler  # noqa: E402


# ---------- recommend() ----------

def test_recommend_downsize_low_cpu():
    assert handler.recommend(cpu=10.0, mem=50.0) == "Downsize candidate"


def test_recommend_ok_mid_cpu():
    assert handler.recommend(cpu=35.0, mem=50.0) == "OK"


def test_recommend_upsize_high_cpu():
    assert handler.recommend(cpu=75.0, mem=50.0) == "Upsize candidate (CPU pressure)"


def test_recommend_upsize_memory_pressure_overrides_low_cpu():
    """Memory pressure overrides low-CPU downsize signal."""
    assert handler.recommend(cpu=10.0, mem=90.0) == "Upsize candidate (memory pressure)"


def test_recommend_insufficient_data_when_cpu_none():
    assert handler.recommend(cpu=None, mem=50.0) == "Insufficient data"


def test_recommend_mem_none_treated_as_no_memory_signal():
    """Mem None → fall through to CPU-based recommendation."""
    assert handler.recommend(cpu=5.0, mem=None) == "Downsize candidate"


def test_recommend_boundary_cpu_20():
    """CPU exactly 20 is NOT downsize (strict less-than)."""
    assert handler.recommend(cpu=20.0, mem=50.0) == "OK"


def test_recommend_boundary_cpu_60():
    """CPU exactly 60 is NOT upsize (strict greater-than)."""
    assert handler.recommend(cpu=60.0, mem=50.0) == "OK"


# ---------- format_digest() ----------

def test_format_digest_subject_length_under_99():
    subject, _ = handler.format_digest(
        period_label="2026-04",
        instances=[],
        cost_by_type={},
    )
    # SNS subject hard cap = 100 chars; we publish[:99]
    assert len(subject) < 100
    assert "kitehub" in subject
    assert "2026-04" in subject


def test_format_digest_includes_table_header():
    _, html = handler.format_digest(
        period_label="2026-04",
        instances=[],
        cost_by_type={},
    )
    assert "<th>Instance</th>" in html
    assert "<th>Type</th>" in html
    assert "<th>Cost (last month)</th>" in html
    assert "<th>Recommendation</th>" in html


def test_format_digest_empty_instances_shows_fallback_row():
    _, html = handler.format_digest(
        period_label="2026-04",
        instances=[],
        cost_by_type={},
    )
    assert "No EC2 instances tagged Project=Kite found" in html


def test_format_digest_renders_instance_row_with_cost_and_recommendation():
    _, html = handler.format_digest(
        period_label="2026-04",
        instances=[
            {
                "instance_id": "i-abc123",
                "name": "kitehub-kh-backend",
                "instance_type": "t3.medium",
                "state": "running",
                "avg_cpu": 8.5,
                "avg_mem": 42.0,
                "recommendation": "Downsize candidate",
            }
        ],
        cost_by_type={"t3.medium": Decimal("27.45")},
    )
    assert "kitehub-kh-backend" in html
    assert "t3.medium" in html
    assert "$27.45" in html
    assert "8.5%" in html
    assert "42.0%" in html
    assert "Downsize candidate" in html


def test_format_digest_handles_missing_cost_data():
    _, html = handler.format_digest(
        period_label="2026-04",
        instances=[
            {
                "instance_id": "i-xyz789",
                "name": "kitehub-kc-app-fe",
                "instance_type": "t3.small",
                "state": "running",
                "avg_cpu": 15.0,
                "avg_mem": None,
                "recommendation": "Downsize candidate",
            }
        ],
        cost_by_type={},  # Cost Explorer call failed
    )
    assert "n/a" in html  # cost cell + mem cell
    assert "kitehub-kc-app-fe" in html


def test_format_digest_thresholds_documented_in_footer():
    _, html = handler.format_digest(
        period_label="2026-04",
        instances=[],
        cost_by_type={},
    )
    # Document thresholds for human reader
    assert "20" in html  # CPU downsize threshold
    assert "60" in html  # CPU upsize threshold
    assert "85" in html  # Mem upsize threshold
    assert "ec2-cost-review.md" in html  # runbook pointer
