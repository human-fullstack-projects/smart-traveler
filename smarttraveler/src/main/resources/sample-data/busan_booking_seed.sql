-- 개발용 부산 숙소, 객실, 앞으로 90일의 판매 재고.
-- 이름과 위치는 프로젝트 숙소 데이터 기준이며 객실, 가격, 재고는 테스트용이다.
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO lodging
    (source_provider, source_content_id, lodging_type, name, address, latitude, longitude,
     checkin_time, checkout_time, status)
VALUES
    ('BOOKING_DEMO', 'BUSAN_SEOMYEON', 'HOTEL', '토요코인호텔 부산서면 (예약 테스트)',
     '부산광역시 부산진구 서전로 39 (전포동)', 35.1579671, 129.0640538, '15:00', '11:00', 'ACTIVE'),
    ('BOOKING_DEMO', 'BUSAN_HAEUNDAE', 'RESORT', '한화리조트 해운대 (예약 테스트)',
     '부산광역시 해운대구 마린시티3로 52', 35.1544482, 129.1450718, '15:00', '11:00', 'ACTIVE')
ON DUPLICATE KEY UPDATE lodging_id = lodging_id;

INSERT INTO room (lodging_id, name, base_capacity, max_capacity, price_off_weekday, price_off_weekend)
SELECT lodging_id, '스탠다드 (최대 2인)', 2, 2, 79000, 99000 FROM lodging
WHERE source_provider='BOOKING_DEMO' AND source_content_id='BUSAN_SEOMYEON'
ON DUPLICATE KEY UPDATE room_id = room_id;

INSERT INTO room (lodging_id, name, base_capacity, max_capacity, price_off_weekday, price_off_weekend)
SELECT lodging_id, '패밀리 (최대 4인)', 2, 4, 149000, 189000 FROM lodging
WHERE source_provider='BOOKING_DEMO' AND source_content_id='BUSAN_HAEUNDAE'
ON DUPLICATE KEY UPDATE room_id = room_id;

SET @busan_today = DATE(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 9 HOUR));

INSERT INTO room_inventory (room_id, stay_date, price, total_quantity)
SELECT r.room_id, DATE_ADD(@busan_today, INTERVAL d.seq DAY),
       CASE WHEN WEEKDAY(DATE_ADD(@busan_today, INTERVAL d.seq DAY)) IN (4, 5)
            THEN r.price_off_weekend ELSE r.price_off_weekday END,
       2
FROM room r
JOIN lodging l ON l.lodging_id=r.lodging_id
CROSS JOIN seq_0_to_89 d
WHERE l.source_provider='BOOKING_DEMO'
  AND ((l.source_content_id='BUSAN_SEOMYEON' AND r.name='스탠다드 (최대 2인)')
       OR (l.source_content_id='BUSAN_HAEUNDAE' AND r.name='패밀리 (최대 4인)'))
ON DUPLICATE KEY UPDATE inventory_id = inventory_id;

COMMIT;
