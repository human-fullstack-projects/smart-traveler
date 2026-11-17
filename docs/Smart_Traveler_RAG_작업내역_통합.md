# Smart Traveler RAG 작업 내역

작성일: 2026-11-17  
대상: RAG 개인화 고도화 및 요구사항3의 1~8번 구현

## 1. 주요 기능

| 기능 | 작업 내용 |
| --- | --- |
| 회원 취향 연동 | 저장된 일정 밀도, 실내·야외, 풍경 취향을 코스 추천에 반영 |
| 대화 취향 우선 | 현재 대화에서 말한 취향을 저장 취향보다 우선 적용하고 명시적 제외 조건 처리 |
| 개인화 추천 | 지역·일수가 맞는 코스를 임베딩으로 검색한 뒤 취향에 따라 순위를 조정 |
| 추천 결과 표시 | 최대 3개 코스의 제목, 날짜, 방문 순서, 추천 이유 표시 |
| 지역 찾아보기 | `/chat`에서 DB에 코스가 있는 지역을 조회하고 선택한 지역을 채팅 조건에 적용 |
| 첫 질문 개선 | 여행 지역·날짜·기간·동행·인원·교통수단을 묻는 지정 안내 문구 적용 |
| 회원 자동 저장 | 결과 표시 후 1위 코스 1개를 회원 소유 코스로 자동 저장 |
| 비회원 보관 | 최신 추천의 1위 코스 1개를 Spring 서버 세션에 보관 |
| 로그인 후 이전 | 보관한 코스를 인증된 회원 계정에 한 번만 저장 |
| 계정별 접근 보호 | 계정 전환 시 이전 대화 접근과 늦게 도착한 응답의 혼입 차단 |

## 2. 추천 처리

1. 사용자가 여행 조건을 입력하거나 ‘관광지 찾아보기’에서 지역을 선택한다.
2. Spring이 저장된 조건과 필요한 경우 회원의 저장 취향을 Python에 전달한다.
3. Python이 새 조건을 추출·병합하고 부족한 항목을 질문한다.
4. 조건이 변경되면 재확인을 요구한다.
5. 사용자가 확인하거나 추천을 요청하면 유효한 DB 코스에서 추천한다.
6. 결과를 표시한 뒤 회원은 자동 저장하고, 비회원은 서버 세션에 보관한다.

### 추천 기준

- 지정 지역 또는 직계 하위 지역의 활성·완료·미삭제 코스를 사용한다.
- 현재 RAG 대상은 `smart-traveler-rag-1000-v1` 시드 배치의 1~4일 코스다.
- 일차 누락, 비활성 장소, 잘못된 방문 순서는 추천 대상에서 제외한다.
- 명시적 제외 조건을 먼저 적용하고, 임베딩 관련도 상위 20개를 리랭킹한다.
- 점수는 `0.50R + 0.20T + 0.15I + 0.15L`이다. 각 항목은 관련도, 일정 밀도, 실내·야외, 풍경 일치도다.
- 미설정 취향의 가중치는 제외하고 재정규화한다. 후보의 미분류 값은 0.5이며 일치 근거로 설명하지 않는다.
- 정형 취향이 있으면 최대 3개, 없으면 기존처럼 1개를 추천한다.
- 추천 이유 생성이 실패해도 선정한 코스는 기본 설명과 함께 표시한다.

### 첫 안내 문구

```text
우리 일정을 알차게 짜보려면 몇 가지만 알려주세요:
어디로, 언제, 며칠 동안 가실 예정인가요?
누구와 함께 떠나시나요? (혼자, 친구, 연인, 가족?)
성인 몇명, 아동 몇명인가요?
이동할 때 대중교통과 자동차중 무엇을 이용하시나요?
```

### 지역 선택

`region.full_name`과 `course.destination`이 일치하는 지원 지역을 조회한다. 폐지 지역은 제외한다. 선택한 지역은 서버에서 ID를 검증하고 이름과 함께 조건에 저장하며, 기존 조건 확인 상태는 해제한다.

## 3. 코스 저장

### 회원

