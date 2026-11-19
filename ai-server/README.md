# 스마트 트래블 Python AI 서버

Python 서버는 LangChain의 `ChatOpenAI`로 대화에서 조건을 추출하고, LangGraph로 조건 검증과 다음 질문을 처리한다. 대화·조건 저장은 Spring Boot와 MariaDB가 담당한다. 기본 경로는 저장 코스의 임베딩 검색과 취향 리랭킹(`COURSE_RAG_ENABLED` 기본 `true`)을 사용하고, 요청한 출발일부터 일차별 방문지를 표시한다.

## 실행 방법 (Windows PowerShell)

아래 명령은 `ai-server` 폴더에서 실행한다. API 키는 이 Python 서버를 실행하는 터미널에서 설정해야 한다.

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
$env:OPENAI_API_KEY = Read-Host "OpenAI API 키를 입력하세요"
.\.venv\Scripts\python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 8001
```

관광지 조회를 사용하려면 Python 서버를 실행하기 전에 같은 터미널에서 `$env:TOUR_API_KEY = Read-Host "공공데이터 일반 인증키(Decoding)"`를 실행해 한국관광공사 국문 관광정보 서비스용 일반 인증키를 설정한다. 이미 키가 현재 터미널에 설정되어 있다면 다시 입력하지 않아도 된다. PowerShell 창을 닫으면 해당 창에만 설정한 환경변수는 사라진다. 키를 코드, 문서, Git에 기록하거나 공유하지 않는다. 기본 모델은 `gpt-4o-mini`이고 필요하면 Python 서버 실행 전에 `OPENAI_MODEL` 환경변수로 변경할 수 있다.

별도 터미널에서 기존 `smarttraveler`의 `./gradlew.bat bootRun`과 `frontend`의 `npm run dev`를 실행한다. MariaDB는 기존 대화 저장을 위해 필요하다. Spring 서버의 AI 서버 주소 기본값은 `http://127.0.0.1:8001`이며 다른 주소가 필요하면 Spring 서버를 실행하는 환경에서 `AI_SERVICE_URL`을 지정한다.

`GET http://127.0.0.1:8001/health`는 Python 서버의 실행 여부만 보여준다. API 키나 OpenAI 호출의 성공 여부를 확인하는 엔드포인트는 아니다.

`POST /internal/dialogue`는 Spring 서버가 내부적으로 호출한다. 로컬 개발 환경 기준이므로 Python 서버를 `127.0.0.1`에만 바인딩한다. 외부 배포 시 서비스 간 인증·암호화와 접근 제한을 별도로 구성해야 한다.

## 테스트

```powershell
.\.venv\Scripts\python.exe -m pytest -q
```

테스트는 OpenAI의 실제 API를 호출하지 않는다. 실제 키로 서버를 사용하려면 API 사용 권한과 사용량을 확인하고 Python 서버를 키가 설정된 환경에서 실행해야 한다.

## 지역·관광지 데이터

- 공공데이터포털의 한국관광공사 국문 관광정보 서비스(KorService2)의 법정동 지역 코드와 지역기반 관광지 목록을 사용한다. 지역 목록은 Python 프로세스에서 한 시간 동안 캐시한다.
- 대화 화면에서 지역을 선택하고 관광지를 가져오면, **현재 조회한 페이지**의 관광지와 지역만 Spring이 기존 `region`, `region_external_code`, `place`에 저장한다. 전국 전체 자동 수집이나 일괄 동기화는 구현하지 않았다.
- 지역 외부 코드는 제공처와 함께 연결하고, 관광지는 `TOURAPI`와 콘텐츠 ID로 구분한다. 기존 다른 출처 장소나 사용자가 관리하는 설명·이미지·비활성화 및 검증 상태는 덮어쓰지 않는다.
- 현재 저장 항목은 관광지 ID, 이름, 주소, 위경도, 전화번호와 지역이며, 상세 설명과 이미지·운영시간·음식점은 수집하지 않는다. 관광지 정보는 RAG나 AI 일정 생성에 아직 연결되지 않았다.
- API 인증 실패와 오류는 목록을 빈 결과로 위장하지 않고 화면에 오류로 표시한다. 인증키나 요청 URL은 로그에 남기지 않는다.

## 대화에서 관광지 추천

- 조건을 모두 입력한 뒤 추천 요청을 하거나 조건 확인 질문에 `확인했어`라고 답하면, Spring이 지역에 저장된 활성 관광지 중 최대 30개를 Python에 전달한다. LangChain이 이 후보 ID 중 최대 세 곳을 고른다.
- 장소 이름과 주소는 DB에서 표시하며, AI가 후보에 없는 ID를 반환하면 결과를 거부한다. 지역에 가져온 장소가 없다면 화면의 관광지 가져오기 기능을 안내한다.
- 이 기본 관광지 경로는 이름순 DB 후보 조회를 사용한다. 코스 RAG를 사용하려면 아래 설정을 적용한다.

