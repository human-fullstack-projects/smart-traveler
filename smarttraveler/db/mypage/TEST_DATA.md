# 마이페이지 테스트 데이터

## 지역별 DB 장소로 일정 교체 (2026-11-17)

현재 로컬 user1의 예약 코스 6개와 저장된 여행 3개, 일정 장소 40건을 각 지역의 활성 `place` 자료로 교체했다. 장소명·ID·주소·위도·경도를 함께 저장하며 일정의 날짜와 방문 순서는 유지한다. 사진과 설명은 `place_image`·`place.description`의 기존 자료를 사용한다. 사용자 후기의 작성 당시 스냅샷은 별도 자료로 보존한다.

프로젝트 루트에서 아래 스크립트로 미리보기와 재적용을 할 수 있다. 적용 범위는 로컬 user1의 `MYPAGE-USER1-*` 시험 코스와 `mypage-user1-saved-*` 저장 자료로 제한된다.

```powershell
python smarttraveler/scripts/refresh_mypage_places.py
python smarttraveler/scripts/refresh_mypage_places.py --apply
```

적용 전 JSON 백업과 복구 SQL은 Git 제외 경로 `logs/mypage-place-backups/`에 생성한다. 이 작업은 예약의 날짜·금액·숙소와 다른 회원 자료를 변경하지 않는다. 아래의 기존 자료 생성 안내는 초기 시험 데이터 기준이며, 생성 후 위 스크립트를 실행하면 지역별 DB 장소로 교체된다.

로컬 개발 DB `smart_travel_db`의 여행자 계정 **user1**에 저장한 데이터입니다.
기존 로컬 테스트 계정 비밀번호는 `123456`입니다. 실제 숙박·결제·여행 후기가 아닙니다.

| 지역 | 저장된 여행 | 내 예약 | 내 여행 추천 |
| --- | --- | --- | --- |
| 서울 | [테스트] 서울 문화 산책 코스 | TEST-MYPAGE-USER1-20261113-1 | [테스트] 서울 문화 산책 추천 |
| 부산 | [테스트] 부산 바다 여행 코스 | TEST-MYPAGE-USER1-20261113-2 | [테스트] 부산 바다 여행 추천 |
| 제주 | [테스트] 제주 자연 여행 코스 | TEST-MYPAGE-USER1-20261113-3 | [테스트] 제주 자연 여행 추천 |

각 코스는 1박 2일, 방문 장소 5개로 구성됩니다. 예약은 `COMPLETED`, 추천은 `VISIBLE`입니다.
추천 글 수정과 공개 상세, 연결 예약·코스 이동을 확인할 수 있습니다.
모든 데이터는 user1 소유이며, 다른 계정의 마이페이지에는 표시되지 않습니다.

## 화면 확인

user1로 로그인한 뒤 선호도 조사가 나오면 저장을 완료합니다.

- 내 예약: `http://127.0.0.1:5173/#/reservations`
- 저장된 여행: `http://127.0.0.1:5173/#/my-courses`
- 내 여행 추천: `http://127.0.0.1:5173/#/my-posts`

## DB 식별

- 숙소: `source_provider='TEST'`, `source_content_id='MYPAGE-USER1-20261113-번호'`
- 코스: `condition_meta`와 `preferences_snapshot`의 `testData=true`, `fixture` 식별자
- 예약: `reservation_no`의 `TEST-` 접두사와 `request_memo`의 테스트 메타데이터
- 추천: 제목의 `[테스트]`, 본문의 테스트 안내, 연결 예약의 테스트 식별자

```sql
SELECT c.id, c.title, c.condition_meta, r.reservation_id, r.reservation_no,
       r.request_memo, p.post_id, p.title AS post_title
FROM course c
JOIN room_reservation r ON r.course_id=c.id
JOIN human_course_post p ON p.reservation_id=r.reservation_id
WHERE r.reservation_no LIKE 'TEST-MYPAGE-USER1-20261113-%';
```

## 재현과 검증

`test_fixtures_user1.sql`은 위 로컬 DB 전용이며 재실행 시 기존 테스트 데이터를 덮어쓰거나 중복 생성하지 않습니다.
테스트용 숙소·객실, 생성 방법·활동 분류도 함께 준비합니다.

frontend 폴더에서 아래 명령으로 실행 중인 서버·실제 DB·페이지 렌더링을 확인합니다.

```powershell
node src/pages/mypage/tests/mypage-fixtures-live-verify.mjs
```

2026-11-13 검증: 세 목록의 각 3개 표시, 코스 상세 일정, 예약·추천 상세,
공개 추천 상세, 본인 추천 수정 API, user2와의 소유권 분리 통과.
페이지는 실제 API 응답으로 React 컴포넌트를 렌더링해 확인했으며 브라우저 클릭 검증은 포함하지 않았습니다.
