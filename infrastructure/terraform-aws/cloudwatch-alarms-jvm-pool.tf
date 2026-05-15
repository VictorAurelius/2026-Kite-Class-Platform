# =============================================================================
# CloudWatch Alarms - Tier 2 JVM + Tomcat + HikariCP pool monitoring
# Wave 85 Bucket E (GAP-503) - 3 new alarms per Bucket A simulation E-AC1/2/3
# =============================================================================
# Sister-file to cloudwatch.tf (memory alarms GAP-447) - covers JVM heap, Tomcat
# thread pool exhaustion, HikariCP connection wait. All 3 alarms publish to the
# existing aws_sns_topic.memory_alerts (vannkite@outlook.com subscription).
#
# Metrics emitted by Spring Boot Actuator /actuator/prometheus, scraped by
# CloudWatch agent prometheus exporter (configured via cloud-init in ec2.tf).
# Until CWAgent prometheus config is wired, alarms stay INSUFFICIENT_DATA -
# itself a useful Phase 1 BETA observability signal.
#
# Namespace: KiteHub/JVM (custom - emitted by CloudWatch agent prometheus collector).
# =============================================================================

# ----------------- E-AC1: JVM heap usage alarm -----------------
# Per Bucket A simulation cell 5: MaxRAMPercentage=60.0 leaves 1.6GB total / 2GB
# container limit. Heap >90% sustained 5min = OOM risk -> notify + SSH inspect.
# Threshold 90% (not 85%) because 60% RAM cap means heap headroom is intentional;
# only fire when JVM exhausting its allocated quota.
resource "aws_cloudwatch_metric_alarm" "jvm_heap_usage_high" {
  alarm_name          = "${var.project_name}-jvm-heap-usage-high"
  alarm_description   = "JVM heap usage >90% for 5min sustained - OOM risk after Wave 85 Tier 2 (60pct MaxRAMPercentage, t3.small 2GB container). Action: SSH, jcmd GC.heap_info, check for memory leak; consider t3.medium 4GB upsize if recurrent."
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "jvm_memory_used_bytes_heap_percent"
  namespace           = "KiteHub/JVM"
  period              = 300 # 5 min
  statistic           = "Average"
  threshold           = 90

  alarm_actions = [aws_sns_topic.memory_alerts.arn]
  ok_actions    = [aws_sns_topic.memory_alerts.arn]

  treat_missing_data = "notBreaching"

  tags = {
    Name    = "${var.project_name}-jvm-heap-usage-high"
    Purpose = "Tier 2 JVM heap monitoring - GAP-503 Wave 85 Bucket E E-AC1"
    Gap     = "GAP-503"
  }
}

# ----------------- E-AC2: Tomcat thread pool busy alarm -----------------
# Per Bucket A simulation: max=200 threads x 7 services = 1400 capacity.
# Busy threads / max > 80% sustained 5min = thread starvation imminent ->
# notify before requests start rejecting via accept-count overflow.
resource "aws_cloudwatch_metric_alarm" "tomcat_threads_busy_high" {
  alarm_name          = "${var.project_name}-tomcat-threads-busy-high"
  alarm_description   = "Tomcat busy threads >80pct of max=200 for 5min - thread exhaustion imminent. Slow upstream (DB/Redis) OR slow-client attack OR genuine load spike. Action: check upstream latency, P95 endpoint times, consider scale-out."
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "tomcat_threads_busy_percent"
  namespace           = "KiteHub/JVM"
  period              = 300 # 5 min
  statistic           = "Average"
  threshold           = 80

  alarm_actions = [aws_sns_topic.memory_alerts.arn]
  ok_actions    = [aws_sns_topic.memory_alerts.arn]

  treat_missing_data = "notBreaching"

  tags = {
    Name    = "${var.project_name}-tomcat-threads-busy-high"
    Purpose = "Tier 2 Tomcat thread pool monitoring - GAP-503 Wave 85 Bucket E E-AC2"
    Gap     = "GAP-503"
  }
}

# ----------------- E-AC3: HikariCP connection wait alarm -----------------
# Per Bucket A simulation: maximum-pool-size=10 per service; 7 x 10 = 70 total
# < RDS db.t3.micro max_connections=87. connection-wait-count > 5 sustained 1min
# = pool exhaustion symptom (callers blocked waiting for connection acquire).
# Window short (1min) because exhaustion symptoms cascade fast under load.
resource "aws_cloudwatch_metric_alarm" "hikari_connection_wait_high" {
  alarm_name          = "${var.project_name}-hikari-connection-wait-high"
  alarm_description   = "HikariCP pending connection acquire > 5 sustained 1min - pool exhaustion. maximum-pool-size=10 per service exceeded. Action: check slow queries via pg_stat_activity, kill long-running transactions, consider pool size bump (mind RDS max_connections=87)."
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "hikaricp_connections_pending"
  namespace           = "KiteHub/JVM"
  period              = 60 # 1 min - short window, exhaustion cascades fast
  statistic           = "Maximum"
  threshold           = 5

  alarm_actions = [aws_sns_topic.memory_alerts.arn]
  ok_actions    = [aws_sns_topic.memory_alerts.arn]

  treat_missing_data = "notBreaching"

  tags = {
    Name    = "${var.project_name}-hikari-connection-wait-high"
    Purpose = "Tier 2 HikariCP pool monitoring - GAP-503 Wave 85 Bucket E E-AC3"
    Gap     = "GAP-503"
  }
}

# -----------------------------------------------------------------------------
# Follow-up notes (NOT in scope for Wave 85 Bucket E plan-only):
#
# 1. CloudWatch agent prometheus exporter MUST be configured + started on each
#    EC2 to emit KiteHub/JVM metrics. Until wired, alarms = INSUFFICIENT_DATA.
#    Wire-up tracked separately (follow-up gap to file post-merge).
#
# 2. Per-service dimension (kitehub-subscription, kitehub-email, etc.) would
#    let us pinpoint which service is exhausting threads/connections. Phase 1
#    BETA scope: account-level rollup suffices (single t3.small per side).
#    Per-service dimension = Phase 1.5+ when multi-pod scale-out.
#
# 3. Alarm thresholds (80pct/90pct/5) are starting points per Bucket A simulation.
#    Tune after first 2 weeks of Phase 1 BETA production telemetry.
# =============================================================================
