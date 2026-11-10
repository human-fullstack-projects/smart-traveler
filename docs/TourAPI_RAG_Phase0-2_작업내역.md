# TourAPI RAG 여행 코스 — Phase 0~2 작업 내역

작업일: 2026-11-10
브랜치: `lsj1110back` (`lsj1110`에서 분기)
근거 문서: `TourAPI_RAG_여행코스_작업가이드2.md`
대상: KorService2 수집 확장 + ai-server(Chroma) 임베딩 연결. 파일럿 지역은 부산 해운대구(`region_code=26350`, `region_id=17`).

이 문서는 가이드의 Phase 0~2를 실제로 구현하고 **실데이터로 검증**한 결과를 기록한다. 코드만 작성하고 끝낸 것이 아니라, TourAPI·OpenAI API를 실제로 호출해 해운대구 데이터를 수집·임베딩까지 마쳤다.

---

## 1. Phase 0: 사전 정리

- **출처 값 불일치 버그 수정**: `StoredPlaceCandidates`가 `source_provider='TOURAPI'`만 조회하고 있었는데, 관리자 동기화(`TourSyncService`)는 전부 `'KTO_TOURAPI'`로 저장하고 있었다. 즉 **관리자가 동기화한 장소가 채팅 추천 후보에 전혀 나오지 않는 상태**였다. 조건을 `source_provider IN ('TOURAPI','KTO_TOURAPI')`로 넓혀 수정했다.
- 중복 `source_place_id` 확인 SQL 실행 → 0건.
- 파일럿 지역을 해운대구로 확정. `region.is_supported=TRUE` 확인.
- `lclsSystmCode2` API를 실제로 호출해 KorService2 신분류 코드 전체(대분류 10개, 소분류 59개)를 확인. **시장 분류는 `SH06` 하나뿐**임을 확인(다른 대분류 밑에는 시장류 코드가 없음).

## 2. Phase 1: 데이터 수집 확장

### 2-1. 코드 변경

- `TourApiClient`: `attractionPage()`에 박혀 있던 `contentTypeId=12`를 일반화해 `areaPage(lDongRegnCd, lDongSignguCd, contentTypeId, pageNo, rows)`로 분리. `detailCommon(contentId)`·`detailIntro(contentId, contentTypeId)` 추가.
  - **실제 호출로 확인한 사실**: `detailCommon2`는 `overviewYN` 등 선택 플래그 파라미터를 받지 않는다(넣으면 `INVALID_REQUEST_PARAMETER_ERROR`). `contentId`만 넘기면 `overview`·`homepage`·`firstimage` 등이 전부 포함된 응답이 온다.
  - `detailIntro2`는 타입마다 필드명이 다르다(예: 이용시간이 관광지는 `usetime`, 문화시설은 `usetimeculture`, 음식점은 `opentimefood`). `firstNonBlank()`로 타입에 상관없이 하나의 컬럼에 모은다.
- `TourSyncService`:
  - `syncAttractions()` → `syncPlaces()`로 일반화. contentTypeId 12·14·28·38·39를 모두 수집하고 `place_type`을 ATTRACTION·CULTURE·LEISURE·SHOPPING·RESTAURANT로 매핑.
  - `detailCommon2`의 `overview`(HTML 태그 제거)·`homepage`를 `place.description`·`homepage_url`에, `detailIntro2`의 이용시간·휴무일을 `place.opening_hours`·`closed_days`에, 음식점 대표메뉴를 `restaurant_detail.main_menu`에 저장.
  - **증분 수집**: 장소에 `description`이 이미 있고 원본이 안 바뀌었으면 상세 API를 다시 부르지 않음(`needsDetail()`).
  - **시장 판정**: `place.is_market` 컬럼 추가(Place 엔티티에 필드 추가, Hibernate `ddl-auto=update`로 자동 생성). 쇼핑 분류가 `SH06`이거나, 이름에 "시장"이 들어가면서 "마트/백화점/면세"가 아니면 시장으로 판정. 관광지(12)로 등록된 시장(예: 자갈치시장류)도 이름으로 같이 판정.
  - KorService2 신분류 코드(`lclsSystm1`·`lclsSystm2`)를 `place_category`/`place_category_mapping`에 저장(코드 이름표는 Phase 0에서 확인한 69개를 정적 맵으로 내장).
  - 타입별 기본 체류시간(`recommended_minutes`): 식당·쇼핑 60분, 그 외 90분.

