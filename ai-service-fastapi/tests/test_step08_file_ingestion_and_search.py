"""
Step 08 - File Ingestion & OCR E2E smoke test.

Drives the public file_context_service / vector_service path end-to-end
against the live Qdrant configured via .env. Covers what step 08 promises:

  - hydrate accepts pre-extracted content and marks ingestionStatus READY
  - DOCX / XLSX bytes parse through `_extract_bytes_async`
  - session and user file collections index content with the documented
    chunk payload shape (file_id, user_id, name, chunk_index, content_hash,
    source_updated_at, ingested_at)
  - search returns only the requesting user's chunks (ownership enforced)
  - re-ingest is idempotent: re-indexing a file with shorter content
    replaces, not duplicates, the prior chunks
  - get_file_index_status reports INDEXED / NOT_INDEXED correctly
  - delete_file_chunks removes all chunks for one (user, file)
  - _can_extract_content reflects the right capabilities, including OCR
  - OCR disabled -> image bytes return None without crashing

Run from the service root:

    python -m tests.test_step08_file_ingestion_and_search
"""

from __future__ import annotations

import asyncio
import io
import sys
import traceback
import uuid
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from unittest.mock import patch

SERVICE_ROOT = Path(__file__).resolve().parent.parent
if str(SERVICE_ROOT) not in sys.path:
    sys.path.insert(0, str(SERVICE_ROOT))

import httpx  # noqa: E402

from app.core.config import get_settings  # noqa: E402
from app.services.file_context_service import file_context_service  # noqa: E402
from app.services.vector_service import vector_service  # noqa: E402


SETTINGS = get_settings()
USER_A = str(uuid.uuid4())
USER_B = str(uuid.uuid4())
SESSION_A = str(uuid.uuid4())
SESSION_B = str(uuid.uuid4())
FILE_A = str(uuid.uuid4())
FILE_B = str(uuid.uuid4())


@dataclass
class CaseResult:
    name: str
    passed: bool
    detail: str = ""


# Long enough to chunk multiple times (chunk_size default = 1200, overlap = 150).
LONG_TEXT = (
    "Step 08 covers TechHub file ingestion and OCR. The user uploads a file "
    "through File Service, the file-uploaded Kafka event flows to ai-service, "
    "and chunks get indexed in Qdrant. "
) * 60  # ~13000 chars -> ~11 chunks

SHORT_TEXT = "Only one chunk after re-ingest - much shorter content this time."


# ---------------------------------------------------------------------------
# Cleanup helpers (best-effort; we reset state after each test run).
# ---------------------------------------------------------------------------


async def _cleanup_test_files() -> None:
    for collection in (
        SETTINGS.qdrant_user_file_collection,
        SETTINGS.qdrant_session_file_collection,
    ):
        for user_id in (USER_A, USER_B):
            for file_id in (FILE_A, FILE_B):
                try:
                    await vector_service.delete_file_chunks(
                        collection=collection,
                        user_id=user_id,
                        file_id=file_id,
                    )
                except Exception:
                    pass


# ---------------------------------------------------------------------------
# Hydration + extraction
# ---------------------------------------------------------------------------


async def case_hydrate_inline_content() -> CaseResult:
    name = "H1 - hydrate accepts pre-extracted content -> ingestionStatus READY"
    file_item = {
        "id": FILE_A,
        "name": "demo.txt",
        "mimeType": "text/plain",
        "content": LONG_TEXT[:500],
    }
    # Use a unique stub function to avoid hitting File Service or Qdrant
    with patch.object(get_settings(), "file_service_base_url", None):
        hydrated = await file_context_service._hydrate_file_context(  # noqa: SLF001
            user_id=USER_A, file_item=file_item
        )
    if hydrated.get("ingestionStatus") != "READY":
        return CaseResult(name, False, f"expected READY, got {hydrated.get('ingestionStatus')}")
    if not hydrated.get("content") or not hydrated.get("excerpt"):
        return CaseResult(name, False, f"content/excerpt missing: {hydrated.keys()}")
    return CaseResult(name, True, f"content={len(hydrated['content'])} chars, excerpt={len(hydrated['excerpt'])} chars")


