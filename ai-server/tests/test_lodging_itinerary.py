from fastapi.testclient import TestClient

from app import lodging_itinerary, main


def _lodging(**overrides):
    base = {"lodgingId": 1, "name": "숙소", "address": "부산", "regionId": 1,
            "latitude": 35.0, "longitude": 129.0, "regionName": "부산"}
    base.update(overrides)
    return base


def _place(place_id, name=None, address="부산"):
    return {"placeId": place_id, "name": name or f"관광지{place_id}", "address": address,
            "latitude": 35.0, "longitude": 129.0}


def test_missing_lodging_asks_to_reselect(monkeypatch):
    monkeypatch.setattr(lodging_itinerary.db, "connect", lambda: "CONN")
    monkeypatch.setattr(lodging_itinerary.db, "lodging_by_id", lambda conn, lid: None)
    message, places = lodging_itinerary.build({"lodgingId": 999})
    assert places is None and "다시 선택" in message


def test_lodging_without_coordinates_reports_insufficient_candidates(monkeypatch):
    monkeypatch.setattr(lodging_itinerary.db, "connect", lambda: "CONN")
    monkeypatch.setattr(lodging_itinerary.db, "lodging_by_id", lambda conn, lid: _lodging(latitude=None))
    message, places = lodging_itinerary.build({"lodgingId": 1})
    assert places is None and "부족" in message


def test_splits_by_day_and_includes_name_address(monkeypatch):
    monkeypatch.setattr(lodging_itinerary.db, "connect", lambda: "CONN")
    monkeypatch.setattr(lodging_itinerary.db, "lodging_by_id", lambda conn, lid: _lodging())
    monkeypatch.setattr(lodging_itinerary.db, "places_near_lodging",
                         lambda conn, lat, lng, limit=30: [_place(i) for i in range(1, 7)])
    message, places = lodging_itinerary.build({"lodgingId": 1, "durationDays": 2, "startDate": "2026-12-10"})
    assert [p["dayNo"] for p in places] == [1, 1, 1, 2, 2, 2]
    assert places[0]["name"] == "관광지1" and places[0]["address"] == "부산"
    assert "1일차 (2026-12-10)" in message and "2일차 (2026-12-11)" in message


def test_without_start_date_shows_day_only(monkeypatch):
    monkeypatch.setattr(lodging_itinerary.db, "connect", lambda: "CONN")
    monkeypatch.setattr(lodging_itinerary.db, "lodging_by_id", lambda conn, lid: _lodging())
    monkeypatch.setattr(lodging_itinerary.db, "places_near_lodging",
                         lambda conn, lat, lng, limit=30: [_place(i) for i in range(1, 4)])
    message, places = lodging_itinerary.build({"lodgingId": 1, "durationDays": 1})
    assert places and "1일차" in message and "(2026" not in message


def test_insufficient_candidates(monkeypatch):
    monkeypatch.setattr(lodging_itinerary.db, "connect", lambda: "CONN")
    monkeypatch.setattr(lodging_itinerary.db, "lodging_by_id", lambda conn, lid: _lodging())
    monkeypatch.setattr(lodging_itinerary.db, "places_near_lodging",
                         lambda conn, lat, lng, limit=30: [_place(1), _place(2)])
    message, places = lodging_itinerary.build({"lodgingId": 1, "durationDays": 1})
    assert places is None and "부족" in message


def test_reorders_matching_preferences_first(monkeypatch):
    monkeypatch.setattr(lodging_itinerary.db, "connect", lambda: "CONN")
    monkeypatch.setattr(lodging_itinerary.db, "lodging_by_id", lambda conn, lid: _lodging())
    places = [_place(1, name="평범한 공원"), _place(2, name="해운대해수욕장"), _place(3, name="또다른 공원")]
    monkeypatch.setattr(lodging_itinerary.db, "places_near_lodging", lambda conn, lat, lng, limit=30: places)
    message, recommended = lodging_itinerary.build(
        {"lodgingId": 1, "durationDays": 1, "preferences": ["해운대"]})
    assert recommended[0]["placeId"] == 2


def test_api_returns_itinerary(monkeypatch):
    monkeypatch.setattr(main, "build_lodging_itinerary", lambda conditions: (
        "일정 메시지", [{"dayNo": 1, "seq": 1, "placeId": 1, "name": "장소", "address": "부산"}]))
    response = TestClient(main.app).post("/internal/lodging-itinerary", json={"lodgingId": 1, "durationDays": 1})
    assert response.status_code == 200
    data = response.json()
    assert data["message"] == "일정 메시지" and data["recommendedPlaces"][0]["placeId"] == 1


def test_api_rejects_invalid_lodging_id():
    response = TestClient(main.app).post("/internal/lodging-itinerary", json={"lodgingId": 0})
    assert response.status_code == 422


def test_api_error_hides_internal_detail(monkeypatch):
    def fail(conditions):
        raise RuntimeError("secret db detail")
    monkeypatch.setattr(main, "build_lodging_itinerary", fail)
    response = TestClient(main.app).post("/internal/lodging-itinerary", json={"lodgingId": 1})
    assert response.status_code == 503
    assert "secret" not in response.text
