# 인증·관리자 서버 API (M55)

모든 응답은 JSON입니다. 세션 쿠키를 사용합니다. POST 전 `GET /api/auth/csrf`의 `headerName`과 `token`을 받아 요청 헤더에 넣으세요. 로그인과 로그아웃 뒤에는 토큰을 새로 받으세요. `/api/admin/**`와 기존 `/admin/**`는 `ADMIN` 역할만 허용하며 일반 회원은 403입니다.

## 인증

| Method | URL | 요청 | 응답 |
|---|---|---|---|
| GET | `/api/auth/csrf` | 없음 | `{ "headerName":"X-CSRF-TOKEN", "token":"..." }` |
| POST | `/api/auth/signup` | JSON, 아래 예시 | 201 `{ "memberId":11,"role":"USER","ownerReviewStatus":null }` |
| POST | `/api/auth/login` | form `loginId`, `password` | 200 `{ "authenticated":true }` |
| GET | `/api/auth/me` | 세션 | 회원 공개 필드, 미인증 401 |
| POST | `/api/auth/logout` | CSRF 헤더 | 204 |
| GET | `/api/auth/owner-review` | 로그인한 업주 세션 | 200 `{ "status":"REJECTED", "businessNumber":"...", "businessName":"...", "representativeName":"...", "businessAddress":"...", "businessPhone":"...", "history":[{"status":"REJECTED","comment":"...","reviewedAt":"...","createdAt":"..."}] }` |
| POST | `/api/auth/owner-review/reapply` | CSRF + JSON `{"businessNumber":"...","businessName":"...","representativeName":"...","businessAddress":"...","businessPhone":"..."}` | 200 위와 같은 형식, `status:"PENDING"`; 반려 상태가 아니면 400 |

회원가입 예시:

```json
{"role":"USER","loginId":"new_user","password":"Example1234","nickname":"여행자","email":"new_user@example.invalid","consents":[{"termsId":1,"agreed":true},{"termsId":2,"agreed":true}]}
```

업주는 `role:"OWNER"`와 `businessNumber`, `businessName`, `representativeName`, `businessAddress`, `businessPhone`을 추가합니다. 가입과 동시에 `owner_profiles.review_status_code=PENDING`이 됩니다. 약관 ID는 현재 시행 중인 `terms` 행의 ID를 사용합니다. 필수 약관에 동의하지 않으면 400, 아이디·이메일·사업자번호 중복이면 409입니다. 비밀번호는 현재 임시 규칙으로 영문·숫자를 포함한 8~72자이며 BCrypt로 저장합니다. 닉네임·이메일 필수 여부와 비밀번호 상세 규칙은 팀 정책 확정이 필요합니다.

로컬 시험 계정은 `smarttraveler/src/main/resources/sample-data/auth_test_accounts.sql`을 **`smart_travel_db`에만** 실행해 넣습니다. `team_test` / `123456`은 USER, `m55_admin` / `123456`은 ADMIN입니다. SQL에는 BCrypt 해시만 있습니다. 이 파일은 로컬 테스트 약관 2건도 등록합니다. 실제 서비스 계정으로 사용하지 마세요.

## 관리자

목록 GET은 JSON 배열, 상세 GET은 관련 자료를 포함한 JSON 객체를 반환합니다. 조회 필터는 선택 사항입니다.

| Method | URL | 기능·주요 요청 |
|---|---|---|
| GET | `/api/admin` 또는 `/api/admin/dashboard` | 회원·주문·숙소·휴먼 코스·대기 업주·미처리 신고 수 |
| GET | `/api/admin/orders` | `status,type,query,lodgingId,from,to` |
| GET | `/api/admin/orders/{id}` | 예약, 결제, 결제 시도, 처리 이력 |
| POST | `/api/admin/orders/{id}/status` | `{"status":"CONFIRMED","reason":"..."}` |
| GET | `/api/admin/lodgings` | `status,ownerId,region,query` |
| GET | `/api/admin/lodgings/{id}` | 숙소, 객실, 예약 |
| POST | `/api/admin/lodgings/{id}/status` | `{"status":"ACTIVE","reason":null}` |
| POST | `/api/admin/lodgings/{id}/edit` | `{"name":"...","address":"...","description":"..."}` |
| POST | `/api/admin/lodgings/{id}/delete` | CSRF + JSON `{"reason":"..."}`; 200 `{"status":"DELETED","message":"숙소를 삭제했어요."}` 또는 진행 중 예약이 있으면 `{"status":"SUSPENDED","message":"진행 중인 예약이 있어 삭제 대신 운영을 중지했어요."}` |
| GET | `/api/admin/members` | `role,status,query,from,to`; 비밀번호 제외 |
| GET | `/api/admin/members/{id}` | 회원, 업주 정보·심사·숙소·정지 이력 |
| POST | `/api/admin/members/{id}/status` | `{"status":"SUSPENDED","reasonCode":"OTHER","reason":"..."}`; `ACTIVE`, `WITHDRAWN` 지원 |
| GET | `/api/admin/owners/pending` | 승인 대기 업주 |
| POST | `/api/admin/owners/{id}/review` | `{"decision":"APPROVED","comment":"..."}` |
| GET | `/api/admin/human-courses` | `status,reportStatus,query` |
| GET | `/api/admin/human-courses/{id}` | 글, 신고, 처리 이력, 추천 노출 여부 |
| GET | `/api/admin/human-reports` | 전체 신고 |
| POST | `/api/admin/human-courses/{id}/status` | `{"status":"BLINDED","reason":"..."}` |
| POST | `/api/admin/human-courses/{id}/feature` | `{"featured":true}` |
| POST | `/api/admin/human-reports/{id}/resolve` | `{"status":"RESOLVED","reason":"..."}` |
| GET | `/api/admin/ai-courses` | `status,query` |
| GET | `/api/admin/ai-courses/{id}` | 코스·연결 예약 |
| GET | `/api/admin/ai-courses/errors` | 생성 실패 코스 |
| GET | `/api/admin/ai-courses/criteria` | 추천 기준 |
| POST | `/api/admin/ai-courses/criteria/{id}` | `{"value":"..."}` |
| GET | `/api/admin/tour-data` | `region` 기본값 `47130`: 지역·건수·중심 순위·수집 로그 |
| POST | `/api/admin/tour-data/sync` | `{"region":"47130"}`; 수집 결과 |

조회는 200, 권한 부족은 403, 잘못된 처리 상태는 400입니다. 예약 확정은 결제 최종 상태·금액과 `reservation_inventory`의 유효 점유를 확인한 뒤 재고 수량을 옮깁니다. 기존 Thymeleaf `/admin/**`도 같은 서비스 메서드를 사용합니다.

관리자 전용 표는 `admin_reservation_action_history`, `admin_human_action_history`, `admin_recommendation_criteria`, `admin_recommendation_criteria_history`, `admin_human_featured`, `place_hub_rank`입니다. 팀 ERD 표에는 새 칸을 추가하지 않습니다.
