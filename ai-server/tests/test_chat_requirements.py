from fastapi.testclient import TestClient
from app import main
from app.graph import CHECK, INTRO, build_graph
from app.schemas import ExtractedConditions
from .test_course_rag import conditions, rows, offline, empty_extraction


def test_intro_answer_extracts_all_conditions_then_confirms(monkeypatch):
    graph = build_graph(lambda state: ExtractedConditions(
        regionName="부산", startDate="2026-12-10", durationDays=2, adults=2, children=1,
        companionType="FAMILY", transportType="CAR", preferences=None, lodgingName=None))
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
