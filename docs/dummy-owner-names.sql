-- 로컬 smart_travel_db 전용. 서버 자동 적재와 같은 기존 더미 계정 이름/비밀번호 이행입니다.
-- 두 번 실행해도 같은 결과입니다. 새 숙소/계정 생성은 서버 자동 적재가 담당합니다.
-- 실행 전 접속 대상이 로컬 개발 DB인지 확인하세요.
USE smart_travel_db;

-- 1 : 126715 : 홍길동
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner1' OR taken.email='owner1@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner1',m.email='owner1@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '홍길동' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '홍길동' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_126715' AND m.email='dummy_owner_126715@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-126715' AND taken.member_id IS NULL;

-- 2 : 127654 : 김민수
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner2' OR taken.email='owner2@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner2',m.email='owner2@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '김민수' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '김민수' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_127654' AND m.email='dummy_owner_127654@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-127654' AND taken.member_id IS NULL;

-- 3 : 127812 : 김철수
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner3' OR taken.email='owner3@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner3',m.email='owner3@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '김철수' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '김철수' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_127812' AND m.email='dummy_owner_127812@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-127812' AND taken.member_id IS NULL;

-- 4 : 129565 : 김태현
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner4' OR taken.email='owner4@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner4',m.email='owner4@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '김태현' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '김태현' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_129565' AND m.email='dummy_owner_129565@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-129565' AND taken.member_id IS NULL;

-- 5 : 136039 : 김서연
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner5' OR taken.email='owner5@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner5',m.email='owner5@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '김서연' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '김서연' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_136039' AND m.email='dummy_owner_136039@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136039' AND taken.member_id IS NULL;

-- 6 : 136063 : 김도윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner6' OR taken.email='owner6@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner6',m.email='owner6@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '김도윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '김도윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_136063' AND m.email='dummy_owner_136063@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136063' AND taken.member_id IS NULL;

-- 7 : 136093 : 김수빈
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner7' OR taken.email='owner7@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner7',m.email='owner7@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '김수빈' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '김수빈' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_136093' AND m.email='dummy_owner_136093@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136093' AND taken.member_id IS NULL;

-- 8 : 136208 : 김현우
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner8' OR taken.email='owner8@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner8',m.email='owner8@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '김현우' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '김현우' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_136208' AND m.email='dummy_owner_136208@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136208' AND taken.member_id IS NULL;

-- 9 : 136211 : 김하윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner9' OR taken.email='owner9@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner9',m.email='owner9@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '김하윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '김하윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_136211' AND m.email='dummy_owner_136211@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136211' AND taken.member_id IS NULL;

-- 10 : 136213 : 김준서
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner10' OR taken.email='owner10@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner10',m.email='owner10@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '김준서' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '김준서' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_136213' AND m.email='dummy_owner_136213@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136213' AND taken.member_id IS NULL;

-- 11 : 136249 : 김예린
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner11' OR taken.email='owner11@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner11',m.email='owner11@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '김예린' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '김예린' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_136249' AND m.email='dummy_owner_136249@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136249' AND taken.member_id IS NULL;

-- 12 : 136422 : 김태민
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner12' OR taken.email='owner12@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner12',m.email='owner12@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '김태민' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '김태민' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_136422' AND m.email='dummy_owner_136422@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136422' AND taken.member_id IS NULL;

-- 13 : 136453 : 김소윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner13' OR taken.email='owner13@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner13',m.email='owner13@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '김소윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '김소윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_136453' AND m.email='dummy_owner_136453@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136453' AND taken.member_id IS NULL;

-- 14 : 136485 : 김우진
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner14' OR taken.email='owner14@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner14',m.email='owner14@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '김우진' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '김우진' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_136485' AND m.email='dummy_owner_136485@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136485' AND taken.member_id IS NULL;

-- 15 : 136520 : 김채원
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner15' OR taken.email='owner15@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner15',m.email='owner15@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '김채원' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '김채원' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_136520' AND m.email='dummy_owner_136520@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136520' AND taken.member_id IS NULL;

-- 16 : 136538 : 김시우
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner16' OR taken.email='owner16@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner16',m.email='owner16@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '김시우' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '김시우' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_136538' AND m.email='dummy_owner_136538@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136538' AND taken.member_id IS NULL;

-- 17 : 136542 : 김유진
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner17' OR taken.email='owner17@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner17',m.email='owner17@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '김유진' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '김유진' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_136542' AND m.email='dummy_owner_136542@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136542' AND taken.member_id IS NULL;

-- 18 : 136639 : 김지민
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner18' OR taken.email='owner18@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner18',m.email='owner18@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '김지민' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '김지민' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_136639' AND m.email='dummy_owner_136639@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136639' AND taken.member_id IS NULL;

-- 19 : 136641 : 이태현
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner19' OR taken.email='owner19@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner19',m.email='owner19@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '이태현' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '이태현' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_136641' AND m.email='dummy_owner_136641@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136641' AND taken.member_id IS NULL;

-- 20 : 136642 : 이서연
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner20' OR taken.email='owner20@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner20',m.email='owner20@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '이서연' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '이서연' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_136642' AND m.email='dummy_owner_136642@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136642' AND taken.member_id IS NULL;

-- 21 : 137315 : 이도윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner21' OR taken.email='owner21@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner21',m.email='owner21@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '이도윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '이도윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_137315' AND m.email='dummy_owner_137315@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137315' AND taken.member_id IS NULL;

-- 22 : 137379 : 이수빈
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner22' OR taken.email='owner22@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner22',m.email='owner22@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '이수빈' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '이수빈' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_137379' AND m.email='dummy_owner_137379@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137379' AND taken.member_id IS NULL;

-- 23 : 137476 : 이현우
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner23' OR taken.email='owner23@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner23',m.email='owner23@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '이현우' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '이현우' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_137476' AND m.email='dummy_owner_137476@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137476' AND taken.member_id IS NULL;

-- 24 : 137477 : 이하윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner24' OR taken.email='owner24@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner24',m.email='owner24@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '이하윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '이하윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_137477' AND m.email='dummy_owner_137477@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137477' AND taken.member_id IS NULL;

-- 25 : 137483 : 이준서
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner25' OR taken.email='owner25@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner25',m.email='owner25@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '이준서' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '이준서' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_137483' AND m.email='dummy_owner_137483@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137483' AND taken.member_id IS NULL;

-- 26 : 137486 : 이예린
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner26' OR taken.email='owner26@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner26',m.email='owner26@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '이예린' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '이예린' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_137486' AND m.email='dummy_owner_137486@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137486' AND taken.member_id IS NULL;

-- 27 : 137497 : 이태민
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner27' OR taken.email='owner27@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner27',m.email='owner27@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '이태민' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '이태민' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_137497' AND m.email='dummy_owner_137497@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137497' AND taken.member_id IS NULL;

-- 28 : 137499 : 이소윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner28' OR taken.email='owner28@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner28',m.email='owner28@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '이소윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '이소윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_137499' AND m.email='dummy_owner_137499@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137499' AND taken.member_id IS NULL;

-- 29 : 137706 : 이우진
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner29' OR taken.email='owner29@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner29',m.email='owner29@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '이우진' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '이우진' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_137706' AND m.email='dummy_owner_137706@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137706' AND taken.member_id IS NULL;

-- 30 : 137762 : 이채원
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner30' OR taken.email='owner30@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner30',m.email='owner30@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '이채원' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '이채원' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_137762' AND m.email='dummy_owner_137762@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137762' AND taken.member_id IS NULL;

-- 31 : 137788 : 이시우
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner31' OR taken.email='owner31@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner31',m.email='owner31@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '이시우' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '이시우' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_137788' AND m.email='dummy_owner_137788@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137788' AND taken.member_id IS NULL;

-- 32 : 137799 : 이유진
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner32' OR taken.email='owner32@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner32',m.email='owner32@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '이유진' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '이유진' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_137799' AND m.email='dummy_owner_137799@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137799' AND taken.member_id IS NULL;

-- 33 : 137809 : 이지민
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner33' OR taken.email='owner33@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner33',m.email='owner33@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '이지민' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '이지민' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_137809' AND m.email='dummy_owner_137809@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137809' AND taken.member_id IS NULL;

-- 34 : 137814 : 박태현
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner34' OR taken.email='owner34@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner34',m.email='owner34@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '박태현' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '박태현' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_137814' AND m.email='dummy_owner_137814@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137814' AND taken.member_id IS NULL;

-- 35 : 137826 : 박서연
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner35' OR taken.email='owner35@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner35',m.email='owner35@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '박서연' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '박서연' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_137826' AND m.email='dummy_owner_137826@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137826' AND taken.member_id IS NULL;

-- 36 : 137891 : 박도윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner36' OR taken.email='owner36@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner36',m.email='owner36@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '박도윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '박도윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_137891' AND m.email='dummy_owner_137891@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137891' AND taken.member_id IS NULL;

-- 37 : 137953 : 박수빈
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner37' OR taken.email='owner37@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner37',m.email='owner37@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '박수빈' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '박수빈' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_137953' AND m.email='dummy_owner_137953@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137953' AND taken.member_id IS NULL;

-- 38 : 137965 : 박현우
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner38' OR taken.email='owner38@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner38',m.email='owner38@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '박현우' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '박현우' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_137965' AND m.email='dummy_owner_137965@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137965' AND taken.member_id IS NULL;

-- 39 : 138081 : 박하윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner39' OR taken.email='owner39@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner39',m.email='owner39@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '박하윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '박하윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_138081' AND m.email='dummy_owner_138081@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138081' AND taken.member_id IS NULL;

-- 40 : 138089 : 박준서
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner40' OR taken.email='owner40@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner40',m.email='owner40@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '박준서' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '박준서' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_138089' AND m.email='dummy_owner_138089@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138089' AND taken.member_id IS NULL;

-- 41 : 138116 : 박예린
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner41' OR taken.email='owner41@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner41',m.email='owner41@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '박예린' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '박예린' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_138116' AND m.email='dummy_owner_138116@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138116' AND taken.member_id IS NULL;

-- 42 : 138184 : 박태민
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner42' OR taken.email='owner42@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner42',m.email='owner42@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '박태민' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '박태민' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_138184' AND m.email='dummy_owner_138184@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138184' AND taken.member_id IS NULL;

-- 43 : 138185 : 박소윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner43' OR taken.email='owner43@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner43',m.email='owner43@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '박소윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '박소윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_138185' AND m.email='dummy_owner_138185@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138185' AND taken.member_id IS NULL;

-- 44 : 138195 : 박우진
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner44' OR taken.email='owner44@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner44',m.email='owner44@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '박우진' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '박우진' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_138195' AND m.email='dummy_owner_138195@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138195' AND taken.member_id IS NULL;

-- 45 : 138217 : 박채원
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner45' OR taken.email='owner45@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner45',m.email='owner45@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '박채원' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '박채원' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_138217' AND m.email='dummy_owner_138217@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138217' AND taken.member_id IS NULL;

