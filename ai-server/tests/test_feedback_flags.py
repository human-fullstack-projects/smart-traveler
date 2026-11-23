"""피드백 반영: 조건만 읽기(extractOnly) · 바로 추천(forceRecommend) · 장소 설명 한 줄."""
from app import course_rag
from app.graph import build_graph
from app.schemas import ExtractedConditions


def extracted(**changes):
    values = dict(regionName=None, startDate=None, durationDays=None, adults=None, children=None,
                  companionType=None, transportType=None, preferences=None)
    values.update(changes)
    return ExtractedConditions(**values)


FULL = dict(regionName="부산", startDate="2026-12-10", durationDays=2, adults=2, children=0,
            companionType="FRIENDS", transportType="CAR", flowType="COURSE_FIRST")


def test_조건만_읽기는_조건이_다_있어도_추천하지_않음():
    called = []
    graph = build_graph(lambda state: extracted(), lambda state: called.append(1) or "추천")
    result = graph.invoke({"message": "추천해줘", "previousQuestion": "", "conditions": dict(FULL), "extractOnly": True})
    assert not called
    assert not result.get("confirmed")


def test_바로_추천은_조건을_다시_읽지_않고_추천():
    def must_not_extract(state):
        raise AssertionError("forceRecommend에서는 조건을 다시 읽지 않는다")
    graph = build_graph(must_not_extract, lambda state: "장소 추천")
    conditions = {"regionName": "부산", "startDate": "2026-12-10", "durationDays": 2, "adults": 1, "children": 0}
    result = graph.invoke({"message": "부산 가고 싶어", "previousQuestion": "", "conditions": conditions,
                           "candidates": [{"placeId": 1, "name": "해운대", "address": "부산"}], "forceRecommend": True})
    assert result["question"] == "장소 추천"
    assert result["confirmed"] is True


def test_코스_장소_옆에_설명_한_줄(monkeypatch):
    rows = []
    for seq, (name, summary) in enumerate([("해운대해수욕장", "부산 대표 해변."), ("동백섬", "")], 1):
        rows.append({"courseId": 7, "title": "부산 바다 코스", "destination": "부산", "regionId": 6, "companion": "FRIENDS",
                     "transport": "CAR", "theme": "바다", "pace": "normal", "environment": "outdoor", "landscapes": ["sea"],
                     "dayNo": 1, "dayTheme": "바다", "seq": seq, "placeId": 100 + seq, "placeName": name, "placeSummary": summary})
    monkeypatch.setattr(course_rag, "document_vectors", lambda model, texts: [[1.0, 0.0] for _ in texts])
    monkeypatch.setattr(course_rag, "OpenAIEmbeddings", lambda **kw: type("E", (), {"embed_query": lambda self, q: [1.0, 0.0]})())
    monkeypatch.setattr(course_rag, "course_reason", lambda course, matched: "이유")
    state = {"merged": {"regionName": "부산", "startDate": "2026-12-10", "durationDays": 1}, "courseCandidates": rows}
    text, course_id = course_rag.recommend_course_result(state)
    assert course_id == 7
    assert "1. 해운대해수욕장 — 부산 대표 해변." in text
    assert "2. 동백섬\n" in text + "\n"
