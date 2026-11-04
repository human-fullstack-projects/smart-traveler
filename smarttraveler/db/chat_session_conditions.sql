-- 기존 대화 세션과 1:1로 연결되는 여행 조건이다.
-- 서버가 사용하는 DB를 확인한 뒤 한 번 실행한다. 기존 채팅 데이터는 변경하지 않는다.
CREATE TABLE IF NOT EXISTS chat_session_conditions (
    session_id BIGINT NOT NULL PRIMARY KEY,
    conditions_json JSON NOT NULL,
    confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT FK_chat_session_conditions_session FOREIGN KEY (session_id)
        REFERENCES chat_sessions (session_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
