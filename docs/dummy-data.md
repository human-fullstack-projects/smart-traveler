# 팀 공통 더미 데이터

서버 시작 시 `src/main/resources/dummy/`의 JSON을 각 팀원 PC의 로컬 DB에 적재합니다. 새 행은 한 번만 넣습니다. 이미 적재한 TourAPI 숙소 중 `dummy_owner_<원본 ID>` 계정 소유인 숙소 정보와 `[예시 객실·가격]` 표시가 있는 객실 3개는 JSON 값으로 갱신합니다. 객실은 기존 객실 ID 순서대로 새 JSON의 객실 순서와 맞춥니다. 다른 업주가 소유한 숙소와 객실은 수정하지 않습니다. 관광지와 계정은 기존처럼 누락된 행만 추가합니다. 실제 DB를 공유할 필요가 없습니다.

- 숙소: 한국관광공사 TourAPI의 실제 이름·주소·좌표·연락처·사진. 업주 계정과 승인 프로필, 숙소당 객실 3개를 만듭니다. **업주 사업자번호·연락처와 객실 구성·가격은 전부 예시**이며 실제 예약 상품이 아닙니다.
- 관광지: TourAPI 관광지. 기존 관광지 동기화 자료와 같은 `KTO_TOURAPI` 원본 ID면 건너뜁니다.
- 여행자: `dummy_user_1`~`dummy_user_5`. 업주: `dummy_owner_<TourAPI 숙소 contentid>`.
- 테스트 계정 공통 비밀번호: `Demo1234!` (BCrypt 저장). 로컬 개발용 계정이므로 공개 서버에서 이 데이터 적재를 끄세요.

끄기: `application.properties`의 `app.dummy-data.enabled=false`로 변경합니다. 재생성: 프로젝트 최상위에서 `D:\miniconda3\python.exe -I -X utf8 도구\더미데이터\make_dummy.py --offline`을 실행하면 저장된 응답만 읽습니다. 필요한 캐시가 하나라도 없으면 오류로 중단하고 기존 JSON을 보존합니다. 온라인 재생성은 `--offline`을 빼고 실행하며 `TOUR_API_KEY` 환경변수 또는 `%USERPROFILE%\.tourapi_key.txt` 첫 줄을 사용합니다. 키는 JSON·캐시에 쓰지 않습니다. API 호출은 실행당 최대 900회이며 성공한 응답은 `도구\더미데이터\cache\`에 저장해 재실행 시 재사용합니다. 생성된 JSON을 팀 저장소에 포함해야 팀원에게 같은 자료가 들어갑니다.

출처: 한국관광공사 [TourAPI KorService2](https://api.visitkorea.or.kr/). 사진 등 개별 자료의 이용 조건은 한국관광공사 제공 조건을 확인하세요.
