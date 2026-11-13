# ai-server 변경 내역 (f93eb13 → HEAD)

- 비교 범위: `f93eb13` (2026-11-04, "사용방법 md파일제작") → `HEAD` (`9abb8b2`, main)
- 대상 경로: `ai-server/`
- 확인 명령: `git diff f93eb13 HEAD -- ai-server`, `git log f93eb13..HEAD -- ai-server`
- 규모: 파일 19개, **+1151 / -5줄**

## 1. 한눈에 보기

f93eb13 시점의 ai-server는 **대화 조건 추출(LangGraph 대화 그래프)과 TourAPI 지역·관광지 조회**까지만 하던 서버였다. README에도 "RAG와 날짜별 일정 생성은 아직 연결하지 않았다"고 적혀 있었다.

이후 두 커밋으로 **RAG 기반 코스 추천 파이프라인**이 추가되었다.

1. 장소 데이터를 OpenAI 임베딩(`text-embedding-3-small`)으로 **ChromaDB**에 저장 (Spring이 보내거나, ai-server가 MariaDB를 직접 읽어서)
2. 앵커(대표 관광지) 1곳 + 반경 안의 슬롯별(식사·즐길거리·시장) 후보를 **벡터 유사도 + 거리 + 인기도** 점수로 검색
3. **LangGraph 코스 그래프**(검색 → 조합 → OpenAI 문장 다듬기 → 검증)로 시간표가 붙은 코스를 생성
4. 여러 팀원이 지역을 나눠 동기화할 수 있는 **일괄 동기화 스크립트**와 지역 큐 파일 추가

> 단, Spring의 사용자용 코스 추천 API(`/api/courses/recommendations`)는 아직 이 기능에 연결되지 않았고 503을 반환한다. 현재는 ai-server 내부 API로만 확인할 수 있다(README 기준).

## 2. 커밋 목록

| 커밋 | 날짜 | 작성자 | 내용 | ai-server 변경 |
| --- | --- | --- | --- | --- |
| `ee23e24` | 2026-11-10 | qhtm102 | TourAPI RAG Phase 0~2: KorService2 다중 타입 수집 + Chroma 임베딩 연결 | 4개 파일, +120/-1 |
| `eed1ed4` | 2026-11-11 | qhtm102 | TourAPI RAG Phase 3~4: MariaDB→Chroma 적재, 검색/코스추천 API, LangGraph 코스 그래프 | 19개 파일, +1044/-17 |

- **Phase 0~2 (`ee23e24`)**: `rag.py` 신설(Chroma 컬렉션, 문서 변환, 인기도, upsert/삭제), `PlaceRow` 등 스키마, `/internal/places/index`·`/internal/places/remove` API, `chromadb` 의존성.
- **Phase 3~4 (`eed1ed4`)**: MariaDB 직접 읽기(`db.py`), 증분 적재, 검색(`search.py`), 코스 조합(`compose.py`), 코스 그래프(`course_graph.py`), 신규 API 3개, 스크립트, 테스트, README 갱신.

## 3. 파일별 변경

| 파일 | 구분 | 줄 수 | 요약 |
| --- | --- | --- | --- |
| `app/rag.py` | 신규 | +125 | Chroma 저장소·임베딩·MariaDB→Chroma 증분 적재 |
| `app/search.py` | 신규 | +101 | 앵커 찾기, 슬롯별 반경 검색과 점수 계산 |
| `app/compose.py` | 신규 | +133 | 코스 조합·시간표·초안 문장, OpenAI 문장 다듬기 |
| `app/course_graph.py` | 신규 | +79 | LangGraph 코스 그래프 |
| `app/db.py` | 신규 | +39 | MariaDB(읽기 전용) 접속과 장소 조회 SQL |
| `app/schemas.py` | 수정 | +80 | 장소·검색·코스 요청/응답 모델 |
| `app/main.py` | 수정 | +57/-3 | `.env` 로드, 신규 내부 API 5개 |
| `app/tour_api.py` | 수정 | +20/-1 | TourAPI 키 파일 fallback |
| `requirements.txt` | 수정 | +3 | `chromadb`, `python-dotenv`, `pymysql` |
| `README.md` | 수정 | +42/-1 | 적재·코스 추천 사용법 추가 |
| `scripts/sync_mariadb_to_chroma.py` | 신규 | +16 | 명령줄 적재 스크립트 |
| `scripts/sync_regions_batch.ps1` | 신규 | +150 | 지역 큐 기반 일괄 동기화(PowerShell) |
| `scripts/region_queue_member1~5.txt` | 신규 | 각 +29 | 팀원별 시군구 코드 목록 |
| `scripts/__init__.py` | 신규 | 0 | `python -m scripts...` 실행용 |
| `tests/test_search.py` | 신규 | +161 | 적재·검색·코스 그래프·API 테스트 |