- 사용자와 확정한 기준대로 **추천 메시지마다 1위 코스 1개를 자동 저장**한다.
- 회원 ID는 Spring 인증에서 확인한다. 브라우저가 보낸 회원 ID나 임의 코스 내용을 사용하지 않는다.
- Python의 `recommendedCourseId`가 실제 전달한 후보에 포함되는지 확인한다.
- 원본 코스의 소유자를 바꾸지 않고 회원 소유의 `course`, `course_day`를 생성한다.
- 같은 추천 메시지의 반복 저장 요청은 기존 저장 코스 ID를 반환한다. 새로운 추천 메시지는 별도 결과로 취급한다.
- 저장 실패 시 추천 내용은 유지하고 ‘다시 저장’을 제공한다.

### 비회원

- 추천 표시 후 최신 1위 코스를 Spring `HttpSession`에 보관한다.
- 코스명·지역·날짜·여행 조건·일차별 장소 ID와 이름·방문 순서를 보관한다.
- 늦게 도착한 이전 추천의 요청이 최신 보관 코스를 덮어쓰지 않도록 처리한다.
- 새로고침 후에도 보관 상태를 조회하고 ‘내 코스’에 요약과 로그인 링크를 표시한다.
- 로그인·회원가입 화면에서 보관한 코스의 이전을 안내한다.

### 회원가입·로그인 후 이전

1. 가입만 완료한 시점에는 회원 코스를 만들지 않는다.
2. 일반·소셜 로그인 또는 로그인 상태로 앱 재진입 시 인증 확인 후 이전 API를 호출한다.
3. 서버 세션의 보관 정보를 현재 인증 회원에게 연결한다.
4. 반복·동시 요청은 같은 저장 코스 ID를 반환한다.
5. DB 커밋 성공 후에만 세션의 대기 정보를 지운다.
6. 저장 실패 시 보관 정보를 유지하고 재시도할 수 있다. 처음 이전을 시도한 계정과 다른 계정으로는 이전하지 않는다.
7. 저장 완료 후 ‘내 코스’를 갱신한다. 이전 계정의 채팅 접근 권한은 복구하지 않는다.

비회원 정보는 서버 세션 만료·로그아웃·서버 세션 소실 시 사라진다. 회원 DB 저장 성공 후에는 회원 코스로 유지된다. 기존 React 메모리의 조건 임시안과 실제 RAG 추천이 중복 저장되지 않도록 채팅의 기존 임시 보관 버튼은 제거했다.

## 4. API 및 DB 변경

| API | 역할 |
| --- | --- |
| `GET /api/chat/course-regions` | 코스 목적지와 일치하는 지역 목록 조회 |
| `POST /api/chat/sessions/{id}/recommendations/{messageId}/save` | 회원의 1위 추천 코스 저장 |
| `POST /api/chat/sessions/{id}/recommendations/{messageId}/guest` | 비회원 추천 코스 세션 보관 |
| `GET /api/chat/guest-course` | 비회원 보관 상태 조회 |
| `POST /api/chat/guest-course/claim` | 로그인 회원에게 보관 코스 이전 |

- `chat_messages.recommendation_json`: 추천 스냅샷과 저장 결과를 기록하는 nullable JSON 컬럼 추가.
- `chat_sessions.member_id`: 회원 채팅 생성과 비회원 코스 이전 시 인증 회원 연결.
- `course`, `course_day`: 회원의 새 코스와 요청 날짜에 맞춘 일차 저장.
- `course.condition_meta.itinerary`: 방문지와 순서 저장. 방문 시간이 없는 데이터에 임의 시간을 만들지 않는다.
- 마이페이지 상세 조회는 `activity`가 없을 때 일정 메타데이터의 장소를 표시한다.
- Java·Python의 추천 응답 및 후속 질문 한도를 20,000자로 맞췄다.

스키마 자동 갱신을 사용하지 않는 환경은 [컬럼 추가 SQL](../data/rag_recommendation_storage.sql)을 적용해야 한다.

## 5. 주요 변경 파일

