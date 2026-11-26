"""app.dialogue(/internal/dialogue의 1회성 조회·추천, research/chat_test.py 포팅)에 대한 테스트."""
import json

from fastapi.testclient import TestClient

from app import dialogue, main


def _matches(metadata, where):
    if where is None:
        return True
    if "$and" in where:
        return all(_matches(metadata, clause) for clause in where["$and"])
    (key, cond), = where.items()
    if isinstance(cond, dict):
        if "$in" in cond:
            return metadata.get(key) in cond["$in"]
        if "$nin" in cond:
            return metadata.get(key) not in cond["$nin"]
        raise AssertionError(f"unsupported operator: {cond}")
    return metadata.get(key) == cond


class FakeCollection:
    def __init__(self, items):
        self.items = items  # list[(course_id, document, metadata)]

    def get(self, include=None):
        return {"metadatas": [meta for _, _, meta in self.items]}

    def query(self, query_embeddings, n_results, where=None):
        matched = [item for item in self.items if _matches(item[2], where)][:n_results]
        ids = [str(cid) for cid, _, _ in matched]
        return {
            "ids": [ids],
            "documents": [[doc for _, doc, _ in matched]],
            "metadatas": [[meta for _, _, meta in matched]],
            "distances": [[round(0.1 * i, 3) for i in range(len(matched))]],
        }


def meta(course_id, destination="부산광역시 해운대구", **overrides):
    base = {"course_id": course_id, "destination": destination}
    base.update(overrides)
    return base


class FakeToolCallClient:
    """extract_slots가 기대하는 OpenAI function-calling 응답 모양을 흉내낸다."""
    def __init__(self, arguments):
        self._arguments = arguments
        self.chat = self
        self.completions = self

    def create(self, **kwargs):
        call = type("Call", (), {"function": type("Fn", (), {"arguments": json.dumps(self._arguments)})()})
        message = type("Msg", (), {"tool_calls": [call]})()
        choice = type("Choice", (), {"message": message})()
        return type("Resp", (), {"choices": [choice]})()


def test_extract_slots_parses_function_call_arguments():
    client = FakeToolCallClient({"destination": "부산", "residual_text": "바다 보고 싶어"})
    assert dialogue.extract_slots("부산 바다 보고 싶어", client=client) == {
        "destination": "부산", "residual_text": "바다 보고 싶어"}


def test_handle_uses_extracted_slots_and_residual_text_as_query(monkeypatch):
    # region_themes("바다")는 끝까지 완화되지 않는 조건이라, 메타데이터에도 일치하는
    # region_theme_sea 플래그가 있어야 한다(실제 색인 문서와 동일한 모양).
    collection = FakeCollection([(1356, "부산 해운대 코스 문서", meta(1356, region_theme_sea=True))])
    client = FakeToolCallClient({
        "destination": "해운대", "region_themes": ["바다"], "purpose_theme": "맛집",
        "duration_days": 2, "companion_type": "FRIENDS", "transport_type": "CAR",
        "residual_text": "조용하고 한적한 곳",
    })
    captured = {}

    def fake_embed(text):
        captured["query_text"] = text
        return [0.0]

    detail = {"destination": "부산광역시 해운대구", "start_date": "2026-12-10", "end_date": "2026-12-11",
              "adults": 2, "children": 0, "companion_type": "FRIENDS", "transport_type": "CAR",
              "itinerary": [{"day_no": 1, "visits": [{"place_name": "해운대해수욕장"}]}]}
    monkeypatch.setattr(dialogue.db, "connect", lambda: "CONN")
    monkeypatch.setattr(dialogue.db, "fetch_course_detail", lambda conn, course_id: detail)

    message, course_id, course_ids = dialogue.handle(
        "해운대에서 조용하고 한적한 곳으로 2박3일 친구랑 자동차로 맛집 다니고 싶어",
        collection=collection, embed_fn=fake_embed, client=client,
    )

    assert course_id == 1356 and course_ids == [1356]
    assert captured["query_text"] == "조용하고 한적한 곳"
    assert "해운대해수욕장" in message
    assert "코스 #1356" in message


def test_handle_excludes_given_course_ids(monkeypatch):
    collection = FakeCollection([(1, "a", meta(1)), (2, "b", meta(2))])
    client = FakeToolCallClient({"residual_text": "부산 아무 코스나"})
    monkeypatch.setattr(dialogue.db, "connect", lambda: "CONN")
    monkeypatch.setattr(dialogue.db, "fetch_course_detail", lambda conn, course_id: None)

    _message, course_id, course_ids = dialogue.handle(
        "부산 아무 코스나", excluded_course_ids=[1], collection=collection, embed_fn=lambda t: [0.0], client=client)

    assert course_id == 2 and course_ids == [2]