## 4. 상세 내용

### 4-1. `app/rag.py` — 임베딩과 Chroma 적재 (신규)

- Chroma 저장 위치: `ai-server/chroma_data` (실행 위치와 무관하게 고정), 컬렉션 `places`, 거리 함수 **cosine**.
- 임베딩: `OpenAIEmbeddings(model="text-embedding-3-small")`. `OPENAI_API_KEY`가 없으면 import 시점이 아니라 **실제 호출 시점**에 실패하도록 지연 생성.
- `to_document()`: 임베딩할 텍스트. `[이름] 주소 / 분류 / 소개(600자)` 형식이며, 식당은 대표메뉴, 시장은 휴무일을 추가. 전화번호·URL은 넣지 않음.
- `popularity()`: KorService2에 조회수가 없어 대체 인기 신호(0~1)를 사용. 중심관광지 순위(`hubRank`)가 있으면 `1 - rank/100`, 없으면 이미지 유무 50% + 소개 길이 50%.
- `is_indexable()`: 주소와 소개가 모두 없는(이름만 있는) 장소는 제외.
- 메타데이터: `place_id, place_type, is_market, sigungu_code, lat, lng, popularity`.
- `index_places()` / `remove_places()`: upsert(같은 place_id 덮어쓰기)와 삭제.
- `sync_from_mariadb(sigungu_code)` — **증분 적재**
  1. MariaDB에서 해당 시군구의 활성·좌표 보유 장소를 읽음
  2. 문서가 새로 생기거나 바뀐 장소만 100개 단위로 임베딩 (OpenAI 비용 절약)
  3. 문서가 같은 장소는 메타데이터만 갱신
  4. MariaDB에서 비활성/제외된 장소는 Chroma에서 삭제
  5. 결과 `{"read", "skipped", "embedded", "unchanged", "removed"}` 반환
- `to_bool()`: MariaDB `BIT(1)` 칼럼을 pymysql이 `b'\x00'`/`b'\x01'`로 돌려주는 문제 처리 (`bool(b'\x00')`이 True가 되는 버그 방지).

### 4-2. `app/db.py` — MariaDB 읽기 (신규)

- `.env`의 `DB_HOST / DB_PORT / DB_NAME / DB_USERNAME / DB_PASSWORD`로 `smart_travel_db`에 접속. ai-server는 **읽기만** 하고 쓰기는 Spring 담당.
- `PLACES_SQL`: Spring `TourSyncService.placesForIndexing()`과 같은 조건·같은 칸. `place` + `region` + `restaurant_detail`(대표메뉴) 조인, 대표 이미지(`sort_order = 0`)와 최소 `hub_rank`를 서브쿼리로 가져옴. 조건은 `region_code` 일치, `is_active = TRUE`, 위경도 존재.

### 4-3. `app/search.py` — 후보 검색 (신규)

- 슬롯 템플릿

  | 슬롯 | 대상 | 기본 질의 |
  | --- | --- | --- |
  | ANCHOR | ATTRACTION, CULTURE | 대표 관광 명소 |
  | MEAL | RESTAURANT | 점심 식사하기 좋은 맛집 |
  | FUN | LEISURE, CULTURE, ATTRACTION | 체험하거나 즐길 거리 |
  | MARKET | `is_market = True`만 | 전통시장 먹거리 구경 |

- 반경: 대중교통 3km, 자동차 10km. 후보가 없으면 반경을 **1.5배로 한 번만** 넓힘.
- `find_anchor()`: 앵커 이름을 주면 문서 내 이름 검색이 우선(정확히 일치 > 그 단어로 시작 > 나머지, 동률은 인기도순). 없으면 질의 임베딩 검색 후 `0.6 × 유사도 + 0.4 × 인기도`.
- `search_slot()`: Chroma 메타데이터 필터(타입·시장 여부·위경도 사각형)로 `k × 4`개를 받은 뒤, 파이썬에서 haversine 거리로 반경 밖을 버리고 점수 계산
  - 점수 = `0.4 × 유사도 + 0.4 × 가까움(1 - km/반경) + 0.2 × 인기도`
  - 시장은 MARKET 슬롯에서만 사용, 앵커와 `excludePlaceIds`는 후보에서 제외
