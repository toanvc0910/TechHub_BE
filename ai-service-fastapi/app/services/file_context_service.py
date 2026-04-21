from __future__ import annotations

import base64
import io
import json
import logging
import re
from typing import Any

import httpx

from app.core.config import get_settings
from app.services.observability_service import runtime_observability_service
from app.services.vector_service import vector_service

logger = logging.getLogger(__name__)

_OCR_IMAGE_MIMES = {
    "image/png", "image/jpeg", "image/jpg", "image/webp",
    "image/tiff", "image/bmp", "image/gif",
}


class FileContextService:
    def __init__(self) -> None:
        self._settings = get_settings()
        self._client = httpx.AsyncClient(timeout=self._settings.file_download_timeout_seconds)

    async def hydrate_and_index_contexts(
        self,
        *,
        user_id: str,
        session_id: str,
        files: list[dict[str, Any]],
    ) -> list[dict[str, Any]]:
        hydrated_files = [await self._hydrate_file_context(user_id=user_id, file_item=item) for item in files]
        analyzable = [item for item in hydrated_files if item.get("content")]
        indexing_stats = {"indexed": 0, "failed": 0}
        if analyzable:
            try:
                indexing_stats = await vector_service.index_session_file_contexts(
                    user_id=user_id,
                    session_id=session_id,
                    files=analyzable,
                )
                for item in analyzable:
                    item["vectorIndexStatus"] = "INDEXED"
            except Exception:
                indexing_stats = {"indexed": 0, "failed": len(analyzable)}
                for item in analyzable:
                    item["vectorIndexStatus"] = "FAILED"
        await runtime_observability_service.record_file_ingestion(
            files_total=len(files),
            hydrated=len(analyzable),
            chunks_indexed=indexing_stats.get("indexed", 0),
            unsupported=sum(1 for item in hydrated_files if item.get("ingestionStatus") == "UNSUPPORTED_MIME"),
        )
        return hydrated_files

    async def ingest_uploaded_event(self, payload: dict[str, Any]) -> dict[str, Any]:
        user_id = str(payload.get("userId") or payload.get("user_id") or "")
        hydrated = await self._hydrate_file_context(user_id=user_id, file_item=payload)
        if not hydrated.get("content") or not user_id:
            await runtime_observability_service.record_file_ingestion(
                files_total=1,
                hydrated=0,
                chunks_indexed=0,
                unsupported=1,
            )
            return {
                "success": False,
                "message": "File event received but no analyzable text content was available.",
                "file": hydrated,
                "stats": {"indexed": 0, "failed": 1},
            }

        try:
            indexing_stats = await vector_service.index_user_file_contexts(user_id=user_id, files=[hydrated])
            hydrated["vectorIndexStatus"] = "INDEXED"
        except Exception:
            indexing_stats = {"indexed": 0, "failed": 1}
            hydrated["vectorIndexStatus"] = "FAILED"
        await runtime_observability_service.record_file_ingestion(
            files_total=1,
            hydrated=1,
            chunks_indexed=indexing_stats.get("indexed", 0),
            unsupported=0,
        )
        return {
            "success": indexing_stats.get("failed", 0) == 0,
            "message": "Uploaded file event ingested into user-level retrieval index.",
            "file": hydrated,
            "stats": indexing_stats,
        }

    async def search_relevant_chunks(
        self,
        *,
        user_id: str,
        session_id: str,
        query: str,
        limit: int = 4,
    ) -> list[dict[str, Any]]:
        session_hits = await vector_service.search_session_file_chunks(
            user_id=user_id,
            session_id=session_id,
            query=query,
            limit=limit,
        )
        if session_hits:
            return session_hits
        return await vector_service.search_user_file_chunks(user_id=user_id, query=query, limit=limit)

    async def _hydrate_file_context(self, *, user_id: str, file_item: dict[str, Any]) -> dict[str, Any]:
        normalized = dict(file_item)
        file_id = str(normalized.get("id") or normalized.get("fileId") or normalized.get("referenceId") or "")
        name = str(
            normalized.get("name")
            or normalized.get("filename")
            or normalized.get("originalName")
            or file_id
            or "uploaded-file"
        )
        mime_type = str(normalized.get("mimeType") or normalized.get("mime_type") or "")
        if file_id and self._settings.file_service_base_url:
            metadata = await self._fetch_file_metadata(file_id=file_id, user_id=user_id)
            if metadata:
                normalized = {**metadata, **normalized}
                mime_type = str(normalized.get("mimeType") or normalized.get("mime_type") or mime_type)
                name = str(
                    normalized.get("name")
                    or normalized.get("filename")
                    or normalized.get("originalName")
                    or name
                )

        content = normalized.get("content") or normalized.get("text") or normalized.get("excerpt")
        if content:
            cleaned = self._normalize_text(str(content), mime_type=mime_type, name=name)
            normalized["content"] = cleaned[: self._settings.file_max_chars]
            normalized["excerpt"] = cleaned[: self._settings.file_excerpt_chars]
            normalized["ingestionStatus"] = "READY"
            return normalized

        url = self._best_url(normalized)
        attempted_download = False
        if url and self._can_extract_content(mime_type=mime_type, name=name):
            attempted_download = True
            downloaded = await self._download_and_extract_content(url, mime_type=mime_type, name=name)
            if downloaded:
                normalized["content"] = downloaded[: self._settings.file_max_chars]
                normalized["excerpt"] = downloaded[: self._settings.file_excerpt_chars]
                normalized["ingestionStatus"] = "READY"
                return normalized

        metadata_text = self._metadata_text(normalized)
        if metadata_text:
            cleaned = self._normalize_text(metadata_text, mime_type=mime_type, name=name)
            normalized["content"] = cleaned[: self._settings.file_max_chars]
            normalized["excerpt"] = cleaned[: self._settings.file_excerpt_chars]
            normalized["ingestionStatus"] = "READY_METADATA"
            return normalized

        if attempted_download:
            normalized["ingestionStatus"] = "FETCH_FAILED"
            return normalized

        normalized["ingestionStatus"] = "UNSUPPORTED_MIME"
        return normalized

    async def _fetch_file_metadata(self, *, file_id: str, user_id: str) -> dict[str, Any] | None:
        base_url = (self._settings.file_service_base_url or "").rstrip("/")
        if not base_url:
            return None
        candidates = []
        if "/api/files" in base_url:
            candidates.append(f"{base_url}/{file_id}")
        else:
            candidates.extend(
                [
                    f"{base_url}/api/files/{file_id}",
                    f"{base_url}/files/{file_id}",
                    f"{base_url}/{file_id}",
                ]
            )
        seen = set()
        for candidate in candidates:
            if candidate in seen:
                continue
            seen.add(candidate)
            try:
                response = await self._client.get(candidate, params={"userId": user_id})
                response.raise_for_status()
                payload = response.json()
                data = self._unwrap_response_data(payload)
                if data:
                    return data
            except Exception:
                continue
        return None

    async def _download_and_extract_content(self, url: str, *, mime_type: str, name: str) -> str | None:
        try:
            response = await self._client.get(url)
            response.raise_for_status()
            raw = response.content[: self._settings.file_max_download_bytes]
            # Use async version that supports OCR fallback
            extracted = await self._extract_bytes_async(raw, mime_type=mime_type, name=name)
            if not extracted:
                return None
            return self._normalize_text(extracted, mime_type=mime_type, name=name)[: self._settings.file_max_chars]
        except Exception:
            return None

    @staticmethod
    def _unwrap_response_data(payload: Any) -> dict[str, Any] | None:
        if isinstance(payload, dict):
            if isinstance(payload.get("data"), dict):
                return payload["data"]
            nested_payload = payload.get("payload")
            if isinstance(nested_payload, dict) and isinstance(nested_payload.get("data"), dict):
                return nested_payload["data"]
        return None

    @staticmethod
    def _best_url(file_item: dict[str, Any]) -> str | None:
        candidates = [
            file_item.get("secureUrl"),
            file_item.get("publicUrl"),
            file_item.get("url"),
            file_item.get("cloudinarySecureUrl"),
            file_item.get("cloudinaryUrl"),
        ]
        for candidate in candidates:
            if isinstance(candidate, str) and candidate.strip():
                return candidate.strip()
        return None

    @staticmethod
    def _decode_bytes(raw: bytes) -> str:
        for encoding in ("utf-8", "utf-16", "latin-1"):
            try:
                return raw.decode(encoding)
            except UnicodeDecodeError:
                continue
        return raw.decode("utf-8", errors="ignore")

    def _normalize_text(self, text: str, *, mime_type: str, name: str) -> str:
        normalized = text.replace("\x00", "").strip()
        if mime_type == "application/json" or name.lower().endswith(".json"):
            try:
                normalized = json.dumps(json.loads(normalized), ensure_ascii=True, indent=2)
            except json.JSONDecodeError:
                pass
        if mime_type == "text/html" or name.lower().endswith((".html", ".htm")):
            normalized = re.sub(r"<[^>]+>", " ", normalized)
        normalized = re.sub(r"\r\n?", "\n", normalized)
        normalized = re.sub(r"\n{3,}", "\n\n", normalized)
        normalized = re.sub(r"[ \t]{2,}", " ", normalized)
        return normalized.strip()

    @classmethod
    def _metadata_text(cls, file_item: dict[str, Any]) -> str | None:
        candidates = [
            ("transcript", file_item.get("transcript")),
            ("caption", file_item.get("caption")),
            ("altText", file_item.get("altText")),
            ("description", file_item.get("description")),
        ]
        merged = "\n".join(
            cleaned
            for field_name, candidate in candidates
            for cleaned in [cls._clean_metadata_candidate(field_name, candidate)]
            if cleaned
        )
        return merged or None

    @staticmethod
    def _clean_metadata_candidate(field_name: str, value: Any) -> str | None:
        if not isinstance(value, str):
            return None
        cleaned = value.strip()
        if not cleaned:
            return None
        normalized = " ".join(cleaned.lower().split())
        if field_name == "description" and normalized in {
            "attached in ai chat",
            "uploaded in ai chat",
            "attached file in ai chat",
        }:
            return None
        return cleaned

    async def _extract_bytes_async(self, raw: bytes, *, mime_type: str, name: str) -> str | None:
        """Extract text from binary data. Falls back to Gemini Vision OCR for
        scanned PDFs (empty pypdf text) and image files."""
        if self._is_text_like(mime_type=mime_type, name=name):
            return self._decode_bytes(raw)
        lower_name = name.lower()
        lower_mime = mime_type.lower()

        # Image files → OCR directly
        if lower_mime in _OCR_IMAGE_MIMES or lower_name.endswith(
            (".png", ".jpg", ".jpeg", ".webp", ".tiff", ".bmp", ".gif")
        ):
            return await self._ocr_via_gemini(raw, mime_type, name)

        if lower_mime == "application/pdf" or lower_name.endswith(".pdf"):
            text = self._parse_pdf(raw)
            # Scanned PDF: pypdf returns empty/very short text → try OCR
            if (not text or len(text.strip()) < 50) and self._settings.ocr_enabled:
                ocr_text = await self._ocr_via_gemini(raw, "application/pdf", name)
                if ocr_text and len(ocr_text.strip()) > len((text or "").strip()):
                    return ocr_text
            return text

        if lower_mime in {
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
        } or lower_name.endswith((".docx", ".doc")):
            return self._parse_docx(raw)
        if lower_mime in {
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-powerpoint",
        } or lower_name.endswith((".pptx", ".ppt")):
            return self._parse_pptx(raw)
        if lower_mime in {
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel",
        } or lower_name.endswith((".xlsx", ".xls")):
            return self._parse_xlsx(raw)
        return None

    def _extract_bytes(self, raw: bytes, *, mime_type: str, name: str) -> str | None:
        """Sync wrapper — used by _download_and_extract_content. Cannot call OCR."""
        if self._is_text_like(mime_type=mime_type, name=name):
            return self._decode_bytes(raw)
        lower_name = name.lower()
        lower_mime = mime_type.lower()
        if lower_mime == "application/pdf" or lower_name.endswith(".pdf"):
            return self._parse_pdf(raw)
        if lower_mime in {
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
        } or lower_name.endswith((".docx", ".doc")):
            return self._parse_docx(raw)
        if lower_mime in {
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-powerpoint",
        } or lower_name.endswith((".pptx", ".ppt")):
            return self._parse_pptx(raw)
        if lower_mime in {
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel",
        } or lower_name.endswith((".xlsx", ".xls")):
            return self._parse_xlsx(raw)
        return None

    async def _ocr_via_gemini(self, data: bytes, mime: str, name: str) -> str | None:
        """Call Gemini Vision API to extract text from image/scanned PDF.
        Uses the same GEMINI_API_KEY + GEMINI_BASE_URL already in config."""
        if not self._settings.ocr_enabled:
            return None
        api_key = self._settings.gemini_api_key
        if not api_key:
            return None
        try:
            file_b64 = base64.b64encode(data).decode("utf-8")
            model = self._settings.gemini_chat_model
            url = (
                f"{self._settings.gemini_base_url}/models/{model}:generateContent"
                f"?key={api_key}"
            )
            payload = {
                "contents": [{
                    "parts": [
                        {"inlineData": {"mimeType": mime, "data": file_b64}},
                        {"text": (
                            "Extract ALL text content from this document or image. "
                            "Return plain text only, preserve paragraph structure. "
                            "If the document has tables, convert them to readable text. "
                            "If there is no text, return empty string."
                        )},
                    ]
                }]
            }
            async with httpx.AsyncClient(timeout=self._settings.ocr_timeout_seconds) as client:
                response = await client.post(url, json=payload)
                if response.status_code == 200:
                    result = response.json()
                    candidates = result.get("candidates", [])
                    if candidates:
                        parts = candidates[0].get("content", {}).get("parts", [])
                        if parts:
                            text = parts[0].get("text", "")
                            logger.info("OCR via Gemini succeeded for %s: %d chars extracted.", name, len(text))
                            return text
                else:
                    logger.warning("OCR via Gemini returned %d for %s", response.status_code, name)
        except Exception as exc:
            logger.warning("OCR via Gemini failed for %s: %s", name, exc)
        return None

    @staticmethod
    def _parse_pdf(raw: bytes) -> str | None:
        try:
            from pypdf import PdfReader
        except Exception:
            return None
        try:
            reader = PdfReader(io.BytesIO(raw))
            return "\n\n".join((page.extract_text() or "").strip() for page in reader.pages if page.extract_text())
        except Exception:
            return None

    @staticmethod
    def _parse_docx(raw: bytes) -> str | None:
        try:
            import docx
        except Exception:
            return None
        try:
            document = docx.Document(io.BytesIO(raw))
            paragraphs = [paragraph.text.strip() for paragraph in document.paragraphs if paragraph.text.strip()]
            return "\n".join(paragraphs)
        except Exception:
            return None

    @staticmethod
    def _parse_pptx(raw: bytes) -> str | None:
        try:
            from pptx import Presentation
        except Exception:
            return None
        try:
            presentation = Presentation(io.BytesIO(raw))
            slides: list[str] = []
            for index, slide in enumerate(presentation.slides, start=1):
                texts = []
                for shape in slide.shapes:
                    if hasattr(shape, "text") and str(shape.text).strip():
                        texts.append(str(shape.text).strip())
                if texts:
                    slides.append(f"Slide {index}\n" + "\n".join(texts))
            return "\n\n".join(slides)
        except Exception:
            return None

    @staticmethod
    def _parse_xlsx(raw: bytes) -> str | None:
        try:
            from openpyxl import load_workbook
        except Exception:
            return None
        try:
            workbook = load_workbook(io.BytesIO(raw), read_only=True, data_only=True)
            worksheets: list[str] = []
            for sheet in workbook.worksheets:
                rows: list[str] = []
                for row in sheet.iter_rows(values_only=True):
                    values = [str(cell).strip() for cell in row if cell is not None and str(cell).strip()]
                    if values:
                        rows.append(" | ".join(values))
                if rows:
                    worksheets.append(f"Sheet {sheet.title}\n" + "\n".join(rows[:200]))
            return "\n\n".join(worksheets)
        except Exception:
            return None

    def _can_extract_content(self, *, mime_type: str, name: str) -> bool:
        if self._is_text_like(mime_type=mime_type, name=name):
            return True
        if name.lower().endswith((".pdf", ".docx", ".doc", ".pptx", ".ppt", ".xlsx", ".xls")):
            return True
        if mime_type.lower() in {
            "application/pdf",
            "application/msword",
            "application/vnd.ms-powerpoint",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        }:
            return True
        # OCR-capable: image files when OCR is enabled
        if self._settings.ocr_enabled and (
            mime_type.lower() in _OCR_IMAGE_MIMES
            or name.lower().endswith((".png", ".jpg", ".jpeg", ".webp", ".tiff", ".bmp", ".gif"))
        ):
            return True
        return False

    @staticmethod
    def _is_text_like(*, mime_type: str, name: str) -> bool:
        lowered_mime = mime_type.lower()
        lowered_name = name.lower()
        if lowered_mime.startswith("text/"):
            return True
        if lowered_mime in {
            "application/json",
            "application/xml",
            "application/javascript",
            "application/x-javascript",
            "application/sql",
            "text/csv",
            "text/markdown",
        }:
            return True
        return lowered_name.endswith(
            (
                ".txt",
                ".md",
                ".markdown",
                ".csv",
                ".json",
                ".xml",
                ".yaml",
                ".yml",
                ".html",
                ".htm",
                ".js",
                ".ts",
                ".tsx",
                ".jsx",
                ".py",
                ".java",
                ".sql",
                ".css",
                ".sh",
            )
        )


file_context_service = FileContextService()