-- 46 : 138352 : 박시우
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner46' OR taken.email='owner46@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner46',m.email='owner46@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '박시우' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '박시우' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_138352' AND m.email='dummy_owner_138352@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138352' AND taken.member_id IS NULL;

-- 47 : 138363 : 박유진
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner47' OR taken.email='owner47@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner47',m.email='owner47@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '박유진' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '박유진' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_138363' AND m.email='dummy_owner_138363@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138363' AND taken.member_id IS NULL;

-- 48 : 138486 : 박지민
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner48' OR taken.email='owner48@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner48',m.email='owner48@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '박지민' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '박지민' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_138486' AND m.email='dummy_owner_138486@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138486' AND taken.member_id IS NULL;

-- 49 : 138513 : 최태현
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner49' OR taken.email='owner49@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner49',m.email='owner49@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '최태현' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '최태현' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_138513' AND m.email='dummy_owner_138513@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138513' AND taken.member_id IS NULL;

-- 50 : 138574 : 최서연
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner50' OR taken.email='owner50@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner50',m.email='owner50@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '최서연' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '최서연' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_138574' AND m.email='dummy_owner_138574@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138574' AND taken.member_id IS NULL;

-- 51 : 138575 : 최도윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner51' OR taken.email='owner51@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner51',m.email='owner51@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '최도윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '최도윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_138575' AND m.email='dummy_owner_138575@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138575' AND taken.member_id IS NULL;

-- 52 : 138675 : 최수빈
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner52' OR taken.email='owner52@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner52',m.email='owner52@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '최수빈' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '최수빈' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_138675' AND m.email='dummy_owner_138675@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138675' AND taken.member_id IS NULL;

-- 53 : 138724 : 최현우
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner53' OR taken.email='owner53@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner53',m.email='owner53@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '최현우' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '최현우' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_138724' AND m.email='dummy_owner_138724@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138724' AND taken.member_id IS NULL;

-- 54 : 138764 : 최하윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner54' OR taken.email='owner54@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner54',m.email='owner54@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '최하윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '최하윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_138764' AND m.email='dummy_owner_138764@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138764' AND taken.member_id IS NULL;

-- 55 : 138964 : 최준서
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner55' OR taken.email='owner55@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner55',m.email='owner55@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '최준서' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '최준서' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_138964' AND m.email='dummy_owner_138964@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138964' AND taken.member_id IS NULL;

-- 56 : 139008 : 최예린
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner56' OR taken.email='owner56@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner56',m.email='owner56@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '최예린' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '최예린' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_139008' AND m.email='dummy_owner_139008@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-139008' AND taken.member_id IS NULL;

-- 57 : 139488 : 최태민
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner57' OR taken.email='owner57@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner57',m.email='owner57@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '최태민' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '최태민' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_139488' AND m.email='dummy_owner_139488@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-139488' AND taken.member_id IS NULL;

-- 58 : 139512 : 최소윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner58' OR taken.email='owner58@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner58',m.email='owner58@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '최소윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '최소윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_139512' AND m.email='dummy_owner_139512@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-139512' AND taken.member_id IS NULL;

-- 59 : 139880 : 최우진
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner59' OR taken.email='owner59@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner59',m.email='owner59@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '최우진' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '최우진' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_139880' AND m.email='dummy_owner_139880@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-139880' AND taken.member_id IS NULL;

-- 60 : 139909 : 최채원
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner60' OR taken.email='owner60@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner60',m.email='owner60@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '최채원' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '최채원' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_139909' AND m.email='dummy_owner_139909@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-139909' AND taken.member_id IS NULL;

-- 61 : 142721 : 최시우
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner61' OR taken.email='owner61@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner61',m.email='owner61@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '최시우' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '최시우' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_142721' AND m.email='dummy_owner_142721@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142721' AND taken.member_id IS NULL;

-- 62 : 142722 : 최유진
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner62' OR taken.email='owner62@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner62',m.email='owner62@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '최유진' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '최유진' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_142722' AND m.email='dummy_owner_142722@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142722' AND taken.member_id IS NULL;

-- 63 : 142723 : 최지민
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner63' OR taken.email='owner63@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner63',m.email='owner63@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '최지민' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '최지민' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_142723' AND m.email='dummy_owner_142723@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142723' AND taken.member_id IS NULL;

-- 64 : 142729 : 정태현
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner64' OR taken.email='owner64@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner64',m.email='owner64@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '정태현' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '정태현' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_142729' AND m.email='dummy_owner_142729@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142729' AND taken.member_id IS NULL;

-- 65 : 142730 : 정서연
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner65' OR taken.email='owner65@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner65',m.email='owner65@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '정서연' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '정서연' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_142730' AND m.email='dummy_owner_142730@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142730' AND taken.member_id IS NULL;

-- 66 : 142801 : 정도윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner66' OR taken.email='owner66@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner66',m.email='owner66@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '정도윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '정도윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_142801' AND m.email='dummy_owner_142801@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142801' AND taken.member_id IS NULL;

-- 67 : 142833 : 정수빈
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner67' OR taken.email='owner67@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner67',m.email='owner67@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '정수빈' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '정수빈' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_142833' AND m.email='dummy_owner_142833@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142833' AND taken.member_id IS NULL;

-- 68 : 142839 : 정현우
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner68' OR taken.email='owner68@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner68',m.email='owner68@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '정현우' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '정현우' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_142839' AND m.email='dummy_owner_142839@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142839' AND taken.member_id IS NULL;

-- 69 : 142840 : 정하윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner69' OR taken.email='owner69@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner69',m.email='owner69@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '정하윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '정하윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_142840' AND m.email='dummy_owner_142840@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142840' AND taken.member_id IS NULL;

-- 70 : 142851 : 정준서
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner70' OR taken.email='owner70@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner70',m.email='owner70@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '정준서' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '정준서' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_142851' AND m.email='dummy_owner_142851@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142851' AND taken.member_id IS NULL;

-- 71 : 142852 : 정예린
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner71' OR taken.email='owner71@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner71',m.email='owner71@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '정예린' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '정예린' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_142852' AND m.email='dummy_owner_142852@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142852' AND taken.member_id IS NULL;

-- 72 : 142853 : 정태민
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner72' OR taken.email='owner72@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner72',m.email='owner72@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '정태민' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '정태민' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_142853' AND m.email='dummy_owner_142853@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142853' AND taken.member_id IS NULL;

-- 73 : 142884 : 정소윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner73' OR taken.email='owner73@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner73',m.email='owner73@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '정소윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '정소윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_142884' AND m.email='dummy_owner_142884@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142884' AND taken.member_id IS NULL;

-- 74 : 142892 : 정우진
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner74' OR taken.email='owner74@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner74',m.email='owner74@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '정우진' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '정우진' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_142892' AND m.email='dummy_owner_142892@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142892' AND taken.member_id IS NULL;

-- 75 : 142893 : 정채원
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner75' OR taken.email='owner75@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner75',m.email='owner75@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '정채원' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '정채원' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_142893' AND m.email='dummy_owner_142893@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142893' AND taken.member_id IS NULL;

-- 76 : 142894 : 정시우
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner76' OR taken.email='owner76@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner76',m.email='owner76@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '정시우' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '정시우' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_142894' AND m.email='dummy_owner_142894@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142894' AND taken.member_id IS NULL;

-- 77 : 142898 : 정유진
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner77' OR taken.email='owner77@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner77',m.email='owner77@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '정유진' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '정유진' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_142898' AND m.email='dummy_owner_142898@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142898' AND taken.member_id IS NULL;

-- 78 : 142903 : 정지민
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner78' OR taken.email='owner78@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner78',m.email='owner78@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '정지민' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '정지민' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_142903' AND m.email='dummy_owner_142903@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142903' AND taken.member_id IS NULL;

-- 79 : 142962 : 강태현
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner79' OR taken.email='owner79@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner79',m.email='owner79@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '강태현' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '강태현' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_142962' AND m.email='dummy_owner_142962@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142962' AND taken.member_id IS NULL;

-- 80 : 143000 : 강서연
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner80' OR taken.email='owner80@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner80',m.email='owner80@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '강서연' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '강서연' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_143000' AND m.email='dummy_owner_143000@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-143000' AND taken.member_id IS NULL;

-- 81 : 143023 : 강도윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner81' OR taken.email='owner81@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner81',m.email='owner81@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '강도윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '강도윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_143023' AND m.email='dummy_owner_143023@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-143023' AND taken.member_id IS NULL;

-- 82 : 143042 : 강수빈
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner82' OR taken.email='owner82@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner82',m.email='owner82@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '강수빈' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '강수빈' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_143042' AND m.email='dummy_owner_143042@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-143042' AND taken.member_id IS NULL;

-- 83 : 143064 : 강현우
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner83' OR taken.email='owner83@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner83',m.email='owner83@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '강현우' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '강현우' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_143064' AND m.email='dummy_owner_143064@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-143064' AND taken.member_id IS NULL;

-- 84 : 143090 : 강하윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner84' OR taken.email='owner84@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner84',m.email='owner84@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '강하윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '강하윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_143090' AND m.email='dummy_owner_143090@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-143090' AND taken.member_id IS NULL;

-- 85 : 143106 : 강준서
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner85' OR taken.email='owner85@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner85',m.email='owner85@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '강준서' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '강준서' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_143106' AND m.email='dummy_owner_143106@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-143106' AND taken.member_id IS NULL;

-- 86 : 143138 : 강예린
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner86' OR taken.email='owner86@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner86',m.email='owner86@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '강예린' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '강예린' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_143138' AND m.email='dummy_owner_143138@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-143138' AND taken.member_id IS NULL;

-- 87 : 143140 : 강태민
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner87' OR taken.email='owner87@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner87',m.email='owner87@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '강태민' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '강태민' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_143140' AND m.email='dummy_owner_143140@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-143140' AND taken.member_id IS NULL;

-- 88 : 143151 : 강소윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner88' OR taken.email='owner88@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner88',m.email='owner88@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '강소윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '강소윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_143151' AND m.email='dummy_owner_143151@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-143151' AND taken.member_id IS NULL;

-- 89 : 143169 : 강우진
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner89' OR taken.email='owner89@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner89',m.email='owner89@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '강우진' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '강우진' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_143169' AND m.email='dummy_owner_143169@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-143169' AND taken.member_id IS NULL;

-- 90 : 228912 : 강채원
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner90' OR taken.email='owner90@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner90',m.email='owner90@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '강채원' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '강채원' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_228912' AND m.email='dummy_owner_228912@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-228912' AND taken.member_id IS NULL;

-- 91 : 1003654 : 강시우
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner91' OR taken.email='owner91@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner91',m.email='owner91@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '강시우' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '강시우' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1003654' AND m.email='dummy_owner_1003654@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1003654' AND taken.member_id IS NULL;

