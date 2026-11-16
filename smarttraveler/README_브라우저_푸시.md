# 브라우저 알림 테스트

메인 화면의 토스 1,000원 테스트 결제 바로 아래에 `즉시 알림 받기`와 `1분 후 알림 받기` 버튼이 있습니다. 로그인하지 않아도 테스트할 수 있으며, POST 요청에는 기존 세션 CSRF 검증을 그대로 적용합니다.

## 실행

1. `smarttraveler` 디렉터리에서 Spring Boot 서버를 실행합니다.
2. `frontend` 디렉터리에서 `npm run dev`를 실행하고 `http://localhost:5173` 또는 `http://127.0.0.1:5173`에 접속합니다. 개발 API 프록시를 그대로 사용합니다.
3. 버튼을 누르고 브라우저 알림 권한을 허용합니다.
4. 1분 후 알림의 예약 완료 문구가 나오면 사이트 탭을 닫아도 됩니다. 서버가 60초 뒤 푸시 서비스로 전송하며, 서비스 워커가 알림을 표시합니다. 알림을 클릭하면 메인 화면으로 이동합니다.

로컬 개발에는 별도 서비스 가입이나 키 발급이 필요하지 않습니다. 서버가 `.local/web-push/vapid.properties`에 VAPID 키를 최초 한 번 생성하고 재사용합니다. 이 디렉터리는 Git에서 제외합니다. 비밀 키와 브라우저 구독 정보는 공유하지 마세요. 키 파일은 실행 디렉터리를 기준으로 하므로 위 실행 위치를 유지하거나 `WEB_PUSH_KEY_FILE`을 절대 경로로 지정하세요.

## 배포 설정

- 프런트엔드는 HTTPS로 제공하고 `public/push-sw.js`가 빌드 결과의 `/push-sw.js`에서 JavaScript로 응답하도록 합니다. SPA HTML fallback이나 로그인 화면으로 대체되면 안 됩니다.
- 프런트엔드와 같은 출처의 `/api`를 Spring Boot에 프록시합니다.
- `WEB_PUSH_PUBLIC_KEY`, `WEB_PUSH_PRIVATE_KEY`: URL-safe Base64 VAPID 키 한 쌍입니다. 둘 다 생략하면 로컬 키 파일을 사용합니다. 여러 인스턴스에서는 같은 키를 사용하세요.
- `WEB_PUSH_SUBJECT`: 운영 담당자의 실제 `mailto:` 주소 또는 HTTPS 연락처 URL로 설정하세요. 기본 `mailto:push@example.com`은 로컬 테스트용입니다.
- `WEB_PUSH_KEY_FILE`: 환경변수 키가 없을 때 사용할 영속 키 파일 경로입니다. 배포 시 키를 유지해야 기존 브라우저 구독을 재사용할 수 있습니다.
- 서버는 Google FCM, Mozilla Push, Apple Web Push, Windows Push의 HTTPS 엔드포인트에 접근할 수 있어야 합니다. 그 외 호스트, 사용자 정보가 있는 URL, 별도 포트 등은 거부합니다.

## 테스트 범위와 제한

- 실제 Web Push입니다. 브라우저 페이지의 타이머나 `new Notification()`으로 지연 알림을 흉내 내지 않습니다.
- 예약/구독 정보는 테스트 작업에 필요한 동안만 서버 메모리에 유지합니다. 예약은 서버 재시작 시 취소되며 여러 서버 간 공유되지 않습니다. 실제 서비스 연동 시 회원별 구독 저장소와 영속 작업 큐를 추가하세요.
- 서버는 테스트 결과를 10분 동안 조회할 수 있게 하고 다음 요청에서 만료 기록을 정리합니다. 결과는 요청한 세션에서만 조회할 수 있습니다. 최대 500개 기록, 짧은 연속 요청 제한, 중복 예약 방지를 적용합니다.
- `SENT`는 푸시 서비스가 요청을 수락했다는 뜻이며 기기에 표시되었다는 보장은 아닙니다. 브라우저 완전 종료, 네트워크 단절, 절전, OS 알림 설정에 따라 지연될 수 있습니다. 메시지 보관 시간은 5분입니다.
- Chrome/Edge 데스크톱부터 확인하세요. iOS/iPadOS에서는 홈 화면에 추가한 웹 앱 등 플랫폼별 요구사항이 추가로 필요할 수 있으며 현재 데모는 PWA 설치 UI를 제공하지 않습니다.
- 화면을 새로고침해도 같은 탭의 예약 결과를 다시 확인합니다. 화면을 떠나거나 탭을 닫아도 서버 예약은 유지됩니다.

## API

- `GET /api/push/public-key`: 공개 VAPID 키만 반환합니다.
- `POST /api/push/tests`: `{ subscription: PushSubscription.toJSON(), delaySeconds: 0 | 60 }`.
- `GET /api/push/tests/{id}`: `SCHEDULED`, `SENT`, `FAILED`, `EXPIRED` 상태 및 안내 문구를 반환합니다.

참고: [MDN Push API](https://developer.mozilla.org/en-US/docs/Web/API/Push_API), [webpush-java](https://github.com/web-push-libs/webpush-java).
