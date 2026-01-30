# Báo Cáo Phân Tích Media Service cho KiteClass Platform

**Ngày tạo:** 2026-01-07
**Phiên bản:** 1.0
**Tác giả:** Nguyễn Văn Kiệt

---

## 1. Tổng Quan

### 1.1. Mục đích báo cáo

Phân tích và đề xuất giải pháp Media Service cho KiteClass Platform sử dụng các công nghệ open-source, tự host và customize theo nhu cầu.

### 1.2. Yêu cầu Media Service cho KiteClass

| Tính năng | Mô tả | Độ ưu tiên |
|-----------|-------|------------|
| Video on Demand (VOD) | Upload và phát video bài giảng đã ghi | Cao |
| Video Transcoding | Chuyển đổi video sang nhiều độ phân giải (360p, 720p, 1080p) | Cao |
| Adaptive Bitrate | Tự động chọn chất lượng theo băng thông người xem | Cao |
| Live Streaming | Phát trực tiếp lớp học | Trung bình |
| Recording | Ghi lại buổi live stream | Trung bình |
| Image Processing | Resize, crop, optimize ảnh | Thấp |
| Storage | Lưu trữ video, ảnh, tài liệu | Cao |

---

## 2. So Sánh Các Giải Pháp Open-Source

### 2.1. Video Streaming Server

| Project | GitHub Stars | License | VOD | Live | WebRTC | Transcoding | Độ khó |
|---------|--------------|---------|-----|------|--------|-------------|--------|
| **Ant Media Server CE** | 4,000+ | Apache 2.0 | ✅ | ✅ | ✅ | ✅ | Trung bình |
| **OvenMediaEngine** | 3,000+ | GPLv2 | ✅ | ✅ | ✅ | ✅ | Trung bình |
| **MediaMTX** | 10,000+ | MIT | ✅ | ✅ | ✅ | ❌ | Dễ |
| **nginx-rtmp-module** | 13,000+ | BSD | ✅ | ✅ | ❌ | ❌ (cần FFmpeg) | Dễ |
| **LiveKit** | 8,000+ | Apache 2.0 | ❌ | ✅ | ✅ | ❌ | Khó |
| **Jitsi** | 22,000+ | Apache 2.0 | ❌ | ✅ | ✅ | ❌ | Trung bình |

### 2.2. Storage Solutions

| Project | GitHub Stars | License | S3 Compatible | Clustering | Độ khó |
|---------|--------------|---------|---------------|------------|--------|
| **MinIO** | 45,000+ | AGPL v3 | ✅ | ✅ | Dễ |
| **SeaweedFS** | 20,000+ | Apache 2.0 | ✅ | ✅ | Trung bình |
| **Ceph** | 13,000+ | LGPL | ✅ | ✅ | Khó |

### 2.3. Transcoding Tools

| Tool | Mô tả | License |
|------|-------|---------|
| **FFmpeg** | Công cụ transcoding mạnh nhất, chuẩn công nghiệp | LGPL/GPL |
| **HandBrake** | GUI wrapper cho FFmpeg | GPL |
| **Shaka Packager** | Đóng gói HLS/DASH từ Google | Apache 2.0 |

---

## 3. Đánh Giá Chi Tiết Các Giải Pháp

### 3.1. Ant Media Server Community Edition

**GitHub:** https://github.com/ant-media/Ant-Media-Server

**Ưu điểm:**
- Đầy đủ tính năng: VOD, Live, WebRTC, Recording
- Tự động transcoding sang adaptive bitrate (HLS, DASH)
- Dashboard quản lý có sẵn
- REST API đầy đủ
- Documentation tốt
- Hỗ trợ clustering (Enterprise)

**Nhược điểm:**
- Community Edition giới hạn một số tính năng
- Cần server mạnh cho transcoding
- Clustering chỉ có ở bản Enterprise

**Yêu cầu hệ thống:**
- CPU: 4+ cores (recommend 8 cores)
- RAM: 8GB+ (recommend 16GB)
- Storage: SSD
- OS: Ubuntu 18.04/20.04/22.04

**Tính năng Community vs Enterprise:**

| Tính năng | Community | Enterprise |
|-----------|-----------|------------|
| WebRTC Streaming | ✅ | ✅ |
| Adaptive Bitrate | ✅ | ✅ |
| Recording | ✅ | ✅ |
| VOD Streaming | ✅ | ✅ |
| REST API | ✅ | ✅ |
| Clustering | ❌ | ✅ |
| RTMP Ingestion | ✅ | ✅ |
| SRT Support | ❌ | ✅ |

