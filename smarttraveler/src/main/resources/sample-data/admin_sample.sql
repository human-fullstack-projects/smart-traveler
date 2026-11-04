-- LEGACY M51 sample data. Do not run after M55: it targets retired admin/code tables.
-- M51 fake local demo data. Run manually after the application has created admin_* tables.
-- Idempotent IDs are reserved in the 510000 range; INSERT IGNORE leaves existing rows untouched.
-- Demo password for every fake account: Admin123! (BCrypt hash below).
-- Never use these accounts in a deployed environment.

INSERT IGNORE INTO member_role_codes(role_code, role_name) VALUES
('ADMIN','관리자'),('USER','사용자'),('OWNER','업주');
INSERT IGNORE INTO account_status_codes(status_code,status_name) VALUES
('ACTIVE','활성'),('SUSPENDED','정지'),('WITHDRAWN','탈퇴');
INSERT IGNORE INTO suspension_reason_codes(reason_code,reason_name) VALUES
('ADMIN','관리자 처리'),('POLICY','운영 정책 위반'),
('POLICY_VIOLATION','운영 정책 위반'),('FRAUD','부정 이용'),('OTHER','기타');
INSERT IGNORE INTO owner_review_status(review_status_code,review_status_name) VALUES
('PENDING','심사 대기'),('APPROVED','승인'),('REJECTED','반려');
INSERT IGNORE INTO generation_method(id,code) VALUES (1,'ai'),(2,'user'),(3,'ai_edited'),(4,'template');

INSERT IGNORE INTO members(member_id,login_id,password,nickname,email,created_at,updated_at,status,role_code) VALUES
(510001,'m51_admin','$2a$10$bwPsf4L1GK9Ydr.CQND7t.fWrdwm/jB0wI6thiDr4DwWyMgHI2lB.','관리자','m51-admin@example.test',NOW(),NOW(),'ACTIVE','ADMIN'),
(510002,'m51_user1','$2a$10$bwPsf4L1GK9Ydr.CQND7t.fWrdwm/jB0wI6thiDr4DwWyMgHI2lB.','여행이','m51-user1@example.test',NOW(),NOW(),'ACTIVE','USER'),
(510003,'m51_user2','$2a$10$bwPsf4L1GK9Ydr.CQND7t.fWrdwm/jB0wI6thiDr4DwWyMgHI2lB.','바다길','m51-user2@example.test',NOW(),NOW(),'ACTIVE','USER'),
(510004,'m51_user3','$2a$10$bwPsf4L1GK9Ydr.CQND7t.fWrdwm/jB0wI6thiDr4DwWyMgHI2lB.','산책가','m51-user3@example.test',NOW(),NOW(),'SUSPENDED','USER'),
(510005,'m51_user4','$2a$10$bwPsf4L1GK9Ydr.CQND7t.fWrdwm/jB0wI6thiDr4DwWyMgHI2lB.','길동이','m51-user4@example.test',NOW(),NOW(),'ACTIVE','USER'),
(510006,'m51_user5','$2a$10$bwPsf4L1GK9Ydr.CQND7t.fWrdwm/jB0wI6thiDr4DwWyMgHI2lB.','여름별','m51-user5@example.test',NOW(),NOW(),'ACTIVE','USER'),
(510011,'m51_owner1','$2a$10$bwPsf4L1GK9Ydr.CQND7t.fWrdwm/jB0wI6thiDr4DwWyMgHI2lB.','경주숙소','m51-owner1@example.test',NOW(),NOW(),'ACTIVE','OWNER'),
(510012,'m51_owner2','$2a$10$bwPsf4L1GK9Ydr.CQND7t.fWrdwm/jB0wI6thiDr4DwWyMgHI2lB.','부산숙소','m51-owner2@example.test',NOW(),NOW(),'ACTIVE','OWNER'),
(510013,'m51_owner3','$2a$10$bwPsf4L1GK9Ydr.CQND7t.fWrdwm/jB0wI6thiDr4DwWyMgHI2lB.','전주숙소','m51-owner3@example.test',NOW(),NOW(),'ACTIVE','OWNER');

