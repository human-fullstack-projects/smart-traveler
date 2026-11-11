import chromadb
import pytest
from fastapi.testclient import TestClient
from app import db, main, rag, search
from app.course_graph import build_course_graph
from app.schemas import CourseComposeRequest, PolishedCourse, PolishedStop

# 해운대해수욕장 근처 가짜 데이터. 임베딩은 [관광, 식사, 시장] 3차원 가짜 벡터
ROWS = [
    (1, "해운대해수욕장", "ATTRACTION", False, 35.1590, 129.1603, [1, 0, 0], 0.9),
    (2, "할매집", "RESTAURANT", False, 35.1631, 129.1649, [0, 1, 0], 0.5),
    (3, "오반장", "RESTAURANT", False, 35.1600, 129.1580, [0, 1, 0.1], 0.4),
    (4, "기장 먼식당", "RESTAURANT", False, 35.2440, 129.2220, [0, 1, 0], 1.0),   # 약 11km 밖
    (5, "해운대시장", "SHOPPING", True, 35.1615, 129.1627, [0, 0.3, 1], 0.6),
    (6, "센텀 백화점", "SHOPPING", False, 35.1690, 129.1290, [0, 0, 1], 0.9),     # 시장 아님
    (7, "씨라이프부산아쿠아리움", "ATTRACTION", False, 35.1592, 129.1610, [1, 0, 0.1], 0.7),
    (8, "해운대 블루라인파크", "LEISURE", False, 35.1610, 129.1720, [0.9, 0, 0], 0.6),
]
QUERY = {"대표 관광 명소": [1, 0, 0], "점심 식사하기 좋은 맛집": [0, 1, 0],
         "체험하거나 즐길 거리": [1, 0, 0], "전통시장 먹거리 구경": [0, 0, 1]}


class FakeEmbeddings:
    """OpenAI 대신 쓰는 가짜 임베딩. 질의는 QUERY 표, 문서는 길이로 만든 3차원 벡터."""
    def embed_query(self, text):
        return QUERY[text]

    def embed_documents(self, docs):
        return [[1.0, len(d) % 7 / 7, 0.5] for d in docs]


@pytest.fixture(autouse=True)
def fake_chroma(monkeypatch):
    """메모리 안에서만 쓰는 Chroma에 가짜 장소 8곳을 넣고 rag.places를 바꿔 끼운다."""
    client = chromadb.EphemeralClient()
    try:
        client.delete_collection("test_places")
    except Exception:
        pass
    col = client.create_collection("test_places", metadata={"hnsw:space": "cosine"})
    col.add(ids=[str(r[0]) for r in ROWS], embeddings=[r[6] for r in ROWS],
            documents=[f"[{r[1]}] 부산광역시 해운대구\n분류: {r[2]}\n소개: {r[1]} 소개" for r in ROWS],
            metadatas=[{"place_id": r[0], "place_type": r[2], "is_market": r[3], "sigungu_code": "26350",
                        "lat": r[4], "lng": r[5], "popularity": r[7]} for r in ROWS])
    monkeypatch.setattr(rag, "places", col)
    monkeypatch.setattr(rag, "get_embeddings", lambda: FakeEmbeddings())


def mariadb_row(place_id, name, address="부산 해운대구", description="소개"):
    """db.read_places()가 돌려주는 한 행과 같은 모양."""
    return {"place_id": place_id, "place_type": "ATTRACTION", "name": name, "address": address,
            "description": description, "closed_days": None, "is_market": False,
            "region_code": "26350", "latitude": 35.16, "longitude": 129.16, "main_menu": None,
            "image_url": None, "hub_rank": None}


def test_MariaDB에서_Chroma로_적재한다(monkeypatch):
    calls = []
    original = rag.index_places
    monkeypatch.setattr(rag, "index_places", lambda rows: calls.append([r.placeId for r in rows]) or original(rows))
    monkeypatch.setattr(db, "read_places", lambda code: [
        mariadb_row(1, "해운대해수욕장"),                             # 이미 있음, 문서가 바뀜 → 다시 임베딩
        mariadb_row(100, "새 장소"),                                  # 새로 생김 → 임베딩
        mariadb_row(200, "이름만", address=None, description=None),   # 이름만 → 넣지 않음
    ])
    result = rag.sync_from_mariadb("26350")
    assert result == {"read": 3, "skipped": 1, "embedded": 2, "unchanged": 0, "removed": 7}
    assert calls == [[1, 100]]
    assert sorted(rag.places.get(include=[])["ids"]) == ["1", "100"]   # MariaDB에 없는 2~8은 지워짐

    # 한 번 더 돌리면 바뀐 게 없으니 OpenAI를 부르지 않는다
    calls.clear()
    assert rag.sync_from_mariadb("26350")["embedded"] == 0 and calls == []


def test_이름으로_앵커를_찾는다():
    anchor = search.find_anchor("26350", "해운대", "대표 관광 명소", set())
    assert anchor["name"] == "해운대해수욕장"


