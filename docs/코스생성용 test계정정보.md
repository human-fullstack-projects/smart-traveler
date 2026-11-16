# 코스 생성용 테스트 계정 정보 (RAG 코스 시드 전용)

RAG 코스 시드(`data/smart_traveler_rag_courses_1000_multiaccount.sql`)로 코스 1,000건을 만들 때, 소유자를 한 계정에 몰아넣지 않고 신규 테스트 계정 **100개**에 코스를 **10개씩** 나눠 배정했습니다. 비밀번호는 `docs/시험계정.md`의 다른 시험 계정들과 동일하게 **`123456`**입니다 (같은 bcrypt 해시 재사용).

| 항목 | 값 |
|---|---|
| 아이디 패턴 | `rag_seed_test_001` ~ `rag_seed_test_100` |
| 비밀번호 | `123456` (전 계정 동일) |
| 닉네임 패턴 | `RAG 테스트 001` ~ `RAG 테스트 100` |
| member_id 범위 | 159 ~ 258 |
| role_code | USER |
| status | ACTIVE |
| email | NULL |
| 생성 스크립트 | `data/smart_traveler_rag_courses_1000_multiaccount.sql` |

## 계정 ↔ 코스 매핑

계정 1명당 코스 10개씩, `course.id` 오름차순으로 순서대로 배정됩니다.

| 아이디 | member_id | 소유 course.id 범위 |
|---|---:|---|
| `rag_seed_test_001` | 159 | 1~10 |
| `rag_seed_test_002` | 160 | 11~20 |
| `rag_seed_test_003` | 161 | 21~30 |
| ... | ... | ... |
| `rag_seed_test_099` | 257 | 981~990 |
| `rag_seed_test_100` | 258 | 991~1000 |

공식: `member_id = 158 + n` (n = 1~100), 해당 계정의 `course.id` 범위 = `((n-1)*10+1)` ~ `(n*10)`

## 주의

- 이 계정들은 RAG 코스 시드 전용 테스트 계정이며, 실제 서비스 이용자가 아닙니다.
- `members.login_id`는 UNIQUE라서 시드 스크립트를 다시 실행하면 중복 오류로 막힙니다(의도된 동작, 중복 적재 방지).
- 비밀번호 해시는 `smarttraveler/src/main/resources/sample-data/auth_test_accounts.sql`에서 `admin`/`team_test` 계정에 쓰는 것과 동일한 해시를 그대로 재사용했습니다.
- 이 코스들은 `course`/`course_day` 테이블에만 들어있고 `activity` 테이블 행은 생성하지 않았습니다. 일정 전체는 `course.condition_meta`의 JSON `itinerary` 안에 들어있습니다.
