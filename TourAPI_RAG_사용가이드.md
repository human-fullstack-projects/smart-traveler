# TourAPI RAG — 지금까지 만든 것 사용 가이드

작성일: 2026-11-10 · 업데이트: 2026-11-11 (Phase 3~4 반영: MariaDB → ChromaDB 적재, 검색 API, 코스 추천 API + LangGraph 코스 그래프 추가)
대상: `lsj1110back` → `main`에 머지된 TourAPI RAG Phase 0~2 (데이터 수집 + Chroma 임베딩) + `RAG_다음단계_Phase3-4_가이드_3.md`로 구현한 Phase 3~4
범위: 장소를 모으고 의미 검색이 되게 만든 것(Phase 0~2)에 더해, 이제 **MariaDB에서 직접 Chroma로 장소를 적재**하고, **검색 API**와 **LangGraph 코스 그래프로 코스를 추천하는 API**까지 쓸 수 있다. 다만 Spring의 최종 사용자용 코스 추천 API(`/api/courses/recommendations`)는 아직 이 기능에 연결되지 않아 503을 반환한다(5장 참고) — 지금은 ai-server 내부 API로만 확인할 수 있다.

---

## 0. 준비물 (키 2개)

둘 다 저장소에 커밋되지 않으니, 이 서버를 새로 띄우는 사람마다 한 번씩 등록해야 한다.

| 키 | 저장 위치 | 비고 |
| --- | --- | --- |
| TourAPI 서비스키 | `%USERPROFILE%\.tourapi_key.txt` (파일 첫 줄) 또는 환경변수 `TOUR_API_KEY` | data.go.kr에서 받은 키가 "Encoding"(퍼센트 인코딩된) 형태면 **디코딩한 값**을 저장해야 한다. 인코딩된 값을 그대로 넣으면 `TourApiClient`가 다시 인코딩해서 깨진다. BOM 없는 UTF-8(또는 ASCII)로 저장할 것 — PowerShell `Set-Content -Encoding utf8`은 BOM을 붙이므로 피한다. |
| OpenAI API 키 | `ai-server/.env`의 `OPENAI_API_KEY=sk-...` | `ai-server/app/main.py`가 `python-dotenv`로 이 파일을 실행 위치와 무관하게 읽는다. 키가 없어도 서버 자체(`/health` 등)는 정상 기동하고, 임베딩이 필요한 요청만 503으로 실패한다. |
| MariaDB 접속 정보 (2-1에서 쓰는 적재 기능용) | `ai-server/.env`의 `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USERNAME`/`DB_PASSWORD` | Spring과 같은 MariaDB를 **읽기만** 한다. Spring과 같은 계정 또는 SELECT 권한만 있는 계정을 쓴다. 이 값이 없어도 Phase 0~2(동기화·검색)는 그대로 동작하고, MariaDB → Chroma 적재(2-1)만 503으로 실패한다. |

확인 명령 (PowerShell):
```powershell
Get-Content "$env:USERPROFILE\.tourapi_key.txt"   # TourAPI 키가 평문으로 보이면 정상
Get-Content "D:\sjtool\workspace\team-project3\travle\smart-traveler\ai-server\.env"  # OPENAI_API_KEY=..., DB_*=... 확인
```

---

## 1. 서버 두 개 띄우기

### 1-1. Spring (smarttraveler)

MariaDB(`smart_travel_db`)가 떠 있어야 한다. 그다음:

```powershell
cd D:\sjtool\workspace\team-project3\travle\smart-traveler\smarttraveler
.\gradlew.bat bootRun
```

### 1-2. ai-server (FastAPI, 임베딩 담당)

```powershell
cd D:\sjtool\workspace\team-project3\travle\smart-traveler\ai-server
# 최초 1회: pip install -r requirements.txt (chromadb, python-dotenv 포함)
python -m uvicorn app.main:app --port 8001
```

Spring은 기본값으로 `http://127.0.0.1:8001`을 ai-server 주소로 쓴다(`application.properties`의 `AI_SERVICE_URL`, 기본값 `http://127.0.0.1:8001`). 포트를 바꾸려면 Spring 쪽 `AI_SERVICE_URL` 환경변수도 같이 바꿔야 한다.