INSERT IGNORE INTO owner_profiles(owner_id,business_number,business_name,representative_name,business_address,business_phone,review_status_code,created_at,updated_at,member_id) VALUES
(510011,'M51-000-01','경주 별채','김가람','경북 경주시','010-0000-5101','APPROVED',NOW(),NOW(),510011),
(510012,'M51-000-02','부산 파도집','이가온','부산 해운대구','010-0000-5102','PENDING',NOW(),NOW(),510012),
(510013,'M51-000-03','전주 한옥집','박도윤','전북 전주시','010-0000-5103','REJECTED',NOW(),NOW(),510013);
INSERT IGNORE INTO owner_review_history(review_id,owner_id,reviewer_id,review_status_code,review_comment,reviewed_at,created_at) VALUES
(510011,510011,510001,'APPROVED','서류 확인',NOW(),NOW()),
(510013,510013,510001,'REJECTED','등록번호 재확인 필요',NOW(),NOW());

INSERT IGNORE INTO lodging(lodging_id,owner_id,source_provider,lodging_type,name,address,latitude,longitude,checkin_time,checkout_time,status,reject_reason,approved_at,created_at,updated_at) VALUES
(510001,510011,'OWNER','PENSION','경주 별채','경북 경주시 첨성로 1',35.8346000,129.2189000,'15:00:00','11:00:00','ACTIVE',NULL,NOW(),NOW(),NOW()),
(510002,510011,'OWNER','HOTEL','황리단길 스테이','경북 경주시 포석로 2',35.8361000,129.2100000,'15:00:00','11:00:00','SUSPENDED',NULL,NOW(),NOW(),NOW()),
(510003,510012,'OWNER','PENSION','파도 앞집','부산 해운대구 해변로 3',35.1587000,129.1604000,'15:00:00','11:00:00','PENDING',NULL,NULL,NOW(),NOW()),
(510004,510013,'OWNER','HANOK','한옥 마당','전북 전주시 완산구 4',35.8140000,127.1520000,'15:00:00','11:00:00','REJECTED','정보 보완 필요',NULL,NOW(),NOW());
INSERT IGNORE INTO room(room_id,lodging_id,name,base_capacity,max_capacity,price_off_weekday,price_off_weekend,is_active,created_at,updated_at) VALUES
(510001,510001,'별 101',2,4,99000,119000,TRUE,NOW(),NOW()),
(510002,510001,'별 102',2,3,89000,109000,TRUE,NOW(),NOW()),
(510003,510002,'스테이 A',2,2,120000,140000,TRUE,NOW(),NOW()),
(510004,510003,'오션룸',2,4,130000,150000,TRUE,NOW(),NOW());

-- The local lodging schema still has its legacy NOT NULL lodging_id; derive it from room here.
INSERT IGNORE INTO reservation(reservation_id,reservation_no,member_id,lodging_id,room_id,checkin_date,checkout_date,guest_count,booker_name,booker_phone,total_price,status,version,created_at,updated_at) VALUES
(510001,'M51-ORDER-001',510002,510001,510001,CURDATE()+INTERVAL 10 DAY,CURDATE()+INTERVAL 11 DAY,2,'김여행','010-0000-1001',99000,'PENDING_PAYMENT',0,NOW(),NOW()),
(510002,'M51-ORDER-002',510003,510001,510002,CURDATE()+INTERVAL 12 DAY,CURDATE()+INTERVAL 13 DAY,2,'이바다','010-0000-1002',89000,'CONFIRMED',0,NOW(),NOW()),
(510003,'M51-ORDER-003',510005,510001,510001,CURDATE()-INTERVAL 10 DAY,CURDATE()-INTERVAL 9 DAY,2,'박산책','010-0000-1003',99000,'COMPLETED',0,NOW(),NOW()),
(510004,'M51-ORDER-004',510006,510001,510002,CURDATE()-INTERVAL 2 DAY,CURDATE()-INTERVAL 1 DAY,2,'최여름','010-0000-1004',89000,'REFUNDING',0,NOW(),NOW()),
(510005,'M51-ORDER-005',510002,510002,510003,CURDATE()+INTERVAL 20 DAY,CURDATE()+INTERVAL 21 DAY,2,'김여행','010-0000-1001',120000,'REFUND_FAILED',0,NOW(),NOW()),
(510006,'M51-ORDER-006',510003,510001,510001,CURDATE()+INTERVAL 30 DAY,CURDATE()+INTERVAL 31 DAY,2,'이바다','010-0000-1002',99000,'CANCELLED',0,NOW(),NOW()),
(510007,'M51-ORDER-007',510005,510001,510002,CURDATE()+INTERVAL 25 DAY,CURDATE()+INTERVAL 26 DAY,2,'박산책','010-0000-1003',89000,'PENDING_PAYMENT',0,NOW(),NOW()),
(510008,'M51-ORDER-008',510006,510001,510001,CURDATE()+INTERVAL 15 DAY,CURDATE()+INTERVAL 16 DAY,2,'최여름','010-0000-1004',99000,'CONFIRMED',0,NOW(),NOW());

