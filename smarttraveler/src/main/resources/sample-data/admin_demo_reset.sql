-- LEGACY M51 reset. Do not run after M55: it targets retired admin/inventory columns.
-- 관리자 화면 시연용 상태 되돌리기 (가짜 데이터 510000번대만 · smart-travel-db 전용).
-- admin_sample.sql 다음에 실행. 몇 번을 실행해도 같은 상태가 된다. 지우는 줄 없음.
-- 결과: 업주 가입 승인대기 1 · 숙소 승인대기 1 · 결제대기 주문 1 · 신고 대기 1.

-- 업주 가입 승인대기 (새 가짜 업주 m51_owner4, 비밀번호는 다른 가짜 계정과 같음)
INSERT IGNORE INTO members(member_id,login_id,password,nickname,email,created_at,updated_at,status,role_code)
SELECT 510014,'m51_owner4',password,'강릉숙소','m51-owner4@example.test',NOW(),NOW(),'ACTIVE','OWNER'
FROM members WHERE member_id=510011;
INSERT IGNORE INTO owner_profiles(owner_id,member_id,business_number,business_name,representative_name,business_address,business_phone,review_status_code,created_at,updated_at)
VALUES (510014,510014,'M51-000-04','강릉 솔밭집','정솔','강원 강릉시','010-0000-5104','PENDING',NOW(),NOW());
UPDATE owner_profiles SET review_status_code='PENDING', updated_at=NOW() WHERE owner_id=510014;

-- 숙소 승인대기 (승인된 업주 510012의 「파도 앞집」)
UPDATE lodging SET status='PENDING', approved_at=NULL, reject_reason=NULL WHERE lodging_id=510003;

-- 결제대기 주문 (M51-ORDER-007 · 결제 준비 · 객실 임시 점유 1일)
UPDATE reservation SET status='PENDING_PAYMENT', updated_at=NOW() WHERE reservation_id=510007;
UPDATE payment SET status='READY', updated_at=NOW() WHERE payment_id=510007;
INSERT IGNORE INTO room_inventory(inventory_id,room_id,stay_date,price,price_type,is_on_sale,status,reservation_id,hold_expires_at,updated_at)
VALUES (510007,510002,CURDATE()+INTERVAL 25 DAY,89000,'OWNER_SET',TRUE,'HELD',510007,NOW()+INTERVAL 1 DAY,NOW());
UPDATE room_inventory SET status='HELD', reservation_id=510007, hold_expires_at=NOW()+INTERVAL 1 DAY WHERE inventory_id=510007;

-- 신고 대기 (휴먼 추천 코스 510002에 대한 신고)
UPDATE admin_human_report SET status='OPEN', resolved_at=NULL WHERE id=510001;
UPDATE admin_human_course SET status='VISIBLE' WHERE id=510002;