def test_handle_returns_not_found_message_without_touching_db(monkeypatch):
    collection = FakeCollection([])
    client = FakeToolCallClient({"residual_text": "아무것도 없음"})

    def fail_connect():
        raise AssertionError("결과가 없으면 DB에 연결하지 않아야 한다")

    monkeypatch.setattr(dialogue.db, "connect", fail_connect)

    message, course_id, course_ids = dialogue.handle(
        "존재하지않는 지역 여행", collection=collection, embed_fn=lambda t: [0.0], client=client)

    assert course_id is None and course_ids is None
    assert "찾지 못했어요" in message


def test_handle_falls_back_to_full_message_when_extraction_fails(monkeypatch):
    collection = FakeCollection([(1, "doc", meta(1))])
    captured = {}

    class BrokenClient:
        chat = None

    def fake_embed(text):
        captured["query_text"] = text
        return [0.0]

    monkeypatch.setattr(dialogue.db, "connect", lambda: "CONN")
    monkeypatch.setattr(dialogue.db, "fetch_course_detail", lambda conn, course_id: None)

    dialogue.handle("부산 가자", collection=collection, embed_fn=fake_embed, client=BrokenClient())
    assert captured["query_text"] == "부산 가자"


def test_handle_returns_off_topic_guidance_without_searching(monkeypatch):
    client = FakeToolCallClient({"residual_text": "오늘 점심 뭐 먹지", "is_travel_request": False})

    def fail_embed(text):
        raise AssertionError("여행과 무관하면 임베딩/검색을 하지 않아야 한다")

    def fail_connect():
        raise AssertionError("여행과 무관하면 DB에 연결하지 않아야 한다")

    monkeypatch.setattr(dialogue.db, "connect", fail_connect)

    message, course_id, course_ids = dialogue.handle(
        "오늘 점심 뭐 먹지", collection=FakeCollection([]), embed_fn=fail_embed, client=client)

    assert message == dialogue.OFF_TOPIC_REPLY
    assert course_id is None and course_ids is None


def test_handle_treats_extraction_failure_as_travel_request(monkeypatch):
    # 추출 자체가 실패한 경우(네트워크/파싱 오류)는 "여행과 무관함"과 다른 문제이므로 차단하지 않는다.
    collection = FakeCollection([(1, "doc", meta(1))])

    class BrokenClient:
        chat = None

    monkeypatch.setattr(dialogue.db, "connect", lambda: "CONN")
    monkeypatch.setattr(dialogue.db, "fetch_course_detail", lambda conn, course_id: None)

    message, course_id, _course_ids = dialogue.handle(
        "부산 가자", collection=collection, embed_fn=lambda t: [0.0], client=BrokenClient())

    assert message != dialogue.OFF_TOPIC_REPLY
    assert course_id == 1


def test_api_returns_recommendation(monkeypatch):
    monkeypatch.setattr(main.dialogue, "handle", lambda message, excluded: ("추천 메시지", 11, [11, 12]))
    response = TestClient(main.app).post("/internal/dialogue", json={"message": "부산 여행"})
    assert response.status_code == 200
    assert response.json() == {"message": "추천 메시지", "recommendedCourseId": 11, "recommendedCourseIds": [11, 12]}


def test_api_rejects_blank_message():
    response = TestClient(main.app).post("/internal/dialogue", json={"message": ""})
    assert response.status_code == 422


def test_api_hides_internal_error_detail(monkeypatch):
    def fail(message, excluded):
        raise RuntimeError("secret detail")
    monkeypatch.setattr(main.dialogue, "handle", fail)
    response = TestClient(main.app).post("/internal/dialogue", json={"message": "부산 여행"})
    assert response.status_code == 503
    assert "secret" not in response.text


def test_api_defaults_excluded_course_ids_to_empty_list(monkeypatch):
    captured = {}

    def fake_handle(message, excluded):
        captured["excluded"] = excluded
        return "메시지", None, None

    monkeypatch.setattr(main.dialogue, "handle", fake_handle)
    TestClient(main.app).post("/internal/dialogue", json={"message": "부산 여행"})
    assert captured["excluded"] == []


def test_api_passes_excluded_course_ids_through(monkeypatch):
    captured = {}

    def fake_handle(message, excluded):
        captured["excluded"] = excluded
        return "메시지", None, None

    monkeypatch.setattr(main.dialogue, "handle", fake_handle)
    TestClient(main.app).post("/internal/dialogue", json={"message": "부산 여행", "excludedCourseIds": [1, 2]})
    assert captured["excluded"] == [1, 2]
