from app import vector_store


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
    """Chroma collection.query()의 where 평가만 모방한 테스트 더블. 거리는 입력 순서로 매긴다."""
    def __init__(self, items):
        self.items = items  # list[(course_id, metadata)]

    def query(self, query_embeddings, n_results, where=None):
        matched = [item for item in self.items if _matches(item[1], where)][:n_results]
        ids = [str(course_id) for course_id, _ in matched]
        distances = [round(0.1 * i, 3) for i in range(len(matched))]
        return {
            "ids": [ids],
            "documents": [[""] * len(matched)],
            "metadatas": [[meta for _, meta in matched]],
            "distances": [distances],
        }


def meta(course_id, **overrides):
    base = {"course_id": course_id, "destination": "부산광역시 해운대구", "duration_days": 2,
             "companion_type": "FAMILY", "transport_type": "CAR", "purpose_theme": "맛집"}
    base.update(overrides)
    return base


def test_matches_without_relaxation_when_everything_fits():
    collection = FakeCollection([(1, meta(1)), (2, meta(2, transport_type="PUBLIC_TRANSPORT"))])
    slots = {"destination": "부산광역시 해운대구", "duration_days": 2, "companion_type": "FAMILY", "transport_type": "CAR"}
    result, trace = vector_store.search([0.0], slots, collection=collection)
    assert result["ids"][0] == ["1"]
    assert len(trace) == 1 and trace[0]["dropped"] == []


def test_destination_can_be_a_multi_city_in_list():
    collection = FakeCollection([
        (1, meta(1, destination="부산광역시 해운대구")),
        (2, meta(2, destination="부산광역시 금정구")),
        (3, meta(3, destination="제주특별자치도 제주시")),
    ])
    slots = {"destination": {"$in": ["부산광역시 해운대구", "부산광역시 금정구"]}}
    result, _trace = vector_store.search([0.0], slots, collection=collection)
    assert sorted(result["ids"][0]) == ["1", "2"]


def test_relaxes_transport_then_companion_when_region_identity_present():
    collection = FakeCollection([(1, meta(1, transport_type="PUBLIC_TRANSPORT", companion_type="SOLO"))])
    slots = {"destination": "부산광역시 해운대구", "duration_days": 2, "companion_type": "FAMILY", "transport_type": "CAR"}
    result, trace = vector_store.search([0.0], slots, collection=collection)
    assert result["ids"][0] == ["1"]
    dropped_steps = [t["dropped"] for t in trace]
    assert dropped_steps[-1] == ["companion_type", "transport_type"]


def test_destination_mismatch_is_never_relaxed():
    collection = FakeCollection([(1, meta(1, destination="제주특별자치도 제주시", transport_type="PUBLIC_TRANSPORT"))])
    slots = {"destination": "부산광역시 해운대구", "transport_type": "CAR"}  # course 1의 지역이 다름
    result, trace = vector_store.search([0.0], slots, collection=collection)
    assert result["ids"][0] == []
    assert len(trace) == len(vector_store.RELAX_ORDER) + 1  # 끝까지 완화해도 지역이 달라 매칭 없음


def test_excluded_course_ids_filtered_even_after_relaxation():
    collection = FakeCollection([(1, meta(1, transport_type="PUBLIC_TRANSPORT"))])
    slots = {"destination": "부산광역시 해운대구", "transport_type": "CAR"}
    result, _trace = vector_store.search([0.0], slots, excluded_course_ids=[1], collection=collection)
    assert result["ids"][0] == []


def test_pure_free_text_without_region_identity_does_not_relax():
    collection = FakeCollection([(1, meta(1, transport_type="PUBLIC_TRANSPORT"))])
    slots = {"transport_type": "CAR"}  # destination 없음 -> 지역 식별 조건 없는 자유서술 질의
    result, trace = vector_store.search([0.0], slots, collection=collection)
    assert result["ids"][0] == [] and len(trace) == 1