| 영역 | 파일 |
| --- | --- |
| 대화·조건 | `ChatController.java`, `ConditionDialogueService.java`, `ConditionQuestions.java`, `PythonAiClient.java` |
| 취향·후보 | `ChatPersonalization.java`, `StoredCourseCandidates.java` |
| 지역 선택 | `CourseRegionController.java`, `CourseRegionPicker.jsx` |
| 회원 저장 | `RecommendedCourseService.java`, `ChatMessage.java`, `MyPageRepository.java` |
| 비회원 보관·이전 | `GuestRecommendedCourseService.java`, `GuestRecommendedCourseController.java` |
| Python 추천 | `graph.py`, `course_rag.py`, `course_preferences.py`, `schemas.py`, `main.py` |
| 프론트 연결 | `useAiChat.js`, `AiChatPage.jsx`, `ChatStart.jsx`, `chatIntro.js`, `chatApi.js` |
| 인증·내 코스 | `AuthContext.jsx`, `App.jsx`, `api/auth.js`, `LoginPage.jsx`, `SignupPage.jsx`, `MyCourses.jsx`, `useMyPageStore.js` |

Java 파일은 `smarttraveler/src/main/java/com/smarttraveler/`, Python 파일은 `ai-server/app/`, 프론트 파일은 `frontend/src/` 아래에 위치한다.

## 6. 검증 결과

| 항목 | 최종 확인 결과 |
| --- | --- |
| Python 전체 테스트 | 52개 통과 |
| Java 전체 테스트, DB 감사 활성화 | 111개 통과, 실패·건너뜀 0 |
| 프론트 프로덕션 빌드 | 통과 |
| 첫 질문·지역 선택·긴 추천 결과 React 렌더링 | 통과. 4일 코스 3개, 10,579자 누락 없음 |
| 회원 자동 저장·중복 요청 | 실제 DB 검증 통과 |
| 비회원 보관 후 실제 회원가입·로그인 | 세션 ID 교체 후 보관 유지 및 회원 코스 이전 확인 |
| 동시 이전·소셜 세션 교체 | 같은 코스 ID 반환, DB에 1개 생성 확인 |
| 저장 및 최종 커밋 실패 | 롤백·보관 유지·재시도·계정 변경 차단 확인 |
| 로그인과 보관 요청 순서 | 보관 요청 완료 후 로그인 요청 실행 확인 |
| 초기 RAG 데이터 점검 | 시드 1,000건, 지역·일수 214개 묶음의 정합성 확인 |
| 실제 OpenAI 연결 | 앞선 개인화 작업에서 실제 DB 후보로 부산 1~4일 추천과 취향별 순위 변화 확인 |

DB 통합 테스트에서 만든 계정·동의·채팅·코스는 롤백 또는 테스트 후 정리했다. 신규 컬럼은 스키마 변경으로 유지된다.

실제 브라우저 클릭부터 OpenAI 추천과 회원 저장까지 연속 수행하는 전체 수동 E2E는 미실시다. 소셜 로그인은 앱 내부의 세션 교체·이전 경로를 검증했으며 외부 제공자의 인증 화면은 호출하지 않았다.

## 7. 실행 및 재현

Spring 코스 RAG 활성화 — `smarttraveler`에서 실행:

```powershell
$env:COURSE_RAG_ENABLED='true'
.\gradlew.bat bootRun
```

`COURSE_RAG_ENABLED`를 지정하지 않으면 코드 기본값은 `false`이며 기존 관광지 추천 경로를 사용한다. 설정을 바꾸면 Spring을 재시작해야 한다.

DB 통합 테스트 포함 Java 테스트 — 테스트 DB를 대상으로 실행:

```powershell
$env:RAG_DB_AUDIT='true'
.\gradlew.bat test --console=plain
```

Python 테스트 — `ai-server`에서 실행:

```powershell
.\.venv\Scripts\python.exe -m pytest -q -p no:cacheprovider
```

프론트 검증 — `frontend`에서 실행:

```powershell
npm run build
node src/pages/ai-chat/tests/verify-rag-display.mjs
```

## 8. 상세 문서

- [RAG 개인화 작업 결과](RAG_개인화_작업결과.md)
- [요구사항3 1~6번 작업 결과](요구사항3_1_6번_작업결과.md)
- [요구사항3 7·8번 작업 전 분석](요구사항3_7_8번_작업전_분석.md)
- [요구사항3 7·8번 작업 결과](요구사항3_7_8번_작업결과.md)
