# AI 완료 작업 및 인수인계

기준일: 2026-11-04  
대상: Smart Traveler 여행 일정 추천 기능

이 문서는 현재 코드에서 **구현된 내용**과 **앞으로 구현할 내용**을 구분해 기록한다. 일정 추천과 일정 수정은 별도 기능이며, 현재는 일정 추천을 위한 화면·저장·요청 구조를 먼저 만들었다.

## 1. 현재까지 완료한 작업

### 메인 및 대화 화면

- 메인 화면에 대화로 여행 추천을 시작하는 입력창을 만들고, 조건검색은 메뉴의 별도 페이지로 분리했다.
- 첫 메시지를 입력하면 대화 세션을 만들고 `#/chat` 대화 화면으로 이동한다.
- Enter는 메시지 전송, Shift+Enter는 줄바꿈이다. 한글 조합을 마무리하는 Enter는 전송하지 않는다.
- 대화 입력창은 사용자가 크기를 조절할 수 없도록 고정했다.
- Spring 서버가 Python AI 서버에 메시지와 기존 조건을 전달한다. Python 서버는 LangChain과 OpenAI로 조건을 추출하고 LangGraph로 부족한 조건을 검사해 질문한다. Spring은 결과를 다시 검증해 대화·조건을 저장한다.
- 대화 화면에 여행 조건 확인·직접 수정 영역을 추가했다. 조건을 변경하면 기존 확인 상태가 해제된다.
- 조건 확인 시 필수 입력과 기간 등을 검사한다. 관광지 둘러보기에서 실제 지역을 선택해 조회한 뒤 대화 조건에 반영하면 내부 지역 번호가 연결된다. 대화에서 추출한 지역명만 있는 경우에는 번호 연결 전이다.

### 대화 및 여행 조건 저장

- 대화를 시작하면 기존 `chat_sessions`에 세션을 만들고 첫 메시지를 `chat_messages`에 저장한다. 이후 전송한 메시지도 같은 테이블에 저장한다.
- `chat_session_conditions`에 세션별 여행 조건 JSON과 확인 여부를 저장한다. 조건 입력 중의 빈 값도 저장할 수 있다.
- 조건 입력이 바뀔 때 저장 요청을 순서대로 보내며, 저장 중·실패 상태와 재시도 버튼을 보여준다. 메시지 전송이 실패하면 입력 내용을 유지한다.
- 브라우저 `localStorage`에는 마지막 대화 세션 번호를 저장한다. 같은 서버 세션이 유효할 때 새로고침하면 메시지와 여행 조건을 다시 불러온다. 현재는 대화 목록이나 이전 대화 선택 기능은 없다.
- 채팅 API는 비회원 서버 세션으로 소유자를 구분하고, 다른 세션의 대화 조회를 차단한다. 변경 요청에는 CSRF 토큰을 사용한다.
- `smart_travel_db`에 `chat_session_conditions` 테이블과 `chat_sessions` 외래키를 추가했다. 다른 개발 환경에서는 `smarttraveler/db/chat_session_conditions.sql`을 대상 DB 확인 후 적용해야 한다. SQL은 기존 테이블을 유지하도록 작성되어 있다.

### 지역·관광지 API 및 저장

- Python 서버가 한국관광공사의 법정동 지역 목록과 지역기반 관광지 목록을 읽고, Spring 서버가 선택한 지역과 조회한 페이지의 관광지를 저장한다.
- `region_external_code`는 관광공사 외부 코드와 내부 `region`을 연결한다. `place`는 제공처와 콘텐츠 ID로 중복을 구분한다. 재조회 시 수동으로 관리한 설명·이미지·비활성화·검증 상태를 덮어쓰지 않는다.
- 현재 관광지의 이름·주소·위경도·전화번호를 저장하며, 조회한 페이지 외의 전국 데이터를 자동으로 수집하지 않는다. 장소 상세와 RAG 연결은 추후 작업이다.

### 대화에서 관광지 추천

- 필수 조건을 입력하면 `확인했어` 또는 `추천해줘`라고 답할 수 있음을 안내한다. 확인·추천 답변을 받으면 Spring이 기존 DB의 활성 관광지 후보를 조회하고, Python LangGraph가 OpenAI로 후보 중 최대 세 곳을 선택한다.
- 장소명과 주소는 DB에 저장된 값을 사용하며, AI가 후보에 없는 식별자를 선택하면 결과를 거부한다. 해당 지역에 저장된 관광지가 없다면 먼저 가져오도록 안내한다.
- 확인이 모호하거나 수정 의사를 표현하면 확인 방법이나 수정할 내용을 안내한다. 이전 형식의 확인 메시지도 계속 인식한다.
- 현재는 DB 이름순 최대 30개 후보만 사용하며 RAG, 날짜별 여행 일정 생성, 실시간 운영정보 조회는 구현하지 않았다.