기동 확인:
```powershell
curl http://127.0.0.1:8001/health
# {"status":"ok"} 가 나오면 됨 (OPENAI_API_KEY 없어도 이건 통과함)
```

---

## 2. 지역 데이터 수집 (관리자 동기화)

관리자 계정으로 로그인한 상태에서, 아래 API를 호출하면 그 지역의 관광지·문화시설·레포츠·쇼핑·음식점을 KorService2에서 가져와 DB에 저장하고, 끝나면 자동으로 ai-server에 임베딩까지 보낸다.

```
POST /api/admin/tour-data/sync
Content-Type: application/json

{ "region": "26350" }   // 5자리 시군구 코드. 26350 = 부산 해운대구 (이번에 실제로 검증한 지역)
```

- `region` 코드는 DB `region` 테이블에서 `region_level='SIGUNGU' AND is_supported=TRUE`인 행만 쓸 수 있다. 관리자 화면의 "관광지 찾아보기"에서 지원 지역 목록을 볼 수 있다(`GET /api/admin/tour-data` 류의 화면, `TourSyncService.supportedRegions()`).
- 응답(`SyncResult`)의 `steps` 배열에 각 단계 결과가 나온다. 마지막 `AI_SERVER/INDEX` 단계가 `SUCCESS`면 임베딩까지 끝난 것이다. `FAILED`면 ai-server가 안 떠 있거나 `OPENAI_API_KEY`가 없는 경우다 — **동기화 자체는 실패하지 않고, 다음에 다시 동기화하면 그때 인덱싱을 다시 시도한다.**
- 호출 한도(보통 하루 1,000건) 때문에 지역 하나당 상당량의 TourAPI 호출이 나간다(해운대구 기준 약 300~400건 수준). 자주 돌리지 말 것.

### 결과를 SQL로 바로 확인하고 싶을 때

```sql
SELECT place_type, COUNT(*), SUM(description IS NOT NULL) AS with_desc
FROM place WHERE region_id = (SELECT region_id FROM region WHERE region_code='26350')
  AND is_active=TRUE AND latitude IS NOT NULL
GROUP BY place_type;
```

---

## 2-1. (신규) MariaDB → ChromaDB 적재

1번 경로(관리자 동기화)는 TourAPI 동기화를 돌릴 때만 Chroma에 들어가고 TourAPI 호출 한도를 쓴다. 아래 경로는 **ai-server가 MariaDB를 직접 읽어서(TourAPI 호출 없이)** 같은 Chroma 컬렉션에 넣는다. `chroma_data`를 지웠거나 새 팀원 PC에서 복구할 때, 관리자가 DB에서 장소를 수정·비활성화했을 때, 또는 이전 인덱싱이 실패했을 때 쓴다.

```powershell
cd D:\sjtool\workspace\team-project3\travle\smart-traveler\ai-server
.\.venv\Scripts\python.exe -m scripts.sync_mariadb_to_chroma 26350
# 26350 {'read': 208, 'skipped': 0, 'embedded': 0, 'unchanged': 208, 'removed': 0}
# Chroma 전체 장소 수: 208
```

서버가 떠 있으면 API로도 된다.

```powershell
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8001/internal/places/sync -ContentType "application/json" -Body '{"sigunguCode":"26350"}'
```

결과 값 뜻: `read`(MariaDB에서 읽은 활성·좌표 보유 장소 수), `skipped`(주소·소개가 둘 다 없어 의미 검색에 못 쓰는 "이름만 있는 행", Chroma에 안 넣음), `embedded`(문서가 새로 생겼거나 바뀌어서 OpenAI로 다시 임베딩한 수 — 바뀐 게 없으면 두 번째 실행부터 0이어야 정상), `unchanged`(문서는 그대로라 메타데이터만 갱신한 수), `removed`(MariaDB에서 비활성화됐거나 더는 못 쓰게 된 장소라 Chroma에서 지운 수).

> 실행할 때 `python -m scripts...`처럼 가상환경이 아닌 전역 파이썬으로 돌리면 `ModuleNotFoundError: No module named 'dotenv'`가 난다. 꼭 `.\.venv\Scripts\python.exe`로 실행할 것.

---

## 3. 의미 검색 직접 해 보기 (파이썬 스크립트, 검색 API 대신 써도 됨)