-- 92 : 1073480 : 강유진
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner92' OR taken.email='owner92@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner92',m.email='owner92@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '강유진' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '강유진' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1073480' AND m.email='dummy_owner_1073480@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1073480' AND taken.member_id IS NULL;

-- 93 : 1102250 : 강지민
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner93' OR taken.email='owner93@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner93',m.email='owner93@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '강지민' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '강지민' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1102250' AND m.email='dummy_owner_1102250@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1102250' AND taken.member_id IS NULL;

-- 94 : 1122064 : 조태현
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner94' OR taken.email='owner94@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner94',m.email='owner94@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '조태현' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '조태현' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1122064' AND m.email='dummy_owner_1122064@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1122064' AND taken.member_id IS NULL;

-- 95 : 1144607 : 조서연
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner95' OR taken.email='owner95@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner95',m.email='owner95@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '조서연' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '조서연' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1144607' AND m.email='dummy_owner_1144607@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1144607' AND taken.member_id IS NULL;

-- 96 : 1144770 : 조도윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner96' OR taken.email='owner96@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner96',m.email='owner96@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '조도윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '조도윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1144770' AND m.email='dummy_owner_1144770@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1144770' AND taken.member_id IS NULL;

-- 97 : 1161581 : 조수빈
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner97' OR taken.email='owner97@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner97',m.email='owner97@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '조수빈' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '조수빈' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1161581' AND m.email='dummy_owner_1161581@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1161581' AND taken.member_id IS NULL;

-- 98 : 1161739 : 조현우
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner98' OR taken.email='owner98@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner98',m.email='owner98@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '조현우' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '조현우' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1161739' AND m.email='dummy_owner_1161739@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1161739' AND taken.member_id IS NULL;

-- 99 : 1162469 : 조하윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner99' OR taken.email='owner99@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner99',m.email='owner99@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '조하윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '조하윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1162469' AND m.email='dummy_owner_1162469@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1162469' AND taken.member_id IS NULL;

-- 100 : 1162526 : 조준서
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner100' OR taken.email='owner100@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner100',m.email='owner100@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '조준서' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '조준서' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1162526' AND m.email='dummy_owner_1162526@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1162526' AND taken.member_id IS NULL;

-- 101 : 1177256 : 조예린
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner101' OR taken.email='owner101@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner101',m.email='owner101@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '조예린' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '조예린' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1177256' AND m.email='dummy_owner_1177256@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1177256' AND taken.member_id IS NULL;

-- 102 : 1190304 : 조태민
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner102' OR taken.email='owner102@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner102',m.email='owner102@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '조태민' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '조태민' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1190304' AND m.email='dummy_owner_1190304@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1190304' AND taken.member_id IS NULL;

-- 103 : 1266427 : 조소윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner103' OR taken.email='owner103@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner103',m.email='owner103@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '조소윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '조소윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1266427' AND m.email='dummy_owner_1266427@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1266427' AND taken.member_id IS NULL;

-- 104 : 1271229 : 조우진
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner104' OR taken.email='owner104@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner104',m.email='owner104@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '조우진' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '조우진' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1271229' AND m.email='dummy_owner_1271229@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1271229' AND taken.member_id IS NULL;

-- 105 : 1271286 : 조채원
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner105' OR taken.email='owner105@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner105',m.email='owner105@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '조채원' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '조채원' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1271286' AND m.email='dummy_owner_1271286@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1271286' AND taken.member_id IS NULL;

-- 106 : 1271632 : 조시우
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner106' OR taken.email='owner106@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner106',m.email='owner106@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '조시우' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '조시우' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1271632' AND m.email='dummy_owner_1271632@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1271632' AND taken.member_id IS NULL;

-- 107 : 1272708 : 조유진
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner107' OR taken.email='owner107@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner107',m.email='owner107@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '조유진' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '조유진' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1272708' AND m.email='dummy_owner_1272708@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1272708' AND taken.member_id IS NULL;

-- 108 : 1273588 : 조지민
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner108' OR taken.email='owner108@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner108',m.email='owner108@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '조지민' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '조지민' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1273588' AND m.email='dummy_owner_1273588@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1273588' AND taken.member_id IS NULL;

-- 109 : 1274868 : 윤태현
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner109' OR taken.email='owner109@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner109',m.email='owner109@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '윤태현' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '윤태현' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1274868' AND m.email='dummy_owner_1274868@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1274868' AND taken.member_id IS NULL;

-- 110 : 1281934 : 윤서연
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner110' OR taken.email='owner110@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner110',m.email='owner110@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '윤서연' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '윤서연' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1281934' AND m.email='dummy_owner_1281934@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1281934' AND taken.member_id IS NULL;

-- 111 : 1300411 : 윤도윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner111' OR taken.email='owner111@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner111',m.email='owner111@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '윤도윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '윤도윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1300411' AND m.email='dummy_owner_1300411@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1300411' AND taken.member_id IS NULL;

-- 112 : 1315136 : 윤수빈
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner112' OR taken.email='owner112@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner112',m.email='owner112@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '윤수빈' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '윤수빈' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1315136' AND m.email='dummy_owner_1315136@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1315136' AND taken.member_id IS NULL;

-- 113 : 1327006 : 윤현우
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner113' OR taken.email='owner113@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner113',m.email='owner113@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '윤현우' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '윤현우' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1327006' AND m.email='dummy_owner_1327006@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1327006' AND taken.member_id IS NULL;

-- 114 : 1348840 : 윤하윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner114' OR taken.email='owner114@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner114',m.email='owner114@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '윤하윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '윤하윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1348840' AND m.email='dummy_owner_1348840@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1348840' AND taken.member_id IS NULL;

-- 115 : 1351056 : 윤준서
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner115' OR taken.email='owner115@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner115',m.email='owner115@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '윤준서' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '윤준서' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1351056' AND m.email='dummy_owner_1351056@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1351056' AND taken.member_id IS NULL;

-- 116 : 1365261 : 윤예린
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner116' OR taken.email='owner116@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner116',m.email='owner116@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '윤예린' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '윤예린' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1365261' AND m.email='dummy_owner_1365261@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1365261' AND taken.member_id IS NULL;

-- 117 : 1369549 : 윤태민
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner117' OR taken.email='owner117@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner117',m.email='owner117@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '윤태민' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '윤태민' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1369549' AND m.email='dummy_owner_1369549@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1369549' AND taken.member_id IS NULL;

-- 118 : 1373262 : 윤소윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner118' OR taken.email='owner118@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner118',m.email='owner118@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '윤소윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '윤소윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1373262' AND m.email='dummy_owner_1373262@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1373262' AND taken.member_id IS NULL;

-- 119 : 1638588 : 윤우진
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner119' OR taken.email='owner119@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner119',m.email='owner119@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '윤우진' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '윤우진' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1638588' AND m.email='dummy_owner_1638588@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1638588' AND taken.member_id IS NULL;

-- 120 : 1745972 : 윤채원
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner120' OR taken.email='owner120@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner120',m.email='owner120@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '윤채원' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '윤채원' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1745972' AND m.email='dummy_owner_1745972@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1745972' AND taken.member_id IS NULL;

-- 121 : 1747952 : 윤시우
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner121' OR taken.email='owner121@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner121',m.email='owner121@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '윤시우' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '윤시우' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1747952' AND m.email='dummy_owner_1747952@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1747952' AND taken.member_id IS NULL;

-- 122 : 1747999 : 윤유진
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner122' OR taken.email='owner122@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner122',m.email='owner122@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '윤유진' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '윤유진' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1747999' AND m.email='dummy_owner_1747999@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1747999' AND taken.member_id IS NULL;

-- 123 : 1748010 : 윤지민
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner123' OR taken.email='owner123@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner123',m.email='owner123@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '윤지민' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '윤지민' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1748010' AND m.email='dummy_owner_1748010@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1748010' AND taken.member_id IS NULL;

-- 124 : 1804605 : 장태현
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner124' OR taken.email='owner124@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner124',m.email='owner124@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '장태현' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '장태현' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1804605' AND m.email='dummy_owner_1804605@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1804605' AND taken.member_id IS NULL;

-- 125 : 1882371 : 장서연
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner125' OR taken.email='owner125@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner125',m.email='owner125@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '장서연' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '장서연' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1882371' AND m.email='dummy_owner_1882371@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1882371' AND taken.member_id IS NULL;

-- 126 : 1932079 : 장도윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner126' OR taken.email='owner126@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner126',m.email='owner126@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '장도윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '장도윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1932079' AND m.email='dummy_owner_1932079@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1932079' AND taken.member_id IS NULL;

-- 127 : 1933910 : 장수빈
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner127' OR taken.email='owner127@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner127',m.email='owner127@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '장수빈' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '장수빈' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1933910' AND m.email='dummy_owner_1933910@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1933910' AND taken.member_id IS NULL;

-- 128 : 1952929 : 장현우
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner128' OR taken.email='owner128@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner128',m.email='owner128@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '장현우' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '장현우' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1952929' AND m.email='dummy_owner_1952929@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1952929' AND taken.member_id IS NULL;

-- 129 : 1981882 : 장하윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner129' OR taken.email='owner129@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner129',m.email='owner129@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '장하윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '장하윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_1981882' AND m.email='dummy_owner_1981882@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1981882' AND taken.member_id IS NULL;

-- 130 : 2050197 : 장준서
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner130' OR taken.email='owner130@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner130',m.email='owner130@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '장준서' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '장준서' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_2050197' AND m.email='dummy_owner_2050197@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2050197' AND taken.member_id IS NULL;

-- 131 : 2366080 : 장예린
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner131' OR taken.email='owner131@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner131',m.email='owner131@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '장예린' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '장예린' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_2366080' AND m.email='dummy_owner_2366080@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2366080' AND taken.member_id IS NULL;

-- 132 : 2377774 : 장태민
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner132' OR taken.email='owner132@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner132',m.email='owner132@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '장태민' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '장태민' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_2377774' AND m.email='dummy_owner_2377774@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2377774' AND taken.member_id IS NULL;

-- 133 : 2392995 : 장소윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner133' OR taken.email='owner133@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner133',m.email='owner133@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '장소윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '장소윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_2392995' AND m.email='dummy_owner_2392995@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2392995' AND taken.member_id IS NULL;

-- 134 : 2570731 : 장우진
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner134' OR taken.email='owner134@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner134',m.email='owner134@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '장우진' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '장우진' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_2570731' AND m.email='dummy_owner_2570731@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2570731' AND taken.member_id IS NULL;

-- 135 : 2570790 : 장채원
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner135' OR taken.email='owner135@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner135',m.email='owner135@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '장채원' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '장채원' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_2570790' AND m.email='dummy_owner_2570790@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2570790' AND taken.member_id IS NULL;

