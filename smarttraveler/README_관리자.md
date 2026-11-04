# 관리자 페이지 M51

작업용 복사본입니다. 팀 원본 저장소에는 반영하지 않았습니다. PC 브라우저에서 `/admin`으로 접속합니다.

## 실행

Java 21과 로컬 MariaDB `smart-travel-db`가 필요합니다. PowerShell에서 이 폴더로 이동한 뒤 실행합니다. DB 비밀번호는 `%USERPROFILE%\.team_db.txt`의 `password=` 줄에서 런타임에만 읽습니다.

```powershell
$line = Get-Content -LiteralPath (Join-Path $env:USERPROFILE '.team_db.txt') | Where-Object { $_ -like 'password=*' } | Select-Object -First 1
$env:SPRING_DATASOURCE_PASSWORD = $line.Substring(9)
$env:SPRING_DATASOURCE_URL = 'jdbc:mariadb://localhost:3306/smart-travel-db?characterEncoding=UTF-8&serverTimezone=Asia/Seoul'
$env:LOGGING_LEVEL_ORG_HIBERNATE_ORM_CONNECTIONS_POOLING = 'ERROR'
$env:GRADLE_USER_HOME = 'C:\Users\human\Desktop\3차프로젝트\03_제작작업\실제페이지_20261030\.gradle_home'
.\gradlew.bat --offline bootRun
```

처음 실행하면 기존 `spring.jpa.hibernate.ddl-auto=update` 설정이 관리자 표를 생성합니다. 샘플 데이터는 자동 삽입하지 않습니다. 기동 뒤 `src/main/resources/sample-data/admin_sample.sql`을 **`smart-travel-db`에만** 수동 실행합니다. 로컬의 기존 예약 표에는 최신 ERD와 달리 `lodging_id NOT NULL` 칸이 남아 있어 샘플 SQL에 해당 칸을 넣었습니다. `admin_local_schema_fix.sql`은 이 로컬 DB의 숙소 TEXT 칸 두 개를 ERD 타입으로 되돌리는 파일이며 이 PC에서 실행했습니다. 다른 DB에는 실행하지 마세요.

이 Codex 샌드박스에서는 `gradlew --offline compileJava`가 클래스 파일 생성 후 JDK ZipFS의 Gradle 캐시 JAR `AccessDeniedException`으로 실패 처리됐습니다. 클래스 생성 시각을 확인하고 `gradlew --offline bootRun -x compileJava --args='--server.port=18080'`으로 실제 기동·HTTP·DB 저장을 확인했습니다. Claude가 일반 권한 터미널에서 `compileJava`를 다시 확인해야 합니다.

## 가짜 계정

모든 샘플 계정의 비밀번호는 `Admin123!`입니다. 실제 계정은 넣지 않았습니다.

| 아이디 | 역할 | 용도 |
|---|---|---|
| `m51_admin` | ADMIN | 관리자 로그인 |
| `m51_user1` ~ `m51_user5` | USER | 목록·403 확인 (`user3`은 정지 상태) |
| `m51_owner1` ~ `m51_owner3` | OWNER | 승인·대기·반려 상태 |

## 구현 범위

