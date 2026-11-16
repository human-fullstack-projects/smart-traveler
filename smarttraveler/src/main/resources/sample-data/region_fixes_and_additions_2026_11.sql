-- 2026-11 대구/여수/목포/광주/포항/인천/충북 place 적재를 위한 region 보강.
-- 여러 번 실행해도 안전함: region.region_code에는 실제 UNIQUE 제약이 없으므로(실제 유니크 키는 h_code)
-- INSERT IGNORE 대신 매번 NOT EXISTS로 직접 중복을 막는다. UPDATE/DELETE는 대상이 없으면 0행으로 넘어간다.
-- parent_region_id는 하드코딩하지 않고 region_code로 매번 다시 찾는다(DB마다 auto_increment 값이 다름).
USE smart_travel_db;

-- 1) 누락된 상위 시도 추가 (목포시·광산구가 참조)
INSERT INTO region (region_code, region_name, full_name, region_level, parent_region_id, is_supported)
SELECT '46','전라남도','전라남도','SIDO',NULL,TRUE
WHERE NOT EXISTS (SELECT 1 FROM region WHERE region_code='46');
INSERT INTO region (region_code, region_name, full_name, region_level, parent_region_id, is_supported)
SELECT '29','광주광역시','광주광역시','SIDO',NULL,TRUE
WHERE NOT EXISTS (SELECT 1 FROM region WHERE region_code='29');

-- 2) 누락된 시군구 추가 (parent는 LIMIT 1로 고정해 혹시 상위가 중복되어 있어도 더 늘리지 않음)
INSERT INTO region (region_code, region_name, full_name, region_level, parent_region_id, is_supported)
SELECT '27110','중구','대구광역시 중구','SIGUNGU', p.region_id, TRUE
FROM (SELECT region_id FROM region WHERE region_code='27' LIMIT 1) p
WHERE NOT EXISTS (SELECT 1 FROM region WHERE region_code='27110');

INSERT INTO region (region_code, region_name, full_name, region_level, parent_region_id, is_supported)
SELECT '47113','포항시 남구','경상북도 포항시 남구','SIGUNGU', p.region_id, TRUE
FROM (SELECT region_id FROM region WHERE region_code='47' LIMIT 1) p
WHERE NOT EXISTS (SELECT 1 FROM region WHERE region_code='47113');

INSERT INTO region (region_code, region_name, full_name, region_level, parent_region_id, is_supported)
SELECT '46110','목포시','전라남도 목포시','SIGUNGU', p.region_id, TRUE
FROM (SELECT region_id FROM region WHERE region_code='46' LIMIT 1) p
WHERE NOT EXISTS (SELECT 1 FROM region WHERE region_code='46110');

INSERT INTO region (region_code, region_name, full_name, region_level, parent_region_id, is_supported)
SELECT '29200','광산구','광주광역시 광산구','SIGUNGU', p.region_id, TRUE
FROM (SELECT region_id FROM region WHERE region_code='29' LIMIT 1) p
WHERE NOT EXISTS (SELECT 1 FROM region WHERE region_code='29200');

INSERT INTO region (region_code, region_name, full_name, region_level, parent_region_id, is_supported)
SELECT '28200','남동구','인천광역시 남동구','SIGUNGU', p.region_id, TRUE
FROM (SELECT region_id FROM region WHERE region_code='28' LIMIT 1) p
WHERE NOT EXISTS (SELECT 1 FROM region WHERE region_code='28200');

INSERT INTO region (region_code, region_name, full_name, region_level, parent_region_id, is_supported)
SELECT '28237','부평구','인천광역시 부평구','SIGUNGU', p.region_id, TRUE
FROM (SELECT region_id FROM region WHERE region_code='28' LIMIT 1) p
WHERE NOT EXISTS (SELECT 1 FROM region WHERE region_code='28237');

-- 2026년 신설구(검단구·서해구)는 공식 법정동코드가 아직 확인되지 않아 자체 생성한 코드다.
INSERT INTO region (region_code, region_name, full_name, region_level, parent_region_id, is_supported)
SELECT '28130','검단구','인천광역시 검단구','SIGUNGU', p.region_id, TRUE
FROM (SELECT region_id FROM region WHERE region_code='28' LIMIT 1) p
WHERE NOT EXISTS (SELECT 1 FROM region WHERE region_code='28130');

INSERT INTO region (region_code, region_name, full_name, region_level, parent_region_id, is_supported)
SELECT '28135','서해구','인천광역시 서해구','SIGUNGU', p.region_id, TRUE
FROM (SELECT region_id FROM region WHERE region_code='28' LIMIT 1) p
WHERE NOT EXISTS (SELECT 1 FROM region WHERE region_code='28135');

INSERT INTO region (region_code, region_name, full_name, region_level, parent_region_id, is_supported)
SELECT '43760','괴산군','충청북도 괴산군','SIGUNGU', p.region_id, TRUE
FROM (SELECT region_id FROM region WHERE region_code='43' LIMIT 1) p
WHERE NOT EXISTS (SELECT 1 FROM region WHERE region_code='43760');