### 2-2. 해운대구 실제 수집 결과

관리자 동기화 로직(`TourSyncService.sync("26350")`)을 Spring Boot 테스트 컨텍스트로 직접 실행해 **실제 TourAPI를 호출**했다.

| contentTypeId | place_type | 전체 건수 | 수집 건수(상한) | 설명 보유율 |
| --- | --- | --- | --- | --- |
| 12 | ATTRACTION | 41 | 41 | 100% |
| 14 | CULTURE | 18 | 18 | 100% |
| 28 | LEISURE | 12 | 12 | 100% |
| 38 | SHOPPING | 118 | 50(상한) | 100% |
| 39 | RESTAURANT | 87 | 87 | 100% |

- 시장 3곳 확인: 좌동재래시장, 해운대시장, 반송3동골목시장.
- 가이드 5-2 완료 기준(식당 30건↑, 레저+문화 10건↑, 시장 2건↑, 설명 70%↑) **전부 충족**.
- 호출 한도(`LIMITED_NUMBER_OF_SERVICE_REQUESTS`)에 걸리지 않고 전체 완료.
- 기존 `syncHub()`(중심관광지 순위)가 만드는 설명 없는 placeholder 장소(73건, `source_provider='KTO_HUB'`)는 Phase 1 대상이 아니라 원래부터 있던 별도 경로이며 의도된 동작이다.

### 2-3. 수집 중 발견·수정한 추가 버그: `place_image` 중복 저장

- `place_image`에 `(place_id, sort_order)` 유니크 키가 없어, `TourSyncService.upsertImage()`의 `ON DUPLICATE KEY UPDATE`가 걸릴 유니크 키 자체가 없어서 **재동기화할 때마다 같은 이미지가 새 행으로 계속 쌓이고 있었다**(기존 버그, DB 전체에 1988행 중 1058행이 중복). 인덱싱 조회 쿼리가 이 중복 때문에 "Subquery returns more than 1 row" 오류로 죽는 걸 보고 발견했다.
- 조치: ① 중복 그룹별로 가장 최신 행만 남기고 나머지 삭제(1988→930행), ② `PlaceImage` 엔티티에 `@UniqueConstraint(place_id, sort_order)` 추가해 Hibernate `ddl-auto=update`로 제약을 실제로 생성. 이후 해운대구를 다시 동기화해 `place_image` 행 수가 늘지 않고(352→352) `ON DUPLICATE KEY UPDATE`가 정상 동작함을 확인, DB 전체에 중복 그룹이 0건임도 재확인했다.

## 3. Phase 2: ai-server 임베딩(Chroma) 연결

### 3-1. 코드 변경

- `ai-server/requirements.txt`: `chromadb`, `python-dotenv` 추가.
- `ai-server/app/rag.py` 신설: 타입별 문서 텍스트(`to_document`), 대체 인기 신호(`popularity` — `place_hub_rank` 순위가 있으면 우선 사용, 없으면 이미지 유무·설명 길이로 대체), `index_places`/`remove_places`.
  - OpenAI 임베딩 클라이언트는 **지연 생성**으로 작성(`get_embeddings()`). 처음에는 모듈 최상단에서 바로 생성했는데, 그러면 `OPENAI_API_KEY`가 없을 때 `/health`를 포함한 **서버 전체가 기동 실패**하는 문제를 발견해 수정함(기존 `graph.py`의 지연 생성 패턴과 통일).
- `ai-server/app/main.py`: `POST /internal/places/index`, `POST /internal/places/remove` 추가. `python-dotenv`로 `ai-server/.env`의 `OPENAI_API_KEY`를 실행 위치와 무관하게 로드.
- Spring `PlaceIndexClient` 신설, `TourSyncService.sync()`가 동기화 끝나면 활성·좌표 보유 장소를 100개씩 묶어 자동 전송(`data_sync_log`에 `AI_SERVER/INDEX` 단계로 기록).

