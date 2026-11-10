# 토스 테스트 결제 모듈

## 예약 없이 실행하는 1,000원 데모

메인 화면의 `토스 1,000원 테스트 결제` 버튼은 서버 예약과 별개인 임시 주문을 만듭니다. 서버는 상품명과 금액을 메모리에 30분 보관하고, 성공 URL에서 돌아온 값이 원래 주문과 같을 때만 토스 승인 API를 호출합니다. 서버를 재시작하면 아직 완료하지 않은 데모 주문은 사라집니다. 예약·객실 재고·환불에는 영향을 주지 않습니다.

사용자가 제공한 문서용 테스트 키는 Git이 무시하는 `smarttraveler/toss-test.properties`에 로컬로만 저장합니다. 백엔드를 다시 시작해야 설정이 반영됩니다. Vite는 `npm.cmd run dev`로 실행합니다. 메인 화면에서 데모 버튼을 누르고, 주문서에서 결제수단을 선택한 뒤 `토스로 테스트 결제`를 누릅니다.

현재 구현 범위: 로그인한 회원의 서버 예약(`PENDING_PAYMENT`)을 대상으로 결제 준비, 토스 주문서형 SDK v2 테스트 결제, 서버 승인, PG 결제 조회를 제공합니다. 결제금액은 `room_reservation.total_price`를 사용하고, 승인 전 객실의 날짜별 `HELD` 점유와 10분 만료를 확인합니다. 토스 응답의 주문번호·금액·결제 키가 저장된 값과 일치할 때만 결제를 기록합니다.

## 설정

백엔드 실행 환경에 다음 값을 설정합니다. 두 키는 같은 토스 테스트 상점의 주문서형 연동 키여야 합니다.

```text
TOSS_TEST_CLIENT_KEY=test_gck_...
TOSS_TEST_SECRET_KEY=test_gsk_...
```

실제 키 값은 저장소나 프런트엔드 환경 파일에 기록하지 않습니다. 클라이언트 키만 로그인 사용자의 결제 준비 응답으로 전달합니다. 테스트 키 외에는 결제 준비·승인이 동작하지 않습니다.

1. 서버에 예약과 날짜별 `reservation_inventory`의 유효한 `HELD` 점유가 있어야 합니다.
2. 로그인 후 `내 예약`의 결제 대기 항목에서 `토스 테스트 결제`를 선택합니다.
3. 결제창에서 테스트 결제를 마치면 성공 URL의 값을 서버가 검증하고 토스 승인 API를 호출합니다.
4. 결과가 불확실하면 `결제 상태 다시 조회`로 토스 결제 상태를 조회합니다.

## API

| 요청 | 용도 |
| --- | --- |
| `POST /api/payments/reservations/{id}/prepare` | 내 예약의 결제 주문 준비 |
| `POST /api/payments/confirm` | `{orderId,paymentKey,amount}`로 승인 및 예약 확정 |
| `POST /api/payments/reservations/{id}/sync` | `paymentKey` 기준 PG 상태 재조회 |
| `GET /api/payments/reservation/{id}` | 본인 결제 조회 |

모든 변경 요청에는 로그인 세션과 CSRF 토큰이 필요합니다.

## 남은 연결점

- 현재 숙소 예약 화면은 정적 샘플이며 서버 예약 생성 API가 없습니다. 따라서 이 화면의 예약 버튼만으로 결제 가능한 주문이 생성되지는 않습니다. 서버에서 생성된 결제 대기 예약으로만 실제 테스트 흐름을 확인할 수 있습니다.
- 취소·부분 환불은 숙소별 환불 비율과 부분 취소 단위가 WBS에서 미확정이므로, 임의 환불을 수행하지 않습니다.
- 토스 웹훅 수신은 보안 키에 의한 서명 검증, 이벤트 중복 처리, PG 조회 후 반영이 함께 필요합니다. 현재는 로그인 사용자의 조회 API로 응답 유실을 복구합니다.
- 승인 뒤 점유가 만료됐다면 결제 상태와 예약 상태를 분리해 두며, 운영자가 재고·환불을 확인해야 합니다. 자동 확정하지 않습니다.

토스 문서: [주문서형 연동](https://docs.tosspayments.com/guides/v2/payment-widget/integration), [코어 API](https://docs.tosspayments.com/reference), [웹훅](https://docs.tosspayments.com/reference/using-api/webhook-events).
