"""
Monthly EC2 cost report Lambda — GAP-414 Wave 84 Bucket G.

Triggered by EventBridge cron `cron(0 8 1 * ? *)` (1st of month, 08:00 UTC).
Workflow:
  1. List EC2 instances tagged Project=Kite via ec2.describe_instances
  2. Cost Explorer (us-east-1 endpoint): last-month cost grouped by
     INSTANCE_TYPE filtered by tag Project=Kite
  3. CloudWatch Metrics: 30-day avg CPU + mem_used_percent (CWAgent) per instance
  4. Build HTML table digest with Downsize/OK recommendation per instance
  5. Publish to SNS topic kitehub-cost-alerts

Recommendation thresholds:
  - Avg CPU < 20% over 30 days  → "Downsize candidate"
  - Avg CPU 20-60%               → "OK"
  - Avg CPU > 60%                → "Upsize candidate"
  - Mem >85% (when CWAgent available) → "Upsize candidate (memory pressure)"

The Lambda uses two regional boto3 clients:
  - Default region (ap-southeast-1) for ec2, cloudwatch, sns
  - us-east-1 explicit for ce (Cost Explorer endpoint per AWS docs)
"""

from __future__ import annotations

import json
import logging
import os
from datetime import datetime, timedelta, timezone
from decimal import Decimal
from typing import Any

import boto3

logger = logging.getLogger()
logger.setLevel(logging.INFO)

SNS_TOPIC_ARN = os.environ["SNS_TOPIC_ARN"]
AWS_REGION_CE = os.environ.get("AWS_REGION_CE", "us-east-1")
PROJECT_NAME = os.environ.get("PROJECT_NAME", "kitehub")
ENVIRONMENT = os.environ.get("ENVIRONMENT", "production")
TAG_KEY = os.environ.get("TARGET_TAG_KEY", "Project")
TAG_VAL = os.environ.get("TARGET_TAG_VAL", "Kite")

# CPU recommendation thresholds (percent)
CPU_DOWNSIZE_THRESHOLD = 20.0
CPU_UPSIZE_THRESHOLD = 60.0
MEM_UPSIZE_THRESHOLD = 85.0


def list_kite_instances(ec2_client) -> list[dict[str, str]]:
    """Return [{instance_id, instance_type, name}] for instances tagged Project=Kite."""
    paginator = ec2_client.get_paginator("describe_instances")
    out: list[dict[str, str]] = []
    for page in paginator.paginate(
        Filters=[
            {"Name": f"tag:{TAG_KEY}", "Values": [TAG_VAL]},
            {"Name": "instance-state-name", "Values": ["running", "stopped"]},
        ]
    ):
        for reservation in page["Reservations"]:
            for inst in reservation["Instances"]:
                tags = {t["Key"]: t["Value"] for t in inst.get("Tags", [])}
                out.append(
                    {
                        "instance_id": inst["InstanceId"],
                        "instance_type": inst["InstanceType"],
                        "name": tags.get("Name", inst["InstanceId"]),
                        "state": inst["State"]["Name"],
                    }
                )
    return out


def get_monthly_cost_by_instance(ce_client, start: str, end: str) -> dict[str, Decimal]:
    """
    Cost Explorer GetCostAndUsage grouped by INSTANCE_TYPE filtered by Project=Kite.
    Returns dict {instance_type: monthly_cost_usd}.

    Note: Cost Explorer groups EC2 cost by instance type, not instance-id directly.
    The cost is allocated across all instances of the same type. For sub-$100/month
    cost projection, type-level granularity is sufficient.
    """
    try:
        resp = ce_client.get_cost_and_usage(
            TimePeriod={"Start": start, "End": end},
            Granularity="MONTHLY",
            Metrics=["UnblendedCost"],
            Filter={
                "And": [
                    {"Tags": {"Key": TAG_KEY, "Values": [TAG_VAL]}},
                    {"Dimensions": {"Key": "SERVICE", "Values": ["Amazon Elastic Compute Cloud - Compute"]}},
                ]
            },
            GroupBy=[{"Type": "DIMENSION", "Key": "INSTANCE_TYPE"}],
        )
    except Exception as exc:
        logger.warning("Cost Explorer call failed: %s — continuing without cost data", exc)
        return {}

    out: dict[str, Decimal] = {}
    for result in resp.get("ResultsByTime", []):
        for group in result.get("Groups", []):
            inst_type = group["Keys"][0] if group.get("Keys") else "unknown"
            amount = Decimal(group["Metrics"]["UnblendedCost"]["Amount"])
            out[inst_type] = out.get(inst_type, Decimal("0")) + amount
    return out