### 3-2. 검증 중 발견한 버그 — Java `HttpClient`의 HTTP/2 평문 업그레이드 문제

Spring → ai-server 연동을 실제로 돌렸을 때 `PlaceIndexClient`가 매번 422로 실패했다. 원인 분석 결과, **Java 21의 `HttpClient`가 평문(http://) 연결에서도 기본적으로 HTTP/2 업그레이드를 시도**하는데, uvicorn(h11 기반)이 이를 처리하다가 POST 요청 본문을 빈 채로 전달하는 문제였다(`HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)`로 고정하면 즉시 해결됨을 재현 테스트로 확인).

이 버그는 `PlaceIndexClient`만의 문제가 아니라, **기존에 `/internal/dialogue`를 호출하는 `PythonAiClient`도 동일한 패턴을 쓰고 있어 같은 버그를 갖고 있었다.** 두 클라이언트 모두 `HTTP_1_1`로 고정해 함께 수정했다. (챗봇 대화 기능이 실제로 이 문제의 영향을 받고 있었는지는 운영 환경 조건에 따라 다를 수 있으나, 코드 상 동일한 결함이 있었던 것은 확인됨.)

### 3-3. 해운대구 실제 임베딩 검증

- OpenAI API 키를 `.env`로 적용한 뒤, 해운대구 동기화를 재실행 → **활성·좌표 보유 장소 281건 전부 임베딩 성공**(`AI_SERVER/INDEX` 단계 `SUCCESS 281/281`).
- `Chroma` 컬렉션 개수(281) == DB 활성·좌표 보유 장소 수(281) 일치 확인.
- 의미 검색 결과 확인(둘 다 `place_type` 필터만 적용, 아직 반경 필터는 Phase 3 이전):
  - "전통시장 먹거리 구경"(SHOPPING) → 상위 5개 중 **상위 3개가 정확히 좌동재래시장·반송3동골목시장·해운대시장**(해운대구에 있는 시장 전부).
  - "바다 보면서 먹는 해산물"(RESTAURANT) → 해운대해변로 등 해변 인근 식당들이 상위에 등장.

## 4. 다음 단계 (미구현)

- **Phase 3**: 앵커 선택(이름 우선 매칭) + 반경 필터(위경도 사각형 → haversine) + 의미 검색 + 점수 조합, `/internal/places/search` API.
- **Phase 4**: LLM이 슬롯별 1곳 선택 + 이유(strict schema), 서버 검증, 시간 계산.
- **Phase 5**: `PlaceCandidateFinder`/`AiCourseGenerator` 구현체 연결, `RecommendationService`의 503 교체, 프론트 연동.

## 5. 참고

### 5-1. 외부 키 저장 위치 (저장소에 커밋되지 않음)

- TourAPI 서비스키: `%USERPROFILE%\.tourapi_key.txt` (BOM 없는 UTF-8, 평문 디코딩 키). data.go.kr의 "Encoding" 키를 그대로 쓰면 `TourApiClient`가 다시 URL 인코딩해 깨지므로, 디코딩한 값을 저장해야 한다.
- OpenAI 키: `ai-server/.env`의 `OPENAI_API_KEY` (`.gitignore`에 이미 포함).

### 5-2. 변경된 파일

- `smarttraveler/src/main/java/com/smarttraveler/chat/StoredPlaceCandidates.java`
- `smarttraveler/src/main/java/com/smarttraveler/chat/PythonAiClient.java`
- `smarttraveler/src/main/java/com/smarttraveler/domain/entity/Place.java`
- `smarttraveler/src/main/java/com/smarttraveler/domain/entity/PlaceImage.java`
- `smarttraveler/src/main/java/com/smarttraveler/tour/client/TourApiClient.java`
- `smarttraveler/src/main/java/com/smarttraveler/tour/client/PlaceIndexClient.java` (신규)
- `smarttraveler/src/main/java/com/smarttraveler/tour/service/TourSyncService.java`
- `ai-server/app/main.py`
- `ai-server/app/schemas.py`
- `ai-server/app/rag.py` (신규)
- `ai-server/requirements.txt`
- `.gitignore` (`ai-server/chroma_data/` 제외 추가)