### 추천 요청과 결과 구조

- 서버에 `RecommendationRequest`를 만들고 여행 지역, 출발일, 기간, 인원, 동행, 교통, 취향, 고정 일정, 진행 방식, 숙소 선택 형식을 정했다.
- 추천 결과와 추후 일정 수정 기능에서 참고할 `CourseDraft` 형식을 만들었다. 일차별 활동, 장소 ID, 활동 식별자, 시간, 추천 이유 등을 담는다.
- 실제 장소 조회와 일정 생성을 나중에 연결할 수 있도록 `PlaceCandidateFinder`, `AiCourseGenerator` 인터페이스를 만들었다.
- `POST /api/courses/recommendations` 요청의 조건과 고정 일정 기간을 검증한다. 추천 자체는 미구현이므로 유효한 요청에도 현재 HTTP 503을 반환한다.

## 2. 아직 구현하지 않은 기능

- AI가 질문이나 자유로운 대화 응답을 생성하는 기능. OpenAI는 조건 추출에 사용하며, 추가 질문은 Python의 정해진 문구를 사용한다.
- 대화에서 말한 지역을 자동으로 실제 지역에 매핑하는 기능. 현재 관광지 둘러보기에서 직접 지역을 선택해 반영한다.
- 전국 관광지 자동 일괄 수집·상세 정보 동기화, 장소 후보 검색, 임베딩·벡터 검색을 이용한 RAG
- AI를 이용한 날짜별 일정 생성, 추천 결과 화면, 추천 코스의 정식 저장
- 회원 로그인과 대화 연결, 여러 기기에서 대화 복원, 과거 대화 목록
- 일정 수정, 변경안 승인 및 기존 일정에 반영하는 기능

별도 **조건검색 페이지**는 현재 화면 형태로만 구성되어 있다. 대화 화면의 조건 저장이나 실제 추천 API와 연결된 기능으로 생각하면 안 된다.

## 3. 현재 요청 데이터 형식

화면과 서버의 여행 조건은 같은 필드 이름을 사용한다. 아래는 필수 조건을 모두 입력한 예시다. 실제 추천 결과나 실제 지역 데이터는 아니다.

```json
{
  "regionId": 1,
  "startDate": "2026-12-10",
  "durationDays": 3,
  "adults": 2,
  "children": 0,
  "companionType": "FRIENDS",
  "transportType": "CAR",
  "preferences": ["바다", "카페"],
  "fixedEvents": [],
  "flowType": "COURSE_FIRST",
  "lodgingSelection": null
}
```

- `regionName`은 현재 대화에서만 사용하는 임시 지역명이다. 추천 DTO의 `regionId`와는 다르며, 실제 지역 연결 전에는 추천 요청에 사용할 수 없다.
- `durationDays`는 종료일이 아니라 여행 일수다. 현재 서버와 화면의 유효성 검사는 1~7일이다.
- `fixedEvents` 항목은 `date`, `startTime`, `placeName`으로 구성하며 여행 기간 안에 있어야 한다.
- `flowType`은 `COURSE_FIRST` 또는 `STAY_FIRST`다. 숙소 먼저일 때 `lodgingSelection`에는 `lodgingId`, `roomId`, `checkIn`, `checkOut`이 필요하다.
- `companionType`, `transportType`은 화면에서 코드를 선택하지만, 서버 추천 DTO는 현재 문자열 필수 여부와 길이를 검사한다. 팀 공통 코드 및 실제 지역·숙소와의 매핑은 확정이 필요하다.
- 작성 중인 조건은 빈 문자열이나 `null`을 포함할 수 있다. 저장은 가능하지만 추천 요청에 필요한 검증을 통과한 상태라는 뜻은 아니다.

조건 저장 API는 위 객체를 바로 보내는 형식이 아니라 `conditionsJson` 문자열과 `confirmed`를 감싸서 보낸다. 화면의 `frontend/src/chatApi.js`를 참고한다.

## 4. 주요 API

