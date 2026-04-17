# BÁO CÁO KỸ THUẬT: AI QUIZ GENERATOR

## KiteClass Platform - Tính năng sinh câu hỏi tự động bằng AI

| Thuộc tính | Giá trị |
|------------|---------|
| **Ngày** | 23/12/2025 |
| **Phiên bản** | 1.0 |
| **Loại tài liệu** | Báo cáo kỹ thuật chi tiết |
| **Tác giả** | KiteClass Development Team |

---

## MỤC LỤC

1. [Tổng quan](#phần-1-tổng-quan)
2. [Cơ chế hoạt động](#phần-2-cơ-chế-hoạt-động)
3. [Mô hình AI sử dụng](#phần-3-mô-hình-ai-sử-dụng)
4. [Nguồn dữ liệu](#phần-4-nguồn-dữ-liệu)
5. [Các loại câu hỏi](#phần-5-các-loại-câu-hỏi)
6. [Đánh giá chất lượng](#phần-6-đánh-giá-chất-lượng)
7. [Kiến trúc tích hợp](#phần-7-kiến-trúc-tích-hợp)
8. [Chi tiết triển khai](#phần-8-chi-tiết-triển-khai)
9. [Thách thức và giải pháp](#phần-9-thách-thức-và-giải-pháp)
10. [Chi phí ước tính](#phần-10-chi-phí-ước-tính)

---

## PHẦN 1: TỔNG QUAN

### 1.1. Định nghĩa

**AI Quiz Generator** là một hệ thống sử dụng trí tuệ nhân tạo để tự động sinh câu hỏi kiểm tra từ nội dung bài học, bao gồm văn bản, transcript video, và tài liệu đính kèm.

### 1.2. Mục tiêu

| # | Mục tiêu | Mô tả |
|---|----------|-------|
| 1 | **Tiết kiệm thời gian** | Giảm 80% thời gian tạo bài kiểm tra cho instructor |
| 2 | **Đa dạng câu hỏi** | Sinh nhiều dạng câu hỏi từ cùng một nội dung |
| 3 | **Phân cấp độ khó** | Tự động phân loại theo Bloom's Taxonomy |
| 4 | **Nhất quán chất lượng** | Đảm bảo câu hỏi đạt chuẩn giáo dục |
| 5 | **Cá nhân hóa** | Tạo câu hỏi phù hợp với từng nhóm học viên |

### 1.3. So sánh với Azota.vn

| Tiêu chí | Azota.vn | KiteClass (Đề xuất) |
|----------|----------|---------------------|
| **Input** | Đề thi có sẵn (scan/ảnh) | Nội dung bài học (text, video) |
| **Mục đích** | Số hóa đề thi | Sinh câu hỏi mới từ nội dung |
| **AI Task** | OCR + Phân loại | NLU + Generation |
| **Unique Value** | Digitize existing | Create new from content |

> **Điểm khác biệt:** Azota tập trung **số hóa đề thi có sẵn**, trong khi KiteClass sẽ **sinh câu hỏi mới** trực tiếp từ nội dung khóa học.

---

## PHẦN 2: CƠ CHẾ HOẠT ĐỘNG

### 2.1. Pipeline tổng quan

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        AI QUIZ GENERATOR PIPELINE                        │
└─────────────────────────────────────────────────────────────────────────┘

 ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
 │   INPUT      │    │   PROCESS    │    │   GENERATE   │    │   OUTPUT     │
 │   LAYER      │───▶│   LAYER      │───▶│   LAYER      │───▶│   LAYER      │
 └──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
       │                   │                   │                   │
       ▼                   ▼                   ▼                   ▼
 ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
 │ • Lesson Text│    │ • Chunking   │    │ • LLM Call   │    │ • Questions  │
 │ • Video Trans│    │ • Embedding  │    │ • Prompting  │    │ • Answers    │
 │ • PDF/Docs   │    │ • Key Extract│    │ • Formatting │    │ • Metadata   │
 └──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
```

### 2.2. Chi tiết từng bước

#### Bước 1: Thu thập nội dung (Content Collection)

```
INPUT SOURCES
├── 📝 Lesson Text Content
│   └── Rich text từ course builder
├── 🎥 Video Transcript
│   └── Whisper API / Manual transcript
├── 📄 Attached Documents
│   └── PDF, DOCX, PPT extraction
└── 🔗 External Resources
    └── URL content fetching
```

**Kỹ thuật sử dụng:**
- **Text extraction**: Parse HTML, extract plain text
- **Video transcription**: OpenAI Whisper hoặc Google Speech-to-Text
- **Document parsing**: Apache Tika, pdf-parse, mammoth.js

#### Bước 2: Tiền xử lý (Preprocessing)

```python
# Pseudo-code cho preprocessing pipeline
def preprocess(content: str) -> ProcessedContent:
    # 1. Clean và normalize text
    cleaned = clean_text(content)

    # 2. Chunk thành đoạn nhỏ (500-1000 tokens)
    chunks = chunk_text(cleaned, max_tokens=800, overlap=100)

    # 3. Trích xuất key concepts
    key_concepts = extract_concepts(chunks)

    # 4. Tạo embeddings cho semantic search
    embeddings = embed_chunks(chunks)

    return ProcessedContent(chunks, key_concepts, embeddings)
```

**Chunking Strategy:**

| Strategy | Mô tả | Use Case |
|----------|-------|----------|
| **Fixed-size** | Chia theo số tokens cố định | General content |
| **Semantic** | Chia theo nghĩa (paragraph, section) | Structured content |
| **Sliding window** | Overlap giữa các chunks | Dense content |

#### Bước 3: Trích xuất khái niệm (Concept Extraction)

```
KEY CONCEPT EXTRACTION
├── Named Entity Recognition (NER)
│   └── Xác định thuật ngữ, tên riêng, khái niệm
├── Keyword Extraction
│   └── TF-IDF, RAKE, YAKE algorithms
├── Topic Modeling
│   └── LDA, BERTopic cho chủ đề chính
└── Relationship Extraction
    └── Xác định quan hệ giữa các khái niệm
```

#### Bước 4: Sinh câu hỏi (Question Generation)

```
GENERATION PROCESS
│
├── 1. SELECT relevant chunk based on:
│   ├── Key concept density
│   ├── Information richness
│   └── Question type suitability
│
├── 2. CONSTRUCT prompt with:
│   ├── Chunk content
│   ├── Question type specification
│   ├── Difficulty level
│   └── Output format template
│
├── 3. CALL LLM API
│   └── GPT-4 / Claude / Open-source
│
└── 4. PARSE & VALIDATE output
    ├── JSON parsing
    ├── Format validation
    └── Quality checks
```

#### Bước 5: Hậu xử lý (Post-processing)

```python
def postprocess(generated_questions: List[Question]) -> List[Question]:
    validated = []
    for q in generated_questions:
        # 1. Validate format
        if not validate_format(q):
            continue

        # 2. Check for duplicates
        if is_duplicate(q, validated):
            continue

        # 3. Verify answer correctness
        if not verify_answer(q):
            q = regenerate_answer(q)

        # 4. Calculate difficulty score
        q.difficulty = calculate_difficulty(q)

        # 5. Add metadata
        q.metadata = generate_metadata(q)

        validated.append(q)

    return validated
```

---

## PHẦN 3: MÔ HÌNH AI SỬ DỤNG

### 3.1. So sánh các mô hình

| Model | Provider | Ưu điểm | Nhược điểm | Chi phí (1M tokens) |
|-------|----------|---------|------------|---------------------|
| **GPT-4o** | OpenAI | Chất lượng cao, đa ngôn ngữ | Đắt | ~$5 input / $15 output |
| **GPT-4o-mini** | OpenAI | Nhanh, rẻ, chất lượng tốt | Kém hơn GPT-4o | ~$0.15 / $0.60 |
| **Claude 3.5 Sonnet** | Anthropic | Rất tốt cho tiếng Việt | API hạn chế region | ~$3 / $15 |
| **Gemini 1.5 Pro** | Google | Multimodal, context dài | Chất lượng VN kém hơn | ~$1.25 / $5 |
| **Llama 3.1 70B** | Meta (Self-host) | Miễn phí, privacy | Cần GPU, phức tạp | ~$0 (infra cost) |
| **Qwen 2.5 72B** | Alibaba | Tốt cho tiếng Việt/Trung | Self-host phức tạp | ~$0 (infra cost) |

### 3.2. Đề xuất chiến lược Multi-Model

```
┌─────────────────────────────────────────────────────────────────┐
│                    MULTI-MODEL STRATEGY                          │
└─────────────────────────────────────────────────────────────────┘

                    ┌─────────────────┐
                    │  Request Router │
                    └────────┬────────┘
                             │
          ┌──────────────────┼──────────────────┐
          │                  │                  │
          ▼                  ▼                  ▼
   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
   │  TIER 1     │    │  TIER 2     │    │  TIER 3     │
   │  GPT-4o     │    │ GPT-4o-mini │    │ Llama 3.1   │
   │  Premium    │    │  Standard   │    │  Self-host  │
   └─────────────┘    └─────────────┘    └─────────────┘
         │                  │                  │
         ▼                  ▼                  ▼
   Complex/Essay      Multiple Choice     Simple/Bulk
   Long-form          Fill-in-blank       Generation
```

**Routing Logic:**

| Loại câu hỏi | Model đề xuất | Lý do |
|--------------|---------------|-------|
| **Essay/Long-form** | GPT-4o | Cần reasoning sâu |
| **Multiple Choice** | GPT-4o-mini | Cân bằng chất lượng/chi phí |
| **True/False** | Llama 3.1 | Đơn giản, bulk generation |
| **Fill-in-blank** | GPT-4o-mini | Pattern matching tốt |
| **Matching** | GPT-4o-mini | Cần hiểu quan hệ |

### 3.3. Prompt Engineering

#### Template cơ bản:

```
SYSTEM PROMPT:
Bạn là một chuyên gia giáo dục với nhiệm vụ tạo câu hỏi kiểm tra
chất lượng cao từ nội dung bài học. Câu hỏi phải:
1. Phù hợp với nội dung được cung cấp
2. Rõ ràng, không mơ hồ
3. Có độ khó phù hợp với cấp độ yêu cầu
4. Tuân theo chuẩn Bloom's Taxonomy

USER PROMPT:
## Nội dung bài học:
{lesson_content}

## Yêu cầu:
- Loại câu hỏi: {question_type}
- Số lượng: {num_questions}
- Độ khó: {difficulty_level}
- Cấp độ Bloom: {bloom_level}

## Format đầu ra (JSON):
{output_format_template}
```

#### Output Format Template:

```json
{
  "questions": [
    {
      "id": "q1",
      "type": "multiple_choice",
      "question": "Câu hỏi...",
      "options": [
        {"key": "A", "text": "Đáp án A"},
        {"key": "B", "text": "Đáp án B"},
        {"key": "C", "text": "Đáp án C"},
        {"key": "D", "text": "Đáp án D"}
      ],
      "correct_answer": "B",
      "explanation": "Giải thích tại sao B đúng...",
      "difficulty": "medium",
      "bloom_level": "understand",
      "source_chunk": "chunk_id_123",
      "tags": ["concept_1", "topic_2"]
    }
  ]
}
```

### 3.4. Fine-tuning (Tùy chọn nâng cao)

**Khi nào cần Fine-tuning:**
- Có đủ dữ liệu (>1000 câu hỏi mẫu)
- Cần format/style nhất quán
- Muốn giảm chi phí API dài hạn

**Dataset cho Fine-tuning:**

```json
{
  "messages": [
    {
      "role": "system",
      "content": "Bạn là AI tạo câu hỏi kiểm tra..."
    },
    {
      "role": "user",
      "content": "Nội dung: {content}\nTạo 3 câu hỏi trắc nghiệm..."
    },
    {
      "role": "assistant",
      "content": "{expected_output_json}"
    }
  ]
}
```

---

## PHẦN 4: NGUỒN DỮ LIỆU

### 4.1. Các nguồn dữ liệu đầu vào

```
┌─────────────────────────────────────────────────────────────────┐
│                      DATA SOURCES                                │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ PRIMARY SOURCES │  │SECONDARY SOURCES│  │ ENRICHMENT      │
│ (Bắt buộc)      │  │ (Tùy chọn)      │  │ (Bổ sung)       │
└────────┬────────┘  └────────┬────────┘  └────────┬────────┘
         │                    │                    │
         ▼                    ▼                    ▼
   ┌───────────┐        ┌───────────┐        ┌───────────┐
   │• Lesson   │        │• PDF/DOCX │        │• Wikipedia│
   │  Content  │        │• PPT Slides│       │• Textbooks│
   │• Video    │        │• External │        │• Q&A DB   │
   │  Transcript│       │  Links    │        │           │
   └───────────┘        └───────────┘        └───────────┘
```

### 4.2. Chi tiết từng nguồn

#### 4.2.1. Lesson Content (Nội dung bài học)

```typescript
interface LessonContent {
  id: string;
  title: string;
  content: string;        // HTML/Markdown content
  plainText: string;      // Extracted plain text
  wordCount: number;
  language: "vi" | "en";
  topics: string[];       // Extracted topics
  lastUpdated: Date;
}
```

**Xử lý:**
- Sanitize HTML, remove scripts/styles
- Convert to plain text
- Detect language
- Extract headings as topics

#### 4.2.2. Video Transcript

```typescript
interface VideoTranscript {
  videoId: string;
  duration: number;       // seconds
  transcript: TranscriptSegment[];
  fullText: string;       // Concatenated text
  language: "vi" | "en";
  confidence: number;     // ASR confidence
}

interface TranscriptSegment {
  start: number;          // Start time in seconds
  end: number;
  text: string;
  confidence: number;
}
```

**Transcription Pipeline:**

```
┌──────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────┐
│  Video   │───▶│ Audio Extract│───▶│ Whisper API  │───▶│Transcript│
│  File    │    │   (FFmpeg)   │    │  (OpenAI)    │    │  JSON    │
└──────────┘    └──────────────┘    └──────────────┘    └──────────┘
```

**Chi phí Whisper API:**
- $0.006 / minute
- 1 giờ video = $0.36

#### 4.2.3. Attached Documents

| Format | Library | Notes |
|--------|---------|-------|
| PDF | pdf-parse, Apache Tika | OCR nếu là scan |
| DOCX | mammoth.js | Giữ structure |
| PPTX | pptx-parser | Extract text + notes |
| TXT/MD | Native | Direct read |

### 4.3. Dữ liệu bổ sung (Enrichment)

**Question Bank (Ngân hàng câu hỏi):**
- Lưu trữ câu hỏi đã tạo
- Dùng để check duplicate
- Reference cho similar questions

**External Knowledge:**
- Wikipedia API cho định nghĩa
- Textbook databases (nếu có license)
- Course-specific glossary

---

## PHẦN 5: CÁC LOẠI CÂU HỎI

### 5.1. Taxonomy câu hỏi

```
QUESTION TYPES
│
├── OBJECTIVE (Khách quan)
│   ├── Multiple Choice (Trắc nghiệm)
│   │   ├── Single Answer
│   │   └── Multiple Answers
│   ├── True/False (Đúng/Sai)
│   ├── Fill-in-the-blank (Điền khuyết)
│   └── Matching (Nối cặp)
│
├── SUBJECTIVE (Chủ quan)
│   ├── Short Answer (Trả lời ngắn)
│   └── Essay (Tự luận)
│
└── INTERACTIVE (Tương tác)
    ├── Ordering (Sắp xếp thứ tự)
    └── Drag-and-Drop
```

### 5.2. Chi tiết từng loại

#### Multiple Choice (Trắc nghiệm)

```json
{
  "type": "multiple_choice",
  "subtype": "single_answer",
  "question": "HTTP Status Code 404 có ý nghĩa gì?",
  "options": [
    {"key": "A", "text": "Server Error"},
    {"key": "B", "text": "Not Found"},
    {"key": "C", "text": "Unauthorized"},
    {"key": "D", "text": "Bad Request"}
  ],
  "correct_answer": "B",
  "explanation": "404 Not Found nghĩa là resource không tồn tại...",
  "distractor_analysis": {
    "A": "Đây là 5xx errors",
    "C": "Đây là 401",
    "D": "Đây là 400"
  }
}
```

**Best Practices:**
- 4 options (A, B, C, D)
- Distractors phải hợp lý (plausible)
- Tránh "All of the above" / "None of the above"
- Đáp án đúng random vị trí

#### True/False (Đúng/Sai)

```json
{
  "type": "true_false",
  "statement": "REST API phải sử dụng JSON format cho tất cả request.",
  "correct_answer": false,
  "explanation": "REST API có thể sử dụng nhiều format như XML, JSON,
                  Plain Text. JSON chỉ là format phổ biến nhất."
}
```

#### Fill-in-the-blank (Điền khuyết)

```json
{
  "type": "fill_blank",
  "question": "Trong React, _____ được sử dụng để quản lý state trong
               functional components.",
  "blanks": [
    {
      "id": 1,
      "correct_answers": ["useState", "useState hook", "useState()"],
      "case_sensitive": false
    }
  ]
}
```

#### Matching (Nối cặp)

```json
{
  "type": "matching",
  "instruction": "Nối HTTP method với mục đích sử dụng:",
  "left_items": [
    {"id": "L1", "text": "GET"},
    {"id": "L2", "text": "POST"},
    {"id": "L3", "text": "PUT"},
    {"id": "L4", "text": "DELETE"}
  ],
  "right_items": [
    {"id": "R1", "text": "Tạo mới resource"},
    {"id": "R2", "text": "Lấy thông tin"},
    {"id": "R3", "text": "Xóa resource"},
    {"id": "R4", "text": "Cập nhật toàn bộ"}
  ],
  "correct_pairs": [
    ["L1", "R2"],
    ["L2", "R1"],
    ["L3", "R4"],
    ["L4", "R3"]
  ]
}
```

#### Essay (Tự luận)

```json
{
  "type": "essay",
  "question": "Phân tích ưu và nhược điểm của kiến trúc Microservices
               so với Monolithic. Cho ví dụ cụ thể.",
  "word_limit": {"min": 200, "max": 500},
  "rubric": [
    {"criteria": "Hiểu đúng khái niệm", "max_points": 3},
    {"criteria": "Phân tích ưu điểm", "max_points": 3},
    {"criteria": "Phân tích nhược điểm", "max_points": 3},
    {"criteria": "Ví dụ phù hợp", "max_points": 1}
  ],
  "sample_answer": "Microservices là kiến trúc chia nhỏ ứng dụng...",
  "keywords": ["scalability", "độc lập", "phức tạp", "network latency"]
}
```

### 5.3. Bloom's Taxonomy Mapping

```
BLOOM'S TAXONOMY LEVELS
│
├── Level 1: REMEMBER (Nhớ)
│   └── Question types: True/False, Simple MCQ, Fill-blank (định nghĩa)
│   └── Verbs: Liệt kê, định nghĩa, nhận biết, gọi tên
│
├── Level 2: UNDERSTAND (Hiểu)
│   └── Question types: MCQ with explanation, Short answer
│   └── Verbs: Giải thích, mô tả, phân loại, so sánh
│
├── Level 3: APPLY (Áp dụng)
│   └── Question types: Problem-solving MCQ, Fill-blank (code)
│   └── Verbs: Sử dụng, thực hiện, giải quyết, tính toán
│
├── Level 4: ANALYZE (Phân tích)
│   └── Question types: Complex MCQ, Short essay
│   └── Verbs: Phân tích, so sánh, đối chiếu, kiểm tra
│
├── Level 5: EVALUATE (Đánh giá)
│   └── Question types: Essay, Case study
│   └── Verbs: Đánh giá, phê bình, biện luận, đề xuất
│
└── Level 6: CREATE (Sáng tạo)
    └── Question types: Open-ended essay, Project
    └── Verbs: Thiết kế, xây dựng, phát triển, sáng tạo
```

---

## PHẦN 6: ĐÁNH GIÁ CHẤT LƯỢNG

### 6.1. Khung đánh giá chất lượng câu hỏi

```
QUESTION QUALITY FRAMEWORK
│
├── 1. VALIDITY (Tính hợp lệ)
│   ├── Content validity: Câu hỏi đúng với nội dung
│   ├── Construct validity: Đo đúng kiến thức cần đo
│   └── Face validity: Nhìn vào thấy hợp lý
│
├── 2. RELIABILITY (Tính tin cậy)
│   ├── Consistency: Kết quả nhất quán
│   └── Discrimination: Phân biệt được người giỏi/yếu
│
├── 3. CLARITY (Tính rõ ràng)
│   ├── Unambiguous: Không mơ hồ
│   ├── Single interpretation: Chỉ 1 cách hiểu
│   └── Appropriate language: Ngôn ngữ phù hợp
│
├── 4. DIFFICULTY (Độ khó)
│   ├── Appropriate level: Phù hợp trình độ
│   └── Balanced distribution: Phân bố hợp lý
│
└── 5. RELEVANCE (Tính liên quan)
    ├── Learning objectives: Đúng mục tiêu học
    └── Practical application: Áp dụng thực tế
```

### 6.2. Metrics đánh giá tự động

#### 6.2.1. Content Similarity Score

```python
def calculate_content_similarity(question: str, source_content: str) -> float:
    """
    Đo độ tương đồng giữa câu hỏi và nội dung nguồn
    Sử dụng sentence embeddings (sentence-transformers)

    Returns: 0.0 - 1.0 (1.0 = hoàn toàn liên quan)
    """
    q_embedding = embed(question)
    c_embedding = embed(source_content)

    similarity = cosine_similarity(q_embedding, c_embedding)
    return similarity
```

**Ngưỡng chấp nhận:** similarity >= 0.6

#### 6.2.2. Readability Score

```python
def calculate_readability(question: str, target_level: str) -> dict:
    """
    Đánh giá độ dễ đọc của câu hỏi

    Metrics:
    - Flesch Reading Ease (adapted for Vietnamese)
    - Average sentence length
    - Complex word ratio
    """
    flesch_score = flesch_reading_ease(question)
    avg_sentence_len = len(question.split()) / count_sentences(question)
    complex_ratio = count_complex_words(question) / count_words(question)

    return {
        "flesch_score": flesch_score,
        "avg_sentence_length": avg_sentence_len,
        "complex_word_ratio": complex_ratio,
        "grade_level": estimate_grade_level(flesch_score)
    }
```

#### 6.2.3. Answer Verification Score

```python
def verify_answer(question: dict, source_content: str) -> VerificationResult:
    """
    Xác minh đáp án đúng có thực sự đúng không

    Approach:
    1. Ask LLM to answer the question given the source content
    2. Compare with provided answer
    3. Ask LLM to explain if there's a discrepancy
    """
    # Generate answer from content
    generated_answer = llm_answer_question(question["question"], source_content)

    # Compare with provided answer
    match = compare_answers(generated_answer, question["correct_answer"])

    return VerificationResult(
        is_verified=match,
        confidence=match_confidence,
        explanation=explanation
    )
```

#### 6.2.4. Distractor Quality Score (for MCQ)

```python
def evaluate_distractors(question: dict) -> DistractorAnalysis:
    """
    Đánh giá chất lượng các đáp án nhiễu (distractors)

    Good distractors:
    - Plausible (có vẻ hợp lý)
    - Related to topic
    - Not obviously wrong
    - Distinct from each other
    """
    distractors = [opt for opt in question["options"]
                   if opt["key"] != question["correct_answer"]]

    scores = {
        "plausibility": evaluate_plausibility(distractors),
        "relevance": evaluate_relevance(distractors, question["question"]),
        "distinctness": evaluate_distinctness(distractors),
        "difficulty_balance": evaluate_difficulty(distractors)
    }

    return DistractorAnalysis(
        overall_score=average(scores.values()),
        breakdown=scores,
        suggestions=generate_improvement_suggestions(scores)
    )
```

### 6.3. Human Review Process

```
HUMAN REVIEW WORKFLOW
│
├── STAGE 1: Automated Filtering
│   ├── Remove low similarity (<0.5)
│   ├── Flag duplicate questions
│   └── Flag format errors
│
├── STAGE 2: AI Pre-review
│   ├── LLM checks question quality
│   ├── Suggests improvements
│   └── Confidence scoring
│
├── STAGE 3: Human Expert Review
│   ├── Content accuracy check
│   ├── Pedagogical appropriateness
│   ├── Language/grammar review
│   └── Approve/Reject/Edit
│
└── STAGE 4: Feedback Loop
    ├── Store review decisions
    ├── Improve prompts based on feedback
    └── Update quality thresholds
```

### 6.4. Quality Dashboard Metrics

| Metric | Mô tả | Target |
|--------|-------|--------|
| **Acceptance Rate** | % câu hỏi được approve | >= 80% |
| **Average Quality Score** | Điểm chất lượng trung bình | >= 4.0/5.0 |
| **Answer Accuracy** | % đáp án đúng xác minh được | >= 95% |
| **Time-to-Review** | Thời gian review trung bình | < 30s/question |
| **Regeneration Rate** | % câu hỏi cần sinh lại | < 10% |

---

## PHẦN 7: KIẾN TRÚC TÍCH HỢP

### 7.1. Tích hợp với KiteClass Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         KITECLASS PLATFORM                               │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ KITEHUB (Modular Monolith)                                               │
│ ┌───────────────┐ ┌───────────────┐ ┌───────────────┐ ┌───────────────┐ │
│ │ Sale Module   │ │ Message Module│ │AI Agent Module│ │ Maintaining   │ │
│ └───────────────┘ └───────────────┘ └───────┬───────┘ └───────────────┘ │
└───────────────────────────────────────────────┼─────────────────────────┘
                                                │
                        ┌───────────────────────┴───────────────────────┐
                        │            AI AGENT MODULE                     │
                        │  ┌─────────────┐  ┌─────────────────────────┐ │
                        │  │ Branding    │  │    QUIZ GENERATOR      │ │
                        │  │ Generator   │  │    (NEW SERVICE)       │ │
                        │  │ (Existing)  │  │                         │ │
                        │  └─────────────┘  └─────────────────────────┘ │
                        └───────────────────────────────────────────────┘
                                                │
┌───────────────────────────────────────────────┼─────────────────────────┐
│ KITECLASS INSTANCE (Microservices)            │                          │
│ ┌───────────────┐ ┌───────────────┐ ┌─────────▼─────────┐              │
│ │ Main Class    │ │ CMC Service   │ │   Quiz Service   │◀── Extended  │
│ │ Service       │ │               │ │   (Extended)      │              │
│ └───────────────┘ └───────────────┘ └───────────────────┘              │
└─────────────────────────────────────────────────────────────────────────┘
```

### 7.2. Service Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    AI QUIZ GENERATOR SERVICE                             │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   API Gateway   │───▶│  Quiz Generator │───▶│  Question Store │
│   (Kong/Nginx)  │    │     Service     │    │   (PostgreSQL)  │
└─────────────────┘    └────────┬────────┘    └─────────────────┘
                                │
                    ┌───────────┼───────────┐
                    │           │           │
                    ▼           ▼           ▼
             ┌──────────┐ ┌──────────┐ ┌──────────┐
             │ Content  │ │   LLM    │ │ Quality  │
             │ Processor│ │  Client  │ │ Checker  │
             └──────────┘ └──────────┘ └──────────┘
                    │           │           │
                    ▼           ▼           ▼
             ┌──────────┐ ┌──────────┐ ┌──────────┐
             │ Whisper  │ │ OpenAI/  │ │ Embedding│
             │   API    │ │ Claude   │ │  Model   │
             └──────────┘ └──────────┘ └──────────┘
```

### 7.3. API Design

#### Endpoints

```yaml
openapi: 3.0.0
info:
  title: AI Quiz Generator API
  version: 1.0.0

paths:
  /api/v1/quiz/generate:
    post:
      summary: Generate quiz questions from content
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/GenerateRequest'
      responses:
        '200':
          description: Generated questions
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/GenerateResponse'

  /api/v1/quiz/generate-from-lesson/{lessonId}:
    post:
      summary: Generate quiz from lesson ID
      parameters:
        - name: lessonId
          in: path
          required: true
          schema:
            type: string
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/LessonGenerateRequest'

  /api/v1/quiz/verify:
    post:
      summary: Verify question quality
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/VerifyRequest'

  /api/v1/quiz/regenerate/{questionId}:
    post:
      summary: Regenerate a specific question

  /api/v1/quiz/bank:
    get:
      summary: List questions in bank
    post:
      summary: Add question to bank

components:
  schemas:
    GenerateRequest:
      type: object
      required:
        - content
        - numQuestions
      properties:
        content:
          type: string
          description: Source content for question generation
        numQuestions:
          type: integer
          minimum: 1
          maximum: 50
        questionTypes:
          type: array
          items:
            type: string
            enum: [multiple_choice, true_false, fill_blank, matching, essay]
        difficultyLevel:
          type: string
          enum: [easy, medium, hard, mixed]
        bloomLevels:
          type: array
          items:
            type: string
            enum: [remember, understand, apply, analyze, evaluate, create]
        language:
          type: string
          enum: [vi, en]
          default: vi

    GenerateResponse:
      type: object
      properties:
        jobId:
          type: string
        status:
          type: string
          enum: [pending, processing, completed, failed]
        questions:
          type: array
          items:
            $ref: '#/components/schemas/Question'
        metadata:
          type: object
          properties:
            generatedAt:
              type: string
              format: date-time
            processingTimeMs:
              type: integer
            modelUsed:
              type: string
            qualityScore:
              type: number
```

### 7.4. Database Schema

```sql
-- Questions table
CREATE TABLE questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(50) NOT NULL,
    question_text TEXT NOT NULL,
    options JSONB,           -- For MCQ, matching
    correct_answer JSONB NOT NULL,
    explanation TEXT,
    difficulty VARCHAR(20),
    bloom_level VARCHAR(20),

    -- Source tracking
    source_lesson_id UUID REFERENCES lessons(id),
    source_chunk_id VARCHAR(100),
    content_hash VARCHAR(64),    -- To detect if source changed

    -- Quality metrics
    quality_score DECIMAL(3,2),
    is_verified BOOLEAN DEFAULT FALSE,
    is_approved BOOLEAN DEFAULT FALSE,

    -- Metadata
    language VARCHAR(10) DEFAULT 'vi',
    tags TEXT[],
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    -- Usage stats
    times_used INTEGER DEFAULT 0,
    avg_correct_rate DECIMAL(5,4),
    discrimination_index DECIMAL(5,4)
);

-- Generation jobs table
CREATE TABLE quiz_generation_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    status VARCHAR(20) DEFAULT 'pending',

    -- Input
    input_content TEXT,
    input_lesson_id UUID,
    input_params JSONB,

    -- Output
    generated_questions UUID[],

    -- Tracking
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    model_used VARCHAR(50),
    tokens_used INTEGER,
    cost_usd DECIMAL(10,6),

    created_at TIMESTAMP DEFAULT NOW()
);

-- Question bank (curated questions)
CREATE TABLE question_bank (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID REFERENCES questions(id),
    course_id UUID REFERENCES courses(id),
    category VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    added_at TIMESTAMP DEFAULT NOW(),
    added_by UUID REFERENCES users(id)
);

-- Indexes
CREATE INDEX idx_questions_lesson ON questions(source_lesson_id);
CREATE INDEX idx_questions_type ON questions(type);
CREATE INDEX idx_questions_difficulty ON questions(difficulty);
CREATE INDEX idx_questions_tags ON questions USING GIN(tags);
```

---

## PHẦN 8: CHI TIẾT TRIỂN KHAI

### 8.1. Technology Stack

| Component | Technology | Lý do chọn |
|-----------|------------|------------|
| **Backend** | NestJS (Node.js) | Consistent với KiteClass stack |
| **Database** | PostgreSQL + pgvector | Quan hệ + vector search |
| **Queue** | Redis + BullMQ | Async job processing |
| **LLM Client** | LangChain.js | Multi-model abstraction |
| **Embedding** | OpenAI text-embedding-3-small | Tốt cho multilingual |
| **Transcription** | OpenAI Whisper | Accurate cho tiếng Việt |
| **Cache** | Redis | Response caching |

### 8.2. Code Structure

```
quiz-generator/
├── src/
│   ├── modules/
│   │   ├── generator/
│   │   │   ├── generator.controller.ts
│   │   │   ├── generator.service.ts
│   │   │   ├── generator.module.ts
│   │   │   └── dto/
│   │   │       ├── generate-request.dto.ts
│   │   │       └── generate-response.dto.ts
│   │   │
│   │   ├── processor/
│   │   │   ├── content-processor.service.ts
│   │   │   ├── transcript.service.ts
│   │   │   └── document-parser.service.ts
│   │   │
│   │   ├── llm/
│   │   │   ├── llm.service.ts
│   │   │   ├── prompt-templates/
│   │   │   │   ├── mcq.prompt.ts
│   │   │   │   ├── true-false.prompt.ts
│   │   │   │   ├── fill-blank.prompt.ts
│   │   │   │   └── essay.prompt.ts
│   │   │   └── models/
│   │   │       ├── openai.adapter.ts
│   │   │       ├── claude.adapter.ts
│   │   │       └── llama.adapter.ts
│   │   │
│   │   ├── quality/
│   │   │   ├── quality-checker.service.ts
│   │   │   ├── similarity.service.ts
│   │   │   ├── answer-verifier.service.ts
│   │   │   └── distractor-analyzer.service.ts
│   │   │
│   │   ├── bank/
│   │   │   ├── question-bank.controller.ts
│   │   │   ├── question-bank.service.ts
│   │   │   └── question-bank.module.ts
│   │   │
│   │   └── common/
│   │       ├── entities/
│   │       ├── interfaces/
│   │       └── utils/
│   │
│   ├── jobs/
│   │   ├── generation.processor.ts
│   │   └── quality-check.processor.ts
│   │
│   └── main.ts
│
├── test/
│   ├── unit/
│   └── e2e/
│
├── prisma/
│   └── schema.prisma
│
└── docker/
    ├── Dockerfile
    └── docker-compose.yml
```

### 8.3. Sample Implementation

#### Generator Service

```typescript
// generator.service.ts
@Injectable()
export class GeneratorService {
  constructor(
    private contentProcessor: ContentProcessorService,
    private llmService: LLMService,
    private qualityChecker: QualityCheckerService,
    private questionRepository: QuestionRepository,
    @InjectQueue('quiz-generation') private generationQueue: Queue,
  ) {}

  async generateFromContent(request: GenerateRequestDto): Promise<GenerationJob> {
    // 1. Create job
    const job = await this.createJob(request);

    // 2. Add to queue for async processing
    await this.generationQueue.add('generate', {
      jobId: job.id,
      content: request.content,
      params: request,
    });

    return job;
  }

  async processGeneration(jobId: string, content: string, params: GenerateParams) {
    try {
      // 1. Preprocess content
      const processed = await this.contentProcessor.process(content);

      // 2. Select relevant chunks
      const chunks = this.selectChunks(processed.chunks, params.numQuestions);

      // 3. Generate questions for each chunk
      const questions: Question[] = [];

      for (const chunk of chunks) {
        const generated = await this.generateForChunk(chunk, params);
        questions.push(...generated);
      }

      // 4. Quality check
      const verified = await this.qualityChecker.verifyBatch(questions, content);

      // 5. Save to database
      const saved = await this.questionRepository.saveBatch(verified);

      // 6. Update job status
      await this.updateJobCompleted(jobId, saved);

      return saved;

    } catch (error) {
      await this.updateJobFailed(jobId, error);
      throw error;
    }
  }

  private async generateForChunk(
    chunk: ContentChunk,
    params: GenerateParams
  ): Promise<Question[]> {
    const questionsPerType = this.distributeQuestions(
      params.numQuestions / chunks.length,
      params.questionTypes
    );

    const results: Question[] = [];

    for (const [type, count] of Object.entries(questionsPerType)) {
      const prompt = this.buildPrompt(type, chunk, count, params);
      const response = await this.llmService.generate(prompt, params.model);
      const parsed = this.parseResponse(response, type);
      results.push(...parsed);
    }

    return results;
  }
}
```

#### Prompt Template

```typescript
// prompt-templates/mcq.prompt.ts
export const MCQ_PROMPT = `
Bạn là một chuyên gia giáo dục với nhiệm vụ tạo câu hỏi trắc nghiệm
chất lượng cao từ nội dung bài học.

## Nội dung bài học:
{content}

## Yêu cầu:
- Tạo {numQuestions} câu hỏi trắc nghiệm
- Độ khó: {difficulty}
- Cấp độ Bloom: {bloomLevel}
- Ngôn ngữ: {language}

## Quy tắc tạo câu hỏi:
1. Mỗi câu hỏi phải có 4 lựa chọn (A, B, C, D)
2. Chỉ có 1 đáp án đúng
3. Các đáp án nhiễu (distractors) phải hợp lý, không quá dễ loại bỏ
4. Câu hỏi phải rõ ràng, không mơ hồ
5. Phải có giải thích cho đáp án đúng
6. Đáp án đúng phải nằm ở vị trí ngẫu nhiên

## Format đầu ra (JSON):
{
  "questions": [
    {
      "question": "Nội dung câu hỏi",
      "options": [
        {"key": "A", "text": "Đáp án A"},
        {"key": "B", "text": "Đáp án B"},
        {"key": "C", "text": "Đáp án C"},
        {"key": "D", "text": "Đáp án D"}
      ],
      "correct_answer": "B",
      "explanation": "Giải thích chi tiết tại sao B đúng...",
      "difficulty": "medium",
      "bloom_level": "understand"
    }
  ]
}

Chỉ trả về JSON, không có text khác.
`;
```

---

## PHẦN 9: THÁCH THỨC VÀ GIẢI PHÁP

### 9.1. Thách thức kỹ thuật

| # | Thách thức | Giải pháp |
|---|------------|-----------|
| 1 | **Chất lượng câu hỏi không ổn định** | Multi-stage validation + Human review cho batch đầu |
| 2 | **Đáp án sai** | Answer verification pipeline + LLM cross-check |
| 3 | **Distractors quá dễ/khó** | Distractor quality analysis + Regeneration |
| 4 | **Duplicate questions** | Semantic similarity check với embedding |
| 5 | **Content quá ngắn** | Enrich với external knowledge + Warning |
| 6 | **Tiếng Việt có dấu** | Sử dụng model hỗ trợ tốt (GPT-4, Claude) |
| 7 | **Latency cao** | Queue-based async processing + Caching |
| 8 | **Cost cao** | Tiered model strategy + Response caching |

### 9.2. Chi tiết giải pháp

#### Vấn đề 1: Chất lượng không ổn định

```
QUALITY ASSURANCE PIPELINE
│
├── LAYER 1: Prompt Engineering
│   ├── Detailed instructions
│   ├── Few-shot examples
│   └── Output format constraints
│
├── LAYER 2: Post-processing
│   ├── JSON validation
│   ├── Field completeness check
│   └── Format normalization
│
├── LAYER 3: Automated QA
│   ├── Similarity score check
│   ├── Answer verification
│   └── Difficulty calibration
│
└── LAYER 4: Human Review (initial batches)
    ├── Expert review queue
    ├── Feedback collection
    └── Prompt improvement
```

#### Vấn đề 2: Latency cao

```
PERFORMANCE OPTIMIZATION
│
├── ASYNC PROCESSING
│   ├── Queue-based generation
│   ├── Webhook/Polling for results
│   └── Batch processing
│
├── CACHING
│   ├── Response cache (same content = same questions)
│   ├── Embedding cache
│   └── Prompt template cache
│
├── PARALLEL PROCESSING
│   ├── Generate different types in parallel
│   ├── Process multiple chunks concurrently
│   └── Batch LLM calls
│
└── MODEL SELECTION
    ├── Fast model for simple questions
    ├── Powerful model for complex ones
    └── Local model for bulk generation
```

#### Vấn đề 3: Cost optimization

```
COST REDUCTION STRATEGIES
│
├── MODEL TIERING
│   │
│   ├── Tier 1: GPT-4o ($15/1M output)
│   │   └── Complex essay, edge cases
│   │
│   ├── Tier 2: GPT-4o-mini ($0.60/1M output)
│   │   └── Standard MCQ, fill-blank
│   │
│   └── Tier 3: Self-hosted Llama ($0)
│       └── True/false, bulk generation
│
├── CACHING
│   ├── Hash content → Cache generated questions
│   └── TTL: 30 days (or until content changes)
│
├── PROMPT OPTIMIZATION
│   ├── Concise prompts (fewer input tokens)
│   └── Efficient output format
│
└── BATCHING
    └── Combine multiple generation requests
```

---

## PHẦN 10: CHI PHÍ ƯỚC TÍNH

### 10.1. Chi phí API

| Component | Pricing | Est. Usage/Month | Est. Cost/Month |
|-----------|---------|------------------|-----------------|
| **GPT-4o-mini** | $0.15/1M in, $0.60/1M out | 10M tokens | ~$7.50 |
| **GPT-4o** (10% requests) | $2.50/1M in, $10/1M out | 1M tokens | ~$12.50 |
| **Whisper API** | $0.006/minute | 100 hours | ~$36 |
| **Embeddings** | $0.02/1M tokens | 5M tokens | ~$0.10 |
| **Total API** | | | **~$56/month** |

### 10.2. Chi phí Infrastructure

| Component | Specification | Est. Cost/Month |
|-----------|---------------|-----------------|
| **Quiz Service** (ECS) | 1 vCPU, 2GB RAM | ~$30 |
| **PostgreSQL** (RDS) | db.t3.micro + pgvector | ~$25 |
| **Redis** (ElastiCache) | cache.t3.micro | ~$15 |
| **S3** (documents) | 10GB storage | ~$2 |
| **Total Infra** | | **~$72/month** |

### 10.3. Tổng chi phí

| Category | Monthly | Yearly |
|----------|---------|--------|
| API Costs | $56 | $672 |
| Infrastructure | $72 | $864 |
| **TOTAL** | **$128** | **$1,536** |

> **Note:** Chi phí sẽ scale theo usage. Ước tính trên dựa trên:
> - 100 courses active
> - 500 lessons/month cần generate quiz
> - 10 questions/lesson average

### 10.4. Cost per Question

```
Cost Breakdown per Question:
├── API cost: ~$0.01
├── Infra cost: ~$0.005
└── Total: ~$0.015/question

Comparison with manual:
├── Instructor time: 5-10 mins/question
├── Hourly rate: $20/hour (Vietnam)
├── Manual cost: $1.67 - $3.33/question
│
└── Savings: 99%+ cost reduction
```

---

## PHẦN 11: ROADMAP TRIỂN KHAI

### Phase 1: MVP (4 tuần)

```
Week 1-2: Core Development
├── [ ] Setup project structure
├── [ ] Implement content processor
├── [ ] Integrate OpenAI API
├── [ ] Basic MCQ generation
└── [ ] Database schema

Week 3: Quality & Testing
├── [ ] Quality checker service
├── [ ] Answer verification
├── [ ] Unit tests
└── [ ] Integration tests

Week 4: Integration
├── [ ] API endpoints
├── [ ] Connect with Main Class Service
├── [ ] Basic UI for instructors
└── [ ] Documentation
```

### Phase 2: Enhancement (3 tuần)

```
├── [ ] Additional question types (fill-blank, matching)
├── [ ] Video transcript integration
├── [ ] Question bank management
├── [ ] Difficulty calibration
├── [ ] Model tiering (cost optimization)
└── [ ] Admin dashboard
```

### Phase 3: Advanced (3 tuần)

```
├── [ ] Essay question generation
├── [ ] AI grading for essay (separate feature)
├── [ ] Analytics & reporting
├── [ ] A/B testing framework
├── [ ] Fine-tuning pipeline (optional)
└── [ ] Mobile optimization
```

---

## PHẦN 12: KẾT LUẬN

### 12.1. Tóm tắt

AI Quiz Generator là một tính năng quan trọng giúp KiteClass:
1. **Tiết kiệm thời gian** cho instructors (80%+ reduction)
2. **Tăng chất lượng** bài kiểm tra với đánh giá tự động
3. **Đa dạng hóa** loại câu hỏi và độ khó
4. **Cạnh tranh** với Azota trong thị trường Việt Nam

### 12.2. Điểm khác biệt với Azota

| Tiêu chí | Azota | KiteClass (Đề xuất) |
|----------|-------|---------------------|
| Focus | Số hóa đề thi có sẵn | Sinh câu hỏi mới từ content |
| Input | Ảnh/scan đề thi | Nội dung khóa học |
| Tích hợp | Standalone platform | Tích hợp trong LMS |
| Value | Save time digitizing | Save time creating |

### 12.3. Khuyến nghị

1. **Bắt đầu với MVP** tập trung MCQ - loại câu hỏi phổ biến nhất
2. **Human-in-the-loop** cho 3 tháng đầu để collect feedback
3. **Iterate fast** dựa trên instructor feedback
4. **Monitor costs** và optimize model usage

---

## NGUỒN THAM KHẢO

1. OpenAI API Documentation - https://platform.openai.com/docs
2. Anthropic Claude Documentation - https://docs.anthropic.com
3. LangChain.js - https://js.langchain.com
4. Bloom's Taxonomy - https://www.bloomstaxonomy.net
5. Azota.vn - https://azota.vn
6. Question Generation Research Papers:
   - "Neural Question Generation: A Survey" (2022)
   - "Automatic Question Generation from Text" (2023)

---

*Báo cáo được tạo bởi: KiteClass Development Team*
*Ngày: 23/12/2025*
*Phiên bản: 1.0*