-- 136 : 2570801 : 장시우
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner136' OR taken.email='owner136@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner136',m.email='owner136@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '장시우' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '장시우' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_2570801' AND m.email='dummy_owner_2570801@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2570801' AND taken.member_id IS NULL;

-- 137 : 2594874 : 장유진
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner137' OR taken.email='owner137@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner137',m.email='owner137@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '장유진' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '장유진' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_2594874' AND m.email='dummy_owner_2594874@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2594874' AND taken.member_id IS NULL;

-- 138 : 2668426 : 장지민
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner138' OR taken.email='owner138@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner138',m.email='owner138@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '장지민' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '장지민' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_2668426' AND m.email='dummy_owner_2668426@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2668426' AND taken.member_id IS NULL;

-- 139 : 2708329 : 임태현
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner139' OR taken.email='owner139@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner139',m.email='owner139@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '임태현' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '임태현' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_2708329' AND m.email='dummy_owner_2708329@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2708329' AND taken.member_id IS NULL;

-- 140 : 2770436 : 임서연
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner140' OR taken.email='owner140@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner140',m.email='owner140@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '임서연' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '임서연' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_2770436' AND m.email='dummy_owner_2770436@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2770436' AND taken.member_id IS NULL;

-- 141 : 2770452 : 임도윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner141' OR taken.email='owner141@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner141',m.email='owner141@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '임도윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '임도윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_2770452' AND m.email='dummy_owner_2770452@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2770452' AND taken.member_id IS NULL;

-- 142 : 2913368 : 임수빈
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner142' OR taken.email='owner142@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner142',m.email='owner142@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '임수빈' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '임수빈' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_2913368' AND m.email='dummy_owner_2913368@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2913368' AND taken.member_id IS NULL;

-- 143 : 2930309 : 임현우
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner143' OR taken.email='owner143@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner143',m.email='owner143@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '임현우' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '임현우' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_2930309' AND m.email='dummy_owner_2930309@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2930309' AND taken.member_id IS NULL;

-- 144 : 2931417 : 임하윤
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner144' OR taken.email='owner144@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner144',m.email='owner144@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '임하윤' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '임하윤' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_2931417' AND m.email='dummy_owner_2931417@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2931417' AND taken.member_id IS NULL;

-- 145 : 4064963 : 임준서
UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON (taken.login_id='owner145' OR taken.email='owner145@example.test') AND taken.member_id<>m.member_id
SET m.login_id='owner145',m.email='owner145@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname LIKE '숙소 예시 %' THEN '임준서' ELSE m.nickname END,m.updated_at=NOW(),
    o.representative_name=CASE WHEN o.representative_name='예시 업주' THEN '임준서' ELSE o.representative_name END,o.updated_at=NOW()
WHERE m.login_id='dummy_owner_4064963' AND m.email='dummy_owner_4064963@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-4064963' AND taken.member_id IS NULL;

UPDATE members m LEFT JOIN members taken ON (taken.login_id='user1' OR taken.email='user1@example.test') AND taken.member_id<>m.member_id
SET m.login_id='user1',m.email='user1@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname='여행자 예시 1' THEN '여행자1' ELSE m.nickname END,m.updated_at=NOW()
WHERE m.login_id='dummy_user_1' AND m.email='dummy_user_1@example.test' AND m.role_code='USER' AND taken.member_id IS NULL;

UPDATE members m LEFT JOIN members taken ON (taken.login_id='user2' OR taken.email='user2@example.test') AND taken.member_id<>m.member_id
SET m.login_id='user2',m.email='user2@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname='여행자 예시 2' THEN '여행자2' ELSE m.nickname END,m.updated_at=NOW()
WHERE m.login_id='dummy_user_2' AND m.email='dummy_user_2@example.test' AND m.role_code='USER' AND taken.member_id IS NULL;

UPDATE members m LEFT JOIN members taken ON (taken.login_id='user3' OR taken.email='user3@example.test') AND taken.member_id<>m.member_id
SET m.login_id='user3',m.email='user3@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname='여행자 예시 3' THEN '여행자3' ELSE m.nickname END,m.updated_at=NOW()
WHERE m.login_id='dummy_user_3' AND m.email='dummy_user_3@example.test' AND m.role_code='USER' AND taken.member_id IS NULL;

UPDATE members m LEFT JOIN members taken ON (taken.login_id='user4' OR taken.email='user4@example.test') AND taken.member_id<>m.member_id
SET m.login_id='user4',m.email='user4@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname='여행자 예시 4' THEN '여행자4' ELSE m.nickname END,m.updated_at=NOW()
WHERE m.login_id='dummy_user_4' AND m.email='dummy_user_4@example.test' AND m.role_code='USER' AND taken.member_id IS NULL;

UPDATE members m LEFT JOIN members taken ON (taken.login_id='user5' OR taken.email='user5@example.test') AND taken.member_id<>m.member_id
SET m.login_id='user5',m.email='user5@example.test',m.password='$2a$10$34P7j5RsomxUAe3CWO2DZe2l1jAatHOFPPBw0uHAlsTi2QxfGNAbu',m.nickname=CASE WHEN m.nickname='여행자 예시 5' THEN '여행자5' ELSE m.nickname END,m.updated_at=NOW()
WHERE m.login_id='dummy_user_5' AND m.email='dummy_user_5@example.test' AND m.role_code='USER' AND taken.member_id IS NULL;

-- 확인: 145명 모두 자동 적재되어 있다면 owner_count=145, 서로 다른 대표명=145
SELECT COUNT(*) AS owner_count,COUNT(DISTINCT o.representative_name) AS distinct_names FROM members m JOIN owner_profiles o ON o.member_id=m.member_id WHERE m.role_code='OWNER' AND m.login_id REGEXP '^owner([1-9]|[1-9][0-9]|1[0-3][0-9]|14[0-5])$' AND m.email=CONCAT(m.login_id,'@example.test') AND o.business_number LIKE 'DUMMY-%';
SELECT m.login_id,o.business_number,o.representative_name FROM members m JOIN owner_profiles o ON o.member_id=m.member_id WHERE m.role_code='OWNER' AND m.login_id REGEXP '^owner([1-9]|[1-9][0-9]|1[0-3][0-9]|14[0-5])$' ORDER BY CAST(SUBSTRING(m.login_id,6) AS UNSIGNED);

