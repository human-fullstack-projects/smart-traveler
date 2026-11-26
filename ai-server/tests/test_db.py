import json

from app import db


class FakeCursor:
    def __init__(self, row):
        self._row = row

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        return False

    def execute(self, sql, params=None):
        self.params = params

    def fetchone(self):
        return self._row


class FakeConn:
    def __init__(self, row):
        self._cursor = FakeCursor(row)

    def cursor(self):
        return self._cursor


def test_fetch_course_detail_returns_none_when_missing():
    assert db.fetch_course_detail(FakeConn(None), 999) is None


def test_fetch_course_detail_parses_itinerary_from_condition_meta():
    row = {
        "destination": "부산광역시 해운대구", "start_date": "2026-12-10", "end_date": "2026-12-11",
        "adults": 2, "children": 0, "companion_type": "FRIENDS", "transport_type": "CAR",
        "condition_meta": json.dumps({"itinerary": [
            {"day_no": 1, "visits": [{"place_name": "해운대해수욕장"}, {"place_name": "동백섬"}]},
        ]}),
    }
    detail = db.fetch_course_detail(FakeConn(row), 1356)
    assert detail["destination"] == "부산광역시 해운대구"
    assert detail["itinerary"][0]["visits"][0]["place_name"] == "해운대해수욕장"


def test_fetch_course_detail_tolerates_missing_or_broken_condition_meta():
    row = {"destination": "부산", "start_date": "2026-12-10", "end_date": "2026-12-10",
           "adults": 1, "children": 0, "companion_type": "", "transport_type": "", "condition_meta": None}
    assert db.fetch_course_detail(FakeConn(row), 1)["itinerary"] == []

    row["condition_meta"] = "not json"
    assert db.fetch_course_detail(FakeConn(row), 1)["itinerary"] == []