아래는 `/internal/places/search` API(3-1) 대신, ai-server의 Chroma 컬렉션을 **파이썬 스크립트로 직접** 조회하는 방법이다. 빠르게 벡터나 메타데이터를 들여다볼 때 편하다. 아래 스크립트를 ai-server 가상환경에서 실행:

```python
# check_search.py — ai-server 디렉터리에서 실행
import sys, pathlib
sys.path.insert(0, ".")
from dotenv import load_dotenv
load_dotenv(pathlib.Path(".").resolve() / ".env")
from app.rag import places, get_embeddings

query = "전통시장 먹거리 구경"       # 원하는 문장으로 바꿔도 됨
place_type = "SHOPPING"             # ATTRACTION/CULTURE/LEISURE/SHOPPING/RESTAURANT

res = places.query(
    query_embeddings=[get_embeddings().embed_query(query)],
    n_results=5,
    where={"place_type": place_type},
)
for doc, meta, dist in zip(res["documents"][0], res["metadatas"][0], res["distances"][0]):
    print(doc.split("\n")[0], "| 거리:", round(dist, 3), "| 시장여부:", meta.get("is_market"))
```

```powershell
cd D:\sjtool\workspace\team-project3\travle\smart-traveler\ai-server
python check_search.py
```

`dist`는 코사인 거리라 **작을수록 더 비슷함**이다. 주의: 이 단계는 아직 **반경(거리) 필터가 없다** — `place_type`만 걸고 전국(또는 그 지역 데이터 전체)에서 찾으므로, 결과가 멀리 떨어진 장소일 수 있다. 특정 지역으로 좁히려면 `where`에 `"sigungu_code": "26350"`를 같이 넣으면 된다:

```python
res = places.query(
    query_embeddings=[get_embeddings().embed_query(query)],
    n_results=5,
    where={"$and": [{"place_type": place_type}, {"sigungu_code": "26350"}]},
)
```

---

## 3-1. (신규) 검색 API로 확인하기

`/internal/places/search`는 앵커 1곳(이름을 말하면 그 장소, 아니면 질의 임베딩 + 인기도로 자동 선택)과 그 반경 안의 슬롯별(MEAL/FUN/MARKET) 후보 Top-5를 돌려준다. 거리(`km`)는 Chroma의 메타데이터 필터(위경도 사각형)로 먼저 좁히고 파이썬에서 정확히 계산한다.

```powershell
$body = @{ sigunguCode = "26350"; anchorName = "해운대"; transportType = "PUBLIC_TRANSPORT" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8001/internal/places/search `
  -ContentType "application/json; charset=utf-8" -Body ([Text.Encoding]::UTF8.GetBytes($body)) | ConvertTo-Json -Depth 5