async def case_extract_docx_bytes() -> CaseResult:
    name = "H2 - synthetic DOCX bytes parse via _extract_bytes_async"
    try:
        import docx  # noqa: F401
    except Exception:
        return CaseResult(name, True, "python-docx not installed; skipping")
    from docx import Document  # type: ignore

    buffer = io.BytesIO()
    document = Document()
    document.add_paragraph("This is a synthetic DOCX paragraph for step 08 test.")
    document.add_paragraph("Second paragraph to verify multi-paragraph parsing.")
    document.save(buffer)
    raw = buffer.getvalue()

    text = await file_context_service._extract_bytes_async(  # noqa: SLF001
        raw,
        mime_type="application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        name="step08.docx",
    )
    if not text or "synthetic DOCX paragraph" not in text:
        return CaseResult(name, False, f"DOCX extract missing expected text; got={text!r}")
    if "Second paragraph" not in text:
        return CaseResult(name, False, f"DOCX extract missing second paragraph; got={text!r}")
    return CaseResult(name, True, f"extracted {len(text)} chars from synthetic DOCX")


async def case_extract_xlsx_bytes() -> CaseResult:
    name = "H3 - synthetic XLSX bytes parse via _extract_bytes_async"
    try:
        from openpyxl import Workbook  # noqa: F401
    except Exception:
        return CaseResult(name, True, "openpyxl not installed; skipping")
    from openpyxl import Workbook  # type: ignore

    wb = Workbook()
    ws = wb.active
    ws.title = "Sheet1"
    ws.append(["Course", "Status"])
    ws.append(["Python 101", "PUBLISHED"])
    ws.append(["React Pro", "DRAFT"])
    buffer = io.BytesIO()
    wb.save(buffer)
    raw = buffer.getvalue()

    text = await file_context_service._extract_bytes_async(  # noqa: SLF001
        raw,
        mime_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        name="step08.xlsx",
    )
    if not text or "Python 101" not in text or "PUBLISHED" not in text:
        return CaseResult(name, False, f"XLSX extract missing expected cells; got={text!r}")
    return CaseResult(name, True, f"extracted {len(text)} chars from synthetic XLSX")


