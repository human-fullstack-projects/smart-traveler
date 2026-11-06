# 마이페이지 1단계: 데이터·접근·로그인 연결

백엔드 코드는 `src/main/java/com/smarttraveler/mypage`의 controller, dto, service, repository, entity로 분리했다.

## DB 준비

기존 members, course, course_day, activity, room_reservation, room, lodging, human_course_post 스키마가 필요하다.

새 `mypage_course_draft`는 미확정 임시 코스의 회원 보관용이다. 기존 AI 생성 코스나 예약 완료로 처리하지 않는다. 현재 프로젝트의 Hibernate `ddl-auto=update`가 적용된 서버를 재시작하면 새 엔티티의 테이블 생성 대상이 된다. 스키마를 직접 관리하는 환경에서는 연결할 DB를 확인하고 `course_draft.sql`을 한 번 적용한다. 이번 코드 작업에서는 DB에 실행하지 않았다.

회원 ID + client_id 유일키로 동일 코스 연결 요청의 재전송을 중복 저장하지 않는다. 한 배치는 트랜잭션이며 회원 행을 잠가 동일 회원의 동시 연결을 직렬화한다. 기존 데이터·구형 localStorage 샘플을 실제 회원 자료로 자동 이전하지 않는다.

## API

| 메서드 | 경로 | 기능 |
| --- | --- | --- |
| GET | /api/mypage | 로그인 회원의 코스·예약·게시글 조회 |
| GET | /api/mypage/courses/{id} | 본인 코스 상세. draft-번호 또는 course-번호 |
| GET | /api/mypage/reservations/{id} | 본인 예약 상세 |
| GET | /api/mypage/posts/{id} | 본인 게시글 상세 |
| POST | /api/mypage/courses/import | expectedMemberId와 courses로 임시 코스 연결 |

모든 API는 인증이 필요하다. 서버가 현재 로그인 계정의 ACTIVE·미탈퇴 상태를 다시 확인한다. X-MyPage-Member 조회 헤더와 expectedMemberId는 권한을 부여하는 값이 아니라 로그인 도중 계정 전환을 감지하는 일치 검사값이다. 실제 소유자는 로그인에서 결정한다. 타인·없는 자료는 같은 404를 반환한다. POST는 기존 `/api/auth/csrf` 토큰이 필요하다.

ChatSecurity의 catch-all 체인은 1단계에서 정리했다가 사용자 요청으로 되돌렸다. 현재 보안 설정의 최종 적용은 실제 서버 통합 검증에서 확인해야 한다. 2단계에서는 이 파일을 변경하지 않는다.

## 비회원·로그인 동작

- App 메모리에만 비회원 코스를 보관한다. hash 페이지 이동·가입·로그인 화면 이동에서는 유지한다.
- 새로고침, 탭 종료, 외부 사이트 이탈(pagehide) 시 폐기한다. 뒤로가기 캐시로 돌아와도 임시 코스는 복원하지 않는다.
- 비회원은 마이페이지 홈·내 코스만 이용한다. 회원 전용 주소 직접 진입은 로그인 안내다.
- 회원가입은 기존처럼 자동 로그인하지 않는다. 같은 페이지 생명주기에서 로그인하면 연결한다.
- 첫 연결을 시도한 회원에게 초안 소유권을 고정한다. 실패 후 다른 계정으로 로그인해도 그 계정으로 자동 전송하지 않는다. 원래 계정으로 돌아와 재시도한다.
- 저장 성공 응답 후에만 임시 코스를 비운다. 응답이 유실되면 동일 client_id로 재시도한다.
- 계정 전환 중 이전 계정 응답은 화면에서 폐기한다. 정지·탈퇴 계정의 API 접근도 차단한다.
- 현재 임시 코스 입력은 직접 작성 초안의 보관이다. AI 일정 생성·취소·환불·공개 게시의 저장은 후속 단계다.

## 확인

프론트 상태 테스트는 빌드 없이 `frontend`에서 `node src/pages/mypage/tests/verify.mjs`로 실행한다.

백엔드의 MyPageAccessTest는 실제 보안 체인·컨트롤러와 모의 Repository로 비로그인·타인 자료·계정 전환·CSRF를 검사한다. 사용자 요청에 따라 이번에는 Java 컴파일·Gradle 테스트·프론트 빌드를 실행하지 않았다. 실제 DB·브라우저 통합 검증도 별도로 필요하다.

## 2단계: 내 정보·여행 스타일

프로필은 `mypage/profile`의 controller / dto / service / repository / entity로, 여행 취향은 `mypage/preferences`의 controller / dto / service / repository로 분리했다.

| 메서드 | 경로 | 기능 |
| --- | --- | --- |
| GET | /api/mypage/profile | 본인 아이디·닉네임·이메일·프로필 이미지 조회 |
| PUT | /api/mypage/profile | 닉네임·이메일·이미지와 선택적 비밀번호 변경 |
| GET | /api/mypage/preferences | 본인 기본 취향·선택 태그·태그 목록 조회 |
| PUT | /api/mypage/preferences | 기본 취향과 태그별 선호·기피·강도 저장 |

GET은 선택적으로 `X-MyPage-Member`, PUT은 필수 `expectedMemberId`와 기존 인증 CSRF 헤더를 사용한다. 회원 ID는 로그인으로 결정한다. 프로필 요청에 loginId를 받지 않으며 아이디는 변경하지 않는다. 이메일 중복은 409, 잘못된 현재 비밀번호·새 비밀번호·이미지는 400이다. 비밀번호는 BCrypt로 저장하고 응답에 포함하지 않는다. 비밀번호 변경 후 현재 로그인 상태는 유지하며 다른 기기의 기존 세션을 일괄 만료시키는 기능은 별도다.

프로필 이미지용 새 테이블 `mypage_member_profile_image`는 엔티티의 스키마 자동 갱신 대상이다. 직접 스키마를 관리하면 `profile_image.sql`을 연결 DB 확인 후 적용한다. 이번에는 DB에 SQL을 적용하거나 서버를 빌드·재시작하지 않았다.

취향은 기존 `member_preference`, `member_preference_tag`, `preference_tag` 테이블을 그대로 사용한다. 태그가 비어 있어도 기본 카탈로그를 조회할 수 있고, 실제 저장 시 선택한 기본 태그만 등록한다. 기존 태그도 조회 목록에 유지한다. 같은 태그 중복·알 수 없는 태그·강도 범위 밖 값은 거부한다. 같은 회원 행을 잠그고 설정·선택 태그를 트랜잭션으로 저장한다. 예약·코스·확정 조건 테이블은 변경하지 않는다.

`ProfileAndPreferenceTest`에는 현재 비밀번호 오류 시 무변경, 비밀번호 해시 저장, 잘못된 이미지 차단, 회원 불일치, 태그 검증 테스트를 추가했다. Java 테스트는 빌드 금지 요청에 따라 실행하지 않았다. 사용자 요청으로 되돌린 `chat/ChatSecurity.java`는 2단계에서 수정하지 않았다.
