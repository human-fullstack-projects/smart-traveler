-- 기존 전체 스키마를 준비한 뒤, Spring이 연결할 개발 DB에서 실행한다.
-- AI 생성/예약 확정 코스와 구분되는 회원 소유 임시 코스다.
CREATE TABLE IF NOT EXISTS mypage_course_draft (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    client_id VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    region VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    days_json JSON NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_mypage_draft_owner_client (member_id, client_id),
    CONSTRAINT fk_mypage_draft_member FOREIGN KEY (member_id) REFERENCES members(member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
