"""
Release smoke runner: drives every step test in sequence and produces a single
machine-readable release report.

Each step's test file already does end-to-end work against PostgreSQL + Qdrant
(live) + mocked HTTP for Java publishers. This runner:

  1. Imports each `tests.test_stepNN_*` module and invokes its `main()`.
  2. Captures the per-case PASS/FAIL output of every step.
  3. Snapshots release readiness via `ReleaseReadinessService` after all steps run.
  4. Writes a JSON report to `release_smoke_report.json` and prints a summary.

Run from the service root:

    python -m scripts.e2e.run_release_smoke

Exit 0 only if every step test passes; otherwise exits with the count of
failed steps so CI/admin can see the failure shape immediately.
"""

from __future__ import annotations

import asyncio
import importlib
import io
import json
import sys
import traceback
from contextlib import redirect_stderr, redirect_stdout
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

SERVICE_ROOT = Path(__file__).resolve().parents[2]
if str(SERVICE_ROOT) not in sys.path:
    sys.path.insert(0, str(SERVICE_ROOT))

from app.services.release_readiness_service import release_readiness_service  # noqa: E402


STEPS: tuple[tuple[str, str], ...] = (
    ("step03_analytics_semantic_layer", "tests.test_step03_analytics_semantic_layer"),
    ("step04_recommendation_signal_layer", "tests.test_step04_recommendation_signal_layer"),
    ("step05_vector_index_and_events", "tests.test_step05_vector_index_and_events"),
    ("step06_learning_path_publish_flow", "tests.test_step06_learning_path_publish_flow"),
    ("step07_exercise_publish_flow", "tests.test_step07_exercise_publish_flow"),
)


async def _run_step(label: str, module_path: str) -> dict[str, Any]:
    started = datetime.now(timezone.utc).isoformat()
    captured = io.StringIO()
    err_captured = io.StringIO()
    exit_code: int
    try:
        with redirect_stdout(captured), redirect_stderr(err_captured):
            module = importlib.import_module(module_path)
            # Each step test exposes async main() -> int (exit code)
            exit_code = await module.main()
    except Exception as exc:  # noqa: BLE001
        exit_code = 99
        captured.write("\nRUNNER EXCEPTION:\n")
        captured.write(traceback.format_exception_only(type(exc), exc)[0])
    finished = datetime.now(timezone.utc).isoformat()
    stdout = captured.getvalue()
    # Extract the "PASSED: X / Y" line if present.
    pass_line = next(
        (line for line in stdout.splitlines() if line.strip().startswith("PASSED:")),
        None,
    )
    return {
        "label": label,
        "module": module_path,
        "exitCode": exit_code,
        "startedAt": started,
        "finishedAt": finished,
        "summary": pass_line or "(no summary line found)",
        "stdoutTail": "\n".join(stdout.splitlines()[-40:]),
        "stderrTail": "\n".join(err_captured.getvalue().splitlines()[-20:]),
    }


async def main() -> int:
    print(f"=== Release smoke runner started at {datetime.now(timezone.utc).isoformat()} ===\n")
    step_results: list[dict[str, Any]] = []
    failed_count = 0
    for label, module_path in STEPS:
        print(f"--- Running {label} ({module_path})")
        result = await _run_step(label, module_path)
        if result["exitCode"] != 0:
            failed_count += 1
            print(f"   FAIL exit={result['exitCode']} :: {result['summary']}")
        else:
            print(f"   PASS :: {result['summary']}")
        step_results.append(result)

    print("\n=== Snapshotting release readiness ===")
    try:
        readiness = await release_readiness_service.snapshot()
    except Exception as exc:  # noqa: BLE001
        readiness = {"error": f"readiness snapshot failed: {type(exc).__name__}: {exc}"}

    report = {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "totalSteps": len(STEPS),
        "failedSteps": failed_count,
        "steps": step_results,
        "releaseReadiness": readiness,
    }
    report_path = SERVICE_ROOT / "release_smoke_report.json"
    report_path.write_text(json.dumps(report, indent=2, default=str), encoding="utf-8")
    print(f"\nReport written to {report_path}")

    capabilities = (readiness or {}).get("capabilities") or {}
    if capabilities:
        print("\nCapability status:")
        for name, info in capabilities.items():
            print(f"  - {name:<28} {info.get('status', '?'):<18} {info.get('reason', '')}")

    print(f"\nSUMMARY: {len(STEPS) - failed_count}/{len(STEPS)} steps passed")
    return failed_count


if __name__ == "__main__":
    sys.exit(asyncio.run(main()))