INSERT INTO region (region_code, region_name, full_name, region_level, parent_region_id, is_supported)
SELECT '43730','옥천군','충청북도 옥천군','SIGUNGU', p.region_id, TRUE
FROM (SELECT region_id FROM region WHERE region_code='43' LIMIT 1) p
WHERE NOT EXISTS (SELECT 1 FROM region WHERE region_code='43730');

INSERT INTO region (region_code, region_name, full_name, region_level, parent_region_id, is_supported)
SELECT '43770','음성군','충청북도 음성군','SIGUNGU', p.region_id, TRUE
FROM (SELECT region_id FROM region WHERE region_code='43' LIMIT 1) p
WHERE NOT EXISTS (SELECT 1 FROM region WHERE region_code='43770');

-- "청주시"(구 미표기) 원본 행을 위한 상위코드. 청주시 흥덕구(43114) 등 기존 구 단위 코드는 그대로 둔다.
INSERT INTO region (region_code, region_name, full_name, region_level, parent_region_id, is_supported)
SELECT '43110','청주시','충청북도 청주시','SIGUNGU', p.region_id, TRUE
FROM (SELECT region_id FROM region WHERE region_code='43' LIMIT 1) p
WHERE NOT EXISTS (SELECT 1 FROM region WHERE region_code='43110');

-- 3) 과거에 잘못 생성된 가짜 시도 "전남광주통합특별시"(region_code='12') 밑에 들어있던 12개 지역을
--    실제 법정동코드 기준 전라남도(46)/광주광역시(29) 산하로 재배치.
--    해당 region_code가 없으면(이 DB에 그 문제가 없었으면) 아래 UPDATE들은 그냥 0행 처리되고 넘어간다.
UPDATE region SET region_code='46130', full_name='전라남도 여수시',
  parent_region_id=(SELECT region_id FROM region WHERE region_code='46' LIMIT 1), updated_at=NOW()
WHERE region_code='12130';
UPDATE region SET region_code='29110', full_name='광주광역시 동구',
  parent_region_id=(SELECT region_id FROM region WHERE region_code='29' LIMIT 1), updated_at=NOW()
WHERE region_code='12210';
UPDATE region SET region_code='29140', full_name='광주광역시 서구',
  parent_region_id=(SELECT region_id FROM region WHERE region_code='29' LIMIT 1), updated_at=NOW()
WHERE region_code='12240';
UPDATE region SET region_code='29155', full_name='광주광역시 남구',
  parent_region_id=(SELECT region_id FROM region WHERE region_code='29' LIMIT 1), updated_at=NOW()
WHERE region_code='12270';
UPDATE region SET region_code='46710', full_name='전라남도 담양군',
  parent_region_id=(SELECT region_id FROM region WHERE region_code='46' LIMIT 1), updated_at=NOW()
WHERE region_code='12710';
UPDATE region SET region_code='46730', full_name='전라남도 구례군',
  parent_region_id=(SELECT region_id FROM region WHERE region_code='46' LIMIT 1), updated_at=NOW()
WHERE region_code='12730';
UPDATE region SET region_code='46780', full_name='전라남도 보성군',
  parent_region_id=(SELECT region_id FROM region WHERE region_code='46' LIMIT 1), updated_at=NOW()
WHERE region_code='12750';
UPDATE region SET region_code='46800', full_name='전라남도 장흥군',
  parent_region_id=(SELECT region_id FROM region WHERE region_code='46' LIMIT 1), updated_at=NOW()
WHERE region_code='12770';
UPDATE region SET region_code='46820', full_name='전라남도 해남군',
  parent_region_id=(SELECT region_id FROM region WHERE region_code='46' LIMIT 1), updated_at=NOW()
WHERE region_code='12790';
UPDATE region SET region_code='46890', full_name='전라남도 완도군',
  parent_region_id=(SELECT region_id FROM region WHERE region_code='46' LIMIT 1), updated_at=NOW()
WHERE region_code='12850';
UPDATE region SET region_code='46900', full_name='전라남도 진도군',
  parent_region_id=(SELECT region_id FROM region WHERE region_code='46' LIMIT 1), updated_at=NOW()
WHERE region_code='12860';
UPDATE region SET region_code='46910', full_name='전라남도 신안군',
  parent_region_id=(SELECT region_id FROM region WHERE region_code='46' LIMIT 1), updated_at=NOW()
WHERE region_code='12870';

-- 4) 자식이 하나도 안 남았을 때만 가짜 시도 삭제 (혹시 다른 자식이 더 있으면 안전하게 보존)
DELETE FROM region
WHERE region_code='12'
  AND NOT EXISTS (SELECT 1 FROM region r2 WHERE r2.parent_region_id = region.region_id);
