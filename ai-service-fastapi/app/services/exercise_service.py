from __future__ import annotations

import re
from collections import Counter
from datetime import datetime, timezone
from typing import Any

from app.core.config import get_settings
from app.core.enums import AiTaskStatus, AiTaskType, DifficultyLevel, ExerciseFormat
from app.db.models import AiGenerationTaskModel
from app.db.session import get_db_session
from app.schemas.exercise import (
    AiExerciseGenerateRequest,
    AiExerciseGenerationResponse,
    QuizFeedbackRequest,
    QuizFeedbackResponse,
    QuizReviewSuggestion,
)
from app.services.catalog_service import catalog_service
from app.services.llm_gateway import switchable_ai_gateway
from app.services.provider_config import provider_config_service
from app.services.runtime_request_context import runtime_request_context_service
from app.services.vector_service import vector_service


class ExerciseService:
    def __init__(self) -> None:
        self._settings = get_settings()

    async def generate(self, request: AiExerciseGenerateRequest) -> AiExerciseGenerationResponse:
        with runtime_request_context_service.begin(scope="exercise_generate") as runtime_state:
            lesson = await vector_service.get_lesson(str(request.lessonId))
            pipeline = "live"
            if lesson is None:
                lesson = await catalog_service.fetch_lesson_by_id(str(request.lessonId))
                pipeline = "fallback"
            if lesson is None:
                raise ValueError("Lesson content not found in AI index or PostgreSQL. Please reindex lessons first.")

            prompt = self._build_prompt(request, lesson)
            fallback = self._fallback_exercises(lesson, request)
            if self._settings.business_safe_mode_enabled:
                exercises = fallback
                pipeline = "fallback"
            else:
                ai_payload = await switchable_ai_gateway.generate_structured_json(prompt=prompt, fallback_payload=fallback)
                exercises = self._normalize_payload(ai_payload, fallback)
                if exercises == fallback:
                    pipeline = "fallback"

            async with get_db_session() as session:
                task = AiGenerationTaskModel(
                    task_type=AiTaskType.EXERCISE_GENERATION.value,
                    status=AiTaskStatus.DRAFT.value,
                    target_reference=str(request.lessonId),
                    request_payload=request.model_dump(mode="json"),
                    result_payload=exercises,
                    prompt=prompt,
                    model_used=await provider_config_service.get_active_chat_model(),
                )
                session.add(task)
                await session.flush()
                return AiExerciseGenerationResponse(
                    taskId=str(task.id),
                    status=AiTaskStatus.DRAFT,
                    exercises=exercises,
                    drafts=exercises,
                    message="Exercise draft created successfully. Admin can review and approve.",
                    metadata={
                        "courseId": str(request.courseId),
                        "lessonId": str(request.lessonId),
                        "generatedAt": datetime.now(timezone.utc).isoformat(),
                        "totalCount": sum(len(items) for items in exercises.values()),
                        "formats": [fmt.value for fmt in request.formats],
                        "difficulties": [level.value for level in request.difficulties],
                        "pipeline": pipeline,
                        "requestId": runtime_state.request_id,
                        "tokenUsage": runtime_request_context_service.snapshot(),
                        "citations": [
                            {
                                "kind": "lesson",
                                "lessonId": lesson.get("id"),
                                "title": lesson.get("title"),
                                "courseTitle": lesson.get("course_title"),
                            }
                        ],
                    },
                )

    async def generate_feedback(self, request: QuizFeedbackRequest) -> QuizFeedbackResponse:
        fallback = self._fallback_quiz_feedback(request)
        if self._settings.business_safe_mode_enabled:
            return fallback

        prompt = self._build_feedback_prompt(request)
        ai_payload = await switchable_ai_gateway.generate_structured_json(
            prompt=prompt,
            fallback_payload=fallback.model_dump(mode="json"),
        )
        return self._normalize_feedback_payload(ai_payload, fallback)

    def _build_prompt(self, request: AiExerciseGenerateRequest, lesson: dict[str, Any]) -> str:
        formats = [fmt.value for fmt in request.formats]
        difficulties = [level.value for level in request.difficulties]
        content_preview = str(lesson.get("content") or "")[:6000]
        workspace_langs = ", ".join(lesson.get("workspace_languages") or []) or "none"

        return (
            "# ROLE\n"
            "Ban la he thong tao bai tap chat luong cao cho nen tang hoc tap TechHub.\n\n"
            "# RULES\n"
            "- Tra ve JSON only. Key ngoai cung: mcq, essay, coding (lowercase).\n"
            "- Moi bai tap PHAI lien quan truc tiep toi noi dung lesson. Cam dua vao thong tin ngoai.\n"
            "- Cac phuong an MCQ phai khac biet ro rang, khong trung y.\n"
            "- Coding exercises: starterCode phai la code chay duoc, testCases phai cu the.\n"
            "- Essay: rubric phai ro rang, co the danh gia duoc.\n"
            "- Difficulty phai tuong ung: BEGINNER (nhan biet), INTERMEDIATE (van dung), ADVANCED (phan tich/sang tao).\n\n"
            "# LESSON CONTEXT\n"
            f"Course: {lesson.get('course_title') or 'N/A'}\n"
            f"Lesson: {lesson.get('title')}\n"
            f"Description: {lesson.get('description') or 'N/A'}\n"
            f"Content type: {lesson.get('content_type')}\n"
            f"Workspace languages: {workspace_langs}\n"
            f"Content:\n{content_preview}\n\n"
            "# REQUEST\n"
            f"Formats: {formats}\n"
            f"Difficulties: {difficulties}\n"
            f"Count per format: {max(1, request.count // max(len(formats), 1))}\n"
            f"Include explanations: {request.includeExplanations}\n"
            f"Include test cases: {request.includeTestCases}\n"
            + (f"Custom instruction: {request.customInstruction}\n" if request.customInstruction else "")
            + "\n# OUTPUT FORMAT EXAMPLE\n"
            '{"mcq": [{"question": "...", "options": [{"text": "...", "correct": true}, '
            '{"text": "...", "correct": false}], "difficulty": "BEGINNER", "explanation": "..."}], '
            '"coding": [{"question": "...", "starterCode": "def solve():\\n  pass", '
            '"testCases": [{"input": "...", "expectedOutput": "..."}], '
            '"difficulty": "INTERMEDIATE", "explanation": "..."}], '
            '"essay": [{"question": "...", "rubric": ["Criteria 1", "Criteria 2"], '
            '"difficulty": "ADVANCED", "explanation": "..."}]}\n\n'
            "Tra ve JSON exercises voi chat luong production-ready."
        )

    def _build_feedback_prompt(self, request: QuizFeedbackRequest) -> str:
        selected = ", ".join(request.selectedAnswers) or "khong co dap an nao"
        correct = ", ".join(request.correctAnswers) or "khong co dap an dung duoc gui"
        options = "\n".join(
            f"- {str(item.get('text') or '').strip()} | correct={item.get('correct') or item.get('isCorrect')}"
            for item in request.options[:8]
            if isinstance(item, dict)
        )
        existing_explanation = request.explanation or "N/A"

        return (
            "# ROLE\n"
            "Ban la tro ly hoc tap cua TechHub, giai thich dap an trac nghiem cho hoc vien.\n\n"
            "# RULES\n"
            "- Tra ve JSON only, dung schema duoc yeu cau.\n"
            "- Viet bang tieng Viet co dau, ngan gon, dung ngu canh bai hoc.\n"
            "- Khong chi trich nguoi hoc; tap trung vao ly do sai va buoc hoc lai.\n"
            "- Neu dap an sai, giai thich vi sao dap an da chon sai va vi sao dap an dung phu hop hon.\n"
            "- Goi y hoc lai phai gan voi lesson hien tai, khong bia course/lesson khong co trong context.\n\n"
            "# CONTEXT\n"
            f"Course: {request.courseTitle or 'N/A'}\n"
            f"Lesson: {request.lessonTitle or 'N/A'}\n"
            f"Question: {request.question}\n"
            f"Options:\n{options or 'N/A'}\n"
            f"Selected answers: {selected}\n"
            f"Correct answers: {correct}\n"
            f"Existing explanation: {existing_explanation}\n\n"
            "# OUTPUT JSON SCHEMA\n"
            "{\n"
            '  "correct": false,\n'
            '  "summary": "Mot cau tom tat ket qua",\n'
            '  "explanation": "Giai thich ngan gon 2-4 cau",\n'
            '  "selectedAnswers": ["..."],\n'
            '  "correctAnswers": ["..."],\n'
            '  "weakConcepts": ["khai niem yeu"],\n'
            '  "reviewSuggestions": [{"lessonId": "id neu co", "title": "ten bai", "reason": "ly do", "action": "viec can lam"}],\n'
            '  "nextAction": "Buoc tiep theo",\n'
            '  "source": "AI_SERVICE"\n'
            "}"
        )

    def _fallback_quiz_feedback(self, request: QuizFeedbackRequest) -> QuizFeedbackResponse:
        selected = ", ".join(request.selectedAnswers) or "chua chon dap an"
        correct = ", ".join(request.correctAnswers) or "dap an dung"
        lesson_title = request.lessonTitle or "bai hoc hien tai"
        explanation = request.explanation or (
            f"Dap an ban chon la {selected}, trong khi dap an dung la {correct}. "
            "Hay doc lai phan noi dung lien quan truc tiep den cau hoi nay."
        )
        if request.isCorrect:
            explanation = request.explanation or "Ban da chon dap an phu hop voi noi dung bai hoc."

        return QuizFeedbackResponse(
            correct=request.isCorrect,
            summary=(
                "Ban da tra loi dung cau hoi nay."
                if request.isCorrect
                else "Cau tra loi chua khop voi trong tam kien thuc cua bai hoc."
            ),
            explanation=explanation,
            selectedAnswers=request.selectedAnswers,
            correctAnswers=request.correctAnswers,
            weakConcepts=self._extract_feedback_concepts(request.question),
            reviewSuggestions=[
                QuizReviewSuggestion(
                    lessonId=request.lessonId,
                    title=lesson_title,
                    reason=(
                        "Tiep tuc hoc bai tiep theo de giu mach kien thuc."
                        if request.isCorrect
                        else "On lai phan noi dung lien quan den cau hoi vua sai."
                    ),
                    action=(
                        "Chuyen sang cau hoi tiep theo."
                        if request.isCorrect
                        else "Doc lai lesson, ghi chu y chinh, sau do lam lai cau hoi."
                    ),
                )
            ],
            nextAction=(
                "Tiep tuc voi cau hoi tiep theo."
                if request.isCorrect
                else f"On lai '{lesson_title}' truoc khi lam lai."
            ),
            source="AI_SERVICE_FALLBACK",
        )

    def _normalize_feedback_payload(
        self,
        payload: dict[str, Any],
        fallback: QuizFeedbackResponse,
    ) -> QuizFeedbackResponse:
        if not isinstance(payload, dict):
            return fallback
        merged = fallback.model_dump(mode="json")
        for key, value in payload.items():
            if value not in (None, "", []):
                merged[key] = value
        try:
            return QuizFeedbackResponse.model_validate(merged)
        except Exception:
            return fallback

    @staticmethod
    def _extract_feedback_concepts(question: str) -> list[str]:
        tokens = [
            token
            for token in re.findall(r"\w{5,}", question or "")
            if token.lower()
            not in {
                "trong",
                "duoc",
                "nhung",
                "chinh",
                "question",
                "which",
                "about",
                "lesson",
            }
        ]
        concepts: list[str] = []
        for token in tokens:
            if token not in concepts:
                concepts.append(token)
            if len(concepts) >= 3:
                break
        return concepts or ["noi dung bai hoc"]

    def _fallback_exercises(self, lesson: dict[str, Any], request: AiExerciseGenerateRequest) -> dict[str, list[dict[str, Any]]]:
        concepts = self._extract_concepts(lesson)
        if not concepts:
            concepts = [lesson.get("title") or "main topic"]

        difficulties = [level.value for level in request.difficulties] or [request.difficulty]
        exercises: dict[str, list[dict[str, Any]]] = {}

        for format_index, exercise_format in enumerate(request.formats):
            key = exercise_format.value.lower()
            items: list[dict[str, Any]] = []
            for item_index in range(max(1, request.count // max(len(request.formats), 1))):
                concept = concepts[(format_index + item_index) % len(concepts)]
                difficulty = difficulties[(format_index + item_index) % len(difficulties)]
                if exercise_format == ExerciseFormat.MCQ:
                    items.append(self._build_mcq_item(lesson, concept, difficulty))
                elif exercise_format == ExerciseFormat.ESSAY:
                    items.append(self._build_essay_item(lesson, concept, difficulty))
                elif exercise_format == ExerciseFormat.CODING:
                    items.append(self._build_coding_item(lesson, concept, difficulty))
            exercises[key] = items

        return exercises

    def _normalize_payload(self, payload: dict[str, Any], fallback: dict[str, list[dict[str, Any]]]) -> dict[str, list[dict[str, Any]]]:
        if not isinstance(payload, dict):
            return fallback

        normalized: dict[str, list[dict[str, Any]]] = {}
        for key, fallback_items in fallback.items():
            raw_items = payload.get(key)
            if not isinstance(raw_items, list):
                normalized[key] = fallback_items
                continue
            cleaned = [item for item in raw_items if isinstance(item, dict)]
            normalized[key] = cleaned or fallback_items
        return normalized or fallback

    @staticmethod
    def _extract_concepts(lesson: dict[str, Any]) -> list[str]:
        text = " ".join(
            [
                str(lesson.get("title") or ""),
                str(lesson.get("description") or ""),
                str(lesson.get("content") or ""),
            ]
        )
        tokens = [
            token
            for token in re.findall(r"[A-Za-z_]{4,}", text)
            if token.lower() not in {"this", "that", "with", "from", "into", "lesson", "course"}
        ]
        counts = Counter(token.lower() for token in tokens)
        return [token for token, _ in counts.most_common(8)]

    @staticmethod
    def _build_mcq_item(lesson: dict[str, Any], concept: str, difficulty: str) -> dict[str, Any]:
        title = lesson.get("title") or "lesson"
        return {
            "question": f"Trong bai '{title}', nhan dinh nao mo ta dung nhat ve {concept}?",
            "options": [
                {"text": f"{concept} la mot y chinh duoc trinh bay trong bai hoc.", "correct": True},
                {"text": f"{concept} khong lien quan toi muc tieu cua bai hoc.", "correct": False},
                {"text": f"{concept} chi duoc nhac toi nhu vi du ngoai le, khong can nam.", "correct": False},
                {"text": f"{concept} chi co gia tri khi bo qua noi dung con lai cua bai.", "correct": False},
            ],
            "difficulty": difficulty,
            "explanation": f"Cau hoi kiem tra kha nang nhan dien vai tro cua {concept} trong bai hoc.",
        }

    @staticmethod
    def _build_essay_item(lesson: dict[str, Any], concept: str, difficulty: str) -> dict[str, Any]:
        return {
            "question": (
                f"Giai thich {concept} trong ngu canh bai '{lesson.get('title')}'. "
                "Neu duoc, hay lien he voi muc tieu hoc tap va tinh huong ap dung thuc te."
            ),
            "difficulty": difficulty,
            "rubric": [
                "Trinh bay dung khai niem/ky thuat trong bai hoc.",
                "Lien ket duoc voi noi dung tong the cua lesson.",
                "Neu vi du hoac cach ap dung thuc te.",
            ],
            "explanation": f"Bai tu luan danh gia kha nang dien giai va ket noi kien thuc ve {concept}.",
        }

    @staticmethod
    def _build_coding_item(lesson: dict[str, Any], concept: str, difficulty: str) -> dict[str, Any]:
        starter = ""
        workspace_template = lesson.get("workspace_template") or {}
        if isinstance(workspace_template, dict):
            starter = str(workspace_template.get("starterCode") or workspace_template.get("template") or "")

        return {
            "question": (
                f"Viet mot doan ma ngan de minh hoa {concept} dua tren bai '{lesson.get('title')}'. "
                "Neu bai hoc co API/ham mau thi su dung lai cau truc tuong tu."
            ),
            "difficulty": difficulty,
            "starterCode": starter,
            "testCases": [
                {
                    "input": f"sample input for {concept}",
                    "expectedOutput": f"expected behavior demonstrating {concept}",
                }
            ],
            "explanation": f"Bai tap code nay buoc nguoi hoc ap dung truc tiep {concept}.",
        }


exercise_service = ExerciseService()