def get_avg_cpu(cw_client, instance_id: str, days: int = 30) -> float | None:
    """Average CPUUtilization over last N days. None if no data."""
    end = datetime.now(timezone.utc)
    start = end - timedelta(days=days)
    resp = cw_client.get_metric_statistics(
        Namespace="AWS/EC2",
        MetricName="CPUUtilization",
        Dimensions=[{"Name": "InstanceId", "Value": instance_id}],
        StartTime=start,
        EndTime=end,
        Period=86400,  # 1 day buckets
        Statistics=["Average"],
    )
    datapoints = resp.get("Datapoints", [])
    if not datapoints:
        return None
    total = sum(dp["Average"] for dp in datapoints)
    return round(total / len(datapoints), 2)


def get_avg_mem(cw_client, instance_id: str, days: int = 30) -> float | None:
    """
    Average mem_used_percent over last N days from CWAgent namespace.
    Returns None if CloudWatch agent not configured (which is the current state
    pre-Wave 81+; alarms in cloudwatch.tf stay in INSUFFICIENT_DATA until agent
    configured). See cloudwatch.tf header comment.
    """
    end = datetime.now(timezone.utc)
    start = end - timedelta(days=days)
    resp = cw_client.get_metric_statistics(
        Namespace="CWAgent",
        MetricName="mem_used_percent",
        Dimensions=[{"Name": "InstanceId", "Value": instance_id}],
        StartTime=start,
        EndTime=end,
        Period=86400,
        Statistics=["Average"],
    )
    datapoints = resp.get("Datapoints", [])
    if not datapoints:
        return None
    total = sum(dp["Average"] for dp in datapoints)
    return round(total / len(datapoints), 2)


def recommend(cpu: float | None, mem: float | None) -> str:
    """Combine CPU + memory signals into single recommendation label."""
    if cpu is None:
        return "Insufficient data"
    if mem is not None and mem > MEM_UPSIZE_THRESHOLD:
        return "Upsize candidate (memory pressure)"
    if cpu < CPU_DOWNSIZE_THRESHOLD:
        return "Downsize candidate"
    if cpu > CPU_UPSIZE_THRESHOLD:
        return "Upsize candidate (CPU pressure)"
    return "OK"


