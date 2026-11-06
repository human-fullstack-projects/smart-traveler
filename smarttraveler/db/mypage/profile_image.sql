-- 기존 members 테이블이 있는 개발 DB에서 적용한다. 기존 회원 컬럼·데이터는 변경하지 않는다.
CREATE TABLE IF NOT EXISTS mypage_member_profile_image (
    member_id BIGINT NOT NULL PRIMARY KEY,
    avatar_data LONGTEXT NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mypage_profile_image_member FOREIGN KEY (member_id) REFERENCES members(member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