### 3.2. OvenMediaEngine (OME)

**GitHub:** https://github.com/AirenSoft/OvenMediaEngine

**Ưu điểm:**
- Hiệu năng rất cao (Made in Korea)
- Hỗ trợ WebRTC, HLS, DASH, RTMP
- Ultra-low latency streaming
- Hoàn toàn miễn phí, không giới hạn
- Transcoding tích hợp
- Sub-second latency với WebRTC

**Nhược điểm:**
- Documentation chủ yếu tiếng Hàn
- Community nhỏ hơn Ant Media
- Không có dashboard quản lý (cần tự build)

**Yêu cầu hệ thống:**
- CPU: 4+ cores
- RAM: 8GB+
- OS: Ubuntu 18.04+, CentOS 7+

### 3.3. MediaMTX (trước đây là rtsp-simple-server)

**GitHub:** https://github.com/bluenviron/mediamtx

**Ưu điểm:**
- Rất nhẹ, single binary
- Dễ cài đặt và cấu hình
- Hỗ trợ nhiều protocol: RTSP, RTMP, HLS, WebRTC
- Không cần transcoding nếu source đã đúng format
- MIT License

**Nhược điểm:**
- Không có transcoding tích hợp (cần FFmpeg riêng)
- Không có dashboard
- Phù hợp cho use case đơn giản

**Yêu cầu hệ thống:**
- CPU: 2+ cores
- RAM: 2GB+
- Rất nhẹ, có thể chạy trên Raspberry Pi

### 3.4. nginx-rtmp-module

**GitHub:** https://github.com/arut/nginx-rtmp-module

**Ưu điểm:**
- Rất ổn định, đã được sử dụng nhiều năm
- Tích hợp với nginx (web server phổ biến)
- Nhẹ và hiệu quả
- Dễ kết hợp với FFmpeg để transcoding

**Nhược điểm:**
- Không có WebRTC
- Cần tự setup transcoding pipeline
- Cần kiến thức về nginx

**Yêu cầu hệ thống:**
- CPU: 2+ cores (thêm nếu transcoding)
- RAM: 4GB+

---

## 4. Kiến Trúc Đề Xuất

### 4.1. Option A: Đơn giản (Cho MVP/Đồ án)

```
┌─────────────────────────────────────────────────────────────┐
│                    Media Service Stack                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   User Upload                                                │
│       │                                                      │
│       ▼                                                      │
│   ┌─────────┐     ┌─────────┐     ┌─────────────────┐       │
│   │ KiteClass│────▶│ MinIO   │────▶│ FFmpeg Worker   │       │
│   │ Backend │     │ Storage │     │ (Transcoding)   │       │
│   └─────────┘     └─────────┘     └────────┬────────┘       │
│                                            │                 │
│                                            ▼                 │
│                                   ┌─────────────────┐       │
│                                   │ HLS Files       │       │
│                                   │ (360p/720p/1080p)│       │
│                                   └────────┬────────┘       │
│                                            │                 │
│                                            ▼                 │
│   User Watch                      ┌─────────────────┐       │
│       │                           │     nginx       │       │
│       └──────────────────────────▶│  (HLS Server)   │       │
│                                   └─────────────────┘       │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**Stack:**
- MinIO: Object storage
- FFmpeg: Transcoding
- nginx: HLS delivery
- Video.js: Frontend player

**Chi phí server:** ~$40-60/tháng

### 4.2. Option B: Đầy đủ tính năng (Production)

```
┌─────────────────────────────────────────────────────────────┐
│                    Media Service Stack                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   ┌─────────────────────────────────────────────────────┐   │
│   │              Ant Media Server CE                     │   │
│   │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌────────┐ │   │
│   │  │  RTMP   │  │ WebRTC  │  │   HLS   │  │ Record │ │   │
│   │  │ Ingest  │  │  P2P    │  │  Output │  │  VOD   │ │   │
│   │  └─────────┘  └─────────┘  └─────────┘  └────────┘ │   │
│   │                      │                              │   │
│   │              ┌───────▼───────┐                      │   │
│   │              │  Transcoding  │                      │   │
│   │              │   (FFmpeg)    │                      │   │
│   │              └───────────────┘                      │   │
│   └─────────────────────────────────────────────────────┘   │
│                          │                                   │
│                          ▼                                   │
│   ┌─────────────────────────────────────────────────────┐   │
│   │                     MinIO                            │   │
│   │              (S3-compatible Storage)                 │   │
│   │         Videos, HLS segments, Thumbnails            │   │
│   └─────────────────────────────────────────────────────┘   │
│                          │                                   │
│                          ▼                                   │
│   ┌─────────────────────────────────────────────────────┐   │
│   │              nginx (Reverse Proxy + CDN)             │   │
│   └─────────────────────────────────────────────────────┘   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**Stack:**
- Ant Media Server CE: All-in-one streaming
- MinIO: Object storage
- nginx: Reverse proxy + caching
- Video.js / hls.js: Frontend player