-- ROLLBACK: 아래 주석에서 필요한 행만 주석을 풀어 실행하세요. 수동 수정한 닉네임/대표명은 보존합니다.
-- 기존 BCrypt 비밀번호 해시는 행마다 달랐으므로 이 SQL로 원복할 수 없습니다. 필요하면 DB 백업에서 복원하세요.
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_126715' OR taken.email='dummy_owner_126715@example.test'
-- SET m.login_id='dummy_owner_126715',m.email='dummy_owner_126715@example.test',m.nickname=CASE WHEN m.nickname='홍길동' THEN '숙소 예시 1' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='홍길동' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner1' AND m.email='owner1@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-126715' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_127654' OR taken.email='dummy_owner_127654@example.test'
-- SET m.login_id='dummy_owner_127654',m.email='dummy_owner_127654@example.test',m.nickname=CASE WHEN m.nickname='김민수' THEN '숙소 예시 2' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='김민수' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner2' AND m.email='owner2@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-127654' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_127812' OR taken.email='dummy_owner_127812@example.test'
-- SET m.login_id='dummy_owner_127812',m.email='dummy_owner_127812@example.test',m.nickname=CASE WHEN m.nickname='김철수' THEN '숙소 예시 3' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='김철수' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner3' AND m.email='owner3@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-127812' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_129565' OR taken.email='dummy_owner_129565@example.test'
-- SET m.login_id='dummy_owner_129565',m.email='dummy_owner_129565@example.test',m.nickname=CASE WHEN m.nickname='김태현' THEN '숙소 예시 4' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='김태현' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner4' AND m.email='owner4@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-129565' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_136039' OR taken.email='dummy_owner_136039@example.test'
-- SET m.login_id='dummy_owner_136039',m.email='dummy_owner_136039@example.test',m.nickname=CASE WHEN m.nickname='김서연' THEN '숙소 예시 5' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='김서연' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner5' AND m.email='owner5@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136039' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_136063' OR taken.email='dummy_owner_136063@example.test'
-- SET m.login_id='dummy_owner_136063',m.email='dummy_owner_136063@example.test',m.nickname=CASE WHEN m.nickname='김도윤' THEN '숙소 예시 6' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='김도윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner6' AND m.email='owner6@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136063' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_136093' OR taken.email='dummy_owner_136093@example.test'
-- SET m.login_id='dummy_owner_136093',m.email='dummy_owner_136093@example.test',m.nickname=CASE WHEN m.nickname='김수빈' THEN '숙소 예시 7' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='김수빈' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner7' AND m.email='owner7@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136093' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_136208' OR taken.email='dummy_owner_136208@example.test'
-- SET m.login_id='dummy_owner_136208',m.email='dummy_owner_136208@example.test',m.nickname=CASE WHEN m.nickname='김현우' THEN '숙소 예시 8' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='김현우' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner8' AND m.email='owner8@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136208' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_136211' OR taken.email='dummy_owner_136211@example.test'
-- SET m.login_id='dummy_owner_136211',m.email='dummy_owner_136211@example.test',m.nickname=CASE WHEN m.nickname='김하윤' THEN '숙소 예시 9' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='김하윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner9' AND m.email='owner9@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136211' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_136213' OR taken.email='dummy_owner_136213@example.test'
-- SET m.login_id='dummy_owner_136213',m.email='dummy_owner_136213@example.test',m.nickname=CASE WHEN m.nickname='김준서' THEN '숙소 예시 10' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='김준서' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner10' AND m.email='owner10@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136213' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_136249' OR taken.email='dummy_owner_136249@example.test'
-- SET m.login_id='dummy_owner_136249',m.email='dummy_owner_136249@example.test',m.nickname=CASE WHEN m.nickname='김예린' THEN '숙소 예시 11' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='김예린' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner11' AND m.email='owner11@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136249' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_136422' OR taken.email='dummy_owner_136422@example.test'
-- SET m.login_id='dummy_owner_136422',m.email='dummy_owner_136422@example.test',m.nickname=CASE WHEN m.nickname='김태민' THEN '숙소 예시 12' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='김태민' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner12' AND m.email='owner12@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136422' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_136453' OR taken.email='dummy_owner_136453@example.test'
-- SET m.login_id='dummy_owner_136453',m.email='dummy_owner_136453@example.test',m.nickname=CASE WHEN m.nickname='김소윤' THEN '숙소 예시 13' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='김소윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner13' AND m.email='owner13@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136453' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_136485' OR taken.email='dummy_owner_136485@example.test'
-- SET m.login_id='dummy_owner_136485',m.email='dummy_owner_136485@example.test',m.nickname=CASE WHEN m.nickname='김우진' THEN '숙소 예시 14' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='김우진' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner14' AND m.email='owner14@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136485' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_136520' OR taken.email='dummy_owner_136520@example.test'
-- SET m.login_id='dummy_owner_136520',m.email='dummy_owner_136520@example.test',m.nickname=CASE WHEN m.nickname='김채원' THEN '숙소 예시 15' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='김채원' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner15' AND m.email='owner15@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136520' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_136538' OR taken.email='dummy_owner_136538@example.test'
-- SET m.login_id='dummy_owner_136538',m.email='dummy_owner_136538@example.test',m.nickname=CASE WHEN m.nickname='김시우' THEN '숙소 예시 16' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='김시우' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner16' AND m.email='owner16@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136538' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_136542' OR taken.email='dummy_owner_136542@example.test'
-- SET m.login_id='dummy_owner_136542',m.email='dummy_owner_136542@example.test',m.nickname=CASE WHEN m.nickname='김유진' THEN '숙소 예시 17' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='김유진' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner17' AND m.email='owner17@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136542' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_136639' OR taken.email='dummy_owner_136639@example.test'
-- SET m.login_id='dummy_owner_136639',m.email='dummy_owner_136639@example.test',m.nickname=CASE WHEN m.nickname='김지민' THEN '숙소 예시 18' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='김지민' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner18' AND m.email='owner18@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136639' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_136641' OR taken.email='dummy_owner_136641@example.test'
-- SET m.login_id='dummy_owner_136641',m.email='dummy_owner_136641@example.test',m.nickname=CASE WHEN m.nickname='이태현' THEN '숙소 예시 19' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='이태현' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner19' AND m.email='owner19@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136641' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_136642' OR taken.email='dummy_owner_136642@example.test'
-- SET m.login_id='dummy_owner_136642',m.email='dummy_owner_136642@example.test',m.nickname=CASE WHEN m.nickname='이서연' THEN '숙소 예시 20' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='이서연' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner20' AND m.email='owner20@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-136642' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_137315' OR taken.email='dummy_owner_137315@example.test'
-- SET m.login_id='dummy_owner_137315',m.email='dummy_owner_137315@example.test',m.nickname=CASE WHEN m.nickname='이도윤' THEN '숙소 예시 21' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='이도윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner21' AND m.email='owner21@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137315' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_137379' OR taken.email='dummy_owner_137379@example.test'
-- SET m.login_id='dummy_owner_137379',m.email='dummy_owner_137379@example.test',m.nickname=CASE WHEN m.nickname='이수빈' THEN '숙소 예시 22' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='이수빈' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner22' AND m.email='owner22@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137379' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_137476' OR taken.email='dummy_owner_137476@example.test'
-- SET m.login_id='dummy_owner_137476',m.email='dummy_owner_137476@example.test',m.nickname=CASE WHEN m.nickname='이현우' THEN '숙소 예시 23' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='이현우' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner23' AND m.email='owner23@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137476' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_137477' OR taken.email='dummy_owner_137477@example.test'
-- SET m.login_id='dummy_owner_137477',m.email='dummy_owner_137477@example.test',m.nickname=CASE WHEN m.nickname='이하윤' THEN '숙소 예시 24' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='이하윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner24' AND m.email='owner24@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137477' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_137483' OR taken.email='dummy_owner_137483@example.test'
-- SET m.login_id='dummy_owner_137483',m.email='dummy_owner_137483@example.test',m.nickname=CASE WHEN m.nickname='이준서' THEN '숙소 예시 25' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='이준서' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner25' AND m.email='owner25@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137483' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_137486' OR taken.email='dummy_owner_137486@example.test'
-- SET m.login_id='dummy_owner_137486',m.email='dummy_owner_137486@example.test',m.nickname=CASE WHEN m.nickname='이예린' THEN '숙소 예시 26' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='이예린' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner26' AND m.email='owner26@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137486' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_137497' OR taken.email='dummy_owner_137497@example.test'
-- SET m.login_id='dummy_owner_137497',m.email='dummy_owner_137497@example.test',m.nickname=CASE WHEN m.nickname='이태민' THEN '숙소 예시 27' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='이태민' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner27' AND m.email='owner27@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137497' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_137499' OR taken.email='dummy_owner_137499@example.test'
-- SET m.login_id='dummy_owner_137499',m.email='dummy_owner_137499@example.test',m.nickname=CASE WHEN m.nickname='이소윤' THEN '숙소 예시 28' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='이소윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner28' AND m.email='owner28@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137499' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_137706' OR taken.email='dummy_owner_137706@example.test'
-- SET m.login_id='dummy_owner_137706',m.email='dummy_owner_137706@example.test',m.nickname=CASE WHEN m.nickname='이우진' THEN '숙소 예시 29' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='이우진' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner29' AND m.email='owner29@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137706' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_137762' OR taken.email='dummy_owner_137762@example.test'
-- SET m.login_id='dummy_owner_137762',m.email='dummy_owner_137762@example.test',m.nickname=CASE WHEN m.nickname='이채원' THEN '숙소 예시 30' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='이채원' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner30' AND m.email='owner30@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137762' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_137788' OR taken.email='dummy_owner_137788@example.test'
-- SET m.login_id='dummy_owner_137788',m.email='dummy_owner_137788@example.test',m.nickname=CASE WHEN m.nickname='이시우' THEN '숙소 예시 31' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='이시우' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner31' AND m.email='owner31@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137788' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_137799' OR taken.email='dummy_owner_137799@example.test'
-- SET m.login_id='dummy_owner_137799',m.email='dummy_owner_137799@example.test',m.nickname=CASE WHEN m.nickname='이유진' THEN '숙소 예시 32' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='이유진' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner32' AND m.email='owner32@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137799' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_137809' OR taken.email='dummy_owner_137809@example.test'
-- SET m.login_id='dummy_owner_137809',m.email='dummy_owner_137809@example.test',m.nickname=CASE WHEN m.nickname='이지민' THEN '숙소 예시 33' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='이지민' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner33' AND m.email='owner33@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137809' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_137814' OR taken.email='dummy_owner_137814@example.test'
-- SET m.login_id='dummy_owner_137814',m.email='dummy_owner_137814@example.test',m.nickname=CASE WHEN m.nickname='박태현' THEN '숙소 예시 34' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='박태현' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner34' AND m.email='owner34@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137814' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_137826' OR taken.email='dummy_owner_137826@example.test'
-- SET m.login_id='dummy_owner_137826',m.email='dummy_owner_137826@example.test',m.nickname=CASE WHEN m.nickname='박서연' THEN '숙소 예시 35' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='박서연' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner35' AND m.email='owner35@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137826' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_137891' OR taken.email='dummy_owner_137891@example.test'
-- SET m.login_id='dummy_owner_137891',m.email='dummy_owner_137891@example.test',m.nickname=CASE WHEN m.nickname='박도윤' THEN '숙소 예시 36' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='박도윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner36' AND m.email='owner36@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137891' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_137953' OR taken.email='dummy_owner_137953@example.test'
-- SET m.login_id='dummy_owner_137953',m.email='dummy_owner_137953@example.test',m.nickname=CASE WHEN m.nickname='박수빈' THEN '숙소 예시 37' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='박수빈' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner37' AND m.email='owner37@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137953' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_137965' OR taken.email='dummy_owner_137965@example.test'
-- SET m.login_id='dummy_owner_137965',m.email='dummy_owner_137965@example.test',m.nickname=CASE WHEN m.nickname='박현우' THEN '숙소 예시 38' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='박현우' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner38' AND m.email='owner38@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-137965' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_138081' OR taken.email='dummy_owner_138081@example.test'
-- SET m.login_id='dummy_owner_138081',m.email='dummy_owner_138081@example.test',m.nickname=CASE WHEN m.nickname='박하윤' THEN '숙소 예시 39' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='박하윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner39' AND m.email='owner39@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138081' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_138089' OR taken.email='dummy_owner_138089@example.test'
-- SET m.login_id='dummy_owner_138089',m.email='dummy_owner_138089@example.test',m.nickname=CASE WHEN m.nickname='박준서' THEN '숙소 예시 40' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='박준서' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner40' AND m.email='owner40@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138089' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_138116' OR taken.email='dummy_owner_138116@example.test'
-- SET m.login_id='dummy_owner_138116',m.email='dummy_owner_138116@example.test',m.nickname=CASE WHEN m.nickname='박예린' THEN '숙소 예시 41' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='박예린' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner41' AND m.email='owner41@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138116' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_138184' OR taken.email='dummy_owner_138184@example.test'
-- SET m.login_id='dummy_owner_138184',m.email='dummy_owner_138184@example.test',m.nickname=CASE WHEN m.nickname='박태민' THEN '숙소 예시 42' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='박태민' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner42' AND m.email='owner42@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138184' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_138185' OR taken.email='dummy_owner_138185@example.test'
-- SET m.login_id='dummy_owner_138185',m.email='dummy_owner_138185@example.test',m.nickname=CASE WHEN m.nickname='박소윤' THEN '숙소 예시 43' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='박소윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner43' AND m.email='owner43@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138185' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_138195' OR taken.email='dummy_owner_138195@example.test'
-- SET m.login_id='dummy_owner_138195',m.email='dummy_owner_138195@example.test',m.nickname=CASE WHEN m.nickname='박우진' THEN '숙소 예시 44' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='박우진' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner44' AND m.email='owner44@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138195' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_138217' OR taken.email='dummy_owner_138217@example.test'
-- SET m.login_id='dummy_owner_138217',m.email='dummy_owner_138217@example.test',m.nickname=CASE WHEN m.nickname='박채원' THEN '숙소 예시 45' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='박채원' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner45' AND m.email='owner45@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138217' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_138352' OR taken.email='dummy_owner_138352@example.test'
-- SET m.login_id='dummy_owner_138352',m.email='dummy_owner_138352@example.test',m.nickname=CASE WHEN m.nickname='박시우' THEN '숙소 예시 46' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='박시우' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner46' AND m.email='owner46@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138352' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_138363' OR taken.email='dummy_owner_138363@example.test'
-- SET m.login_id='dummy_owner_138363',m.email='dummy_owner_138363@example.test',m.nickname=CASE WHEN m.nickname='박유진' THEN '숙소 예시 47' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='박유진' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner47' AND m.email='owner47@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138363' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_138486' OR taken.email='dummy_owner_138486@example.test'
-- SET m.login_id='dummy_owner_138486',m.email='dummy_owner_138486@example.test',m.nickname=CASE WHEN m.nickname='박지민' THEN '숙소 예시 48' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='박지민' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner48' AND m.email='owner48@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138486' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_138513' OR taken.email='dummy_owner_138513@example.test'
-- SET m.login_id='dummy_owner_138513',m.email='dummy_owner_138513@example.test',m.nickname=CASE WHEN m.nickname='최태현' THEN '숙소 예시 49' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='최태현' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner49' AND m.email='owner49@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138513' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_138574' OR taken.email='dummy_owner_138574@example.test'
-- SET m.login_id='dummy_owner_138574',m.email='dummy_owner_138574@example.test',m.nickname=CASE WHEN m.nickname='최서연' THEN '숙소 예시 50' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='최서연' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner50' AND m.email='owner50@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138574' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_138575' OR taken.email='dummy_owner_138575@example.test'
-- SET m.login_id='dummy_owner_138575',m.email='dummy_owner_138575@example.test',m.nickname=CASE WHEN m.nickname='최도윤' THEN '숙소 예시 51' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='최도윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner51' AND m.email='owner51@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138575' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_138675' OR taken.email='dummy_owner_138675@example.test'
-- SET m.login_id='dummy_owner_138675',m.email='dummy_owner_138675@example.test',m.nickname=CASE WHEN m.nickname='최수빈' THEN '숙소 예시 52' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='최수빈' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner52' AND m.email='owner52@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138675' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_138724' OR taken.email='dummy_owner_138724@example.test'
-- SET m.login_id='dummy_owner_138724',m.email='dummy_owner_138724@example.test',m.nickname=CASE WHEN m.nickname='최현우' THEN '숙소 예시 53' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='최현우' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner53' AND m.email='owner53@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138724' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_138764' OR taken.email='dummy_owner_138764@example.test'
-- SET m.login_id='dummy_owner_138764',m.email='dummy_owner_138764@example.test',m.nickname=CASE WHEN m.nickname='최하윤' THEN '숙소 예시 54' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='최하윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner54' AND m.email='owner54@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138764' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_138964' OR taken.email='dummy_owner_138964@example.test'
-- SET m.login_id='dummy_owner_138964',m.email='dummy_owner_138964@example.test',m.nickname=CASE WHEN m.nickname='최준서' THEN '숙소 예시 55' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='최준서' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner55' AND m.email='owner55@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-138964' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_139008' OR taken.email='dummy_owner_139008@example.test'
-- SET m.login_id='dummy_owner_139008',m.email='dummy_owner_139008@example.test',m.nickname=CASE WHEN m.nickname='최예린' THEN '숙소 예시 56' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='최예린' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner56' AND m.email='owner56@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-139008' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_139488' OR taken.email='dummy_owner_139488@example.test'
-- SET m.login_id='dummy_owner_139488',m.email='dummy_owner_139488@example.test',m.nickname=CASE WHEN m.nickname='최태민' THEN '숙소 예시 57' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='최태민' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner57' AND m.email='owner57@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-139488' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_139512' OR taken.email='dummy_owner_139512@example.test'
-- SET m.login_id='dummy_owner_139512',m.email='dummy_owner_139512@example.test',m.nickname=CASE WHEN m.nickname='최소윤' THEN '숙소 예시 58' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='최소윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner58' AND m.email='owner58@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-139512' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_139880' OR taken.email='dummy_owner_139880@example.test'
-- SET m.login_id='dummy_owner_139880',m.email='dummy_owner_139880@example.test',m.nickname=CASE WHEN m.nickname='최우진' THEN '숙소 예시 59' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='최우진' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner59' AND m.email='owner59@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-139880' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_139909' OR taken.email='dummy_owner_139909@example.test'
-- SET m.login_id='dummy_owner_139909',m.email='dummy_owner_139909@example.test',m.nickname=CASE WHEN m.nickname='최채원' THEN '숙소 예시 60' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='최채원' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner60' AND m.email='owner60@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-139909' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_142721' OR taken.email='dummy_owner_142721@example.test'
-- SET m.login_id='dummy_owner_142721',m.email='dummy_owner_142721@example.test',m.nickname=CASE WHEN m.nickname='최시우' THEN '숙소 예시 61' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='최시우' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner61' AND m.email='owner61@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142721' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_142722' OR taken.email='dummy_owner_142722@example.test'
-- SET m.login_id='dummy_owner_142722',m.email='dummy_owner_142722@example.test',m.nickname=CASE WHEN m.nickname='최유진' THEN '숙소 예시 62' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='최유진' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner62' AND m.email='owner62@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142722' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_142723' OR taken.email='dummy_owner_142723@example.test'
-- SET m.login_id='dummy_owner_142723',m.email='dummy_owner_142723@example.test',m.nickname=CASE WHEN m.nickname='최지민' THEN '숙소 예시 63' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='최지민' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner63' AND m.email='owner63@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142723' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_142729' OR taken.email='dummy_owner_142729@example.test'
-- SET m.login_id='dummy_owner_142729',m.email='dummy_owner_142729@example.test',m.nickname=CASE WHEN m.nickname='정태현' THEN '숙소 예시 64' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='정태현' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner64' AND m.email='owner64@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142729' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_142730' OR taken.email='dummy_owner_142730@example.test'
-- SET m.login_id='dummy_owner_142730',m.email='dummy_owner_142730@example.test',m.nickname=CASE WHEN m.nickname='정서연' THEN '숙소 예시 65' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='정서연' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner65' AND m.email='owner65@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142730' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_142801' OR taken.email='dummy_owner_142801@example.test'
-- SET m.login_id='dummy_owner_142801',m.email='dummy_owner_142801@example.test',m.nickname=CASE WHEN m.nickname='정도윤' THEN '숙소 예시 66' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='정도윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner66' AND m.email='owner66@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142801' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_142833' OR taken.email='dummy_owner_142833@example.test'
-- SET m.login_id='dummy_owner_142833',m.email='dummy_owner_142833@example.test',m.nickname=CASE WHEN m.nickname='정수빈' THEN '숙소 예시 67' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='정수빈' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner67' AND m.email='owner67@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142833' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_142839' OR taken.email='dummy_owner_142839@example.test'
-- SET m.login_id='dummy_owner_142839',m.email='dummy_owner_142839@example.test',m.nickname=CASE WHEN m.nickname='정현우' THEN '숙소 예시 68' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='정현우' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner68' AND m.email='owner68@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142839' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_142840' OR taken.email='dummy_owner_142840@example.test'
-- SET m.login_id='dummy_owner_142840',m.email='dummy_owner_142840@example.test',m.nickname=CASE WHEN m.nickname='정하윤' THEN '숙소 예시 69' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='정하윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner69' AND m.email='owner69@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142840' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_142851' OR taken.email='dummy_owner_142851@example.test'
-- SET m.login_id='dummy_owner_142851',m.email='dummy_owner_142851@example.test',m.nickname=CASE WHEN m.nickname='정준서' THEN '숙소 예시 70' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='정준서' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner70' AND m.email='owner70@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142851' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_142852' OR taken.email='dummy_owner_142852@example.test'
-- SET m.login_id='dummy_owner_142852',m.email='dummy_owner_142852@example.test',m.nickname=CASE WHEN m.nickname='정예린' THEN '숙소 예시 71' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='정예린' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner71' AND m.email='owner71@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142852' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_142853' OR taken.email='dummy_owner_142853@example.test'
-- SET m.login_id='dummy_owner_142853',m.email='dummy_owner_142853@example.test',m.nickname=CASE WHEN m.nickname='정태민' THEN '숙소 예시 72' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='정태민' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner72' AND m.email='owner72@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142853' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_142884' OR taken.email='dummy_owner_142884@example.test'
-- SET m.login_id='dummy_owner_142884',m.email='dummy_owner_142884@example.test',m.nickname=CASE WHEN m.nickname='정소윤' THEN '숙소 예시 73' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='정소윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner73' AND m.email='owner73@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142884' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_142892' OR taken.email='dummy_owner_142892@example.test'
-- SET m.login_id='dummy_owner_142892',m.email='dummy_owner_142892@example.test',m.nickname=CASE WHEN m.nickname='정우진' THEN '숙소 예시 74' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='정우진' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner74' AND m.email='owner74@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142892' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_142893' OR taken.email='dummy_owner_142893@example.test'
-- SET m.login_id='dummy_owner_142893',m.email='dummy_owner_142893@example.test',m.nickname=CASE WHEN m.nickname='정채원' THEN '숙소 예시 75' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='정채원' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner75' AND m.email='owner75@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142893' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_142894' OR taken.email='dummy_owner_142894@example.test'
-- SET m.login_id='dummy_owner_142894',m.email='dummy_owner_142894@example.test',m.nickname=CASE WHEN m.nickname='정시우' THEN '숙소 예시 76' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='정시우' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner76' AND m.email='owner76@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142894' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_142898' OR taken.email='dummy_owner_142898@example.test'
-- SET m.login_id='dummy_owner_142898',m.email='dummy_owner_142898@example.test',m.nickname=CASE WHEN m.nickname='정유진' THEN '숙소 예시 77' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='정유진' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner77' AND m.email='owner77@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142898' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_142903' OR taken.email='dummy_owner_142903@example.test'
-- SET m.login_id='dummy_owner_142903',m.email='dummy_owner_142903@example.test',m.nickname=CASE WHEN m.nickname='정지민' THEN '숙소 예시 78' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='정지민' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner78' AND m.email='owner78@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142903' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_142962' OR taken.email='dummy_owner_142962@example.test'
-- SET m.login_id='dummy_owner_142962',m.email='dummy_owner_142962@example.test',m.nickname=CASE WHEN m.nickname='강태현' THEN '숙소 예시 79' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='강태현' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner79' AND m.email='owner79@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-142962' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_143000' OR taken.email='dummy_owner_143000@example.test'
-- SET m.login_id='dummy_owner_143000',m.email='dummy_owner_143000@example.test',m.nickname=CASE WHEN m.nickname='강서연' THEN '숙소 예시 80' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='강서연' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner80' AND m.email='owner80@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-143000' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_143023' OR taken.email='dummy_owner_143023@example.test'
-- SET m.login_id='dummy_owner_143023',m.email='dummy_owner_143023@example.test',m.nickname=CASE WHEN m.nickname='강도윤' THEN '숙소 예시 81' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='강도윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner81' AND m.email='owner81@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-143023' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_143042' OR taken.email='dummy_owner_143042@example.test'
-- SET m.login_id='dummy_owner_143042',m.email='dummy_owner_143042@example.test',m.nickname=CASE WHEN m.nickname='강수빈' THEN '숙소 예시 82' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='강수빈' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner82' AND m.email='owner82@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-143042' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_143064' OR taken.email='dummy_owner_143064@example.test'
-- SET m.login_id='dummy_owner_143064',m.email='dummy_owner_143064@example.test',m.nickname=CASE WHEN m.nickname='강현우' THEN '숙소 예시 83' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='강현우' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner83' AND m.email='owner83@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-143064' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_143090' OR taken.email='dummy_owner_143090@example.test'
-- SET m.login_id='dummy_owner_143090',m.email='dummy_owner_143090@example.test',m.nickname=CASE WHEN m.nickname='강하윤' THEN '숙소 예시 84' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='강하윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner84' AND m.email='owner84@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-143090' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_143106' OR taken.email='dummy_owner_143106@example.test'
-- SET m.login_id='dummy_owner_143106',m.email='dummy_owner_143106@example.test',m.nickname=CASE WHEN m.nickname='강준서' THEN '숙소 예시 85' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='강준서' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner85' AND m.email='owner85@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-143106' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_143138' OR taken.email='dummy_owner_143138@example.test'
-- SET m.login_id='dummy_owner_143138',m.email='dummy_owner_143138@example.test',m.nickname=CASE WHEN m.nickname='강예린' THEN '숙소 예시 86' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='강예린' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner86' AND m.email='owner86@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-143138' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_143140' OR taken.email='dummy_owner_143140@example.test'
-- SET m.login_id='dummy_owner_143140',m.email='dummy_owner_143140@example.test',m.nickname=CASE WHEN m.nickname='강태민' THEN '숙소 예시 87' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='강태민' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner87' AND m.email='owner87@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-143140' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_143151' OR taken.email='dummy_owner_143151@example.test'
-- SET m.login_id='dummy_owner_143151',m.email='dummy_owner_143151@example.test',m.nickname=CASE WHEN m.nickname='강소윤' THEN '숙소 예시 88' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='강소윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner88' AND m.email='owner88@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-143151' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_143169' OR taken.email='dummy_owner_143169@example.test'
-- SET m.login_id='dummy_owner_143169',m.email='dummy_owner_143169@example.test',m.nickname=CASE WHEN m.nickname='강우진' THEN '숙소 예시 89' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='강우진' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner89' AND m.email='owner89@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-143169' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_228912' OR taken.email='dummy_owner_228912@example.test'
-- SET m.login_id='dummy_owner_228912',m.email='dummy_owner_228912@example.test',m.nickname=CASE WHEN m.nickname='강채원' THEN '숙소 예시 90' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='강채원' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner90' AND m.email='owner90@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-228912' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1003654' OR taken.email='dummy_owner_1003654@example.test'
-- SET m.login_id='dummy_owner_1003654',m.email='dummy_owner_1003654@example.test',m.nickname=CASE WHEN m.nickname='강시우' THEN '숙소 예시 91' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='강시우' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner91' AND m.email='owner91@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1003654' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1073480' OR taken.email='dummy_owner_1073480@example.test'
-- SET m.login_id='dummy_owner_1073480',m.email='dummy_owner_1073480@example.test',m.nickname=CASE WHEN m.nickname='강유진' THEN '숙소 예시 92' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='강유진' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner92' AND m.email='owner92@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1073480' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1102250' OR taken.email='dummy_owner_1102250@example.test'
-- SET m.login_id='dummy_owner_1102250',m.email='dummy_owner_1102250@example.test',m.nickname=CASE WHEN m.nickname='강지민' THEN '숙소 예시 93' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='강지민' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner93' AND m.email='owner93@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1102250' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1122064' OR taken.email='dummy_owner_1122064@example.test'
-- SET m.login_id='dummy_owner_1122064',m.email='dummy_owner_1122064@example.test',m.nickname=CASE WHEN m.nickname='조태현' THEN '숙소 예시 94' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='조태현' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner94' AND m.email='owner94@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1122064' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1144607' OR taken.email='dummy_owner_1144607@example.test'
-- SET m.login_id='dummy_owner_1144607',m.email='dummy_owner_1144607@example.test',m.nickname=CASE WHEN m.nickname='조서연' THEN '숙소 예시 95' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='조서연' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner95' AND m.email='owner95@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1144607' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1144770' OR taken.email='dummy_owner_1144770@example.test'
-- SET m.login_id='dummy_owner_1144770',m.email='dummy_owner_1144770@example.test',m.nickname=CASE WHEN m.nickname='조도윤' THEN '숙소 예시 96' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='조도윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner96' AND m.email='owner96@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1144770' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1161581' OR taken.email='dummy_owner_1161581@example.test'
-- SET m.login_id='dummy_owner_1161581',m.email='dummy_owner_1161581@example.test',m.nickname=CASE WHEN m.nickname='조수빈' THEN '숙소 예시 97' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='조수빈' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner97' AND m.email='owner97@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1161581' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1161739' OR taken.email='dummy_owner_1161739@example.test'
-- SET m.login_id='dummy_owner_1161739',m.email='dummy_owner_1161739@example.test',m.nickname=CASE WHEN m.nickname='조현우' THEN '숙소 예시 98' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='조현우' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner98' AND m.email='owner98@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1161739' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1162469' OR taken.email='dummy_owner_1162469@example.test'
-- SET m.login_id='dummy_owner_1162469',m.email='dummy_owner_1162469@example.test',m.nickname=CASE WHEN m.nickname='조하윤' THEN '숙소 예시 99' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='조하윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner99' AND m.email='owner99@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1162469' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1162526' OR taken.email='dummy_owner_1162526@example.test'
-- SET m.login_id='dummy_owner_1162526',m.email='dummy_owner_1162526@example.test',m.nickname=CASE WHEN m.nickname='조준서' THEN '숙소 예시 100' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='조준서' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner100' AND m.email='owner100@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1162526' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1177256' OR taken.email='dummy_owner_1177256@example.test'
-- SET m.login_id='dummy_owner_1177256',m.email='dummy_owner_1177256@example.test',m.nickname=CASE WHEN m.nickname='조예린' THEN '숙소 예시 101' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='조예린' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner101' AND m.email='owner101@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1177256' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1190304' OR taken.email='dummy_owner_1190304@example.test'
-- SET m.login_id='dummy_owner_1190304',m.email='dummy_owner_1190304@example.test',m.nickname=CASE WHEN m.nickname='조태민' THEN '숙소 예시 102' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='조태민' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner102' AND m.email='owner102@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1190304' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1266427' OR taken.email='dummy_owner_1266427@example.test'
-- SET m.login_id='dummy_owner_1266427',m.email='dummy_owner_1266427@example.test',m.nickname=CASE WHEN m.nickname='조소윤' THEN '숙소 예시 103' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='조소윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner103' AND m.email='owner103@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1266427' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1271229' OR taken.email='dummy_owner_1271229@example.test'
-- SET m.login_id='dummy_owner_1271229',m.email='dummy_owner_1271229@example.test',m.nickname=CASE WHEN m.nickname='조우진' THEN '숙소 예시 104' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='조우진' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner104' AND m.email='owner104@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1271229' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1271286' OR taken.email='dummy_owner_1271286@example.test'
-- SET m.login_id='dummy_owner_1271286',m.email='dummy_owner_1271286@example.test',m.nickname=CASE WHEN m.nickname='조채원' THEN '숙소 예시 105' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='조채원' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner105' AND m.email='owner105@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1271286' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1271632' OR taken.email='dummy_owner_1271632@example.test'
-- SET m.login_id='dummy_owner_1271632',m.email='dummy_owner_1271632@example.test',m.nickname=CASE WHEN m.nickname='조시우' THEN '숙소 예시 106' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='조시우' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner106' AND m.email='owner106@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1271632' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1272708' OR taken.email='dummy_owner_1272708@example.test'
-- SET m.login_id='dummy_owner_1272708',m.email='dummy_owner_1272708@example.test',m.nickname=CASE WHEN m.nickname='조유진' THEN '숙소 예시 107' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='조유진' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner107' AND m.email='owner107@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1272708' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1273588' OR taken.email='dummy_owner_1273588@example.test'
-- SET m.login_id='dummy_owner_1273588',m.email='dummy_owner_1273588@example.test',m.nickname=CASE WHEN m.nickname='조지민' THEN '숙소 예시 108' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='조지민' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner108' AND m.email='owner108@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1273588' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1274868' OR taken.email='dummy_owner_1274868@example.test'
-- SET m.login_id='dummy_owner_1274868',m.email='dummy_owner_1274868@example.test',m.nickname=CASE WHEN m.nickname='윤태현' THEN '숙소 예시 109' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='윤태현' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner109' AND m.email='owner109@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1274868' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1281934' OR taken.email='dummy_owner_1281934@example.test'
-- SET m.login_id='dummy_owner_1281934',m.email='dummy_owner_1281934@example.test',m.nickname=CASE WHEN m.nickname='윤서연' THEN '숙소 예시 110' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='윤서연' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner110' AND m.email='owner110@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1281934' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1300411' OR taken.email='dummy_owner_1300411@example.test'
-- SET m.login_id='dummy_owner_1300411',m.email='dummy_owner_1300411@example.test',m.nickname=CASE WHEN m.nickname='윤도윤' THEN '숙소 예시 111' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='윤도윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner111' AND m.email='owner111@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1300411' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1315136' OR taken.email='dummy_owner_1315136@example.test'
-- SET m.login_id='dummy_owner_1315136',m.email='dummy_owner_1315136@example.test',m.nickname=CASE WHEN m.nickname='윤수빈' THEN '숙소 예시 112' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='윤수빈' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner112' AND m.email='owner112@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1315136' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1327006' OR taken.email='dummy_owner_1327006@example.test'
-- SET m.login_id='dummy_owner_1327006',m.email='dummy_owner_1327006@example.test',m.nickname=CASE WHEN m.nickname='윤현우' THEN '숙소 예시 113' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='윤현우' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner113' AND m.email='owner113@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1327006' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1348840' OR taken.email='dummy_owner_1348840@example.test'
-- SET m.login_id='dummy_owner_1348840',m.email='dummy_owner_1348840@example.test',m.nickname=CASE WHEN m.nickname='윤하윤' THEN '숙소 예시 114' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='윤하윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner114' AND m.email='owner114@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1348840' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1351056' OR taken.email='dummy_owner_1351056@example.test'
-- SET m.login_id='dummy_owner_1351056',m.email='dummy_owner_1351056@example.test',m.nickname=CASE WHEN m.nickname='윤준서' THEN '숙소 예시 115' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='윤준서' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner115' AND m.email='owner115@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1351056' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1365261' OR taken.email='dummy_owner_1365261@example.test'
-- SET m.login_id='dummy_owner_1365261',m.email='dummy_owner_1365261@example.test',m.nickname=CASE WHEN m.nickname='윤예린' THEN '숙소 예시 116' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='윤예린' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner116' AND m.email='owner116@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1365261' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1369549' OR taken.email='dummy_owner_1369549@example.test'
-- SET m.login_id='dummy_owner_1369549',m.email='dummy_owner_1369549@example.test',m.nickname=CASE WHEN m.nickname='윤태민' THEN '숙소 예시 117' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='윤태민' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner117' AND m.email='owner117@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1369549' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1373262' OR taken.email='dummy_owner_1373262@example.test'
-- SET m.login_id='dummy_owner_1373262',m.email='dummy_owner_1373262@example.test',m.nickname=CASE WHEN m.nickname='윤소윤' THEN '숙소 예시 118' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='윤소윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner118' AND m.email='owner118@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1373262' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1638588' OR taken.email='dummy_owner_1638588@example.test'
-- SET m.login_id='dummy_owner_1638588',m.email='dummy_owner_1638588@example.test',m.nickname=CASE WHEN m.nickname='윤우진' THEN '숙소 예시 119' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='윤우진' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner119' AND m.email='owner119@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1638588' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1745972' OR taken.email='dummy_owner_1745972@example.test'
-- SET m.login_id='dummy_owner_1745972',m.email='dummy_owner_1745972@example.test',m.nickname=CASE WHEN m.nickname='윤채원' THEN '숙소 예시 120' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='윤채원' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner120' AND m.email='owner120@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1745972' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1747952' OR taken.email='dummy_owner_1747952@example.test'
-- SET m.login_id='dummy_owner_1747952',m.email='dummy_owner_1747952@example.test',m.nickname=CASE WHEN m.nickname='윤시우' THEN '숙소 예시 121' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='윤시우' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner121' AND m.email='owner121@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1747952' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1747999' OR taken.email='dummy_owner_1747999@example.test'
-- SET m.login_id='dummy_owner_1747999',m.email='dummy_owner_1747999@example.test',m.nickname=CASE WHEN m.nickname='윤유진' THEN '숙소 예시 122' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='윤유진' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner122' AND m.email='owner122@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1747999' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1748010' OR taken.email='dummy_owner_1748010@example.test'
-- SET m.login_id='dummy_owner_1748010',m.email='dummy_owner_1748010@example.test',m.nickname=CASE WHEN m.nickname='윤지민' THEN '숙소 예시 123' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='윤지민' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner123' AND m.email='owner123@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1748010' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1804605' OR taken.email='dummy_owner_1804605@example.test'
-- SET m.login_id='dummy_owner_1804605',m.email='dummy_owner_1804605@example.test',m.nickname=CASE WHEN m.nickname='장태현' THEN '숙소 예시 124' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='장태현' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner124' AND m.email='owner124@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1804605' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1882371' OR taken.email='dummy_owner_1882371@example.test'
-- SET m.login_id='dummy_owner_1882371',m.email='dummy_owner_1882371@example.test',m.nickname=CASE WHEN m.nickname='장서연' THEN '숙소 예시 125' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='장서연' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner125' AND m.email='owner125@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1882371' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1932079' OR taken.email='dummy_owner_1932079@example.test'
-- SET m.login_id='dummy_owner_1932079',m.email='dummy_owner_1932079@example.test',m.nickname=CASE WHEN m.nickname='장도윤' THEN '숙소 예시 126' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='장도윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner126' AND m.email='owner126@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1932079' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1933910' OR taken.email='dummy_owner_1933910@example.test'
-- SET m.login_id='dummy_owner_1933910',m.email='dummy_owner_1933910@example.test',m.nickname=CASE WHEN m.nickname='장수빈' THEN '숙소 예시 127' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='장수빈' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner127' AND m.email='owner127@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1933910' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1952929' OR taken.email='dummy_owner_1952929@example.test'
-- SET m.login_id='dummy_owner_1952929',m.email='dummy_owner_1952929@example.test',m.nickname=CASE WHEN m.nickname='장현우' THEN '숙소 예시 128' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='장현우' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner128' AND m.email='owner128@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1952929' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_1981882' OR taken.email='dummy_owner_1981882@example.test'
-- SET m.login_id='dummy_owner_1981882',m.email='dummy_owner_1981882@example.test',m.nickname=CASE WHEN m.nickname='장하윤' THEN '숙소 예시 129' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='장하윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner129' AND m.email='owner129@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-1981882' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_2050197' OR taken.email='dummy_owner_2050197@example.test'
-- SET m.login_id='dummy_owner_2050197',m.email='dummy_owner_2050197@example.test',m.nickname=CASE WHEN m.nickname='장준서' THEN '숙소 예시 130' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='장준서' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner130' AND m.email='owner130@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2050197' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_2366080' OR taken.email='dummy_owner_2366080@example.test'
-- SET m.login_id='dummy_owner_2366080',m.email='dummy_owner_2366080@example.test',m.nickname=CASE WHEN m.nickname='장예린' THEN '숙소 예시 131' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='장예린' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner131' AND m.email='owner131@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2366080' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_2377774' OR taken.email='dummy_owner_2377774@example.test'
-- SET m.login_id='dummy_owner_2377774',m.email='dummy_owner_2377774@example.test',m.nickname=CASE WHEN m.nickname='장태민' THEN '숙소 예시 132' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='장태민' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner132' AND m.email='owner132@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2377774' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_2392995' OR taken.email='dummy_owner_2392995@example.test'
-- SET m.login_id='dummy_owner_2392995',m.email='dummy_owner_2392995@example.test',m.nickname=CASE WHEN m.nickname='장소윤' THEN '숙소 예시 133' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='장소윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner133' AND m.email='owner133@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2392995' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_2570731' OR taken.email='dummy_owner_2570731@example.test'
-- SET m.login_id='dummy_owner_2570731',m.email='dummy_owner_2570731@example.test',m.nickname=CASE WHEN m.nickname='장우진' THEN '숙소 예시 134' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='장우진' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner134' AND m.email='owner134@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2570731' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_2570790' OR taken.email='dummy_owner_2570790@example.test'
-- SET m.login_id='dummy_owner_2570790',m.email='dummy_owner_2570790@example.test',m.nickname=CASE WHEN m.nickname='장채원' THEN '숙소 예시 135' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='장채원' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner135' AND m.email='owner135@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2570790' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_2570801' OR taken.email='dummy_owner_2570801@example.test'
-- SET m.login_id='dummy_owner_2570801',m.email='dummy_owner_2570801@example.test',m.nickname=CASE WHEN m.nickname='장시우' THEN '숙소 예시 136' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='장시우' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner136' AND m.email='owner136@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2570801' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_2594874' OR taken.email='dummy_owner_2594874@example.test'
-- SET m.login_id='dummy_owner_2594874',m.email='dummy_owner_2594874@example.test',m.nickname=CASE WHEN m.nickname='장유진' THEN '숙소 예시 137' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='장유진' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner137' AND m.email='owner137@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2594874' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_2668426' OR taken.email='dummy_owner_2668426@example.test'
-- SET m.login_id='dummy_owner_2668426',m.email='dummy_owner_2668426@example.test',m.nickname=CASE WHEN m.nickname='장지민' THEN '숙소 예시 138' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='장지민' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner138' AND m.email='owner138@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2668426' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_2708329' OR taken.email='dummy_owner_2708329@example.test'
-- SET m.login_id='dummy_owner_2708329',m.email='dummy_owner_2708329@example.test',m.nickname=CASE WHEN m.nickname='임태현' THEN '숙소 예시 139' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='임태현' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner139' AND m.email='owner139@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2708329' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_2770436' OR taken.email='dummy_owner_2770436@example.test'
-- SET m.login_id='dummy_owner_2770436',m.email='dummy_owner_2770436@example.test',m.nickname=CASE WHEN m.nickname='임서연' THEN '숙소 예시 140' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='임서연' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner140' AND m.email='owner140@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2770436' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_2770452' OR taken.email='dummy_owner_2770452@example.test'
-- SET m.login_id='dummy_owner_2770452',m.email='dummy_owner_2770452@example.test',m.nickname=CASE WHEN m.nickname='임도윤' THEN '숙소 예시 141' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='임도윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner141' AND m.email='owner141@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2770452' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_2913368' OR taken.email='dummy_owner_2913368@example.test'
-- SET m.login_id='dummy_owner_2913368',m.email='dummy_owner_2913368@example.test',m.nickname=CASE WHEN m.nickname='임수빈' THEN '숙소 예시 142' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='임수빈' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner142' AND m.email='owner142@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2913368' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_2930309' OR taken.email='dummy_owner_2930309@example.test'
-- SET m.login_id='dummy_owner_2930309',m.email='dummy_owner_2930309@example.test',m.nickname=CASE WHEN m.nickname='임현우' THEN '숙소 예시 143' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='임현우' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner143' AND m.email='owner143@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2930309' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_2931417' OR taken.email='dummy_owner_2931417@example.test'
-- SET m.login_id='dummy_owner_2931417',m.email='dummy_owner_2931417@example.test',m.nickname=CASE WHEN m.nickname='임하윤' THEN '숙소 예시 144' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='임하윤' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner144' AND m.email='owner144@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-2931417' AND taken.member_id IS NULL;
-- UPDATE members m JOIN owner_profiles o ON o.member_id=m.member_id LEFT JOIN members taken ON taken.login_id='dummy_owner_4064963' OR taken.email='dummy_owner_4064963@example.test'
-- SET m.login_id='dummy_owner_4064963',m.email='dummy_owner_4064963@example.test',m.nickname=CASE WHEN m.nickname='임준서' THEN '숙소 예시 145' ELSE m.nickname END,
--     o.representative_name=CASE WHEN o.representative_name='임준서' THEN '예시 업주' ELSE o.representative_name END
-- WHERE m.login_id='owner145' AND m.email='owner145@example.test' AND m.role_code='OWNER' AND o.business_number='DUMMY-4064963' AND taken.member_id IS NULL;
-- UPDATE members m LEFT JOIN members taken ON taken.login_id='dummy_user_1' OR taken.email='dummy_user_1@example.test'
-- SET m.login_id='dummy_user_1',m.email='dummy_user_1@example.test',m.nickname=CASE WHEN m.nickname='여행자1' THEN '여행자 예시 1' ELSE m.nickname END
-- WHERE m.login_id='user1' AND m.email='user1@example.test' AND m.role_code='USER' AND taken.member_id IS NULL;
-- UPDATE members m LEFT JOIN members taken ON taken.login_id='dummy_user_2' OR taken.email='dummy_user_2@example.test'
-- SET m.login_id='dummy_user_2',m.email='dummy_user_2@example.test',m.nickname=CASE WHEN m.nickname='여행자2' THEN '여행자 예시 2' ELSE m.nickname END
-- WHERE m.login_id='user2' AND m.email='user2@example.test' AND m.role_code='USER' AND taken.member_id IS NULL;
-- UPDATE members m LEFT JOIN members taken ON taken.login_id='dummy_user_3' OR taken.email='dummy_user_3@example.test'
-- SET m.login_id='dummy_user_3',m.email='dummy_user_3@example.test',m.nickname=CASE WHEN m.nickname='여행자3' THEN '여행자 예시 3' ELSE m.nickname END
-- WHERE m.login_id='user3' AND m.email='user3@example.test' AND m.role_code='USER' AND taken.member_id IS NULL;
-- UPDATE members m LEFT JOIN members taken ON taken.login_id='dummy_user_4' OR taken.email='dummy_user_4@example.test'
-- SET m.login_id='dummy_user_4',m.email='dummy_user_4@example.test',m.nickname=CASE WHEN m.nickname='여행자4' THEN '여행자 예시 4' ELSE m.nickname END
-- WHERE m.login_id='user4' AND m.email='user4@example.test' AND m.role_code='USER' AND taken.member_id IS NULL;
-- UPDATE members m LEFT JOIN members taken ON taken.login_id='dummy_user_5' OR taken.email='dummy_user_5@example.test'
-- SET m.login_id='dummy_user_5',m.email='dummy_user_5@example.test',m.nickname=CASE WHEN m.nickname='여행자5' THEN '여행자 예시 5' ELSE m.nickname END
-- WHERE m.login_id='user5' AND m.email='user5@example.test' AND m.role_code='USER' AND taken.member_id IS NULL;
