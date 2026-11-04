from fastapi.testclient import TestClient
from app.graph import ADULTS, CHECK, CLARIFY, EDIT, LEGACY_CHECK, DATE, REGION, build_graph, requests_recommendation
from app.schemas import ExtractedConditions
from app import main


def extracted(**changes):
    values = dict(regionName=None, startDate=None, durationDays=None, adults=None, children=None, companionType=None, transportType=None, preferences=None)
    values.update(changes)
    return ExtractedConditions(**values)


def test_조건_병합과_질문_분기():
    graph = build_graph(lambda state: extracted(regionName="부산", durationDays=3, companionType="FRIENDS"))
    result = graph.invoke({"message": "친구와 부산 2박 3일", "previousQuestion": "", "conditions": {"regionId": None, "regionName": "", "adults": 2, "preferences": ["바다"]}})
    assert result["changes"]["regionName"] == "부산"
    assert result["merged"]["adults"] == 2
    assert result["merged"]["preferences"] == ["바다"]
    assert result["question"] == DATE


def test_빈값은_추측하지_않고_지역부터_질문():
    graph = build_graph(lambda state: extracted())
    result = graph.invoke({"message": "여행 가고 싶어", "previousQuestion": "", "conditions": {"regionId": None}})
    assert result["changes"] == {}
    assert result["question"] == REGION


def test_잘못된_날짜와_숫자는_기존값을_보존():
    graph = build_graph(lambda state: extracted(startDate="2026-02-30", durationDays=99, adults=-1))
    result = graph.invoke({"message": "바꿔줘", "previousQuestion": "", "conditions": {"regionName": "부산", "startDate": "2026-12-10", "durationDays": 3}})
    assert result["merged"]["startDate"] == "2026-12-10"
    assert result["merged"]["durationDays"] == 3
    assert result["question"] == ADULTS


def test_모든_필수값이_있으면_확인_안내():
    graph = build_graph(lambda state: extracted())
    conditions = dict(regionName="부산", startDate="2026-12-10", durationDays=3, adults=2, children=0, companionType="FRIENDS", transportType="CAR", flowType="COURSE_FIRST")
    result = graph.invoke({"message": "좋아요", "previousQuestion": "", "conditions": conditions})
    assert result["question"] == CHECK


def test_지역이_바뀌면_이전_지역번호_사용_금지():
    graph = build_graph(lambda state: extracted(regionName="제주"))
    result = graph.invoke({"message": "제주로 바꿔줘", "previousQuestion": "", "conditions": {"regionName": "부산", "regionId": 1}})
    assert result["merged"]["regionId"] is None


def test_API_모의_AI_결과(monkeypatch):
    monkeypatch.setattr(main, "graph", build_graph(lambda state: extracted(regionName="부산")))
    response = TestClient(main.app).post("/internal/dialogue", json={"message": "부산", "previousQuestion": "", "conditions": {}})
    assert response.status_code == 200
    assert response.json() == {"changes": {"regionName": "부산"}, "question": DATE, "confirmed": False}


def test_AI_오류는_민감정보_없이_503(monkeypatch):
    def fail(state):
        raise RuntimeError("secret user message")
    monkeypatch.setattr(main, "graph", build_graph(fail))
    response = TestClient(main.app).post("/internal/dialogue", json={"message": "부산", "conditions": {}})
    assert response.status_code == 503
    assert "secret" not in response.text