```

`anchorName`을 빼면 그 지역 관광지 중 임베딩 유사도 + 인기도로 앵커를 자동으로 고른다. `transportType`을 `CAR`로 주면 반경이 3km → 10km로 넓어진다.

---

## 3-2. (신규) 코스 추천 API + LangGraph 코스 그래프

`/internal/courses/recommend`는 검색(3-1과 같은 로직) → 조합 → OpenAI 응답 생성 → 검증을 LangGraph `StateGraph`로 묶어서 코스 하나를 돌려준다.

```
retrieve(검색) → assemble(점수 1등 조합 + 시간 계산, OpenAI 안 씀) → generate(OpenAI로 제목·요약·소개 문장) → validate(장소가 안 바뀌었는지 확인) → 응답
```

**중요**: 장소·순서·시간은 전부 Chroma 검색 점수와 코드(`assemble`)가 정한다. OpenAI(`generate`)는 그 결과를 읽기 좋은 문장으로 다듬기만 하고, 실패하거나 엉뚱한 장소를 말해도 코스는 똑같이 나오고 문장만 초안으로 바뀐다(`polished: false`).

```powershell
$body = @{ sigunguCode = "26350"; anchorName = "해운대"; transportType = "PUBLIC_TRANSPORT"; startTime = "10:00" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8001/internal/courses/recommend `
  -ContentType "application/json; charset=utf-8" -Body ([Text.Encoding]::UTF8.GetBytes($body)) | ConvertTo-Json -Depth 5
```

응답 예시:

```json
{
  "title": "해운대해수욕장 주변 코스",
  "summary": "해운대해수욕장 → 싱싱뽈락회 해운대본점 → 청사포 기찻길 → 해운대시장",
  "stops": [
    { "slot": "ANCHOR", "placeId": 306, "name": "해운대해수욕장", "moveMinutes": 0, "startTime": "10:00", "endTime": "11:30", "description": "..." },
    { "slot": "MEAL", "placeId": 475, "name": "싱싱뽈락회 해운대본점", "moveMinutes": 5, "startTime": "11:35", "endTime": "12:35", "description": "..." }
  ],
  "polished": true,
  "routeSource": "ESTIMATED"
}
```

| 상황 | 응답 |
| --- | --- |
| 정상 | 200 + 위 JSON |
| 그 지역에 앵커 후보가 없음 / 앵커 근처에 식당이 없음 | 422 + 이유 문구 |
| OpenAI 임베딩 키 없음, Chroma 오류 | 503 |
| OpenAI 응답 생성만 실패 | 200 + `polished: false` (초안 문장, 장소·시간은 같음) |

자세한 설계(슬롯 템플릿, 점수 계산, LangGraph 노드)는 루트의 `RAG_다음단계_Phase3-4_가이드_3.md` 4~5장을 참고한다.

---

## 4. 전체 컬렉션 상태 확인

```python
from app.rag import places
print(places.count())  # DB의 활성·좌표 보유 장소 수와 같아야 정상
```

숫자가 DB와 안 맞으면(예: 동기화는 됐는데 인덱싱이 안 됨) 2번의 `AI_SERVER/INDEX` 단계 로그(`data_sync_log` 테이블, `provider='AI_SERVER'`)를 확인한다.

---

## 5. 지금 할 수 없는 것 (아직 안 만듦)

- ai-server 내부에는 "해운대 근처 코스 짜줘"에 해당하는 **코스 추천 API**(`/internal/courses/recommend`, 3-2)가 **생겼다.** 하지만 Spring의 최종 사용자용 `RecommendationService`/`/api/courses/recommendations`는 아직 이 API에 연결되지 않아 여전히 503을 반환한다 — Spring 쪽 연결(Phase 5)이 남은 일이다.
- 휴무일 제외, 여러 날 코스(전날 쓴 장소 제외), 채팅에서 바로 RAG 코스 추천받기도 아직 안 붙었다.
- 지금 할 수 있는 건 "장소 데이터를 모으고(MariaDB 직접 적재 포함), 검색 API로 후보를 찾고, 코스 추천 API로 ai-server 안에서 코스를 만들어 보는 것"까지다.

## 6. 문제가 생기면

| 증상 | 원인 | 확인할 곳 |
| --- | --- | --- |
| `AI_SERVER/INDEX`가 계속 FAILED | ai-server 안 떠 있음 / `OPENAI_API_KEY` 없음 / `AI_SERVICE_URL` 포트 불일치 | ai-server 로그, `.env`, Spring `application.properties` |
| TourAPI 동기화가 중간에 FAILED | 하루 호출 한도 초과 또는 서비스키 문제 | `data_sync_log`의 `error_message` |
| 검색 결과가 이상하게 먼 지역 섞임 | `where`에 `sigungu_code`를 안 넣음 (3번 직접 스크립트 한정, `/internal/places/search`는 항상 적용됨) | 3번처럼 `sigungu_code`를 `where`에 추가 |
| `places.count()`가 DB보다 적음 | 일부 배치 인덱싱 실패 | 동기화를 다시 돌리거나 2-1 적재 스크립트 실행 (증분으로 재시도됨) |
| `scripts.sync_mariadb_to_chroma` 실행 시 `ModuleNotFoundError: No module named 'dotenv'` | 가상환경이 아닌 전역 파이썬으로 실행함 | `.\.venv\Scripts\python.exe -m scripts.sync_mariadb_to_chroma ...`로 실행 |
| `/internal/places/sync` 또는 적재 스크립트가 503 | `.env`의 `DB_*` 접속 정보 없음/오류, 또는 `OPENAI_API_KEY` 없음 | `.env`, MariaDB 접속 가능 여부 |
| `/internal/courses/recommend`가 422 | 그 지역에 앵커가 될 관광지가 없음 또는 앵커 근처에 식당이 없음 | 해당 지역에 관리자 동기화(2장) 또는 적재(2-1)를 먼저 돌렸는지 확인 |
