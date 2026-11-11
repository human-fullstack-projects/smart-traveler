# 팀 공통 더미 데이터

서버 시작 시 DB 주소가 `localhost`, `127.0.0.1`, `::1`인 경우에만 `src/main/resources/dummy/`의 JSON을 자동 적재합니다. `APP_DUMMY_DATA_ENABLED=false`이면 적재하지 않습니다. 팀원 PC에서 코드를 받은 뒤 서버를 다시 켜면 적용됩니다.

- 숙소: 한국관광공사 TourAPI 145곳. 이름·주소·좌표·연락처·사진을 적재합니다. 지도 표시의 필수값은 **위도·경도·숙소 이름**이며 주소 열은 유지합니다. 업주 사업자번호·연락처, 객실 구성·가격은 예시입니다.
- 업주: `owner1`~`owner145`. `source_content_id` 숫자 오름차순의 고정 대응표 [owner_names.csv](../smarttraveler/src/main/resources/dummy/owner_names.csv)를 사용합니다. 닉네임과 대표자명은 서로 같은 가상 한국 이름이고, 비밀번호는 모두 `123456`(BCrypt 저장)입니다.
- 여행자: `user1`~`user5` / `123456`.
- 관광지: TourAPI 관광지. 같은 `KTO_TOURAPI` 원본 ID가 있으면 건너뜁니다.

기존 `dummy_owner_<원본 ID>`와 `dummy_user_<번호>` 계정은 서버 시작 때 새 ID·이메일·비밀번호로 이행합니다. 업주 소유권은 `OWNER` 권한, `DUMMY-<원본 ID>` 사업자번호, 예전 또는 새 **로그인 ID와 이메일**이 모두 맞을 때만 인정합니다. 닉네임이 기존 자동값 `숙소 예시 …`이면 새 이름으로, 대표자명이 `예시 업주`이면 새 이름으로 바꿉니다. 수동으로 바꾼 이름은 유지합니다. 목표 로그인 ID나 이메일이 이미 쓰이고 있으면 이행을 건너뛰고 경고합니다. 실제 업주는 수정하지 않습니다.

기존 TourAPI 숙소 중 위 소유권 조건을 만족하는 행은 JSON 값으로 갱신합니다. `[예시 객실·가격]` 표시가 있는 객실 3개만 갱신하고, 객실명이 기존 다른 객실과 겹치면 그 객실만 건너뜁니다. 재시작해도 계정을 중복 생성하지 않습니다.

팀원과 공유할 수동 이행 파일은 [dummy-owner-names.sql](dummy-owner-names.sql)입니다. **로컬 `smart_travel_db` 전용**이며 기존 계정 이름 이행만 수행합니다. 자동 적재를 켜 둔 서버에서는 SQL을 따로 실행할 필요가 없습니다. 실행 전 접속 DB를 확인하세요.

재생성: 프로젝트 최상위에서 `D:\miniconda3\python.exe -I -X utf8 도구\더미데이터\make_dummy.py --offline`을 실행하면 저장된 응답만 읽습니다. 필요한 캐시가 하나라도 없으면 오류로 중단하고 기존 JSON을 보존합니다. 온라인 재생성은 `--offline`을 빼고 실행하며 `TOUR_API_KEY` 환경변수 또는 `%USERPROFILE%\.tourapi_key.txt` 첫 줄을 사용합니다. 키는 JSON·캐시에 쓰지 않습니다. API 호출은 실행당 최대 900회이며 성공 응답은 `도구\더미데이터\cache\`에 저장해 재실행 시 재사용합니다. 생성된 JSON과 이름 대응표를 팀 저장소에 포함해야 팀원에게 같은 자료가 들어갑니다.

출처: 한국관광공사 [TourAPI KorService2](https://api.visitkorea.or.kr/). 사진 등 개별 자료의 이용 조건은 한국관광공사 제공 조건을 확인하세요.