def test_LangChain_OpenAI_요청을_실제_네트워크_없이_검증(monkeypatch):
    import json
    import httpx2
    from langchain_openai import ChatOpenAI
    from app import graph as graph_module

    requests = []
    def openai_mock(request):
        requests.append(json.loads(request.content))
        body = {"regionName": "부산", "startDate": None, "durationDays": 3, "adults": None, "children": None, "companionType": "FRIENDS", "transportType": None, "preferences": None}
        return httpx2.Response(200, json={"id": "resp_test", "object": "response", "created_at": 1730000000, "status": "completed", "model": "gpt-4o-mini", "output": [{"type": "message", "id": "msg_test", "status": "completed", "role": "assistant", "content": [{"type": "output_text", "text": json.dumps(body, ensure_ascii=False), "annotations": []}]}], "tools": [], "tool_choice": "auto", "parallel_tool_calls": False})
    client = httpx2.Client(transport=httpx2.MockTransport(openai_mock))
    monkeypatch.setenv("OPENAI_API_KEY", "test-key")
    monkeypatch.setattr(graph_module, "ChatOpenAI", lambda **kwargs: ChatOpenAI(**kwargs, http_client=client))
    result = graph_module.build_graph().invoke({"message": "친구와 부산 2박 3일", "previousQuestion": "", "conditions": {}})
    assert result["changes"]["regionName"] == "부산"
    assert result["question"] == DATE
    assert requests[0]["store"] is False
    assert requests[0]["text"]["format"]["type"] == "json_schema"
    assert requests[0]["text"]["format"]["strict"] is True


def complete_conditions():
    return dict(regionName="부산", startDate="2026-12-10", durationDays=3, adults=2, children=0, companionType="FRIENDS", transportType="CAR", flowType="COURSE_FIRST", preferences=[])


def test_확인후_저장된_관광지를_추천한다():
    candidates = [{"placeId": 1, "name": "해변", "address": "부산"}]
    graph = build_graph(lambda state: extracted(), lambda state: "저장된 관광지: " + state["candidates"][0]["name"])
    result = graph.invoke({"message": "확인했어", "previousQuestion": CHECK, "conditions": complete_conditions(), "candidates": candidates})
    assert "해변" in result["question"] and result["confirmed"] is True


def test_후보가_없으면_없는_장소를_만들지_않는다():
    graph = build_graph(lambda state: extracted())
    result = graph.invoke({"message": "여행지 추천해줘", "previousQuestion": CHECK, "conditions": complete_conditions(), "candidates": []})
    assert "관광지 가져오기" in result["question"] and result["confirmed"] is True


def test_조건이_바뀌면_추천보다_재확인을_먼저한다():
    graph = build_graph(lambda state: extracted(regionName="제주"), lambda state: "잘못된 추천")
    result = graph.invoke({"message": "제주로 바꿔서 추천해줘", "previousQuestion": CHECK, "conditions": complete_conditions(), "candidates": [{"placeId":1, "name":"부산", "address":"부산"}]})
    assert result["question"] == CHECK and not result.get("confirmed", False)


def test_부정문은_추천_요청이_아니다():
    assert not requests_recommendation("아직 추천하지마", CHECK)
    assert not requests_recommendation("확인 안 했어", CHECK)
    assert requests_recommendation("수정할 거 없는데 이제 여행지 추천해줘", CHECK)


def test_추천은_후보에_없는_ID를_거절한다(monkeypatch):
    import pytest
    from app import graph as graph_module
    from app.schemas import PlaceRecommendations, SelectedPlace
    class FakeModel:
        def __init__(self, **kwargs): pass
        def with_structured_output(self, *args, **kwargs): return self
        def invoke(self, messages): return PlaceRecommendations(places=[SelectedPlace(placeId=999)])
    monkeypatch.setenv("OPENAI_API_KEY", "test")
    monkeypatch.setattr(graph_module, "ChatOpenAI", FakeModel)
    state = {"candidates": [{"placeId": 1, "name": "해변", "address": "부산"}], "merged": complete_conditions()}
    with pytest.raises(ValueError): graph_module.recommend_with_openai(state)


def test_추천명과_주소는_DB_후보에서_사용한다(monkeypatch):
    from app import graph as graph_module
    from app.schemas import PlaceRecommendations, SelectedPlace
    class FakeModel:
        def __init__(self, **kwargs): pass
        def with_structured_output(self, *args, **kwargs): return self
        def invoke(self, messages): return PlaceRecommendations(places=[SelectedPlace(placeId=1)])
    monkeypatch.setenv("OPENAI_API_KEY", "test")
    monkeypatch.setattr(graph_module, "ChatOpenAI", FakeModel)
    state = {"candidates": [{"placeId": 1, "name": "실제 후보", "address": "부산"}], "merged": complete_conditions()}
    output = graph_module.recommend_with_openai(state)
    assert "실제 후보" in output and "부산" in output