**Chi phí server:** ~$80-120/tháng

### 4.3. Option C: Hiệu năng cao (Scale)

```
┌─────────────────────────────────────────────────────────────┐
│                    Media Service Stack                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   Live Stream Input          VOD Upload                      │
│        │                          │                          │
│        ▼                          ▼                          │
│   ┌──────────────┐         ┌──────────────┐                 │
│   │OvenMedia     │         │ FFmpeg       │                 │
│   │Engine        │         │ Workers      │                 │
│   │(Live)        │         │ (Transcode)  │                 │
│   └──────┬───────┘         └──────┬───────┘                 │
│          │                        │                          │
│          │    ┌───────────────────┘                          │
│          │    │                                              │
│          ▼    ▼                                              │
│   ┌─────────────────────────────────────────────────────┐   │
│   │                   MinIO Cluster                      │   │
│   │    ┌─────────┐    ┌─────────┐    ┌─────────┐        │   │
│   │    │ Node 1  │    │ Node 2  │    │ Node 3  │        │   │
│   │    └─────────┘    └─────────┘    └─────────┘        │   │
│   └─────────────────────────────────────────────────────┘   │
│                          │                                   │
│                          ▼                                   │
│   ┌─────────────────────────────────────────────────────┐   │
│   │              nginx + Cloudflare CDN                  │   │
│   └─────────────────────────────────────────────────────┘   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**Stack:**
- OvenMediaEngine: High-performance live streaming
- FFmpeg cluster: Distributed transcoding
- MinIO cluster: Distributed storage
- nginx + Cloudflare: CDN delivery

**Chi phí server:** ~$150-250/tháng

---

## 5. Hướng Dẫn Cài Đặt

### 5.1. Cài đặt MinIO

```bash
# Download MinIO
wget https://dl.min.io/server/minio/release/linux-amd64/minio
chmod +x minio

# Tạo thư mục data
mkdir -p /data/minio

# Chạy MinIO
export MINIO_ROOT_USER=admin
export MINIO_ROOT_PASSWORD=your_secure_password
./minio server /data/minio --console-address ":9001"

# Hoặc dùng Docker
docker run -d \
  -p 9000:9000 \
  -p 9001:9001 \
  -e "MINIO_ROOT_USER=admin" \
  -e "MINIO_ROOT_PASSWORD=your_secure_password" \
  -v /data/minio:/data \
  minio/minio server /data --console-address ":9001"
```

### 5.2. Cài đặt Ant Media Server CE

```bash
# Download installer
wget https://github.com/ant-media/Ant-Media-Server/releases/download/ams-v2.9.0/ant-media-server-2.9.0-community-2.9.0-20240101_0000.zip

# Unzip
unzip ant-media-server-*.zip

# Install
cd ant-media-server
sudo ./install.sh

# Start service
sudo systemctl start antmedia
sudo systemctl enable antmedia

# Access dashboard
# http://your-server:5080
```

### 5.3. Cài đặt OvenMediaEngine

```bash
# Ubuntu 20.04/22.04
sudo apt-get update
sudo apt-get install -y curl

# Add repository
curl -LOJ https://github.com/AirenSoft/OvenMediaEngine/releases/download/v0.16.0/ovenmediaengine_0.16.0_amd64.deb

# Install
sudo dpkg -i ovenmediaengine_*.deb

# Start
sudo systemctl start ovenmediaengine
sudo systemctl enable ovenmediaengine

# Hoặc dùng Docker
docker run -d \
  -p 1935:1935 \
  -p 3333:3333 \
  -p 3478:3478 \
  -p 8080:8080 \
  -p 9000:9000/udp \
  -p 10000-10009:10000-10009/udp \
  airensoft/ovenmediaengine:latest
```

### 5.4. Cài đặt MediaMTX

```bash
# Download
wget https://github.com/bluenviron/mediamtx/releases/download/v1.5.0/mediamtx_v1.5.0_linux_amd64.tar.gz

