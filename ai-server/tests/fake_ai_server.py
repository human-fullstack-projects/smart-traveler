from app import main
from app.graph import build_graph
from app.schemas import ExtractedConditions


def fake(state):
    text = state["message"]
    region = "부산" if "부산" in text else None
    start = "2026-12-10" if "2026-12-10" in text else None
    return ExtractedConditions(regionName=region, startDate=start, durationDays=3 if "2박 3일" in text else 4 if "3박 4일" in text else None,
        adults=2 if text == "2명" and "성인" in state["previousQuestion"] else None, children=0 if text == "0명" and "아동" in state["previousQuestion"] else None, companionType="FRIENDS" if "친구" in text else None, transportType="CAR" if text == "자동차" else None, preferences=None)

main.graph = build_graph(fake)
app = main.app