def test_관광지_추천의_OpenAI_요청_형식(monkeypatch):
    import json
    import httpx2
    from langchain_openai import ChatOpenAI
    from app import graph as graph_module
    requests = []
    def respond(request):
        requests.append(json.loads(request.content))
        body = {"places": [{"placeId": 1}]}
        return httpx2.Response(200, json={"id":"resp_test", "object":"response", "created_at":1730000000, "status":"completed", "model":"gpt-4o-mini", "output":[{"type":"message", "id":"msg_test", "status":"completed", "role":"assistant", "content":[{"type":"output_text", "text":json.dumps(body), "annotations":[]}]}], "tools":[], "tool_choice":"auto", "parallel_tool_calls":False})
    client = httpx2.Client(transport=httpx2.MockTransport(respond))
    monkeypatch.setenv("OPENAI_API_KEY", "test-key")
    monkeypatch.setattr(graph_module, "ChatOpenAI", lambda **kwargs: ChatOpenAI(**kwargs, http_client=client))
    output = graph_module.recommend_with_openai({"candidates":[{"placeId":1,"name":"실제 후보","address":"부산"}], "merged":complete_conditions()})
    assert "실제 후보" in output
    assert requests[0]["store"] is False
    assert requests[0]["text"]["format"]["type"] == "json_schema"


def test_확인_안내에_답변_방법을_표시한다():
    graph = build_graph(lambda state: extracted())
    result = graph.invoke({"message": "조건 다 말했어", "previousQuestion": "", "conditions": complete_conditions()})
    assert "확인했어" in result["question"] and "추천해줘" in result["question"]


def test_기존_확인_메시지와_여러_긍정_답변을_지원한다():
    graph = build_graph(lambda state: extracted(), lambda state: "관광지 추천 결과")
    for previous in (LEGACY_CHECK, CHECK, CLARIFY, EDIT):
        for message in ("확인했어", "맞아요", "그대로 해주세요", "수정할 거 없어요", "수정할 거 없는데"):
            result = graph.invoke({"message": message, "previousQuestion": previous, "conditions": complete_conditions()})
            assert result["question"] == "관광지 추천 결과" and result["confirmed"]


def test_모호한_답변에는_확인방법을_안내한다():
    graph = build_graph(lambda state: extracted(), lambda state: "잘못된 추천")
    result = graph.invoke({"message": "잘 모르겠어", "previousQuestion": CHECK, "conditions": complete_conditions()})
    assert result["question"] == CLARIFY and not result.get("confirmed", False)
    assert "확인했어" in result["question"]


def test_수정_요청은_추천하지_않고_수정내용을_묻는다():
    graph = build_graph(lambda state: extracted(), lambda state: "잘못된 추천")
    result = graph.invoke({"message": "날짜를 바꾸고 싶어", "previousQuestion": CHECK, "conditions": complete_conditions()})
    assert result["question"] == EDIT and not result.get("confirmed", False)
    assert not requests_recommendation("추천 말고 수정할게", CHECK)
    assert not requests_recommendation("확인 안 했어", CHECK)


def test_API_모호한_답변_후_확인하면_추천한다(monkeypatch):
    graph = build_graph(lambda state: extracted(), lambda state: "관광지 추천 결과")
    monkeypatch.setattr(main, "graph", graph)
    client = TestClient(main.app)
    first = client.post("/internal/dialogue", json={"message": "잘 모르겠어", "previousQuestion": CHECK, "conditions": complete_conditions()})
    assert first.status_code == 200 and first.json()["question"] == CLARIFY
    second = client.post("/internal/dialogue", json={"message": "맞아요", "previousQuestion": first.json()["question"], "conditions": complete_conditions(), "candidates": [{"placeId": 1, "name": "후보", "address": "부산"}]})
    assert second.status_code == 200 and second.json()["question"] == "관광지 추천 결과"
    assert second.json()["confirmed"] is True
