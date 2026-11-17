"""Read Spring's DB snapshot and optionally exercise the configured real OpenAI connection.

Run from ai-server: .venv/Scripts/python.exe scripts/smoke_course_rag.py --live
No DB writes, accounts, or credentials appear in the report.
"""
import argparse
import json
import os
from pathlib import Path
import re
import sys
from time import perf_counter

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from app import course_rag as rag


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--live", action="store_true", help="Use real embeddings and reasons; requires configured OpenAI")
    parser.add_argument("--snapshot", type=Path, default=ROOT.parent / "smarttraveler/build/rag-audit-candidates.json")
    args = parser.parse_args()
    if not args.live:
        parser.error("Pass --live explicitly; deterministic coverage is in tests/test_course_rag.py")
    env_path = ROOT / ".env"
    if env_path.exists():
        for line in env_path.read_text(encoding="utf-8-sig").splitlines():
            key, separator, value = line.partition("=")
            if separator and key.strip() in {"OPENAI_API_KEY", "OPENAI_MODEL", "COURSE_EMBEDDING_MODEL", "OPENAI_BASE_URL"}:
                os.environ.setdefault(key.strip(), value.strip().strip('"\''))
    if not os.getenv("OPENAI_API_KEY", "").strip():
        print("NOT RUN: OPENAI_API_KEY is not configured")
        return 2
    snapshot = json.loads(args.snapshot.read_text(encoding="utf-8"))
    report = []
    profiles = {"outdoor_relaxed": dict(pace="relaxed", environment="outdoor", landscapes=["sea"]),
                "indoor_packed": dict(pace="packed", environment="indoor", landscapes=["city"])}
    for duration in range(1, 5):
        candidates = snapshot[str(duration)]
        tests = profiles.items() if duration == 1 else [("outdoor_relaxed", profiles["outdoor_relaxed"])]
        for profile, saved in tests:
            conditions = dict(regionName="부산", durationDays=duration, startDate="2026-12-10",
                              companionType="FRIENDS", transportType="CAR", preferences=[])
            started = perf_counter()
            answer = rag.recommend_course(dict(merged=conditions, courseCandidates=candidates, savedPreferences=saved))
            ids = [int(value) for value in re.findall(r"추천 \d+ · 코스 #(\d+)", answer)]
            assert 1 <= len(ids) <= 3 and len(ids) == len(set(ids))
            by_id = {row["courseId"]: row for row in candidates}
            for course_id in ids:
                assert course_id in by_id and by_id[course_id]["title"] in answer
                expected = [row["placeName"] for row in candidates if row["courseId"] == course_id]
                assert all(name in answer for name in expected)
            assert f"{duration}일차" in answer and len(answer) <= 20000
            item = dict(duration=duration, profile=profile, selectedCourseIds=ids,
                        candidateCourses=len(by_id), elapsedSeconds=round(perf_counter() - started, 2), characters=len(answer))
            report.append(item)
            print(json.dumps(item, ensure_ascii=False), flush=True)
    assert report[0]["selectedCourseIds"][0] != report[1]["selectedCourseIds"][0]
    output = args.snapshot.parent / "rag-live-smoke.json"
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print("PASS: real DB courses, all durations, differing profile rankings; report: " + str(output))
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as error:
        # Avoid dumping provider request details, credentials, or user data.
        print("FAILED: " + type(error).__name__ + "; check connection/configuration or assertions", file=sys.stderr)
        raise SystemExit(1)