- `search_candidates()`: 앵커 1곳 + 슬롯별 Top-5와 `radiusKm` 반환.

### 4-4. `app/compose.py` — 코스 조합과 문장 다듬기 (신규)

**1단계 (OpenAI 사용 안 함)**
- `decide_slots()`: 후보 수로 코스 길이 결정. 시장 없음 → `MEAL, FUN` (FUN도 없으면 `MEAL`), 시장 있음 → FUN 후보가 2개 이상이면 `MEAL, FUN, MARKET`, 아니면 `MEAL, MARKET`.
- `choose_places()`: 슬롯마다 점수 1등 선택(중복 제외).
- 기본 체류 시간: 앵커 90분, 식사 60분, 즐길거리 90분, 시장 60분.
- 이동 시간: `직선거리 × 1.3 ÷ 속도`(대중교통 15km/h, 자동차 25km/h), 최소 5분.
- `schedule()`: 점심 시작이 11:30보다 이르면 앵커 체류 시간을 늘려 점심을 11:30 이후로 맞춤.
- `draft_text()`: OpenAI 없이 만드는 초안 문장(예: "식사 · 앞 장소에서 이동 7분. 소개…").

**2단계 (OpenAI)**
- `polish_with_openai()`: LangChain `ChatOpenAI`(기본 `gpt-4o-mini`, Responses API, `store=False`, structured output strict)로 **제목·요약·장소 소개 문장만** 다듬음. 장소 추가·삭제·순서 변경과 없는 사실(운영시간·가격·맛·거리) 작성을 금지.
- `apply_polish()`: 결과의 `placeId` 순서가 초안과 다르거나 빈 문장이 있으면 **다듬은 문장을 버리고 초안 유지**. 제목 20자, 요약 120자, 소개 80자로 자름.
- `to_response()`: 응답 JSON. `title, summary, stops[], polished, routeSource: "ESTIMATED"` (내부용 벡터·문서는 제외).

### 4-5. `app/course_graph.py` — LangGraph 코스 그래프 (신규)

```
START → retrieve ─┬─ (앵커 없음) → no_anchor → END
                  ├─ (식당 없음) → no_meal  → END
                  └─ assemble → generate → validate → END
```

- `retrieve`: 질의 임베딩 + Chroma 검색
- `assemble`: 점수 1등 선택, 순서, 시간표, 초안 문장
- `generate`: OpenAI 문장 다듬기. **예외가 나도 다음 단계로 진행**(`polished: None`)
- `validate`: 장소가 바뀌지 않았는지 확인하고 최종 응답 생성
- `polisher`를 주입받아 OpenAI 없이 테스트 가능

### 4-6. `app/schemas.py` (수정, +80)

- `PlaceRow`: 임베딩 대상 장소 1건 (Spring 전송 형식 = MariaDB 행 변환 형식)
- `PlaceIndexRequest`(최대 100건), `PlaceRemoveRequest`(최대 500건), `PlaceSyncRequest`(시군구 코드 5자리)
- `PlaceSearchRequest`: `sigunguCode, anchorName, transportType(PUBLIC_TRANSPORT|CAR), slots, queries, excludePlaceIds`
- `CourseRecommendRequest`: 검색 조건 + `startTime`(기본 10:00) + `conditions`
- `CourseComposeRequest`: 그래프 내부 조합 단계 입력
- `PolishedStop`, `PolishedCourse`: OpenAI structured output 스키마

### 4-7. `app/main.py` — 신규 API (수정)

- 시작 시 `ai-server/.env`를 실행 위치와 무관하게 로드(`python-dotenv`).
- 기존 대화 그래프와 별도로 `course_graph = build_course_graph()` 생성.

| 메서드·경로 | 기능 | 오류 |
| --- | --- | --- |
| `POST /internal/places/index` | Spring이 보낸 장소 임베딩·저장(최대 100개) | 503 |
| `POST /internal/places/remove` | 비활성 장소를 Chroma에서 삭제 | — |
| `POST /internal/places/sync` | MariaDB → Chroma 적재(시군구 단위) | 503 |
| `POST /internal/places/search` | 앵커 + 슬롯별 후보 Top-5 조회(확인용) | 503 |
| `POST /internal/courses/recommend` | 코스 그래프 실행 | 앵커·식당 없음 422, 기타 503 |

### 4-8. `app/tour_api.py` (수정)