def format_digest(
    period_label: str,
    instances: list[dict[str, Any]],
    cost_by_type: dict[str, Decimal],
) -> tuple[str, str]:
    """
    Return (subject, html_body) for SNS publish.

    Subject keeps under 100 chars per SNS limit. Body uses minimal HTML table
    that renders both in email clients and plain-text fallback.
    """
    subject = f"[{PROJECT_NAME}] EC2 cost + right-sizing report — {period_label}"

    rows: list[str] = []
    for rec in instances:
        cost_str = (
            f"${cost_by_type.get(rec['instance_type'], Decimal('0')):.2f}"
            if rec["instance_type"] in cost_by_type
            else "n/a"
        )
        cpu_str = f"{rec['avg_cpu']:.1f}%" if rec["avg_cpu"] is not None else "n/a"
        mem_str = f"{rec['avg_mem']:.1f}%" if rec["avg_mem"] is not None else "n/a"
        rows.append(
            "<tr>"
            f"<td>{rec['name']}</td>"
            f"<td>{rec['instance_type']}</td>"
            f"<td>{rec['state']}</td>"
            f"<td>{cost_str}</td>"
            f"<td>{cpu_str}</td>"
            f"<td>{mem_str}</td>"
            f"<td>{rec['recommendation']}</td>"
            "</tr>"
        )
    table_rows = "\n".join(rows) if rows else "<tr><td colspan='7'>No EC2 instances tagged Project=Kite found.</td></tr>"

    html = f"""
<html>
<body style="font-family: -apple-system, BlinkMacSystemFont, sans-serif;">
<h2>EC2 Cost + Right-Sizing Report — {period_label}</h2>
<p>Project: <b>{PROJECT_NAME}</b> · Environment: <b>{ENVIRONMENT}</b></p>
<table border="1" cellpadding="6" cellspacing="0" style="border-collapse: collapse;">
  <thead style="background:#f3f4f6;">
    <tr>
      <th>Instance</th>
      <th>Type</th>
      <th>State</th>
      <th>Cost (last month)</th>
      <th>Avg CPU (30d)</th>
      <th>Avg Mem (30d)</th>
      <th>Recommendation</th>
    </tr>
  </thead>
  <tbody>
{table_rows}
  </tbody>
</table>
<p style="color:#6b7280;font-size:0.875rem;">
  Thresholds: Downsize if CPU &lt; {CPU_DOWNSIZE_THRESHOLD:.0f}%;
  Upsize if CPU &gt; {CPU_UPSIZE_THRESHOLD:.0f}% OR Mem &gt; {MEM_UPSIZE_THRESHOLD:.0f}%.<br>
  Runbook: <code>documents/05-guides/operations/ec2-cost-review.md</code>.<br>
  Source: GAP-414 Wave 84 Bucket G.
</p>
</body>
</html>
"""
    return subject, html


def lambda_handler(event: dict, context) -> dict:  # noqa: ARG001
    """Entrypoint invoked by EventBridge schedule."""
    logger.info("ec2-cost-report invoked. event=%s", json.dumps(event, default=str))

    # Boto3 clients (default region from Lambda env; CE pinned us-east-1)
    ec2 = boto3.client("ec2")
    cw = boto3.client("cloudwatch")
    sns = boto3.client("sns")
    ce = boto3.client("ce", region_name=AWS_REGION_CE)

    # Last calendar-month window
    today = datetime.now(timezone.utc).date()
    first_of_this_month = today.replace(day=1)
    last_month_end = first_of_this_month  # CE end is exclusive
    last_month_start = (first_of_this_month - timedelta(days=1)).replace(day=1)
    period_label = last_month_start.strftime("%Y-%m")

    # 1. Discover instances
    instances = list_kite_instances(ec2)
    logger.info("discovered_instances=%d", len(instances))

    # 2. Cost (best-effort; absent Cost Explorer = empty dict)
    cost_by_type = get_monthly_cost_by_instance(
        ce,
        start=last_month_start.isoformat(),
        end=last_month_end.isoformat(),
    )
    logger.info("cost_by_type=%s", {k: str(v) for k, v in cost_by_type.items()})

    # 3. Per-instance CPU + memory + recommendation
    enriched: list[dict[str, Any]] = []
    for inst in instances:
        cpu = get_avg_cpu(cw, inst["instance_id"])
        mem = get_avg_mem(cw, inst["instance_id"])
        enriched.append(
            {
                **inst,
                "avg_cpu": cpu,
                "avg_mem": mem,
                "recommendation": recommend(cpu, mem),
            }
        )

    # 4. Build + publish digest
    subject, html_body = format_digest(period_label, enriched, cost_by_type)
    sns.publish(
        TopicArn=SNS_TOPIC_ARN,
        Subject=subject[:99],
        Message=html_body,
    )
    logger.info("digest_published topic=%s", SNS_TOPIC_ARN)

    return {
        "statusCode": 200,
        "period": period_label,
        "instances_evaluated": len(enriched),
        "recommendations": {r["instance_id"]: r["recommendation"] for r in enriched},
    }
