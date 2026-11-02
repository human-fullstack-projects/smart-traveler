# 로그인 API (sein/v1)

## 실행

1. IntelliJ에서 smarttraveler 폴더의 Spring Boot 프로젝트를 열고 SmarttravelerApplication 실행.
2. React 실행 터미널에서 Ctrl+C로 기존 실행을 종료한 뒤 frontend에서 npm run dev 재실행. Vite의 API 프록시 설정이 추가됐으므로 한 번 재실행해야 합니다.
3. 크롬에서 Vite가 표시한 주소를 열고 로그인 클릭.
4. DB에 있는 ACTIVE 상태의 회원으로 로그인. members.password에는 BCrypt로 저장된 비밀번호가 필요합니다. 직접 입력한 평문 비밀번호는 인증되지 않습니다.
5. 로그인 성공 → 홈 이동 및 닉네임/로그아웃 표시 → 새로고침해도 로그인 유지 → 로그아웃 확인.

기본 연결은 React /api → http://127.0.0.1:8080 입니다.
서버 포트가 다르면 React 실행 전 PowerShell에서 $env:SMART_TRAVEL_API_TARGET='http://127.0.0.1:포트번호'를 설정합니다.
개발용 Vite 프록시는 배포 빌드에 포함되지 않습니다. 배포 시 /api 경로를 Spring Boot 서버로 연결해야 합니다.

## API 계약

| 요청 | 기능 | 응답 |
|---|---|---|
| GET /api/auth/csrf | 변경 요청 보호 토큰 발급 | headerName, token |
| POST /api/auth/login | 회원 인증 | 200 authenticated:true / 401 오류 |
| GET /api/auth/me | 현재 회원 조회 | memberId, loginId, nickname, roleCode / 401 |
| POST /api/auth/logout | 세션 종료 | 204 |

로그인 본문은 application/x-www-form-urlencoded 형식의 loginId, password입니다.
POST 요청 전에 csrf를 조회하고 반환된 headerName 이름으로 token 값을 헤더에 넣습니다.
로그인/로그아웃 후 토큰이 바뀌므로 이전 토큰을 계속 재사용하지 않습니다.
React는 같은 출처 프록시를 통해 세션 쿠키를 사용합니다. 비밀번호나 로그인 토큰을 localStorage에 저장하지 않습니다.

## 현재 정책과 범위

- DB 비밀번호 컬럼명 password는 유지하고 값은 BCrypt 해시로 비교합니다.
- ACTIVE이고 deleted_at이 비어 있는 회원만 인증합니다. USER/OWNER/ADMIN 역할은 DB에서 읽습니다.
- 로그인 실패는 없는 아이디/틀린 비밀번호/제한 계정에 공통 안내를 반환합니다.
- 로그인 성공 시 세션 번호 교체는 Spring Security가 처리합니다. 세션 무활동 만료는 임시 30분이며 팀 정책에 따라 조정할 수 있습니다.
- /me는 DB 상태를 다시 확인하며 정지/탈퇴된 회원의 세션을 종료합니다. 향후 다른 업무 API는 별도로 권한과 계정 상태 검사 정책을 적용해야 합니다.
- 현재 공개 허용: GET /api/auth/csrf, POST /api/auth/login, 루트/오류/기존 CSS·JS. 나머지 요청은 로그인 필요입니다. 팀원이 공공 조회 API를 추가하면 이 허용 목록을 함께 조정해야 합니다. 이 설정만으로 업주/관리자 기능별 권한 검사가 완성되지는 않습니다.
- 공용 DB 데이터, 엔티티 및 기존 application.properties는 변경하지 않았습니다. 기존 설정의 ddl-auto=update는 서버 시작 시 스키마에 영향을 줄 수 있으므로 팀에서 관리하세요.
- 회원가입 API와 테스트 계정 생성은 이번 변경에 포함되지 않습니다. 실제 DB에 BCrypt 회원이 없는 경우 성공 경로의 브라우저 테스트는 회원가입 작업 또는 팀의 테스트 계정 준비 후 가능합니다.
- 배포 전 HTTPS/보안 쿠키, 로그인 반복 시도 제한, 실제 DB를 통한 통합 검증을 추가해야 합니다.

## 검사

AuthFlowTest는 실제 Spring Security 필터와 BCrypt를 사용하고 저장소만 가짜로 대체합니다. DB를 사용하거나 변경하지 않습니다.

```powershell
cd smarttraveler
.\gradlew.bat test --tests com.smarttraveler.auth.AuthFlowTest
```

정상 로그인/새로고침용 세션 조회/잘못된 비밀번호/없는 회원/정지/탈퇴/평문 저장 거부/CSRF 누락/로그아웃/세션 교체를 검사합니다.
실제 MariaDB 및 브라우저 쿠키 환경을 통한 검증은 별도로 필요합니다.

공식 참고: https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/dao-authentication-provider.html