- TourAPI 인증키 우선순위 변경: **생성자 인자 > 환경변수 `TOUR_API_KEY` > `%USERPROFILE%\.tourapi_key.txt` 첫 줄** (Spring `TourApiClient`와 같은 규칙).

### 4-9. 스크립트 (신규)

- `scripts/sync_mariadb_to_chroma.py`: `python -m scripts.sync_mariadb_to_chroma 26350 [코드...]` 형태로 여러 시군구 적재 후 Chroma 전체 장소 수 출력. 인자가 없으면 `26350`(부산 해운대구).
- `scripts/sync_regions_batch.ps1`: 큐 파일의 지역을 차례로 처리하는 일괄 동기화
  - 공유 MariaDB에서 해당 지역의 "주소나 소개가 있는 장소"가 **20개 이상이면 이미 완료로 보고 건너뜀** (팀원마다 로컬 Chroma가 달라도 중복 동기화 방지)
  - Spring 관리자 로그인(CSRF 포함) → `POST /api/admin/tour-data/sync` → 성공 시 로컬 Chroma 적재
  - TourAPI 일일 한도 등으로 단계가 실패하면 중단하고, 다음 실행에서 그 지역부터 재개 (작업 스케줄러로 하루 1회 실행 전제)
  - `.env`의 `SPRING_BASE_URL`(기본 `http://127.0.0.1:8080`), `ADMIN_LOGIN_ID`, `ADMIN_LOGIN_PW` 사용. TourAPI 키는 팀원 개인 키 사용 권장
  - 로그: `scripts/region_sync.log`
- `scripts/region_queue_member1~5.txt`: 팀원 5명에게 나눈 시군구 코드 목록(파일당 29줄, `코드  # 지역명` 형식).

### 4-10. 테스트 `tests/test_search.py` (신규)

메모리 Chroma(`EphemeralClient`)에 해운대 주변 가짜 장소 8곳과 3차원 가짜 임베딩을 넣어 OpenAI 없이 검증한다.

- MariaDB → Chroma 증분 적재(바뀐 것만 임베딩, 이름만 있는 장소 제외, 사라진 장소 삭제, 재실행 시 임베딩 0건)
- 이름으로 앵커 찾기
- 반경 밖 식당과 시장이 아닌 쇼핑 제외, 앵커가 FUN 후보에 다시 나오지 않음
- `excludePlaceIds` 반영
- 그래프: 점수 1등 조합, 점심 11:30 이후, OpenAI 실패 시에도 코스 생성(`polished: false`)
- OpenAI가 문장만 바꾸면 반영, 장소를 바꾸면 문장 폐기
- 앵커·식당 없을 때 오류 결과
- 검색 API(200 / 잘못된 코드 422), 코스 추천 API(200 / 데이터 없는 지역 422)

### 4-11. `requirements.txt` / `README.md`

- 추가 의존성: `chromadb==1.5.9`, `python-dotenv==1.2.4`, `pymysql==1.1.2`
- README: "RAG 미연결" 문구 삭제, RAG 코스 추천 소개와 Spring 미연결(503) 상태 명시, `.env` DB 설정, 적재 명령, 신규 API 표와 호출 예시, 루트 `RAG_다음단계_Phase3-4_가이드_3.md` 참조 추가. Spring 동기화로 들어온 장소가 `/internal/places/index`를 통해 Chroma에 자동 반영된다는 설명 추가.

## 5. 새로 필요한 설정 (`ai-server/.env`)

| 키 | 용도 |
| --- | --- |
| `OPENAI_API_KEY` | 임베딩·문장 다듬기 (기존) |
| `OPENAI_MODEL` | 선택, 기본 `gpt-4o-mini` |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | MariaDB 읽기 (읽기 전용 계정 권장) |
| `TOUR_API_KEY` 또는 `~/.tourapi_key.txt` | TourAPI 인증키 |
| `SPRING_BASE_URL`, `ADMIN_LOGIN_ID`, `ADMIN_LOGIN_PW` | 일괄 동기화 스크립트 전용 |

## 6. 참고 사항

- `compose.py`의 `from typing import Callable`은 사용되지 않는 import다.
- 이동 시간은 실제 경로가 아니라 직선거리 기반 추정치이며, 응답에 `routeSource: "ESTIMATED"`로 표시된다.
- `/internal/places/remove`는 다른 내부 API와 달리 예외 처리(503 변환)가 없다.
- 검색 가중치(0.4/0.4/0.2)와 반경은 코드 주석상 "1차 버전 시작값"이다.