INSERT IGNORE INTO payment(payment_id,reservation_id,idempotency_key,pg_provider,amount,status,approved_at,created_at,updated_at) VALUES
(510001,510001,'M51-PAY-001','TOSS',99000,'DONE',NOW(),NOW(),NOW()),
(510002,510002,'M51-PAY-002','TOSS',89000,'DONE',NOW(),NOW(),NOW()),
(510003,510003,'M51-PAY-003','TOSS',99000,'DONE',NOW(),NOW(),NOW()),
(510004,510004,'M51-PAY-004','TOSS',89000,'DONE',NOW(),NOW(),NOW()),
(510005,510005,'M51-PAY-005','TOSS',120000,'DONE',NOW(),NOW(),NOW()),
(510007,510007,'M51-PAY-007','TOSS',89000,'READY',NULL,NOW(),NOW()),
(510008,510008,'M51-PAY-008','TOSS',99000,'DONE',NOW(),NOW(),NOW());
INSERT IGNORE INTO room_inventory(inventory_id,room_id,stay_date,price,price_type,is_on_sale,status,reservation_id,hold_expires_at,updated_at) VALUES
(510001,510001,CURDATE()+INTERVAL 10 DAY,99000,'OWNER_SET',TRUE,'HELD',510001,NOW()+INTERVAL 1 DAY,NOW()),
(510002,510002,CURDATE()+INTERVAL 12 DAY,89000,'OWNER_SET',TRUE,'BOOKED',510002,NULL,NOW()),
(510007,510002,CURDATE()+INTERVAL 25 DAY,89000,'OWNER_SET',TRUE,'HELD',510007,NOW()+INTERVAL 1 DAY,NOW());

INSERT IGNORE INTO admin_human_course(id,member_id,title,destination,summary,status,featured,created_at) VALUES
(510001,510002,'경주 하루 산책','경주','유적을 걸으며 둘러보는 코스','VISIBLE',TRUE,NOW()),
(510002,510003,'부산 바다길','부산','해변과 시장을 함께 보는 코스','VISIBLE',FALSE,NOW()),
(510003,510005,'전주 골목길','전주','한옥마을 산책 코스','BLINDED',FALSE,NOW());
INSERT IGNORE INTO admin_human_report(id,course_id,reporter_id,reason,status,created_at) VALUES
(510001,510002,510006,'장소 정보가 오래되었습니다.','OPEN',NOW()),
(510002,510003,510002,'사진 출처 확인 요청','RESOLVED',NOW());
INSERT IGNORE INTO admin_human_action_history(id,course_id,report_id,admin_id,action,note,created_at) VALUES
(510001,510003,510002,510001,'BLINDED','신고 검토 후 블라인드',NOW());

INSERT IGNORE INTO course(id,member_no,destination,start_date,end_date,adults,children,fixed_events,preferences_snapshot,tips,assumptions,share_consent,generation_method_id,status,is_active,title,summary,error_message,model,created_at,updated_at) VALUES
(510001,510002,'경주',CURDATE()+INTERVAL 5 DAY,CURDATE()+INTERVAL 6 DAY,2,0,'[]','{}','[]','[]',FALSE,1,'done',TRUE,'경주 AI 코스','유적 산책',NULL,'demo-model',NOW(),NOW()),
(510002,510003,'부산',CURDATE()+INTERVAL 8 DAY,CURDATE()+INTERVAL 9 DAY,2,0,'[]','{}','[]','[]',FALSE,1,'failed',TRUE,'부산 AI 코스','바다 여행','TourAPI 응답 실패','demo-model',NOW(),NOW()),
(510003,510005,'전주',CURDATE()+INTERVAL 12 DAY,CURDATE()+INTERVAL 13 DAY,2,0,'[]','{}','[]','[]',FALSE,3,'running',TRUE,'전주 AI 수정 코스','한옥 여행',NULL,'demo-model',NOW(),NOW());
INSERT IGNORE INTO admin_recommendation_criteria(id,name,setting_value,updated_at) VALUES
(510001,'관광지 데이터 범위','공공데이터 등록 장소',NOW()),
(510002,'도보 최대 이동 시간(분)','30',NOW()),
(510003,'차량 최대 이동 시간(분)','60',NOW()),
(510004,'추천 제외 장소','',NOW());

