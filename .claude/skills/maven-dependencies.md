# Skill: Maven Dependencies Management

Quy chuẩn quản lý dependencies cho KiteClass Platform - tránh lỗi version và compatibility.

## Mô tả

Tài liệu quy định:
- Versions chuẩn cho tất cả dependencies
- Dependencies nào cần explicit version
- Dependencies nào được Spring Boot quản lý
- Quy tắc kiểm tra trước khi sử dụng version

## Trigger phrases

- "pom.xml"
- "maven dependency"
- "add dependency"
- "spring boot version"

---

## Spring Boot & Spring Cloud Versions (Cập nhật: 2026-01)

| Framework | Version | Ghi chú |
|-----------|---------|---------|
| Spring Boot | **3.5.10** | LTS, active support |
| Spring Cloud | **2024.0.0** | Compatible với Boot 3.5.x |
| Java | **17** hoặc **21** | LTS versions |

**QUAN TRỌNG:**
- KHÔNG dùng Spring Boot < 3.4.x (hết support)
- Spring Cloud version phải match với Spring Boot

---

## Dependencies PHẢI có explicit version

Các dependencies sau KHÔNG được Spring Boot quản lý, PHẢI khai báo version:

```xml
<!-- JWT - JJWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>

<!-- Rate Limiting -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.10.1</version>
</dependency>

<!-- MapStruct -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.6.3</version>
</dependency>

<!-- OpenAPI/Swagger -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
    <version>2.8.4</version>
</dependency>
<!-- Hoặc cho MVC -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.4</version>
</dependency>

<!-- R2DBC PostgreSQL (cho reactive) -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>r2dbc-postgresql</artifactId>
    <version>1.0.7.RELEASE</version>
    <scope>runtime</scope>
</dependency>
```

---

## Dependencies được Spring Boot quản lý (KHÔNG cần version)

```xml
<!-- Spring Boot Starters - KHÔNG cần version -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-r2dbc</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Database drivers - KHÔNG cần version -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Flyway - KHÔNG cần version -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<!-- CHÚ Ý: flyway-database-postgresql KHÔNG cần cho Flyway 9.x -->

<!-- Lombok - KHÔNG cần version (nhưng cần trong annotationProcessorPaths) -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- Testing - KHÔNG cần version -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.projectreactor</groupId>
    <artifactId>reactor-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Micrometer - KHÔNG cần version -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

---

## Properties chuẩn trong pom.xml

```xml
<properties>
    <java.version>17</java.version>
    <spring-cloud.version>2024.0.0</spring-cloud.version>
    <mapstruct.version>1.6.3</mapstruct.version>
    <lombok.version>1.18.36</lombok.version>
</properties>
```

**QUAN TRỌNG:**
- `lombok.version` PHẢI được định nghĩa nếu dùng trong `annotationProcessorPaths`
- Spring Boot parent định nghĩa `lombok.version` nhưng nên explicit để tránh lỗi

---

## Annotation Processor Configuration

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>${lombok.version}</version>
                    </path>
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>${mapstruct.version}</version>
                    </path>
                    <!-- Lombok-MapStruct binding (nếu dùng cả 2) -->
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok-mapstruct-binding</artifactId>
                        <version>0.2.0</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

---

## Checklist trước khi thêm dependency

1. **Kiểm tra Spring Boot có quản lý không:**
   - Xem [Spring Boot Dependency Versions](https://docs.spring.io/spring-boot/docs/current/reference/html/dependency-versions.html)
   - Nếu có → KHÔNG thêm version
   - Nếu không → PHẢI thêm version

2. **Verify version tồn tại:**
   - Check Maven Central: https://search.maven.org/
   - KHÔNG đoán version, KHÔNG dùng version "latest"

3. **Check compatibility:**
   - Spring Cloud phải match Spring Boot
   - Xem [Spring Cloud Release Train](https://spring.io/projects/spring-cloud)

4. **Tránh các lỗi phổ biến:**
   - ❌ `flyway-database-postgresql` (chỉ cần cho Flyway 10+)
   - ❌ `${lombok.version}` không định nghĩa
   - ❌ Spring Boot EOL versions (< 3.4.x)

---

## Template pom.xml cho Gateway (Reactive)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.10</version>
    </parent>

    <groupId>com.kiteclass</groupId>
    <artifactId>kiteclass-gateway</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2024.0.0</spring-cloud.version>
        <mapstruct.version>1.6.3</mapstruct.version>
        <lombok.version>1.18.36</lombok.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <!-- Dependencies theo danh sách ở trên -->
</project>
```

---

## Template pom.xml cho Core Service (MVC/JPA)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.10</version>
    </parent>

    <groupId>com.kiteclass</groupId>
    <artifactId>kiteclass-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <properties>
        <java.version>17</java.version>
        <mapstruct.version>1.6.3</mapstruct.version>
        <lombok.version>1.18.36</lombok.version>
    </properties>

    <!-- Dependencies cho MVC/JPA service -->
</project>
```

---

## Actions

### Khi tạo pom.xml mới
```
1. Copy template phù hợp (Gateway hoặc Core)
2. Chỉ thêm dependencies cần thiết
3. Verify tất cả explicit versions đúng
4. KHÔNG copy từ plan cũ - luôn check skill này
```

### Khi thêm dependency mới
```
1. Check Maven Central cho version mới nhất
2. Check Spring Boot có quản lý không
3. Test compile: mvn clean compile
4. Update skill này nếu có version mới
```