# Extract
tar -xzf mediamtx_*.tar.gz

# Run
./mediamtx

# Hoặc dùng Docker
docker run -d \
  -p 8554:8554 \
  -p 1935:1935 \
  -p 8888:8888 \
  -p 8889:8889 \
  bluenviron/mediamtx
```

### 5.5. FFmpeg Transcoding Script

```bash
#!/bin/bash
# transcode.sh - Transcode video to multiple resolutions

INPUT=$1
OUTPUT_DIR=$2
FILENAME=$(basename "$INPUT" | cut -d. -f1)

# Tạo thư mục output
mkdir -p "$OUTPUT_DIR/$FILENAME"

# Transcoding to multiple resolutions
ffmpeg -i "$INPUT" \
  -filter_complex "[0:v]split=3[v1][v2][v3]; \
    [v1]scale=640:360[v360]; \
    [v2]scale=1280:720[v720]; \
    [v3]scale=1920:1080[v1080]" \
  -map "[v360]" -c:v libx264 -b:v 800k -map 0:a -c:a aac -b:a 96k \
    "$OUTPUT_DIR/$FILENAME/360p.mp4" \
  -map "[v720]" -c:v libx264 -b:v 2500k -map 0:a -c:a aac -b:a 128k \
    "$OUTPUT_DIR/$FILENAME/720p.mp4" \
  -map "[v1080]" -c:v libx264 -b:v 5000k -map 0:a -c:a aac -b:a 192k \
    "$OUTPUT_DIR/$FILENAME/1080p.mp4"

# Generate HLS
ffmpeg -i "$OUTPUT_DIR/$FILENAME/720p.mp4" \
  -codec: copy \
  -start_number 0 \
  -hls_time 10 \
  -hls_list_size 0 \
  -f hls \
  "$OUTPUT_DIR/$FILENAME/playlist.m3u8"

echo "Transcoding complete: $OUTPUT_DIR/$FILENAME"
```

---

## 6. Tích Hợp với KiteClass Backend

### 6.1. API Design

```java
// VideoController.java
@RestController
@RequestMapping("/api/v1/videos")
public class VideoController {

    @PostMapping("/upload")
    public ResponseEntity<VideoResponse> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("courseId") Long courseId) {
        // 1. Upload to MinIO
        // 2. Queue transcoding job
        // 3. Return video metadata
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<StreamInfo> getStreamUrl(@PathVariable Long id) {
        // Return HLS playlist URL
    }

    @PostMapping("/live/start")
    public ResponseEntity<LiveStreamInfo> startLiveStream(
            @RequestParam("classId") Long classId) {
        // 1. Create stream on Ant Media / OME
        // 2. Return RTMP ingest URL + HLS playback URL
    }
}
```

### 6.2. Video Processing Flow

```
┌─────────────────────────────────────────────────────────────┐
│                   Video Upload Flow                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. User Upload    2. Store Raw    3. Queue Job             │
│       │                  │              │                    │
│       ▼                  ▼              ▼                    │
│  ┌─────────┐      ┌──────────┐   ┌──────────────┐           │
│  │ Frontend│─────▶│  MinIO   │──▶│ RabbitMQ     │           │
│  └─────────┘      │ (raw/)   │   │ (transcode   │           │
│                   └──────────┘   │  queue)      │           │
│                                  └──────┬───────┘           │
│                                         │                    │
│  4. Transcode      5. Store HLS   6. Update DB              │
│       │                  │              │                    │
│       ▼                  ▼              ▼                    │
│  ┌──────────────┐  ┌──────────┐  ┌──────────────┐           │
│  │ FFmpeg       │─▶│  MinIO   │─▶│ PostgreSQL   │           │
│  │ Worker       │  │ (hls/)   │  │ (video meta) │           │
│  └──────────────┘  └──────────┘  └──────────────┘           │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 6.3. Database Schema

```sql
-- videos table
CREATE TABLE videos (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT REFERENCES courses(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    duration_seconds INT,
    status VARCHAR(50) DEFAULT 'PROCESSING',
    -- PROCESSING, READY, FAILED

    -- Storage paths
    raw_path VARCHAR(500),
    hls_path VARCHAR(500),
    thumbnail_path VARCHAR(500),

    -- Metadata
    file_size_bytes BIGINT,
    resolution VARCHAR(20),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- video_qualities table
CREATE TABLE video_qualities (
    id BIGSERIAL PRIMARY KEY,
    video_id BIGINT REFERENCES videos(id),
    quality VARCHAR(20), -- 360p, 720p, 1080p
    path VARCHAR(500),
    bitrate_kbps INT,
    file_size_bytes BIGINT
);

-- live_streams table
CREATE TABLE live_streams (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT REFERENCES classes(id),
    stream_key VARCHAR(100) UNIQUE,
    status VARCHAR(50) DEFAULT 'IDLE',
    -- IDLE, LIVE, ENDED

    rtmp_url VARCHAR(500),
    hls_url VARCHAR(500),
    webrtc_url VARCHAR(500),

    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    recording_path VARCHAR(500)
);
```

