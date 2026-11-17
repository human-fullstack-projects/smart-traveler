import pytest
from app import course_rag as rag
from app.course_preferences import effective_preferences, preference_score, satisfies_exclusions
from app.graph import CHECK, CLARIFY, build_graph
from app.schemas import ExtractedConditions, DialogueRequest


def rows(course_id=1, duration=1, **metadata):
    info = dict(courseId=course_id, title=f"저장 코스 {course_id}", destination="부산", theme="문화",
                companion="FRIENDS", transport="CAR", pace="normal", environment="any", landscapes=[])
    info.update(metadata)
    return [dict(info, dayNo=day, dayTheme=f"테마 {day}", seq=visit, placeId=course_id * 100 + day * 10 + visit,
                 placeName=f"DB 장소 {course_id}-{day}-{visit}")
            for day in range(1, duration + 1) for visit in range(1, 5)]


def conditions(duration=1, preferences=None):
    return dict(regionId=1, regionName="부산", startDate="2026-12-10", durationDays=duration,
                adults=2, children=0, companionType="FRIENDS", transportType="CAR",
                preferences=preferences or [])


@pytest.fixture
def offline(monkeypatch):
    monkeypatch.setattr(rag, "document_vectors", lambda model, texts: [[1., 0.]] * len(texts))
    class Embedder:
        def __init__(self, **kwargs): pass
        def embed_query(self, query): return [1., 0.]
    class BrokenReason:
        def __init__(self, **kwargs): raise RuntimeError("offline")
    monkeypatch.setattr(rag, "OpenAIEmbeddings", Embedder)
    monkeypatch.setattr(rag, "ChatOpenAI", BrokenReason)


def test_saved_preferences_are_optional_and_never_accept_member_id():
    assert DialogueRequest(message="네", conditions={}).savedPreferences is None
    with pytest.raises(ValueError):
        DialogueRequest(message="네", conditions={}, savedPreferences={"memberId": 7})


def test_conversation_overrides_saved_and_both_disables_environment():
    saved = dict(pace="relaxed", environment="outdoor", landscapes=["sea"])
    effective, _, _ = effective_preferences(conditions(preferences=["실내 위주", "알찬 일정", "도시"]), saved)
    assert effective == dict(pace="packed", environment="indoor", landscapes=["city"])
    effective, _, _ = effective_preferences(conditions(preferences=["장소는 둘 다 좋아요"]), saved)
    assert "environment" not in effective and effective["pace"] == "relaxed"


def test_explicit_exclusions_reject_unknown_candidates():
    effective, excluded, unknown = effective_preferences(conditions(preferences=["야외 제외", "바다 제외"]),
                                                        dict(environment="outdoor", landscapes=["sea"]))
    assert effective == {} and not unknown
    assert not satisfies_exclusions(dict(environment="any", landscapes=["city"]), excluded)
    assert not satisfies_exclusions(dict(environment="indoor", landscapes=[]), excluded)
    assert not satisfies_exclusions(dict(environment="indoor", landscapes=["sea"]), excluded)
    assert satisfies_exclusions(dict(environment="indoor", landscapes=["city"]), excluded)


@pytest.mark.parametrize("text", ["야외 제외 실내 위주", "야외 말고 실내", "야외는 제외하고 실내 위주"])
def test_mixed_positive_and_negative_preferences(text):
    effective, excluded, unknown = effective_preferences(conditions(preferences=[text]), dict(environment="outdoor"))
    assert effective["environment"] == "indoor" and excluded["environment"] == {"outdoor"} and not unknown


def test_indoor_outdoor_any_disables_saved_environment():
    effective, _, _ = effective_preferences(conditions(preferences=["실내외 둘 다"]), dict(environment="indoor"))
    assert "environment" not in effective


def test_unknown_exclusion_never_claims_satisfaction(offline):
    output = rag.recommend_course(dict(merged=conditions(preferences=["카페 제외"]), courseCandidates=rows()))
    assert "확인하기 어려워요" in output and "코스 #" not in output


def test_unknown_values_are_neutral_and_not_matching_evidence():
    score, matched = preference_score(dict(pace="unknown", environment="any", landscapes=[]), .5,
                                       dict(pace="normal", environment="indoor", landscapes=["sea"]))
    assert score == pytest.approx(.5) and matched == []
    assert preference_score(dict(pace="packed"), .8, {}) == (.8, [])