| 메서드 | 경로 | 기능 |
| --- | --- | --- |
| GET | `/api/chat/csrf` | 변경 요청에 사용할 CSRF 토큰 조회 |
| POST | `/api/chat/sessions` | 대화 세션 생성, 첫 메시지 처리와 질문 저장 후 전체 대화 반환 |
| GET | `/api/chat/sessions/{id}` | 해당 세션의 메시지와 여행 조건 조회 |
| POST | `/api/chat/sessions/{id}/messages` | 메시지와 질문 저장, 조건 갱신 후 전체 대화 반환 |
| PUT | `/api/chat/sessions/{id}/conditions` | 여행 조건 JSON과 확인 상태 저장 |
| GET | `/api/chat/tour/regions` | 한국관광공사 시도·시군구 목록 조회 |
| POST | `/api/chat/tour/places` | 선택 지역 관광지 조회 후 현재 페이지 DB 저장 (CSRF 필요) |
| POST | `/api/courses/recommendations` | 추천 조건 검증 후 현재 503 반환 |

`/api/chat/**`는 비회원 접근이 가능하지만 세션 소유권과 CSRF를 확인한다. 나머지 요청에는 기존 인증이 필요하도록 보안 설정이 구성되어 있다. 서버의 `HttpSession`이 만료되거나 서버가 재시작되면 DB 데이터가 남아 있어도 현재 비회원 방식으로는 이전 대화에 다시 접근할 수 없다. 저장된 세션 번호 자체는 인증 수단이 아니다.

## 5. 주요 파일 위치

- `frontend/src/App.jsx`: 메인·대화 화면, 대화 불러오기와 조건 저장 흐름
- `frontend/src/TravelConditions.jsx`: 여행 조건 입력·확인 화면
- `frontend/src/travelConditions.js`: 조건 기본값과 화면 검증
- `frontend/src/chatApi.js`: 채팅 API와 CSRF 요청
- `frontend/src/chat.css`: 대화 및 조건 화면 스타일
- `frontend/vite.config.js`: 개발 환경 `/api` 요청을 `localhost:8080`으로 전달
- `smarttraveler/src/main/java/com/smarttraveler/chat/ChatController.java`: 채팅·조건 저장 API
- `ai-server/app/tour_api.py`: 한국관광공사 지역·관광지 API 조회와 지역 캐시
- `smarttraveler/src/main/java/com/smarttraveler/chat/TourController.java`, `TourImportService.java`: 관광지 API 전달과 DB 중복 저장 방지
- `frontend/src/TourSearch.jsx`, `tourApi.js`: 대화 화면의 지역 선택과 관광지 조회
- `ai-server/app/graph.py`: LangChain OpenAI 조건 추출과 LangGraph 질문 흐름
- `ai-server/app/main.py`, `schemas.py`: Python 내부 API와 요청·응답 형식
- `smarttraveler/src/main/java/com/smarttraveler/chat/PythonAiClient.java`, `ConditionMerger.java`, `ConditionDialogueService.java`: Python 호출, 결과 검증과 대화 저장 흐름. 이전 Java 직접 호출 코드는 사용하지 않는다.
- `smarttraveler/src/main/java/com/smarttraveler/chat/ChatSecurity.java`: 채팅 API 보안 설정
- `smarttraveler/db/chat_session_conditions.sql`: 여행 조건 테이블 생성 SQL
- `smarttraveler/src/main/java/com/smarttraveler/recommendation/dto/RecommendationRequest.java`: 추천 조건 DTO
- `smarttraveler/src/main/java/com/smarttraveler/recommendation/dto/CourseDraft.java`: 추천·수정 기능에서 참고할 일정 초안 DTO
- `smarttraveler/src/main/java/com/smarttraveler/recommendation/service/`: 추천 서비스와 장소 검색·AI 생성 인터페이스
- `smarttraveler/src/main/java/com/smarttraveler/recommendation/controller/RecommendationController.java`: 추천 API

## 6. 실행 및 확인 방법

