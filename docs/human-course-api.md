# 휴먼 추천 코스 API

프론트는 `http://localhost:5173`에서 Vite 프록시를 통해 Spring Boot의 `/api`를 호출한다. 서버 연결에 실패하면 샘플 모드라고 표시하며, 서버 데이터와 브라우저 샘플 저장소를 섞지 않는다.

## 실행 준비

- `frontend/.env.example`을 `frontend/.env.local`로 복사하고 카카오 **JavaScript 키**를 설정한다. `.env.local`은 Git에서 제외된다. Vite를 실행 중이었다면 다시 시작해야 반영된다.
- 카카오 개발자 콘솔의 JavaScript SDK 도메인에 실제 접속 출처를 등록한다. 로컬 기본값은 `http://localhost:5173`이다.
- 백엔드는 MariaDB 연결과 `spring.jpa.hibernate.ddl-auto=update` 설정이 필요하다. 게시글의 `category`, `summary` 열은 엔티티 매핑으로 추가된다.
- 새 사진은 기본적으로 사용자 홈의 `.smart-traveler/human-course-photos/`에 저장된다. 이 경로는 `human-course.upload-dir` 속성으로 변경할 수 있다. 사진 파일은 Git으로 공유되지 않으므로 운영 환경에서는 지속되는 저장 경로를 지정해야 한다.

## 엔드포인트

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/api/human-courses` | 공개 글 목록 |
| GET | `/api/human-courses/{id}` | 글 상세·블라인드 상태 |
| GET | `/api/human-courses/eligibility` | 로그인 회원의 예약별 작성 가능 여부·최종 일정 |
| POST | `/api/human-courses/reservations/{id}` | 예약에 연결한 글 작성 |
| PUT / DELETE | `/api/human-courses/{id}` | 본인 글 수정 / 소프트 삭제 |
| POST | `/api/human-courses/{id}/like` | 좋아요 토글 |
| POST / DELETE | `/api/human-courses/{id}/comments` 및 `.../{commentId}` | 댓글 작성 / 본인 댓글 삭제 |
| POST | `/api/human-courses/{id}/reports` | 신고, 회원당 글별 1회 |

변경 요청에는 `/api/auth/csrf`에서 받은 CSRF 헤더가 필요하다. 사진은 최대 5장, 각 5MB(전체 최대 25MB)이며 백엔드에 파일로 저장한다. 기존 사진은 새 파일을 선택하지 않으면 유지된다.

예약은 `COMPLETED`이고 숙소 체크아웃 시각이 지난 뒤에만 작성할 수 있다. 연결된 본인 코스가 `done` 상태이고 일정이 있어야 한다. 확정 예약의 숙박 완료 전환은 서버 시작 시와 매시 0분·30분에 처리한다. 예약당 한 글만 허용하며 삭제한 글도 같은 예약으로 재작성할 수 없다. 글 삭제 후 일정·사진·댓글·좋아요·신고 기록은 감사 및 관계 유지를 위해 DB에 남지만 공개 조회에서 제외된다.

실제 작성 시험에는 로그인 회원 소유의 `room_reservation`과 연결된 `course`(상태 `done`), 그 코스의 `course_day`·`activity` 일정이 필요하다. 관리자 샘플 SQL의 별도 예시 예약과 프론트의 R101 샘플은 이 API의 작성 자격 데이터가 아니다. 서버에서 목록이 비어 있다면 이 연결을 먼저 확인한다.
