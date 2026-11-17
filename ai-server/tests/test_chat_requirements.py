from fastapi.testclient import TestClient
from app import main
from app.graph import CHECK, build_graph
from app.schemas import ExtractedConditions
from .test_course_rag import conditions, rows, offline, empty_extraction

INTRO = """우리 일정을 알차게 짜보려면 몇 가지만 알려주세요:
어디로, 언제, 며칠 동안 가실 예정인가요?
누구와 함께 떠나시나요? (혼자, 친구, 연인, 가족?)
성인 몇명, 아동 몇명인가요?
이동할 때 대중교통과 자동차중 무엇을 이용하시나요?"""


def test_intro_answer_extracts_all_conditions_then_confirms(monkeypatch):
    graph = build_graph(lambda state: ExtractedConditions(
        regionName="부산", startDate="2026-12-10", durationDays=2, adults=2, children=1,
        companionType="FAMILY", transportType="CAR", preferences=None))
    monkeypatch.setattr(main, "graph", graph)
    response = TestClient(main.app).post("/internal/dialogue", json={
        "message": "부산, 2026-12-10 출발 1박2일, 가족 성인2명 아동1명, 자동차", "previousQuestion": INTRO, "conditions": {}})
    assert response.status_code == 200
    data = response.json()
    assert data["question"] == CHECK and not data["confirmed"]
    assert data["changes"]["adults"] == 2 and data["changes"]["children"] == 1
    assert "recommendedCourseId" not in data


def test_api_returns_first_course_id_for_storage(monkeypatch, offline):
    monkeypatch.setattr(main, "graph", build_graph(lambda state: empty_extraction()))
    response = TestClient(main.app).post("/internal/dialogue", json={
        "message": "추천해줘", "previousQuestion": CHECK, "conditions": conditions(),
        "courseCandidates": rows(2) + rows(1), "savedPreferences": {"pace": "normal"}})
    data = response.json()
    assert response.status_code == 200 and data["recommendedCourseId"] == 1
    assert data["question"].startswith("추천 1 · 코스 #1\n")


def test_no_results_have_no_course_id_to_save(monkeypatch):
    monkeypatch.setattr(main, "graph", build_graph(lambda state: empty_extraction()))
    response = TestClient(main.app).post("/internal/dialogue", json={
        "message": "추천해줘", "previousQuestion": CHECK, "conditions": conditions(), "courseCandidates": []})
    assert response.status_code == 200 and "recommendedCourseId" not in response.json()
