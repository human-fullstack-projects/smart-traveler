-- Local development only. Never execute against any database other than smart_travel_db.
USE smart_travel_db;

-- The local DB has no terms yet; these two current versions enable AUTH-03 tests.
INSERT INTO terms(terms_type,terms_version,is_required,effective_date,content,created_at)
SELECT 'SERVICE','test-1',TRUE,CURRENT_DATE(),'로컬 테스트 서비스 약관',NOW()
WHERE NOT EXISTS (SELECT 1 FROM terms WHERE terms_type='SERVICE');
INSERT INTO terms(terms_type,terms_version,is_required,effective_date,content,created_at)
SELECT 'PRIVACY','test-1',TRUE,CURRENT_DATE(),'로컬 테스트 개인정보 약관',NOW()
WHERE NOT EXISTS (SELECT 1 FROM terms WHERE terms_type='PRIVACY');

INSERT IGNORE INTO members(login_id,password,nickname,email,created_at,updated_at,status,role_code)
VALUES
('team_test','$2a$10$NeAuT5x2oMEjXu4o3Hup8e.HJ4hXX.vZHYPPFiLpbJXk1RjfM7Uyu','팀 테스트','team_test@example.invalid',NOW(),NOW(),'ACTIVE','USER'),
('m55_admin','$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu','관리자 테스트','m55_admin@example.invalid',NOW(),NOW(),'ACTIVE','ADMIN');
