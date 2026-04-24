# DOCX 3-Pipeline Taxonomy

Adapted from MiniMax `minimax-docx` skill pattern. Wave 5 implements **Create** only; the other two pipelines are documented here so future waves can extend without rearchitecting.

## 1. Create (Wave 5 — shipped)

Build a fresh `XWPFDocument` programmatically from structured data.

- **Input:** typed data map (teacher name, tenant name, dates, salary, subjects).
- **Output:** byte[] of a new .docx.
- **Library calls:** `new XWPFDocument()` → `createParagraph()` → `createRun()` → `doc.write(stream)`.
- **Validation:** via `XWPFDocument.open(bytes)` round-trip + content assertions in unit tests.
- **Pros:** full control over structure; no template artefacts to maintain; deterministic output.
- **Cons:** layout lives in code, not in a designer-friendly .docx template; harder for non-dev stakeholders to tweak copy.

## 2. Edit-Fill (deferred)

Load an existing .docx template with `{{placeholder}}` markers, substitute, write back.

- **Input:** template file path + data map.
- **Output:** byte[] of rendered .docx with placeholders replaced.
- **Library calls:** `new XWPFDocument(FileInputStream)` → iterate paragraphs → text-replace → `doc.write(stream)`.
- **Validation:** compare rendered output against expected replacement set; schema-validate against Word's OOXML XSD.
- **Pros:** stakeholder can edit template in Word; separates copy from code.
- **Cons:** placeholder-in-run boundary issues (Word often splits `{{foo}}` across `<w:r>` elements when user edits); needs robust run-normalisation logic.

## 3. Reformat (deferred)

Read an existing .docx, restructure, emit a new one.

- **Input:** source .docx bytes + transformation spec (e.g., "strip revision tracking, reapply tenant header").
- **Output:** byte[] of restructured .docx.
- **Library calls:** full XWPF model traversal; paragraph / run manipulation; CT element rewriting.
- **Validation:** XSD schema validation against `CT_Document` definition.
- **Pros:** enables "re-brand an uploaded contract" flows.
- **Cons:** largest surface; most fragile; requires deep understanding of Word OOXML internals.

## XSD Validation Approach (Create pipeline)

POI XWPF does light schema validation at load time (catches malformed OOXML). For production-grade validation:

1. Serialise the generated document to a temporary file.
2. Use `javax.xml.validation.Validator` with the OOXML Part 1 XSD (WordprocessingML).
3. Assert no validation errors on the `word/document.xml` part inside the .docx zip.

Wave 5 relies on POI's implicit validation only (XWPFDocument.open round-trip in tests). Upgrade to explicit XSD validation if Word "file corrupt" reports surface.
