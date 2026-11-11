from typing import Any, Callable, TypedDict
from langgraph.graph import END, START, StateGraph
from .compose import apply_polish, build_draft, polish_with_openai, to_response
from .schemas import CourseComposeRequest, PolishedCourse
from .search import search_candidates

NO_ANCHOR = "이 지역에서 코스를 시작할 관광지를 찾지 못했어요. 관리자 동기화로 장소 데이터를 먼저 채워 주세요."
NO_MEAL = "시작 장소 근처에서 식당을 찾지 못해 코스를 만들 수 없어요."


class CourseState(TypedDict, total=False):
    request: dict[str, Any]          # CourseRecommendRequest.model_dump()
    anchor: dict[str, Any] | None
    candidates: dict[str, list]
    draft: dict[str, Any]
    polished: PolishedCourse | None
    result: dict[str, Any]


def build_course_graph(polisher: Callable[[dict], PolishedCourse] = polish_with_openai):
    """검색(RAG) → 조합 → 응답 생성(OpenAI) → 검증 순서로 코스를 만든다.
    polisher를 바꿔 끼우면 OpenAI 없이 테스트할 수 있다 (graph.py의 build_graph와 같은 방식)."""

    def retrieve(state: CourseState):
        # 질의 임베딩(OpenAI) + Chroma 검색: 앵커 1곳과 슬롯별 후보 Top-5
        r = state["request"]
        found = search_candidates(r["sigunguCode"], r.get("anchorName"), r["transportType"],
                                  r["slots"], r.get("queries", {}), r.get("excludePlaceIds", []))
        return {"anchor": found["anchor"], "candidates": found["candidates"]}

    def route(state: CourseState):
        if not state.get("anchor"):
            return "no_anchor"
        if not state["candidates"].get("MEAL"):
            return "no_meal"
        return "assemble"

    def assemble(state: CourseState):
        # 점수 1등 선택 + 순서 + 시간 + 초안 문장 (OpenAI 안 씀)
        r = state["request"]
        compose_request = CourseComposeRequest(anchor=state["anchor"], candidates=state["candidates"],
                                               transportType=r["transportType"], conditions=r.get("conditions", {}),
                                               startTime=r.get("startTime", "10:00"))
        return {"draft": build_draft(compose_request)}

    def generate(state: CourseState):
        # 검색된 장소 문서를 근거로 OpenAI가 제목·요약·소개 문장을 만든다. 실패해도 다음 단계로 간다.
        try:
            return {"polished": polisher(state["draft"])}
        except Exception:
            return {"polished": None}

    def validate(state: CourseState):
        # 장소가 바뀌지 않았는지 확인하고 최종 응답을 만든다.
        draft = state["draft"]
        is_polished = apply_polish(draft, state.get("polished"))
        return {"result": to_response(draft, is_polished)}

    def no_anchor(state: CourseState):
        return {"result": {"error": NO_ANCHOR}}

    def no_meal(state: CourseState):
        return {"result": {"error": NO_MEAL}}

    graph = StateGraph(CourseState)
    graph.add_node("retrieve", retrieve)
    graph.add_node("assemble", assemble)
    graph.add_node("generate", generate)
    graph.add_node("validate", validate)
    graph.add_node("no_anchor", no_anchor)
    graph.add_node("no_meal", no_meal)
    graph.add_edge(START, "retrieve")
    graph.add_conditional_edges("retrieve", route, {"assemble": "assemble", "no_anchor": "no_anchor", "no_meal": "no_meal"})
    graph.add_edge("assemble", "generate")
    graph.add_edge("generate", "validate")
    graph.add_edge("validate", END)
    graph.add_edge("no_anchor", END)
    graph.add_edge("no_meal", END)
    return graph.compile()