async def case_can_extract_content_classification() -> CaseResult:
    name = "H4 - _can_extract_content classifies mime types correctly"
    yes_cases = [
        ("text/plain", "a.txt"),
        ("application/pdf", "a.pdf"),
        ("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "a.docx"),
        ("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "a.xlsx"),
        ("application/json", "a.json"),
    ]
    for mime, name_in in yes_cases:
        if not file_context_service._can_extract_content(mime_type=mime, name=name_in):  # noqa: SLF001
            return CaseResult(name, False, f"should support {(mime, name_in)}")
    # OCR-gated cases: depend on settings.ocr_enabled
    with patch.object(SETTINGS, "ocr_enabled", False):
        if file_context_service._can_extract_content(mime_type="image/png", name="a.png"):  # noqa: SLF001
            return CaseResult(name, False, "image should be rejected when OCR disabled")
    with patch.object(SETTINGS, "ocr_enabled", True):
        if not file_context_service._can_extract_content(mime_type="image/png", name="a.png"):  # noqa: SLF001
            return CaseResult(name, False, "image should be accepted when OCR enabled")
    return CaseResult(name, True, "5 supported + OCR gate verified")


async def case_ocr_disabled_image_returns_none() -> CaseResult:
    name = "H5 - OCR disabled -> image bytes return None (no crash)"
    fake_png = b"\x89PNG\r\n\x1a\nrandombytes"
    with patch.object(SETTINGS, "ocr_enabled", False):
        text = await file_context_service._extract_bytes_async(  # noqa: SLF001
            fake_png, mime_type="image/png", name="x.png"
        )
    if text is not None:
        return CaseResult(name, False, f"expected None when OCR off, got {text!r}")
    return CaseResult(name, True, "no crash, returned None")


# ---------------------------------------------------------------------------
# Indexing + search + ownership
# ---------------------------------------------------------------------------


async def case_user_file_index_and_search() -> CaseResult:
    name = "S1 - user file ingestion writes chunks; search returns them for owner"
    await _cleanup_test_files()
    file_item = {
        "id": FILE_A,
        "name": "step08-doc.txt",
        "mimeType": "text/plain",
        "content": LONG_TEXT,
        "sourceUpdatedAt": "2026-05-16T00:00:00Z",
    }
    stats = await vector_service.index_user_file_contexts(user_id=USER_A, files=[file_item])
    if stats.get("indexed", 0) < 2:
        return CaseResult(name, False, f"expected multiple chunks indexed, got {stats}")

    status = await vector_service.get_file_index_status(user_id=USER_A, file_id=FILE_A)
    if status.get("status") != "INDEXED" or status.get("chunkCount", 0) < 2:
        return CaseResult(name, False, f"index status wrong after upload: {status}")

    results = await vector_service.search_user_file_chunks(
        user_id=USER_A, query="TechHub file ingestion Kafka event", limit=4
    )
    if not results:
        return CaseResult(name, False, "search returned no chunks for owner")
    first = results[0]
    payload = first.get("payload") or {}
    if str(payload.get("user_id")) != USER_A:
        return CaseResult(name, False, f"chunk payload user_id mismatch: {payload}")
    if str(payload.get("file_id")) != FILE_A:
        return CaseResult(name, False, f"chunk payload file_id mismatch: {payload}")
    if not payload.get("content_hash"):
        return CaseResult(name, False, f"chunk payload missing content_hash: {payload}")
    if not payload.get("ingested_at"):
        return CaseResult(name, False, f"chunk payload missing ingested_at: {payload}")
    return CaseResult(name, True, f"indexed={stats['indexed']} chunks; search returned {len(results)}")


async def case_user_file_ownership_enforced() -> CaseResult:
    name = "S2 - search with wrong user_id returns empty (ownership enforced)"
    results = await vector_service.search_user_file_chunks(
        user_id=USER_B, query="TechHub file ingestion Kafka event", limit=4
    )
    # USER_B never uploaded; results must be empty.
    if results:
        return CaseResult(name, False, f"USER_B should see no chunks; got {len(results)}")
    return CaseResult(name, True, "no leak across users")


async def case_session_file_ownership_enforced() -> CaseResult:
    name = "S3 - session search isolates by (user_id, session_id)"
    await _cleanup_test_files()
    file_item = {
        "id": FILE_B,
        "name": "step08-session.txt",
        "mimeType": "text/plain",
        "content": "Session-specific content about TechHub session file collection.",
    }
    stats = await vector_service.index_session_file_contexts(
        user_id=USER_A, session_id=SESSION_A, files=[file_item]
    )
    if not stats.get("indexed"):
        return CaseResult(name, False, f"failed to index session file: {stats}")

    # Owner + correct session
    hit = await vector_service.search_session_file_chunks(
        user_id=USER_A, session_id=SESSION_A, query="TechHub session file", limit=2
    )
    if not hit:
        return CaseResult(name, False, "owner+correct session got no result")

    # Owner but wrong session
    miss = await vector_service.search_session_file_chunks(
        user_id=USER_A, session_id=SESSION_B, query="TechHub session file", limit=2
    )
    if miss:
        return CaseResult(name, False, f"wrong session should be empty; got {len(miss)}")

    # Wrong user same session
    miss2 = await vector_service.search_session_file_chunks(
        user_id=USER_B, session_id=SESSION_A, query="TechHub session file", limit=2
    )
    if miss2:
        return CaseResult(name, False, f"wrong user should be empty; got {len(miss2)}")
    return CaseResult(name, True, "owner+session match isolated correctly")


async def case_reingest_idempotent() -> CaseResult:
    name = "S4 - re-ingest with shorter content replaces (no orphan chunks)"
    # Set up: index FILE_A with LONG_TEXT, verify multi-chunk, then re-ingest
    # with SHORT_TEXT and verify orphan chunks are gone.
    await _cleanup_test_files()
    initial_item = {
        "id": FILE_A,
        "name": "step08-doc.txt",
        "mimeType": "text/plain",
        "content": LONG_TEXT,
        "sourceUpdatedAt": "2026-05-16T00:00:00Z",
    }
    await vector_service.index_user_file_contexts(user_id=USER_A, files=[initial_item])
    before = await vector_service.get_file_index_status(user_id=USER_A, file_id=FILE_A)
    before_count = int(before.get("chunkCount", 0))
    if before_count < 2:
        return CaseResult(name, False, f"precondition: expected >=2 chunks, got {before_count}")

    short_item = {
        "id": FILE_A,
        "name": "step08-doc.txt",
        "mimeType": "text/plain",
        "content": SHORT_TEXT,
        "sourceUpdatedAt": "2026-05-16T01:00:00Z",
    }
    stats = await vector_service.index_user_file_contexts(user_id=USER_A, files=[short_item])
    if stats.get("indexed", 0) != 1:
        return CaseResult(name, False, f"short content should produce 1 chunk; got {stats}")

    after = await vector_service.get_file_index_status(user_id=USER_A, file_id=FILE_A)
    after_count = int(after.get("chunkCount", 0))
    if after_count != 1:
        return CaseResult(
            name,
            False,
            f"after re-ingest expected exactly 1 chunk; got {after_count} (before={before_count})",
        )
    return CaseResult(
        name,
        True,
        f"chunks went from {before_count} -> {after_count} after re-ingest; no orphans",
    )


async def case_delete_file_chunks() -> CaseResult:
    name = "S5 - delete_file_chunks removes the user's file"
    await vector_service.delete_file_chunks(
        collection=SETTINGS.qdrant_user_file_collection,
        user_id=USER_A,
        file_id=FILE_A,
    )
    status = await vector_service.get_file_index_status(user_id=USER_A, file_id=FILE_A)
    if status.get("status") == "INDEXED" or status.get("chunkCount", 0) > 0:
        return CaseResult(name, False, f"expected NOT_INDEXED after delete; got {status}")
    return CaseResult(name, True, f"delete worked, status={status.get('status')}")


async def case_file_index_status_not_indexed() -> CaseResult:
    name = "S6 - get_file_index_status reports NOT_INDEXED for unknown file"
    status = await vector_service.get_file_index_status(
        user_id=USER_A, file_id=str(uuid.uuid4())
    )
    if status.get("status") == "INDEXED":
        return CaseResult(name, False, f"unknown file should not be INDEXED; got {status}")
    return CaseResult(name, True, f"status={status.get('status')}")


# ---------------------------------------------------------------------------
# Kafka -> ingest contract (via file_context_service.ingest_uploaded_event)
# ---------------------------------------------------------------------------


async def case_ingest_uploaded_event_with_content() -> CaseResult:
    name = "K1 - ingest_uploaded_event with inline content indexes the user file"
    await _cleanup_test_files()
    payload = {
        "fileId": FILE_B,
        "userId": USER_A,
        "name": "kafka-uploaded.txt",
        "mimeType": "text/plain",
        "content": "Kafka file-uploaded event arrived with inline text content for indexing.",
    }
    outcome = await file_context_service.ingest_uploaded_event(payload)
    if not outcome.get("success"):
        return CaseResult(name, False, f"ingest_uploaded_event reported failure: {outcome}")
    status = await vector_service.get_file_index_status(user_id=USER_A, file_id=FILE_B)
    if status.get("status") != "INDEXED":
        return CaseResult(name, False, f"expected INDEXED after ingest event; got {status}")
    return CaseResult(name, True, f"chunks={status.get('chunkCount')}")


async def case_ingest_uploaded_event_no_content_fails_cleanly() -> CaseResult:
    name = "K2 - ingest_uploaded_event without analyzable content fails cleanly"
    payload = {
        "fileId": str(uuid.uuid4()),
        "userId": USER_A,
        "name": "no-content.bin",
        "mimeType": "application/octet-stream",
    }
    outcome = await file_context_service.ingest_uploaded_event(payload)
    if outcome.get("success"):
        return CaseResult(name, False, "expected success=False for unanalyzable file")
    if "no analyzable text" not in str(outcome.get("message") or "").lower():
        return CaseResult(name, False, f"unexpected message: {outcome.get('message')}")
    return CaseResult(name, True, str(outcome.get("message"))[:120])


# ---------------------------------------------------------------------------
# Runner
# ---------------------------------------------------------------------------


async def main() -> int:
    results: list[CaseResult] = []
    cases = [
        case_hydrate_inline_content,
        case_extract_docx_bytes,
        case_extract_xlsx_bytes,
        case_can_extract_content_classification,
        case_ocr_disabled_image_returns_none,
        case_user_file_index_and_search,
        case_user_file_ownership_enforced,
        case_session_file_ownership_enforced,
        case_reingest_idempotent,
        case_delete_file_chunks,
        case_file_index_status_not_indexed,
        case_ingest_uploaded_event_with_content,
        case_ingest_uploaded_event_no_content_fails_cleanly,
    ]
    try:
        for func in cases:
            print(f"\n--- {func.__name__}")
            try:
                result = await func()
            except Exception as exc:  # noqa: BLE001
                tb = "".join(traceback.format_exception_only(type(exc), exc)).strip()
                result = CaseResult(func.__name__, False, f"EXC: {tb}")
            print(("PASS " if result.passed else "FAIL ") + result.name + " :: " + result.detail)
            results.append(result)
    finally:
        await _cleanup_test_files()

    passed = [r for r in results if r.passed]
    failed = [r for r in results if not r.passed]
    print("\n=================================================")
    print(f"PASSED: {len(passed)} / {len(results)}")
    if failed:
        print("FAILED:")
        for r in failed:
            print(f"  - {r.name}: {r.detail}")
    return 0 if not failed else 1


if __name__ == "__main__":
    sys.exit(asyncio.run(main()))