def test_account_preferences_change_ranking(offline):
    candidates = rows(1, environment="indoor", pace="packed", landscapes=["city"])
    candidates += rows(2, environment="outdoor", pace="relaxed", landscapes=["sea"])
    candidates += rows(3, environment="any", pace="normal")
    for saved, expected in ((dict(pace="packed", environment="indoor", landscapes=["city"]), 1),
                            (dict(pace="relaxed", environment="outdoor", landscapes=["sea"]), 2)):
        answer = rag.recommend_course(dict(merged=conditions(), courseCandidates=candidates, savedPreferences=saved))
        assert answer.startswith(f"추천 1 · 코스 #{expected}\n")
        assert answer.count("· 코스 #") == 3
        assert "2026-12-10" in answer


def test_without_structured_preferences_keeps_one_result(offline):
    answer = rag.recommend_course(dict(merged=conditions(preferences=["사진 찍기"]), courseCandidates=rows(2) + rows(1)))
    assert answer.startswith("추천 1 · 코스 #1\n") and answer.count("· 코스 #") == 1


def test_relevance_top_twenty_and_stable_ties():
    courses = [rag.group_courses(rows(i, environment="indoor" if i == 21 else "outdoor"), 1)[0]
               for i in range(1, 22)]
    effective, excluded, _ = effective_preferences(conditions(preferences=["실내"]))
    ranked = rag.rank_courses(courses, [1 - i / 1000 for i in range(21)], effective, excluded)
    assert [x[0]["info"]["courseId"] for x in ranked] == [1, 2, 3]
    ranked = rag.rank_courses(courses[:3][::-1], [.5, .5, .5], effective, excluded)
    assert [x[0]["info"]["courseId"] for x in ranked] == [1, 2, 3]


@pytest.mark.parametrize("count", [0, 1, 2])
def test_small_candidate_counts(offline, count):
    candidates = sum((rows(i) for i in range(1, count + 1)), [])
    answer = rag.recommend_course(dict(merged=conditions(), courseCandidates=candidates,
                                     savedPreferences=dict(pace="normal")))
    assert answer.count("· 코스 #") == count
    if not count: assert "코스가 없어요" in answer


def test_missing_days_inactive_places_and_bad_sequences_rejected():
    assert rag.group_courses(rows(duration=2)[:4], 2) == []
    invalid = rows()
    invalid[1]["placeId"] = None
    assert rag.group_courses(invalid, 1) == []
    invalid = rows()
    invalid[1]["seq"] = 1
    assert rag.group_courses(invalid, 1) == []
    assert rag.group_courses(rows(duration=2)[::-1], 2)[0]["days"][1]["places"][0] == "DB 장소 1-1-1"


def test_four_day_three_courses_fit_response_contract_and_followup(offline):
    candidates = sum((rows(i, duration=4, title="코" * 200) for i in range(1, 4)), [])
    for row in candidates: row["placeName"] = "장" * 200
    answer = rag.recommend_course(dict(merged=conditions(4), courseCandidates=candidates,
                                     savedPreferences=dict(pace="normal")))
    assert 3000 < len(answer) <= 20000
    DialogueRequest(message="다시 추천", previousQuestion=answer, conditions=conditions(4))
    assert answer.count("4일차 (2026-12-13)") == 3


def empty_extraction(**updates):
    values = dict(regionName=None, startDate=None, durationDays=None, adults=None, children=None,
                  companionType=None, transportType=None, preferences=None)
    return ExtractedConditions(**(values | updates))


@pytest.mark.parametrize("previous", [CHECK, CLARIFY])
@pytest.mark.parametrize("message", ["네", "확인했어", "추천해줘"])
def test_course_confirmation_uses_same_candidates(offline, previous, message):
    graph = build_graph(lambda state: empty_extraction())
    result = graph.invoke(dict(message=message, previousQuestion=previous, conditions=conditions(), courseCandidates=rows()))
    assert "코스 #1" in result["question"] and result["confirmed"]


def test_changed_region_or_duration_requires_reconfirmation(offline):
    for changes in (dict(regionName="제주"), dict(durationDays=2)):
        graph = build_graph(lambda state: empty_extraction(**changes))
        result = graph.invoke(dict(message="바꿔서 추천해줘", previousQuestion=CHECK,
                                   conditions=conditions(), courseCandidates=rows()))
        assert result["question"] == CHECK and not result.get("confirmed")