1. MariaDB와 서버의 DB 연결 설정을 확인한다. 새 개발 DB라면 기존 채팅 테이블이 있는지 확인한 후 `smarttraveler/db/chat_session_conditions.sql`을 실행한다.
2. `ai-server` 폴더에서 Python 가상환경을 만들고 `pip install -r requirements.txt`로 필요한 라이브러리를 설치한다.
3. **Python AI 서버를 실행할 터미널**에서 `OPENAI_API_KEY`와 공공데이터 일반 인증키 `TOUR_API_KEY`를 설정하고 `python -m uvicorn app.main:app --host 127.0.0.1 --port 8001`을 실행한다. `OPENAI_MODEL`은 선택이며 기본값은 `gpt-4o-mini`다.
4. 별도 터미널에서 `smarttraveler` 폴더의 `./gradlew.bat bootRun`으로 Spring 서버를 실행한다. Python 주소 기본값은 `http://127.0.0.1:8001`이며 Spring의 `AI_SERVICE_URL`로 바꿀 수 있다.
5. 또 다른 터미널에서 `frontend` 폴더의 `npm run dev`를 실행한다. 개발 화면의 `/api` 요청은 기본적으로 `http://localhost:8080`으로 전달된다.
6. 대화에서 조건을 말하고, 답변과 조건이 표시되는지 확인한다. 저장이 끝난 뒤 새로고침해 대화와 조건을 확인한다.

자세한 설치 명령은 `ai-server/README.md`를 참고한다. Python 서버는 로컬 개발용으로 `127.0.0.1`에 바인딩한다. 외부 배포 시 서비스 간 인증·암호화와 네트워크 접근 제한이 필요하다. OpenAI 또는 Python 서버 오류 시 대화 저장은 롤백된다.

기존 서버 설정에는 Hibernate 스키마 자동 갱신(`spring.jpa.hibernate.ddl-auto=update`, `spring.jpa.generate-ddl=true`)이 켜져 있다. 서버 실행이나 통합 테스트 전에 연결 대상 DB를 반드시 확인한다. 격리된 DB에서 검증할 때는 스키마 자동 갱신도 명시적으로 꺼야 한다. API 키와 DB 비밀번호는 문서나 Git에 기록하지 않는다.

### 검증 결과

- Python 그래프·API 단위 테스트에서 조건 병합, 질문 분기, 잘못된 값과 오류를 검증했다. LangChain의 실제 요청 생성은 모의 OpenAI 서버로 확인했다.
- Spring 테스트와 빌드가 통과했다. 별도 시험 MariaDB·Python·Spring·브라우저를 연결해 대화 저장, 조건 확인과 새로고침 복원을 확인했다. AI 응답은 모의 응답이다.
- 사용자 서버에서 실제 OpenAI 호출을 시험용 조건과 후보로 실행해 확인 답변 후 추천 분기를 검증했다. 실제 사용자 대화나 DB에는 시험 메시지를 저장하지 않았다.
- 이전 통합 테스트 실행 시 프로젝트 기본 DB 설정이 사용된 적이 있다. 스키마 자동 갱신 설정 때문에 당시 기존 DB에 자동 변경이 있었는지는 확정할 수 없으며, 기록된 관련 경고는 없었다.

## 7. 다음 구현 순서

1. **실제 대화 품질 검증**: 다양한 조건과 모호한 확인 표현을 시험하고 추천 후보 범위와 오류 처리를 개선한다.
2. **질문 흐름 보완하기**: 실제 지역 선택과 자연어 표현, 숙소 먼저 및 고정 일정 검증을 확장한다. 자동 추출한 내용은 사용자가 직접 수정·확인할 수 있어야 한다.
3. **장소 수집 확장**: 지역명 자동 매핑, 관광지 전체 동기화·상세 정보 수집과 저장된 장소 후보 조회를 구현한다.
4. **RAG와 일정 생성 연결하기**: 검색한 실제 장소를 근거로 일정을 생성하고 장소 ID, 날짜·시간, 필수 조건을 서버에서 검증한다.
5. **추천 결과 표시와 저장**: 일차별 초안을 보여주고, 사용자의 저장 동작에 맞춰 정식 코스에 반영한다.

## 8. 일정 수정 담당자와 협의할 내용

- `CourseDraft.courseRef`의 `type`, `id`, `revision`은 초안과 저장된 코스를 구분하기 위한 형식이다. 실제 DB 매핑, 식별자 생성과 충돌 검사는 아직 구현되지 않았다.
- 활동별 `activityKey`는 원본과 변경안을 비교하기 위한 키이고 `activityId`는 정식 저장 후 DB 식별자를 위한 값이다. 생성·유지 정책은 팀에서 정해야 한다.
- 수정안은 사용자가 승인하기 전 원본에 반영하지 않는 흐름을 기준으로 협의한다. 소유권, 예약 상태, 수정 기준이 바뀐 경우의 충돌 처리와 수정안 보관 기간도 정해야 한다.
- 현재 프로젝트 방향은 최종 코스 중심이다. 이전 버전 전체 보관 여부는 기능명세·ERD와 함께 팀에서 결정한다.