---

## 7. Chi Phí Ước Tính

### 7.1. Chi phí Server

| Option | Server Specs | Cloud Provider | Chi phí/tháng |
|--------|--------------|----------------|---------------|
| **A: Đơn giản** | 4 vCPU, 8GB RAM, 200GB SSD | DigitalOcean/Vultr | ~$48-60 |
| **B: Đầy đủ** | 8 vCPU, 16GB RAM, 500GB SSD | DigitalOcean/Vultr | ~$96-120 |
| **C: Scale** | 2x (8 vCPU, 16GB) + 1x storage | AWS/GCP | ~$200-300 |

### 7.2. Chi phí Bandwidth

| Lượng người xem | Bandwidth/tháng | Chi phí CDN |
|-----------------|-----------------|-------------|
| 100 users | ~500GB | Free (Cloudflare) |
| 500 users | ~2TB | ~$20-30 |
| 1000 users | ~5TB | ~$50-80 |

### 7.3. So sánh với Outsource

| Giải pháp | 1000 phút video/tháng | 10,000 phút xem/tháng |
|-----------|----------------------|----------------------|
| **Self-hosted (Option A)** | ~$50 fixed | ~$50 fixed |
| **Self-hosted (Option B)** | ~$100 fixed | ~$100 fixed |
| **Bunny.net Stream** | ~$5 | ~$50 |
| **Cloudflare Stream** | ~$6 | ~$50 |
| **Mux** | ~$15 | ~$100 |

**Nhận xét:** Self-hosted có chi phí cố định, phù hợp khi scale lớn. Outsource rẻ hơn khi còn ít users.

---

## 8. Khuyến Nghị

### 8.1. Cho Đồ Án Tốt Nghiệp

**Khuyến nghị: Option A (Đơn giản)**

- Stack: MinIO + FFmpeg + nginx + Video.js
- Lý do:
  - Đủ để demo các tính năng
  - Chi phí thấp (~$50/tháng)
  - Dễ setup và maintain
  - Có thể chạy local để test

### 8.2. Cho MVP Production

**Khuyến nghị: Option B (Ant Media Server)**

- Stack: Ant Media Server CE + MinIO + nginx
- Lý do:
  - Đầy đủ tính năng VOD + Live
  - Dashboard có sẵn
  - API đầy đủ
  - Dễ tích hợp

### 8.3. Cho Scale (>1000 users đồng thời)

**Khuyến nghị: Hybrid**

- Self-hosted cho transcoding + storage
- Cloudflare CDN cho delivery
- Hoặc chuyển sang outsource (Bunny.net, Cloudflare Stream)

---

## 9. Kết Luận

| Tiêu chí | Self-hosted | Outsource |
|----------|-------------|-----------|
| Chi phí ban đầu | Cao hơn | Thấp |
| Chi phí scale | Thấp hơn | Cao hơn |
| Customization | Cao | Thấp |
| Maintenance | Cần effort | Không cần |
| Learning value | Cao | Thấp |
| Time to market | Chậm hơn | Nhanh |

**Quyết định cuối cùng:** Với mục tiêu đồ án tốt nghiệp và học hỏi công nghệ, **self-hosted với Option A hoặc B** là lựa chọn phù hợp. Sau khi có users thực tế, có thể đánh giá lại và chuyển sang hybrid hoặc outsource nếu cần.

---

## 10. Tài Liệu Tham Khảo

- Ant Media Server: https://github.com/ant-media/Ant-Media-Server
- OvenMediaEngine: https://github.com/AirenSoft/OvenMediaEngine
- MediaMTX: https://github.com/bluenviron/mediamtx
- MinIO: https://github.com/minio/minio
- nginx-rtmp: https://github.com/arut/nginx-rtmp-module
- FFmpeg: https://ffmpeg.org/documentation.html
- Video.js: https://videojs.com/
- hls.js: https://github.com/video-dev/hls.js
