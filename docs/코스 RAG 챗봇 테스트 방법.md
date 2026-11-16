# 코스 RAG 챗봇 테스트 방법

`Smart-Traveler-코스-RAG-챗봇-코드_수정본.md`로 구현한 기능(채팅에서 조건을 입력하면 저장된 코스 1,000건 중 하나를 벡터 검색+LLM으로 추천)을 실제로 테스트하는 순서다. 이미 Spring/ai-server 코드 변경과 region/category/place/course 시드는 전부 적용돼 있다는 전제다 (`smarttraveler/src/main/resources/sample-data/`의 4개 SQL 파일이 이미 실행된 상태여야 함).

## 0. 사전 확인 (한 번만)

DB에 아래가 들어있어야 한다. 이미 돼 있으면 건너뛴다.

```powershell
# smarttraveler 폴더에서, DB 접속 정보는 환경에 맞게
mariadb -h 127.0.0.1 -P 3306 -u sixon -p smart_travel_db < src/main/resources/sample-data/region_fixes_and_additions_2026_11.sql
mariadb -h 127.0.0.1 -P 3306 -u sixon -p smart_travel_db < src/main/resources/sample-data/category_seed_2026_11.sql
mariadb -h 127.0.0.1 -P 3306 -u sixon -p smart_travel_db < src/main/resources/sample-data/place_seed_2026_11.sql
mariadb -h 127.0.0.1 -P 3306 -u sixon -p smart_travel_db < src/main/resources/sample-data/rag_course_seed_1000_multiaccount.sql
```

그리고 채팅 조건 저장용 테이블이 있어야 한다 (없으면 기존 채팅 기능 자체가 동작하지 않음):

```powershell
mariadb -h 127.0.0.1 -P 3306 -u sixon -p smart_travel_db < smarttraveler/db/chat_session_conditions.sql
```

## 1. 서버 3개 실행 (각각 다른 터미널)

**① ai-server (Python)** — `ai-server` 폴더에서:

```powershell
$env:OPENAI_API_KEY = Read-Host "OpenAI API 키를 입력하세요"
.\.venv\Scripts\python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 8001
```

**② Spring (smarttraveler)** — `smarttraveler` 폴더에서, 반드시 `COURSE_RAG_ENABLED`를 켠다:

```powershell
$env:COURSE_RAG_ENABLED = "true"
.\gradlew.bat bootRun
```

이 환경변수를 안 켜면 기존 장소 추천(관광지 최대 3곳)만 동작하고 코스 추천은 켜지지 않는다.

**③ frontend** — `frontend` 폴더에서:

```powershell
npm run dev
```

브라우저에서 `http://localhost:5173/chat`으로 들어간다.

## 2. 채팅으로 테스트하기

아래 순서로 채팅창에 입력한다 (지역명은 3번 주의사항 참고):

1. `서귀포시 여행 가려고`
2. `2026년 12월 2일에 출발해`
3. `1박 2일로 여행할거야`
4. `성인 3명이고 아동은 없어`
5. `친구들이랑 같이 가`
6. `자동차로 이동할거야`
7. `역사문화 위주로 여유롭게 여행하고 싶어`
8. 조건 확인 질문이 나오면 → `추천해줘`

8번 입력 후 아래처럼 **코스 1건 + 일차별 방문 장소**가 나오면 성공이다 (실제로 확인한 응답 예시):

```
제주특별자치도 서귀포시 섬 여행 1박 2일 | 고근산 · 삼매봉 · 보목마을
서귀포시의 고근산과 삼매봉, 보목마을 여행은 역사문화적인 요소를 즐기며 자동차로 여유롭게 이동할 수 있어 친구와 함께하는 2일 여행에 적합합니다.
1일차 (2026-12-02)
1. 고근산
2. 삼매봉
2일차 (2026-12-03)
1. 보목마을
2. 위미1리어촌체험마을
```

## 3. 주의사항 — 지역명은 "고유한 이름"으로 말해야 한다

채팅에서 말한 지역명이 `region` 테이블에서 **정확히 하나**만 매칭돼야 추천이 된다. 특히 **"청주시"는 피한다** — region 테이블에 `region_name='청주시'`인 행이 4개(상당구/서원구/흥덕구 + 구 미표기 상위코드) 있어서 이름이 모호해져 항상 "코스가 없다"고 나온다. 이건 이번 기능과 무관한 기존 데이터 문제다.

안전하게 테스트 가능한 지역 예시: `서귀포시`, `목포시`, `완도군`, `진도군`, `신안군`, `대구 달성군`처럼 구 이름이 중복되지 않는 시/군. "동구", "중구", "서구"처럼 여러 시도에 같은 이름이 있는 구는 피한다.

## 4. 코스가 없다고 나올 때 체크리스트

- 말한 지역명이 2번처럼 모호하지 않은지 (위 3번 참고)
- 여행 일수가 1~4일 사이인지 (현재 코스 데이터는 이 범위만 있음)
- 그 지역에 해당 일수의 코스가 실제로 있는지 — 아래 SQL로 미리 확인 가능:

```sql
SELECT r.full_name, DATEDIFF(c.end_date, c.start_date) + 1 AS days, COUNT(*)
FROM course c JOIN region r ON r.region_id = c.region_id
WHERE JSON_VALUE(c.condition_meta, '$.seed_batch') = 'smart-traveler-rag-1000-v1'
GROUP BY r.full_name, days
ORDER BY r.full_name;
```

## 5. 다른 조건으로 더 테스트하기 (선택)

- 중간에 조건을 바꿔보기: 추천 전에 `"지역을 부산으로 바꿔줘"` 같은 메시지를 보내면 "입력된 여행 조건을 확인해 주세요" 재확인 질문이 다시 나와야 한다 (이전 조건으로 곧바로 추천하지 않는지 확인).
- 코스가 없는 조합 테스트: 일수를 `5일`처럼 범위 밖으로 주면 "현재 준비된 코스는 1~4일 일정이에요" 안내가 나와야 한다.

## 6. 끝낼 때

세 서버 모두 터미널에서 `Ctrl+C`로 종료한다. `COURSE_RAG_ENABLED` 환경변수는 그 터미널에만 적용되므로 터미널을 닫으면 자동으로 사라진다.