| WBS | 상태 | 내용 |
|---|---|---|
| ADM-01, ADM-15 | 완료 | 대시보드, 관리자 권한, 로그인, 403, 접근 로그 `logs/admin-access.log` |
| ADM-02 | 완료 | 주문 유형·상태·숙소·기간·회원/번호 검색 |
| ADM-03~04 | 부분 | 주문 상세, 저장된 결제·점유 확인 후 확정, 미결제 취소, 상태 이력과 동시 수정 버전. 실제 Toss 조회·환불 호출은 없음. 환불 중/실패는 관리자 운영 표시이며 `payment.status`는 임의 변경하지 않음 |
| ADM-05~06 | 완료 | 숙소 목록·상세·객실·예약, 승인·반려 사유·운영 중지/재개 |
| ADM-07 | 부분 | 숙소명·주소·소개 수정. 삭제 기능은 미구현 |
| ADM-08~10 | 완료 | 회원 검색·유형, 업주 사업자 정보·숙소·심사 이력, 승인·반려 |
| ADM-11 | 부분 | 회원 정지·해제·강제 탈퇴와 이력. 자동 해제 및 정지 업주 숙소의 예약 차단 연계는 미구현 |
| ADM-12~13 | 부분 | 휴먼 코스·신고 조회, 필터, 신고 처리, 코스 블라인드·해제·소프트 삭제. 댓글 표는 ERD에 없음 |
| ADM-14 | 완료 | 휴먼 코스 관리자 처리 이력 |
| ADM-19 | 부분 | 메인 노출 지정·해제. 지역별·월간 인기 선정은 미구현 |
| ADM-16 | 부분 | AI 코스 목록·상세·상태·오류. 여행계획 버전/흐름과 승인 이력 통합은 미구현 |
| ADM-17 | 부분 | 생성 실패 목록. 재생성·외부 데이터 재수집은 미구현 |
| ADM-18 | 부분 | 기본 추천 기준 저장·변경 이력. 모든 추천 조건과 실제 생성기 연동은 미구현 |
| AUTH-02 | 부분 | 업주 승인·반려와 심사 이력. 업주 등록 화면의 403 차단 연계는 별도 담당 화면이 없어 미구현 |

ERD에 없는 휴먼 코스·신고·처리 이력, 관리자 예약 상태 이력, 추천 기준·변경 이력은 `admin_*` 표로 분리했습니다. 팀 ERD 확정 시 이 표들의 정식 소유와 관계를 결정해야 합니다.

## 확인

`03_제작작업/실제페이지_20261030/확인/verify_admin_http.py`는 조회 테스트입니다. `verify_admin_actions.py`와 `verify_refund_mark.py`는 **샘플 행을 실제로 변경하는 일회성 테스트**이며 이미 이 PC에서 실행했습니다. 재실행 전 샘플 상태를 확인하세요. 1440px 화면 캡처는 Windows 브라우저 제어가 차단되어 생성하지 못했습니다.

## 10-30 추가 (Claude · Codex 한도로 직접)

### 화면 다듬기
- 상태 코드 → 한글(`admin/web/AdminFormat.java`, 템플릿에서 `${@fmt.label(...)}`), 날짜 → `yyyy-MM-dd HH:mm`, 생성 방식 → 이름, 숙소의 업주 → 상호명. DB 값은 그대로.
- 시연 상태 되돌리기: `sample-data/admin_demo_reset.sql` (`admin_sample.sql` 다음에 실행 · 여러 번 실행 가능) → 업주 가입 승인대기 1(m51_owner4) · 숙소 승인대기 1 · 결제대기 주문 1 · 신고 대기 1.

### 관광지 데이터 수집 (WBS 「관광지 연관·중심 정보 연동」 EXT-01·EXT-07)
- 화면: `/admin/tour-data` (AI 코스 관리 화면의 「관광지 데이터」 칸에서 이동) · 지역 선택 → 「관광지 데이터 수집」.
- 표 만들기: 처음 한 번 `sample-data/tour_schema.sql` 실행 (region·region_external_code·place·place_image·data_sync_log = 팀 ERD 칸 그대로, `place_hub_rank`·`place_related` = ERD에 없는 순위·연관 정보 → 팀 합칠 때 결정). 지원 지역은 처음엔 경주시(47130)만.
- 코드: `tour/TourApiClient.java`(호출·실패 1번 재시도) · `tour/TourSyncService.java`(저장·이름 연결·수집 이력) · `tour/TourAdminController.java`.
- 서비스키: 환경변수 `TOUR_API_KEY` 또는 `%USERPROFILE%\.tourapi_key.txt` 첫 줄. 파일·로그에 키를 쓰지 않음.
- 한 번 실행 ≈ API 10회 (개발 계정 하루 1,000회). 실패 단계는 `data_sync_log`에 「미반영」, 기존 자료는 지우지 않음.
