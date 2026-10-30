-- 관광지 연동 표 (smart-travel-db 전용 · 지우는 줄 없음 · 여러 번 실행해도 됨)
-- region / place / place_image / data_sync_log = 팀 통합 ERD new_01(10-29) 칸 그대로.
-- place_hub_rank / place_related = ERD에 없는 순위·연관 정보 → 별도 표 (팀 ERD 합칠 때 결정).

CREATE TABLE IF NOT EXISTS region (
  region_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  parent_region_id BIGINT NULL,
  region_code VARCHAR(30) NOT NULL,
  region_name VARCHAR(100) NOT NULL,
  full_name VARCHAR(200) NOT NULL,
  region_level VARCHAR(20) NOT NULL,
  center_latitude DECIMAL(10,7) NULL,
  center_longitude DECIMAL(10,7) NULL,
  is_supported BOOLEAN NOT NULL DEFAULT TRUE,
  weather_grid_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_region_code (region_code)
);

CREATE TABLE IF NOT EXISTS region_external_code (
  region_external_code_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  provider VARCHAR(30) NOT NULL,
  external_code VARCHAR(100) NOT NULL,
  region_id BIGINT NULL,
  UNIQUE KEY uq_region_external (provider, external_code),
  CONSTRAINT fk_rec_region FOREIGN KEY (region_id) REFERENCES region(region_id)
);

CREATE TABLE IF NOT EXISTS place (
  place_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  region_id BIGINT NOT NULL,
  source_provider VARCHAR(30) NOT NULL,
  source_place_id VARCHAR(100) NOT NULL,
  place_type VARCHAR(30) NOT NULL,
  name VARCHAR(200) NOT NULL,
  description TEXT NULL,
  address VARCHAR(300) NULL,
  address_detail VARCHAR(200) NULL,
  latitude DECIMAL(10,7) NULL,
  longitude DECIMAL(10,7) NULL,
  phone VARCHAR(30) NULL,
  homepage_url VARCHAR(1000) NULL,
  opening_hours TEXT NULL,
  closed_days VARCHAR(500) NULL,
  is_indoor BOOLEAN NULL,
  recommended_minutes INT NULL,
  source_modified_at DATETIME NULL,
  last_synced_at DATETIME NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  verification_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED',
  UNIQUE KEY uq_place_source (source_provider, source_place_id),
  CONSTRAINT fk_place_region FOREIGN KEY (region_id) REFERENCES region(region_id)
);

CREATE TABLE IF NOT EXISTS place_image (
  place_image_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  place_id BIGINT NOT NULL,
  image_url VARCHAR(1000) NOT NULL,
  image_type VARCHAR(30) NULL,
  sort_order INT NOT NULL,
  source_provider VARCHAR(30) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_place_image (place_id, sort_order),
  CONSTRAINT fk_place_image_place FOREIGN KEY (place_id) REFERENCES place(place_id)
);

CREATE TABLE IF NOT EXISTS data_sync_log (
  sync_log_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  region_id BIGINT NULL,
  provider VARCHAR(30) NOT NULL,
  data_type VARCHAR(30) NOT NULL,
  status VARCHAR(20) NOT NULL,
  started_at DATETIME NOT NULL,
  finished_at DATETIME NULL,
  total_count INT NULL,
  success_count INT NULL,
  failure_count INT NULL,
  error_message TEXT NULL,
  CONSTRAINT fk_sync_region FOREIGN KEY (region_id) REFERENCES region(region_id)
);

-- EXT-07 기초지자체 중심 관광지: 기준 연월별 순위
CREATE TABLE IF NOT EXISTS place_hub_rank (
  hub_rank_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  region_id BIGINT NOT NULL,
  base_ym CHAR(6) NOT NULL,
  hub_code VARCHAR(100) NOT NULL,
  hub_name VARCHAR(200) NOT NULL,
  hub_rank INT NOT NULL,
  category_large VARCHAR(50) NULL,
  category_middle VARCHAR(50) NULL,
  place_id BIGINT NULL,
  synced_at DATETIME NOT NULL,
  UNIQUE KEY uq_hub (region_id, base_ym, hub_code),
  CONSTRAINT fk_hub_region FOREIGN KEY (region_id) REFERENCES region(region_id),
  CONSTRAINT fk_hub_place FOREIGN KEY (place_id) REFERENCES place(place_id)
);

-- EXT-01 관광지별 연관 관광지: 기준 관광지 → 연관 관광지 순위
CREATE TABLE IF NOT EXISTS place_related (
  related_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  region_id BIGINT NOT NULL,
  base_ym CHAR(6) NOT NULL,
  source_code VARCHAR(100) NOT NULL,
  source_name VARCHAR(200) NOT NULL,
  related_code VARCHAR(100) NOT NULL,
  related_name VARCHAR(200) NOT NULL,
  related_area_name VARCHAR(100) NULL,
  related_signgu_name VARCHAR(100) NULL,
  related_category VARCHAR(100) NULL,
  related_rank INT NOT NULL,
  source_place_id BIGINT NULL,
  synced_at DATETIME NOT NULL,
  UNIQUE KEY uq_related (base_ym, source_code, related_code),
  CONSTRAINT fk_related_region FOREIGN KEY (region_id) REFERENCES region(region_id),
  CONSTRAINT fk_related_place FOREIGN KEY (source_place_id) REFERENCES place(place_id)
);

-- 지원 지역 (처음엔 경주시만)
INSERT IGNORE INTO region(region_code, region_name, full_name, region_level, center_latitude, center_longitude, is_supported)
VALUES ('47', '경상북도', '경상북도', 'SIDO', NULL, NULL, TRUE),
       ('47130', '경주시', '경상북도 경주시', 'SIGUNGU', 35.8562000, 129.2247000, TRUE);
UPDATE region c JOIN region p ON p.region_code = '47' SET c.parent_region_id = p.region_id WHERE c.region_code = '47130';
INSERT IGNORE INTO region_external_code(provider, external_code, region_id)
SELECT 'KTO_LDONG', '47-130', region_id FROM region WHERE region_code = '47130';
