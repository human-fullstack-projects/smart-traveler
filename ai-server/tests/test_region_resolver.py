"""research/chat_test.py의 resolve_destination을 그대로 포팅한 region_resolver 단위 테스트."""
from app import region_resolver


class FakeCollection:
    def __init__(self, destinations):
        self._metadatas = [{"destination": d} for d in destinations]

    def get(self, include=None):
        return {"metadatas": self._metadatas}


def test_known_destinations_dedupes_and_sorts():
    collection = FakeCollection(["부산광역시 해운대구", "부산광역시 금정구", "부산광역시 해운대구", "제주특별자치도 제주시"])
    assert region_resolver.known_destinations(collection) == sorted(
        {"부산광역시 해운대구", "부산광역시 금정구", "제주특별자치도 제주시"})


def test_resolve_destination_without_name_returns_none():
    assert region_resolver.resolve_destination(None, destinations=["부산광역시 해운대구"]) is None
    assert region_resolver.resolve_destination("", destinations=["부산광역시 해운대구"]) is None


def test_resolve_destination_no_match_returns_none():
    assert region_resolver.resolve_destination("아틀란티스", destinations=["부산광역시 해운대구"]) is None


def test_resolve_destination_single_match_returns_string():
    destinations = ["부산광역시 해운대구", "제주특별자치도 제주시"]
    assert region_resolver.resolve_destination("해운대", destinations=destinations) == "부산광역시 해운대구"


def test_resolve_destination_sido_rollup_returns_in_list():
    destinations = ["부산광역시 해운대구", "부산광역시 금정구", "제주특별자치도 제주시"]
    result = region_resolver.resolve_destination("부산", destinations=destinations)
    assert result == {"$in": ["부산광역시 해운대구", "부산광역시 금정구"]}


def test_resolve_destination_prefers_detailed_over_bare_match():
    # "제주"라는 저정보 destination 자체와 구체적인 destination이 둘 다 매칭되면 구체적인 쪽을 우선한다.
    destinations = ["제주", "제주특별자치도 제주시", "제주특별자치도 서귀포시"]
    result = region_resolver.resolve_destination("제주", destinations=destinations)
    assert result == {"$in": ["제주특별자치도 제주시", "제주특별자치도 서귀포시"]}


def test_resolve_destination_fetches_known_destinations_when_not_supplied():
    collection = FakeCollection(["부산광역시 해운대구"])
    assert region_resolver.resolve_destination("해운대", collection=collection) == "부산광역시 해운대구"