def test_반경_밖과_시장_아닌_쇼핑은_빠진다():
    result = search.search_candidates("26350", "해운대해수욕장", "PUBLIC_TRANSPORT",
                                      ["ANCHOR", "MEAL", "FUN", "MARKET"], {}, [])
    meal_ids = [c["placeId"] for c in result["candidates"]["MEAL"]]
    assert 4 not in meal_ids and set(meal_ids) == {2, 3}
    assert [c["placeId"] for c in result["candidates"]["MARKET"]] == [5]
    assert all(c["km"] <= result["radiusKm"] for cs in result["candidates"].values() for c in cs)
    assert 1 not in [c["placeId"] for c in result["candidates"]["FUN"]]   # 앵커는 FUN에 다시 안 나온다


def test_제외할_장소는_후보에서_빠진다():
    result = search.search_candidates("26350", "해운대해수욕장", "PUBLIC_TRANSPORT", ["MEAL"], {}, [2])
    assert [c["placeId"] for c in result["candidates"]["MEAL"]] == [3]


REQUEST = {"sigunguCode": "26350", "anchorName": "해운대해수욕장", "transportType": "PUBLIC_TRANSPORT",
           "slots": ["ANCHOR", "MEAL", "FUN", "MARKET"], "queries": {}, "excludePlaceIds": [], "startTime": "10:00"}


def run_graph(polisher, **change):
    """코스 그래프를 가짜 OpenAI(polisher)로 실행한다."""
    return build_course_graph(polisher=polisher).invoke({"request": REQUEST | change})["result"]


def top_ids():
    """앵커 + 슬롯별 점수 1등 (테스트 데이터에서는 슬롯끼리 겹치지 않는다)."""
    found = search.search_candidates("26350", "해운대해수욕장", "PUBLIC_TRANSPORT", ["ANCHOR", "MEAL", "FUN", "MARKET"], {}, [])
    return [found["anchor"]["placeId"]] + [found["candidates"][s][0]["placeId"] for s in ["MEAL", "FUN", "MARKET"]]


def broken(draft):
    raise RuntimeError("OpenAI 오류")


def test_그래프는_점수_1등으로_조합하고_점심은_11시30분_이후():
    course = run_graph(broken)
    assert course["polished"] is False                       # 응답 생성이 실패해도 코스는 나온다
    assert [s["slot"] for s in course["stops"]] == ["ANCHOR", "MEAL", "FUN", "MARKET"]
    assert [s["placeId"] for s in course["stops"]] == top_ids()
    assert course["stops"][0]["startTime"] == "10:00" and course["stops"][1]["startTime"] >= "11:30"
    assert all(s["description"] for s in course["stops"])   # 초안 문장이 채워져 있다


def test_OpenAI_응답_문장만_바꾸고_장소는_그대로():
    def polite(draft):
        return PolishedCourse(title="바다와 시장 하루", summary="해변에서 시작해 시장으로 끝나는 코스",
                              stops=[PolishedStop(placeId=s["place"]["placeId"], description=f"{s['place']['name']}에서 쉬어 가요")
                                     for s in draft["stops"]])
    course = run_graph(polite)
    assert course["polished"] is True and course["title"] == "바다와 시장 하루"
    assert [s["placeId"] for s in course["stops"]] == top_ids()
    assert course["stops"][1]["description"] == f"{course['stops'][1]['name']}에서 쉬어 가요"


def test_OpenAI가_장소를_바꾸면_응답_문장을_버린다():
    def sneaky(draft):
        return PolishedCourse(title="엉뚱한 코스", summary="",
                              stops=[PolishedStop(placeId=999, description="없는 장소") for _ in draft["stops"]])
    course = run_graph(sneaky)
    assert course["polished"] is False and course["title"] != "엉뚱한 코스"
    assert [s["placeId"] for s in course["stops"]] == top_ids()


def test_앵커나_식당이_없으면_오류_결과():
    assert "error" in run_graph(broken, sigunguCode="11110")             # 데이터 없는 지역
    assert "error" in run_graph(broken, excludePlaceIds=[2, 3])          # 반경 안 식당을 모두 제외


def test_검색_API():
    client = TestClient(main.app)
    response = client.post("/internal/places/search", json={"sigunguCode": "26350", "anchorName": "해운대"})
    assert response.status_code == 200 and response.json()["anchor"]["placeId"] == 1
    assert client.post("/internal/places/search", json={"sigunguCode": "abc"}).status_code == 422


def test_코스_추천_API(monkeypatch):
    monkeypatch.setattr(main, "course_graph", build_course_graph(polisher=broken))
    client = TestClient(main.app)
    response = client.post("/internal/courses/recommend", json={"sigunguCode": "26350", "anchorName": "해운대"})
    assert response.status_code == 200 and len(response.json()["stops"]) == 4
    assert client.post("/internal/courses/recommend", json={"sigunguCode": "11110"}).status_code == 422