## 개인화 코스 RAG

- 기본값은 `true`(코스 RAG 사용 · 추천 코스가 회원의 내 코스로 저장됨)이다. 예전 관광지 3곳 추천 경로로 돌리려면 Spring 실행 환경에 `COURSE_RAG_ENABLED=false`를 설정한다. Python에 전달하는 `courseCandidates`의 유무로 경로를 구분한다.
- `smart-traveler-rag-1000-v1` 배치의 활성·완료·미삭제 코스 중 지정 지역 또는 직계 하위 지역, 정확히 1~4일인 후보만 사용한다. 일차 누락, 비활성 장소, 잘못된 방문 순서는 추천에서 제외한다.
- 인증된 활성 회원의 실제 저장 취향만 `savedPreferences`로 전달한다. 회원 ID는 전달하지 않고 대화 조건에도 저장하지 않는다. 대화의 명시 취향이 저장 취향보다 우선하며, 실내·야외, 일정 밀도, 풍경의 명시적 제외를 먼저 적용한다. 정보가 부족한 후보는 제외 충족으로 간주하지 않는다.
- 임베딩 관련도 상위 20개를 `0.50R + 0.20T + 0.15I + 0.15L`로 리랭킹한다. 미설정 항목의 가중치를 제외하고 재정규화하며, 후보의 미분류 값은 0.5로 처리한다. 정형 취향이 없으면 기존처럼 1개, 있으면 최대 3개를 반환한다.
- 코스 ID·제목·방문지는 DB 후보를 그대로 표시한다. 추천 이유 생성에 실패하면 일치 태그에 근거한 기본 설명을 표시한다. 실시간 운영·예약 가능 여부는 제공하지 않는다.
- `question`과 후속 요청의 `previousQuestion`은 Java·Python 양쪽에서 최대 20,000자를 허용한다. 프론트는 문자열을 그대로 표시한다.
- 실제 코스가 선정된 응답에는 1위 코스의 `recommendedCourseId`를 별도로 반환한다. 후보 없음·질문·관광지 추천 응답에는 포함하지 않는다. Spring은 전달했던 후보에 포함된 ID만 허용한다.
- `/chat`의 ‘관광지 찾아보기’는 `GET /api/chat/course-regions`에서 코스 목적지와 일치하는 DB 지역을 조회한다. 선택한 지역은 서버 검증 후 채팅 조건에 반영한다.
- 회원에게 추천을 표시한 뒤 `POST /api/chat/sessions/{id}/recommendations/{messageId}/save`로 1위 코스 1개를 저장한다. 회원은 서버 인증으로 식별하며 동일 추천 메시지의 중복 요청은 같은 저장 코스 ID를 반환한다.
- 회원·비회원 추천 스냅샷과 저장 결과는 `chat_messages.recommendation_json`에 기록한다. 스키마 자동 갱신을 사용하지 않는 설치는 `data/rag_recommendation_storage.sql`을 적용한다.
- 비회원은 화면 표시 후 `POST /api/chat/sessions/{id}/recommendations/{messageId}/guest`로 1위 코스를 Spring 세션에 보관한다. `GET /api/chat/guest-course`로 새로고침 뒤에도 보관 상태를 조회한다. 늦게 도착한 이전 추천의 요청이 최신 보관 코스를 덮어쓰지 않는다.
- 로그인한 프론트는 `POST /api/chat/guest-course/claim`을 호출해 보관 코스를 현재 인증 회원에게 이전한다. 일반·소셜 로그인 모두 같은 API를 사용하며, 중복 요청은 기존 저장 ID를 반환한다. DB 커밋 성공 후 세션 보관 정보를 지우고, 실패하면 처음 이전을 시도한 회원에게 연결한 상태로 유지한다.
- 비회원 보관은 서버 세션이 유지되는 동안 유효하다. 로그아웃·세션 만료·서버 세션 소실 시에는 사라진다. 이 흐름에서는 기존 React 메모리 초안 이전 API를 호출하지 않는다.
- DB 점검: `data/rag_candidate_audit.sql`. 테스트 DB의 실제 부산 1~4일 후보 점검은 Spring 테스트 실행 터미널에 `RAG_DB_AUDIT=true`를 설정하면 활성화된다. 이 테스트는 조회만 하고 `smarttraveler/build/rag-audit-candidates.json`에 공개 코스 후보를 기록한다.
