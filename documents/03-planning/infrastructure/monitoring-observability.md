# KiteClass Monitoring & Observability Strategy

**Version:** 1.0
**Created:** 2026-03-10
**Purpose:** Production monitoring, alerting, and observability for KiteClass platform
**Status:** Production-ready design

---

## Table of Contents

1. [Overview](#overview)
2. [Metrics Collection (Prometheus)](#metrics-collection-prometheus)
3. [Visualization (Grafana)](#visualization-grafana)
4. [Distributed Tracing (OpenTelemetry)](#distributed-tracing-opentelemetry)
5. [Log Aggregation](#log-aggregation)
6. [Alerting & Incident Response](#alerting--incident-response)
7. [SLOs & SLIs](#slos--slis)
8. [Cost Optimization](#cost-optimization)

---

## Overview

**Goal:** Achieve full observability of KiteClass platform to ensure reliability, performance, and rapid incident response.

**Three Pillars of Observability:**
1. **Metrics** - What is happening? (CPU, memory, request rate, error rate)
2. **Logs** - Why did it happen? (Error messages, stack traces, audit events)
3. **Traces** - How did it flow? (Request path through microservices)

**Stack:**
- **Metrics**: Prometheus + Micrometer
- **Visualization**: Grafana
- **Tracing**: OpenTelemetry + Jaeger
- **Logs**: Loki (Grafana stack) or CloudWatch Logs
- **Alerting**: Alertmanager + PagerDuty
- **APM**: Spring Boot Actuator

---

## Metrics Collection (Prometheus)

### Spring Boot Actuator Integration

**Add Dependency:**
```xml
<!-- Spring Boot Actuator -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Micrometer Prometheus -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**Configuration:**
```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      environment: ${SPRING_PROFILES_ACTIVE:dev}
```

**Exposed Metrics Endpoint:**
```
GET http://localhost:8080/actuator/prometheus
```

---

### Prometheus Server Setup

**Docker Compose (Local Dev):**
```yaml
# docker-compose.monitoring.yml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:v2.48.0
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--storage.tsdb.retention.time=30d'
    networks:
      - monitoring

volumes:
  prometheus-data:

networks:
  monitoring:
    driver: bridge
```

**Prometheus Configuration:**
```yaml
# monitoring/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  # KiteClass Gateway
  - job_name: 'kiteclass-gateway'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['kiteclass-gateway:8080']
        labels:
          service: 'gateway'
          tier: 'api'

  # KiteClass Core
  - job_name: 'kiteclass-core'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['kiteclass-core:8080']
        labels:
          service: 'core'
          tier: 'backend'

  # KiteHub Subscription
  - job_name: 'kitehub-subscription'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['kitehub-subscription:8080']
        labels:
          service: 'subscription'
          tier: 'platform'

  # KiteHub Branding
  - job_name: 'kitehub-branding'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['kitehub-branding:8080']
        labels:
          service: 'branding'
          tier: 'platform'

  # PostgreSQL Exporter
  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']

  # Redis Exporter
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']
```

---

### Kubernetes ServiceMonitor

**Production (Kubernetes with Prometheus Operator):**
```yaml
# k8s/monitoring/servicemonitor-gateway.yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: kiteclass-gateway
  namespace: kiteclass
  labels:
    app: kiteclass-gateway
spec:
  selector:
    matchLabels:
      app: kiteclass-gateway
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 30s
      scrapeTimeout: 10s
```

---

### Custom Metrics

**Business Metrics:**
```java
@Component
public class BusinessMetrics {

    private final MeterRegistry meterRegistry;

    // Counters
    private final Counter studentRegistrations;
    private final Counter courseEnrollments;
    private final Counter paymentSuccesses;
    private final Counter paymentFailures;

    // Gauges
    private final AtomicInteger activeTrialInstances = new AtomicInteger(0);
    private final AtomicInteger activeSubscriptions = new AtomicInteger(0);

    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Register counters
        this.studentRegistrations = Counter.builder("kiteclass.students.registrations")
            .description("Total student registrations")
            .tag("status", "success")
            .register(meterRegistry);

        this.courseEnrollments = Counter.builder("kiteclass.courses.enrollments")
            .description("Total course enrollments")
            .register(meterRegistry);

        this.paymentSuccesses = Counter.builder("kiteclass.payments.success")
            .description("Successful payments")
            .register(meterRegistry);

        this.paymentFailures = Counter.builder("kiteclass.payments.failure")
            .description("Failed payments")
            .tag("reason", "unknown")
            .register(meterRegistry);

        // Register gauges
        Gauge.builder("kiteclass.instances.trial.active", activeTrialInstances, AtomicInteger::get)
            .description("Active trial instances")
            .register(meterRegistry);

        Gauge.builder("kiteclass.instances.subscriptions.active", activeSubscriptions, AtomicInteger::get)
            .description("Active paid subscriptions")
            .register(meterRegistry);
    }

    public void recordStudentRegistration() {
        studentRegistrations.increment();
    }

    public void recordCourseEnrollment() {
        courseEnrollments.increment();
    }

    public void recordPaymentSuccess() {
        paymentSuccesses.increment();
    }

    public void recordPaymentFailure(String reason) {
        Counter.builder("kiteclass.payments.failure")
            .tag("reason", reason)
            .register(meterRegistry)
            .increment();
    }

    public void updateTrialInstancesCount(int count) {
        activeTrialInstances.set(count);
    }

    public void updateActiveSubscriptionsCount(int count) {
        activeSubscriptions.set(count);
    }
}
```

**Usage in Service:**
```java
@Service
public class StudentServiceImpl implements StudentService {

    @Autowired
    private BusinessMetrics metrics;

    @Override
    public StudentResponse createStudent(CreateStudentRequest request) {
        Student student = studentRepository.save(/* ... */);

        // Record metric
        metrics.recordStudentRegistration();

        return mapper.toResponse(student);
    }
}
```

---

### Key Metrics to Track

**Infrastructure Metrics:**
- `jvm_memory_used_bytes` - JVM memory usage
- `jvm_gc_pause_seconds` - GC pause time
- `system_cpu_usage` - CPU usage
- `hikaricp_connections_active` - Database connections

**Application Metrics:**
- `http_server_requests_seconds` - HTTP request duration
- `http_server_requests_seconds_count` - Total requests
- `http_server_requests_seconds_sum` - Total request time

**Business Metrics:**
- `kiteclass_students_registrations_total` - Student signups
- `kiteclass_courses_enrollments_total` - Course enrollments
- `kiteclass_payments_success_total` - Successful payments
- `kiteclass_payments_failure_total` - Failed payments
- `kiteclass_instances_trial_active` - Active trial instances
- `kiteclass_instances_subscriptions_active` - Active paid subscriptions

---

## Visualization (Grafana)

### Grafana Setup

**Docker Compose:**
```yaml
# docker-compose.monitoring.yml (add to existing)
  grafana:
    image: grafana/grafana:10.2.2
    container_name: grafana
    ports:
      - "3001:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_USERS_ALLOW_SIGN_UP=false
    volumes:
      - grafana-data:/var/lib/grafana
      - ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./monitoring/grafana/datasources:/etc/grafana/provisioning/datasources
    networks:
      - monitoring
    depends_on:
      - prometheus

volumes:
  grafana-data:
```

**Datasource Configuration:**
```yaml
# monitoring/grafana/datasources/prometheus.yml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: false
```

---

### Dashboard Examples

#### Dashboard 1: Service Overview

**Panels:**
1. **Request Rate** (Graph)
   - Query: `rate(http_server_requests_seconds_count{job="kiteclass-gateway"}[5m])`
   - Unit: requests/sec

2. **Error Rate** (Graph)
   - Query: `rate(http_server_requests_seconds_count{status=~"5.."}[5m])`
   - Unit: errors/sec
   - Alert: > 10 errors/min

3. **Response Time (p50, p95, p99)** (Graph)
   - Query p50: `histogram_quantile(0.50, rate(http_server_requests_seconds_bucket[5m]))`
   - Query p95: `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))`
   - Query p99: `histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))`

4. **Active Instances** (Stat)
   - Query: `up{job=~"kiteclass.*"}`
   - Shows: 3/3 services UP

---

#### Dashboard 2: Business Metrics

**Panels:**
1. **Student Registrations (Last 24h)** (Stat)
   - Query: `increase(kiteclass_students_registrations_total[24h])`

2. **Course Enrollments (Last 7d)** (Graph)
   - Query: `increase(kiteclass_courses_enrollments_total[1d])`

3. **Payment Success Rate** (Gauge)
   - Query: `rate(kiteclass_payments_success_total[5m]) / (rate(kiteclass_payments_success_total[5m]) + rate(kiteclass_payments_failure_total[5m])) * 100`
   - Unit: %
   - Thresholds: Green > 95%, Yellow 90-95%, Red < 90%

4. **Active Subscriptions** (Time series)
   - Query: `kiteclass_instances_subscriptions_active`

---

#### Dashboard 3: Infrastructure Health

**Panels:**
1. **CPU Usage** (Graph)
   - Query: `system_cpu_usage{job=~"kiteclass.*"} * 100`
   - Unit: %

2. **Memory Usage** (Graph)
   - Query: `jvm_memory_used_bytes{job=~"kiteclass.*"} / jvm_memory_max_bytes * 100`
   - Unit: %

3. **Database Connections** (Graph)
   - Query: `hikaricp_connections_active{job=~"kiteclass.*"}`

4. **GC Pause Time** (Graph)
   - Query: `rate(jvm_gc_pause_seconds_sum[5m])`
   - Unit: seconds

---

### Dashboard JSON Export

**Export Dashboard:**
```bash
# Export from Grafana UI
Dashboard Settings → JSON Model → Copy to Clipboard

# Save to file
cat > monitoring/grafana/dashboards/kiteclass-overview.json
```

**Import Dashboard:**
```bash
# Via API
curl -X POST http://admin:admin@localhost:3001/api/dashboards/db \
  -H "Content-Type: application/json" \
  -d @monitoring/grafana/dashboards/kiteclass-overview.json
```

---

## Distributed Tracing (OpenTelemetry)

### OpenTelemetry Integration

**Add Dependency:**
```xml
<!-- OpenTelemetry -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-api</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-sdk</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-jaeger</artifactId>
</dependency>
```

**Configuration:**
```yaml
# application.yml
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0  # 100% sampling in dev, 0.1 (10%) in prod
  zipkin:
    tracing:
      endpoint: http://jaeger:9411/api/v2/spans
```

---

### Jaeger Setup

**Docker Compose:**
```yaml
# docker-compose.monitoring.yml (add)
  jaeger:
    image: jaegertracing/all-in-one:1.52
    container_name: jaeger
    ports:
      - "5775:5775/udp"
      - "6831:6831/udp"
      - "6832:6832/udp"
      - "5778:5778"
      - "16686:16686"  # Jaeger UI
      - "14268:14268"
      - "14250:14250"
      - "9411:9411"    # Zipkin compatible endpoint
    environment:
      - COLLECTOR_ZIPKIN_HOST_PORT=:9411
    networks:
      - monitoring
```

**Access Jaeger UI:**
```
http://localhost:16686
```

---

### Custom Spans

**Manual Instrumentation:**
```java
@Service
public class StudentServiceImpl implements StudentService {

    private final Tracer tracer = GlobalOpenTelemetry.getTracer("kiteclass-core");

    @Override
    public StudentResponse createStudent(CreateStudentRequest request) {
        Span span = tracer.spanBuilder("createStudent")
            .setAttribute("student.email", request.getEmail())
            .startSpan();

        try (Scope scope = span.makeCurrent()) {
            // Validation span
            Span validationSpan = tracer.spanBuilder("validateStudent").startSpan();
            validateStudent(request);
            validationSpan.end();

            // Database span
            Span dbSpan = tracer.spanBuilder("saveStudent").startSpan();
            Student student = studentRepository.save(/* ... */);
            dbSpan.end();

            // Audit span
            Span auditSpan = tracer.spanBuilder("auditLog").startSpan();
            auditLogger.logEvent(/* ... */);
            auditSpan.end();

            return mapper.toResponse(student);
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

---

### Trace Correlation

**Propagate Trace Context:**
```java
// Gateway forwards trace headers to Core
@Component
public class TraceContextFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst("X-B3-TraceId");
        String spanId = exchange.getRequest().getHeaders().getFirst("X-B3-SpanId");

        if (traceId != null) {
            // Forward to downstream services
            ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header("X-B3-TraceId", traceId)
                .header("X-B3-SpanId", spanId)
                .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        }

        return chain.filter(exchange);
    }
}
```

---

## Log Aggregation

### Option A: Loki (Grafana Stack)

**Docker Compose:**
```yaml
# docker-compose.monitoring.yml (add)
  loki:
    image: grafana/loki:2.9.3
    container_name: loki
    ports:
      - "3100:3100"
    volumes:
      - ./monitoring/loki/loki-config.yml:/etc/loki/local-config.yaml
      - loki-data:/loki
    command: -config.file=/etc/loki/local-config.yaml
    networks:
      - monitoring

  promtail:
    image: grafana/promtail:2.9.3
    container_name: promtail
    volumes:
      - /var/log:/var/log
      - ./monitoring/promtail/promtail-config.yml:/etc/promtail/config.yml
      - /var/lib/docker/containers:/var/lib/docker/containers:ro
    command: -config.file=/etc/promtail/config.yml
    networks:
      - monitoring
    depends_on:
      - loki

volumes:
  loki-data:
```

**Loki Configuration:**
```yaml
# monitoring/loki/loki-config.yml
auth_enabled: false

server:
  http_listen_port: 3100

ingester:
  lifecycler:
    ring:
      kvstore:
        store: inmemory
      replication_factor: 1

schema_config:
  configs:
    - from: 2020-10-24
      store: boltdb-shipper
      object_store: filesystem
      schema: v11
      index:
        prefix: index_
        period: 24h

storage_config:
  boltdb_shipper:
    active_index_directory: /loki/index
    cache_location: /loki/cache
    shared_store: filesystem
  filesystem:
    directory: /loki/chunks

limits_config:
  enforce_metric_name: false
  reject_old_samples: true
  reject_old_samples_max_age: 168h

chunk_store_config:
  max_look_back_period: 0s

table_manager:
  retention_deletes_enabled: true
  retention_period: 720h  # 30 days
```

**Promtail Configuration:**
```yaml
# monitoring/promtail/promtail-config.yml
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
  - job_name: containers
    docker_sd_configs:
      - host: unix:///var/run/docker.sock
        refresh_interval: 5s
    relabel_configs:
      - source_labels: ['__meta_docker_container_name']
        target_label: 'container'
      - source_labels: ['__meta_docker_container_log_stream']
        target_label: 'stream'
```

**Grafana Loki Datasource:**
```yaml
# monitoring/grafana/datasources/loki.yml
apiVersion: 1

datasources:
  - name: Loki
    type: loki
    access: proxy
    url: http://loki:3100
    editable: false
```

---

### Option B: CloudWatch Logs (AWS)

**Spring Boot Configuration:**
```xml
<dependency>
    <groupId>ca.pjer</groupId>
    <artifactId>logback-awslogs-appender</artifactId>
    <version>1.6.0</version>
</dependency>
```

**logback-spring.xml:**
```xml
<configuration>
    <appender name="AWS_LOGS" class="ca.pjer.logback.AwsLogsAppender">
        <layout>
            <pattern>[%thread] %-5level %logger{35} - %msg%n</pattern>
        </layout>
        <logGroupName>/kiteclass/${SPRING_PROFILES_ACTIVE}/application</logGroupName>
        <logStreamName>${HOSTNAME}-${spring.application.name}</logStreamName>
        <logRegion>ap-southeast-1</logRegion>
        <maxBatchLogEvents>50</maxBatchLogEvents>
        <maxFlushTimeMillis>30000</maxFlushTimeMillis>
        <maxBlockTimeMillis>5000</maxBlockTimeMillis>
    </appender>

    <root level="INFO">
        <appender-ref ref="AWS_LOGS" />
    </root>
</configuration>
```

---

### Structured Logging (JSON)

**Add Dependency:**
```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

**logback-spring.xml:**
```xml
<configuration>
    <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"application":"${spring.application.name}","environment":"${SPRING_PROFILES_ACTIVE}"}</customFields>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="JSON_CONSOLE" />
    </root>
</configuration>
```

**Example JSON Log:**
```json
{
  "@timestamp": "2026-03-10T10:30:00.123Z",
  "level": "INFO",
  "logger": "com.kiteclass.core.service.StudentServiceImpl",
  "message": "Student created successfully",
  "application": "kiteclass-core",
  "environment": "production",
  "thread": "http-nio-8080-exec-1",
  "trace_id": "abc123",
  "span_id": "def456",
  "student_id": "12345",
  "instance_id": "tenant-uuid"
}
```

---

## Alerting & Incident Response

### Prometheus Alertmanager

**Docker Compose:**
```yaml
# docker-compose.monitoring.yml (add)
  alertmanager:
    image: prom/alertmanager:v0.26.0
    container_name: alertmanager
    ports:
      - "9093:9093"
    volumes:
      - ./monitoring/alertmanager/alertmanager.yml:/etc/alertmanager/alertmanager.yml
    command:
      - '--config.file=/etc/alertmanager/alertmanager.yml'
    networks:
      - monitoring
```

**Alertmanager Configuration:**
```yaml
# monitoring/alertmanager/alertmanager.yml
global:
  resolve_timeout: 5m

route:
  receiver: 'pagerduty'
  group_by: ['alertname', 'severity']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 12h
  routes:
    - match:
        severity: critical
      receiver: 'pagerduty'
      continue: true
    - match:
        severity: warning
      receiver: 'slack'

receivers:
  - name: 'pagerduty'
    pagerduty_configs:
      - service_key: '<PAGERDUTY_SERVICE_KEY>'
        description: '{{ .GroupLabels.alertname }}: {{ .Annotations.summary }}'

  - name: 'slack'
    slack_configs:
      - api_url: '<SLACK_WEBHOOK_URL>'
        channel: '#alerts'
        title: '{{ .GroupLabels.alertname }}'
        text: '{{ .Annotations.description }}'
```

---

### Alert Rules

**Prometheus Alert Rules:**
```yaml
# monitoring/prometheus/alert-rules.yml
groups:
  - name: kiteclass_alerts
    interval: 30s
    rules:
      # High Error Rate
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 10
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High error rate on {{ $labels.job }}"
          description: "Error rate is {{ $value }} errors/sec (threshold: 10)"

      # Service Down
      - alert: ServiceDown
        expr: up{job=~"kiteclass.*"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Service {{ $labels.job }} is down"
          description: "Service has been down for more than 1 minute"

      # High Response Time
      - alert: HighResponseTime
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High response time on {{ $labels.job }}"
          description: "P95 response time is {{ $value }}s (threshold: 2s)"

      # High Memory Usage
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes / jvm_memory_max_bytes * 100 > 90
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage on {{ $labels.job }}"
          description: "Memory usage is {{ $value }}% (threshold: 90%)"

      # Database Connection Pool Exhausted
      - alert: DatabasePoolExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max * 100 > 90
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Database connection pool near exhaustion on {{ $labels.job }}"
          description: "Active connections: {{ $value }}% (threshold: 90%)"

      # Payment Failure Rate High
      - alert: HighPaymentFailureRate
        expr: rate(kiteclass_payments_failure_total[5m]) / rate(kiteclass_payments_success_total[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High payment failure rate"
          description: "Payment failure rate is {{ $value | humanizePercentage }} (threshold: 5%)"

      # Trial Expiration Surge
      - alert: TrialExpirationSurge
        expr: increase(kiteclass_instances_trial_expired_total[1h]) > 20
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Unusually high trial expiration rate"
          description: "{{ $value }} trials expired in last hour (threshold: 20)"
```

---

### PagerDuty Integration

**Create Service in PagerDuty:**
1. Go to PagerDuty → Services → New Service
2. Name: KiteClass Production
3. Integration: Prometheus
4. Copy Integration Key

**Add to Alertmanager:**
```yaml
receivers:
  - name: 'pagerduty'
    pagerduty_configs:
      - service_key: 'YOUR_INTEGRATION_KEY'
        severity: '{{ .Labels.severity }}'
        description: |
          Alert: {{ .GroupLabels.alertname }}
          Instance: {{ .Labels.instance }}
          Summary: {{ .Annotations.summary }}
          Description: {{ .Annotations.description }}
        client: 'Prometheus Alertmanager'
        client_url: 'http://prometheus:9090'
```

---

## SLOs & SLIs

### Service Level Indicators (SLIs)

**Availability SLI:**
```
SLI = (Total requests - Failed requests) / Total requests * 100
Target: 99.9% (3 nines)
```

**Latency SLI:**
```
SLI = Requests with latency < threshold / Total requests * 100
Target: 95% of requests < 500ms
```

**Error Rate SLI:**
```
SLI = Failed requests / Total requests * 100
Target: < 0.1% error rate
```

---

### Service Level Objectives (SLOs)

**KiteClass Platform SLOs:**

| Service | Availability | Latency (p95) | Error Rate |
|---------|-------------|---------------|------------|
| Gateway | 99.9% | < 200ms | < 0.1% |
| Core API | 99.95% | < 500ms | < 0.05% |
| Frontend | 99.5% | < 1000ms | < 1% |
| Payment | 99.99% | < 2000ms | < 0.01% |

---

### Error Budget

**Monthly Error Budget:**
```
Uptime Target: 99.9% (3 nines)
Downtime Budget: 0.1% = 43.2 minutes/month

If error budget exhausted:
- Freeze feature releases
- Focus 100% on reliability improvements
- No deployments until error budget recovers
```

**Track Error Budget:**
```promql
# Remaining error budget
(1 - (
  sum(rate(http_server_requests_seconds_count{status=~"5.."}[30d])) /
  sum(rate(http_server_requests_seconds_count[30d]))
)) * 100
```

---

## Cost Optimization

### Reduce Cardinality

**Bad - High Cardinality:**
```java
// DON'T: User ID as tag (millions of unique values)
Counter.builder("requests")
    .tag("user_id", userId.toString())  // ❌ Too many unique values
    .register(registry);
```

**Good - Low Cardinality:**
```java
// DO: User role as tag (few unique values)
Counter.builder("requests")
    .tag("role", userRole.name())  // ✅ Only 5 values (ADMIN, TEACHER, STUDENT, etc.)
    .register(registry);
```

---

### Sampling Strategy

**Production Tracing:**
```yaml
# Trace only 10% of requests in production
management:
  tracing:
    sampling:
      probability: 0.1  # 10% sampling
```

**Sample Strategically:**
```java
// Always trace errors, sample 10% of successes
if (response.getStatus() >= 500) {
    tracing.setFullyTraced(true);
} else {
    tracing.setSampled(Math.random() < 0.1);
}
```

---

### Log Retention

**Tiered Retention:**
- **Hot storage** (searchable): 7 days - CloudWatch/Loki
- **Warm storage** (archived): 30 days - S3 Standard
- **Cold storage** (compliance): 1 year - S3 Glacier

**Cost Comparison:**
```
CloudWatch Logs (7 days): $0.50/GB ingestion + $0.03/GB storage = ~$100/month
S3 Standard (30 days): $0.023/GB = ~$7/month
S3 Glacier (1 year): $0.004/GB = ~$1/month
```

---

## Summary

**Monitoring Stack:**
```
┌─────────────────────────────────────────────────────────┐
│                   Grafana Dashboards                    │
│  (Visualization + Alerting UI)                          │
└─────────────────────────────────────────────────────────┘
         │                    │                    │
         ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   Prometheus    │  │      Loki       │  │     Jaeger      │
│   (Metrics)     │  │     (Logs)      │  │    (Traces)     │
└─────────────────┘  └─────────────────┘  └─────────────────┘
         │                    │                    │
         ▼                    ▼                    ▼
┌──────────────────────────────────────────────────────────┐
│          Spring Boot Services (Actuator)                 │
│  Gateway | Core | Subscription | Branding | Payment     │
└──────────────────────────────────────────────────────────┘
```

**Key Metrics:**
- ✅ Request rate, error rate, latency (RED metrics)
- ✅ CPU, memory, connections (Infrastructure)
- ✅ Business metrics (registrations, enrollments, revenue)
- ✅ SLIs tracking (availability, latency, error budget)

**Alerting:**
- ✅ Critical alerts → PagerDuty (immediate response)
- ✅ Warning alerts → Slack (team notification)
- ✅ Alert rules for errors, downtime, performance degradation

**Observability:**
- ✅ Distributed tracing with OpenTelemetry
- ✅ Structured JSON logs
- ✅ Log aggregation with Loki or CloudWatch
- ✅ Full request path visibility

---

## Related Documentation

- [KiteHub Infrastructure](../implementation/kitehub-infrastructure.md)
- [Security Design](../../04-quality/security-design.md)
- [Deployment Runbooks](./deployment-runbooks.md)

---

**Last Updated:** 2026-03-10
**Status:** Production-ready design
