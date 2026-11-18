-- Local test fixtures only. Each set belongs to user1 and is clearly marked TEST.
-- Re-running preserves existing fixture rows and does not duplicate them.
-- Run only against the named local development database.
USE smart_travel_db;
SET NAMES utf8mb4;
START TRANSACTION;
SET @fixture_owner = (SELECT member_id FROM members WHERE login_id='user1' AND role_code='USER' AND status='ACTIVE' AND deleted_at IS NULL);
INSERT INTO generation_method(id,code) SELECT 990113,'MYPAGE_TEST_261113'
WHERE NOT EXISTS (SELECT 1 FROM generation_method WHERE code='MYPAGE_TEST_261113');
SET @fixture_method = (SELECT id FROM generation_method WHERE code='MYPAGE_TEST_261113');
INSERT INTO category(id,name) SELECT 990113,'테스트관광'
WHERE NOT EXISTS (SELECT 1 FROM category WHERE name='테스트관광');
SET @fixture_category = (SELECT id FROM category WHERE name='테스트관광');

-- MYPAGE-USER1-20261113-1: 서울 문화 산책
INSERT INTO lodging(name,address,latitude,longitude,lodging_type,checkin_time,checkout_time,description,source_provider,source_content_id,status)
SELECT '[테스트] 서울 샘플 숙소','[테스트 주소] 서울 / 실제 숙소가 아닙니다',37.5796,126.977,'HOTEL','15:00:00','11:00:00','{"testData":true,"fixture":"MYPAGE-USER1-20261113-1","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}','TEST','MYPAGE-USER1-20261113-1','APPROVED'
WHERE NOT EXISTS (SELECT 1 FROM lodging WHERE source_provider='TEST' AND source_content_id='MYPAGE-USER1-20261113-1');
SET @fixture_lodging = (SELECT lodging_id FROM lodging WHERE source_provider='TEST' AND source_content_id='MYPAGE-USER1-20261113-1');
INSERT INTO room(lodging_id,name,base_capacity,max_capacity,price_off_weekday,price_off_weekend,description)
SELECT @fixture_lodging,'[테스트] 스탠다드룸',2,4,120000,120000,'{"testData":true,"fixture":"MYPAGE-USER1-20261113-1","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}'
WHERE NOT EXISTS (SELECT 1 FROM room WHERE lodging_id=@fixture_lodging AND name='[테스트] 스탠다드룸');
SET @fixture_room = (SELECT room_id FROM room WHERE lodging_id=@fixture_lodging AND name='[테스트] 스탠다드룸');
INSERT INTO course(member_no,generation_method_id,title,destination,start_date,end_date,status,preferences_snapshot,condition_meta,summary,adults,companion_type,share_consent,lock_version)
SELECT @fixture_owner,@fixture_method,'[테스트] 서울 문화 산책 코스','서울','2026-10-10','2026-10-11','done','{"testData":true,"fixture":"MYPAGE-USER1-20261113-1","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}','{"testData":true,"fixture":"MYPAGE-USER1-20261113-1","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}','[테스트 데이터] 일정 상세·예약 연결·추천 확인용',2,'친구',TRUE,1
WHERE NOT EXISTS (SELECT 1 FROM course WHERE member_no=@fixture_owner AND JSON_UNQUOTE(JSON_EXTRACT(condition_meta,'$.fixture'))='MYPAGE-USER1-20261113-1');
SET @fixture_course = (SELECT id FROM course WHERE member_no=@fixture_owner AND JSON_UNQUOTE(JSON_EXTRACT(condition_meta,'$.fixture'))='MYPAGE-USER1-20261113-1' LIMIT 1);
INSERT INTO course_day(course_id,day_no,visit_date,theme) SELECT @fixture_course,1,'2026-10-10','[테스트] 관광 일정'
WHERE NOT EXISTS (SELECT 1 FROM course_day WHERE course_id=@fixture_course AND day_no=1);
SET @fixture_day = (SELECT id FROM course_day WHERE course_id=@fixture_course AND day_no=1);
INSERT INTO activity(course_day_id,category_id,seq,start_time,end_time,title,place_name,note)
SELECT @fixture_day,@fixture_category,1,'10:00:00','11:00:00','경복궁','경복궁','{"testData":true,"fixture":"MYPAGE-USER1-20261113-1","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}'
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE course_day_id=@fixture_day AND seq=1);
INSERT INTO activity(course_day_id,category_id,seq,start_time,end_time,title,place_name,note)
SELECT @fixture_day,@fixture_category,2,'13:00:00','14:00:00','인사동','인사동','{"testData":true,"fixture":"MYPAGE-USER1-20261113-1","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}'
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE course_day_id=@fixture_day AND seq=2);
INSERT INTO activity(course_day_id,category_id,seq,start_time,end_time,title,place_name,note)
SELECT @fixture_day,@fixture_category,3,'16:00:00','17:00:00','청계천','청계천','{"testData":true,"fixture":"MYPAGE-USER1-20261113-1","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}'
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE course_day_id=@fixture_day AND seq=3);
INSERT INTO course_day(course_id,day_no,visit_date,theme) SELECT @fixture_course,2,'2026-10-11','[테스트] 관광 일정'
WHERE NOT EXISTS (SELECT 1 FROM course_day WHERE course_id=@fixture_course AND day_no=2);
SET @fixture_day = (SELECT id FROM course_day WHERE course_id=@fixture_course AND day_no=2);
INSERT INTO activity(course_day_id,category_id,seq,start_time,end_time,title,place_name,note)
SELECT @fixture_day,@fixture_category,1,'10:00:00','11:00:00','북촌 한옥마을','북촌 한옥마을','{"testData":true,"fixture":"MYPAGE-USER1-20261113-1","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}'
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE course_day_id=@fixture_day AND seq=1);
INSERT INTO activity(course_day_id,category_id,seq,start_time,end_time,title,place_name,note)
SELECT @fixture_day,@fixture_category,2,'13:00:00','14:00:00','서울숲','서울숲','{"testData":true,"fixture":"MYPAGE-USER1-20261113-1","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}'
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE course_day_id=@fixture_day AND seq=2);
INSERT INTO room_reservation(member_id,course_id,room_id,reservation_no,booker_name,booker_phone,checkin_date,checkout_date,guest_count,total_price,status,request_memo)
SELECT @fixture_owner,@fixture_course,@fixture_room,'TEST-MYPAGE-USER1-20261113-1','[테스트] 예약자','000-0000-0000','2026-10-10','2026-10-11',2,120000,'COMPLETED','{"testData":true,"fixture":"MYPAGE-USER1-20261113-1","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}'
WHERE NOT EXISTS (SELECT 1 FROM room_reservation WHERE reservation_no='TEST-MYPAGE-USER1-20261113-1');
SET @fixture_reservation = (SELECT reservation_id FROM room_reservation WHERE reservation_no='TEST-MYPAGE-USER1-20261113-1');
INSERT INTO human_course_post(author_id,course_id,reservation_id,title,content,status,course_snapshot,companion_type,category,summary)
SELECT @fixture_owner,@fixture_course,@fixture_reservation,'[테스트] 서울 문화 산책 추천','[테스트 데이터 / MYPAGE-USER1-20261113-1] 실제 여행 후기나 결제 내역이 아닙니다.
경복궁 → 인사동 → 청계천 순서로 둘러보는 1박 2일 추천 일정입니다.
마이페이지 목록·상세·공개 화면·글 수정·연결 예약 이동을 확인할 수 있습니다.','VISIBLE','{"region":"서울","startDate":"2026-10-10","endDate":"2026-10-11","version":1,"plan":[{"day":1,"stops":["경복궁","인사동","청계천"]},{"day":2,"stops":["북촌 한옥마을","서울숲"]}]}','친구','문화','[테스트 데이터] 마이페이지 기능 확인용 추천 코스'
WHERE NOT EXISTS (SELECT 1 FROM human_course_post WHERE reservation_id=@fixture_reservation);

-- MYPAGE-USER1-20261113-2: 부산 바다 여행
INSERT INTO lodging(name,address,latitude,longitude,lodging_type,checkin_time,checkout_time,description,source_provider,source_content_id,status)
SELECT '[테스트] 부산 샘플 숙소','[테스트 주소] 부산 / 실제 숙소가 아닙니다',35.1587,129.1604,'HOTEL','15:00:00','11:00:00','{"testData":true,"fixture":"MYPAGE-USER1-20261113-2","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}','TEST','MYPAGE-USER1-20261113-2','APPROVED'
WHERE NOT EXISTS (SELECT 1 FROM lodging WHERE source_provider='TEST' AND source_content_id='MYPAGE-USER1-20261113-2');
SET @fixture_lodging = (SELECT lodging_id FROM lodging WHERE source_provider='TEST' AND source_content_id='MYPAGE-USER1-20261113-2');
INSERT INTO room(lodging_id,name,base_capacity,max_capacity,price_off_weekday,price_off_weekend,description)
SELECT @fixture_lodging,'[테스트] 스탠다드룸',2,4,150000,150000,'{"testData":true,"fixture":"MYPAGE-USER1-20261113-2","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}'
WHERE NOT EXISTS (SELECT 1 FROM room WHERE lodging_id=@fixture_lodging AND name='[테스트] 스탠다드룸');
SET @fixture_room = (SELECT room_id FROM room WHERE lodging_id=@fixture_lodging AND name='[테스트] 스탠다드룸');
INSERT INTO course(member_no,generation_method_id,title,destination,start_date,end_date,status,preferences_snapshot,condition_meta,summary,adults,companion_type,share_consent,lock_version)
SELECT @fixture_owner,@fixture_method,'[테스트] 부산 바다 여행 코스','부산','2026-10-17','2026-10-18','done','{"testData":true,"fixture":"MYPAGE-USER1-20261113-2","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}','{"testData":true,"fixture":"MYPAGE-USER1-20261113-2","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}','[테스트 데이터] 일정 상세·예약 연결·추천 확인용',2,'연인',TRUE,1
WHERE NOT EXISTS (SELECT 1 FROM course WHERE member_no=@fixture_owner AND JSON_UNQUOTE(JSON_EXTRACT(condition_meta,'$.fixture'))='MYPAGE-USER1-20261113-2');
SET @fixture_course = (SELECT id FROM course WHERE member_no=@fixture_owner AND JSON_UNQUOTE(JSON_EXTRACT(condition_meta,'$.fixture'))='MYPAGE-USER1-20261113-2' LIMIT 1);
INSERT INTO course_day(course_id,day_no,visit_date,theme) SELECT @fixture_course,1,'2026-10-17','[테스트] 관광 일정'
WHERE NOT EXISTS (SELECT 1 FROM course_day WHERE course_id=@fixture_course AND day_no=1);
SET @fixture_day = (SELECT id FROM course_day WHERE course_id=@fixture_course AND day_no=1);
INSERT INTO activity(course_day_id,category_id,seq,start_time,end_time,title,place_name,note)
SELECT @fixture_day,@fixture_category,1,'10:00:00','11:00:00','해운대 해수욕장','해운대 해수욕장','{"testData":true,"fixture":"MYPAGE-USER1-20261113-2","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}'
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE course_day_id=@fixture_day AND seq=1);
INSERT INTO activity(course_day_id,category_id,seq,start_time,end_time,title,place_name,note)
SELECT @fixture_day,@fixture_category,2,'13:00:00','14:00:00','동백섬','동백섬','{"testData":true,"fixture":"MYPAGE-USER1-20261113-2","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}'
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE course_day_id=@fixture_day AND seq=2);
INSERT INTO activity(course_day_id,category_id,seq,start_time,end_time,title,place_name,note)
SELECT @fixture_day,@fixture_category,3,'16:00:00','17:00:00','광안리','광안리','{"testData":true,"fixture":"MYPAGE-USER1-20261113-2","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}'
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE course_day_id=@fixture_day AND seq=3);
INSERT INTO course_day(course_id,day_no,visit_date,theme) SELECT @fixture_course,2,'2026-10-18','[테스트] 관광 일정'
WHERE NOT EXISTS (SELECT 1 FROM course_day WHERE course_id=@fixture_course AND day_no=2);
SET @fixture_day = (SELECT id FROM course_day WHERE course_id=@fixture_course AND day_no=2);
INSERT INTO activity(course_day_id,category_id,seq,start_time,end_time,title,place_name,note)
SELECT @fixture_day,@fixture_category,1,'10:00:00','11:00:00','감천문화마을','감천문화마을','{"testData":true,"fixture":"MYPAGE-USER1-20261113-2","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}'
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE course_day_id=@fixture_day AND seq=1);
INSERT INTO activity(course_day_id,category_id,seq,start_time,end_time,title,place_name,note)
SELECT @fixture_day,@fixture_category,2,'13:00:00','14:00:00','자갈치시장','자갈치시장','{"testData":true,"fixture":"MYPAGE-USER1-20261113-2","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}'
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE course_day_id=@fixture_day AND seq=2);
INSERT INTO room_reservation(member_id,course_id,room_id,reservation_no,booker_name,booker_phone,checkin_date,checkout_date,guest_count,total_price,status,request_memo)
SELECT @fixture_owner,@fixture_course,@fixture_room,'TEST-MYPAGE-USER1-20261113-2','[테스트] 예약자','000-0000-0000','2026-10-17','2026-10-18',2,150000,'COMPLETED','{"testData":true,"fixture":"MYPAGE-USER1-20261113-2","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}'
WHERE NOT EXISTS (SELECT 1 FROM room_reservation WHERE reservation_no='TEST-MYPAGE-USER1-20261113-2');
SET @fixture_reservation = (SELECT reservation_id FROM room_reservation WHERE reservation_no='TEST-MYPAGE-USER1-20261113-2');
INSERT INTO human_course_post(author_id,course_id,reservation_id,title,content,status,course_snapshot,companion_type,category,summary)
SELECT @fixture_owner,@fixture_course,@fixture_reservation,'[테스트] 부산 바다 여행 추천','[테스트 데이터 / MYPAGE-USER1-20261113-2] 실제 여행 후기나 결제 내역이 아닙니다.
해운대 해수욕장 → 동백섬 → 광안리 순서로 둘러보는 1박 2일 추천 일정입니다.
마이페이지 목록·상세·공개 화면·글 수정·연결 예약 이동을 확인할 수 있습니다.','VISIBLE','{"region":"부산","startDate":"2026-10-17","endDate":"2026-10-18","version":1,"plan":[{"day":1,"stops":["해운대 해수욕장","동백섬","광안리"]},{"day":2,"stops":["감천문화마을","자갈치시장"]}]}','연인','힐링','[테스트 데이터] 마이페이지 기능 확인용 추천 코스'
WHERE NOT EXISTS (SELECT 1 FROM human_course_post WHERE reservation_id=@fixture_reservation);

-- MYPAGE-USER1-20261113-3: 제주 자연 여행
INSERT INTO lodging(name,address,latitude,longitude,lodging_type,checkin_time,checkout_time,description,source_provider,source_content_id,status)
SELECT '[테스트] 제주 샘플 숙소','[테스트 주소] 제주 / 실제 숙소가 아닙니다',33.458,126.942,'HOTEL','15:00:00','11:00:00','{"testData":true,"fixture":"MYPAGE-USER1-20261113-3","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}','TEST','MYPAGE-USER1-20261113-3','APPROVED'
WHERE NOT EXISTS (SELECT 1 FROM lodging WHERE source_provider='TEST' AND source_content_id='MYPAGE-USER1-20261113-3');
SET @fixture_lodging = (SELECT lodging_id FROM lodging WHERE source_provider='TEST' AND source_content_id='MYPAGE-USER1-20261113-3');
INSERT INTO room(lodging_id,name,base_capacity,max_capacity,price_off_weekday,price_off_weekend,description)
SELECT @fixture_lodging,'[테스트] 스탠다드룸',2,4,180000,180000,'{"testData":true,"fixture":"MYPAGE-USER1-20261113-3","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}'
WHERE NOT EXISTS (SELECT 1 FROM room WHERE lodging_id=@fixture_lodging AND name='[테스트] 스탠다드룸');
SET @fixture_room = (SELECT room_id FROM room WHERE lodging_id=@fixture_lodging AND name='[테스트] 스탠다드룸');
INSERT INTO course(member_no,generation_method_id,title,destination,start_date,end_date,status,preferences_snapshot,condition_meta,summary,adults,companion_type,share_consent,lock_version)
SELECT @fixture_owner,@fixture_method,'[테스트] 제주 자연 여행 코스','제주','2026-10-24','2026-10-25','done','{"testData":true,"fixture":"MYPAGE-USER1-20261113-3","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}','{"testData":true,"fixture":"MYPAGE-USER1-20261113-3","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}','[테스트 데이터] 일정 상세·예약 연결·추천 확인용',2,'가족',TRUE,1
WHERE NOT EXISTS (SELECT 1 FROM course WHERE member_no=@fixture_owner AND JSON_UNQUOTE(JSON_EXTRACT(condition_meta,'$.fixture'))='MYPAGE-USER1-20261113-3');
SET @fixture_course = (SELECT id FROM course WHERE member_no=@fixture_owner AND JSON_UNQUOTE(JSON_EXTRACT(condition_meta,'$.fixture'))='MYPAGE-USER1-20261113-3' LIMIT 1);
INSERT INTO course_day(course_id,day_no,visit_date,theme) SELECT @fixture_course,1,'2026-10-24','[테스트] 관광 일정'
WHERE NOT EXISTS (SELECT 1 FROM course_day WHERE course_id=@fixture_course AND day_no=1);
SET @fixture_day = (SELECT id FROM course_day WHERE course_id=@fixture_course AND day_no=1);
INSERT INTO activity(course_day_id,category_id,seq,start_time,end_time,title,place_name,note)
SELECT @fixture_day,@fixture_category,1,'10:00:00','11:00:00','성산일출봉','성산일출봉','{"testData":true,"fixture":"MYPAGE-USER1-20261113-3","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}'
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE course_day_id=@fixture_day AND seq=1);
INSERT INTO activity(course_day_id,category_id,seq,start_time,end_time,title,place_name,note)
SELECT @fixture_day,@fixture_category,2,'13:00:00','14:00:00','섭지코지','섭지코지','{"testData":true,"fixture":"MYPAGE-USER1-20261113-3","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}'
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE course_day_id=@fixture_day AND seq=2);
INSERT INTO activity(course_day_id,category_id,seq,start_time,end_time,title,place_name,note)
SELECT @fixture_day,@fixture_category,3,'16:00:00','17:00:00','함덕해수욕장','함덕해수욕장','{"testData":true,"fixture":"MYPAGE-USER1-20261113-3","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}'
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE course_day_id=@fixture_day AND seq=3);
INSERT INTO course_day(course_id,day_no,visit_date,theme) SELECT @fixture_course,2,'2026-10-25','[테스트] 관광 일정'
WHERE NOT EXISTS (SELECT 1 FROM course_day WHERE course_id=@fixture_course AND day_no=2);
SET @fixture_day = (SELECT id FROM course_day WHERE course_id=@fixture_course AND day_no=2);
INSERT INTO activity(course_day_id,category_id,seq,start_time,end_time,title,place_name,note)
SELECT @fixture_day,@fixture_category,1,'10:00:00','11:00:00','비자림','비자림','{"testData":true,"fixture":"MYPAGE-USER1-20261113-3","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}'
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE course_day_id=@fixture_day AND seq=1);
INSERT INTO activity(course_day_id,category_id,seq,start_time,end_time,title,place_name,note)
SELECT @fixture_day,@fixture_category,2,'13:00:00','14:00:00','동문시장','동문시장','{"testData":true,"fixture":"MYPAGE-USER1-20261113-3","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}'
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE course_day_id=@fixture_day AND seq=2);
INSERT INTO room_reservation(member_id,course_id,room_id,reservation_no,booker_name,booker_phone,checkin_date,checkout_date,guest_count,total_price,status,request_memo)
SELECT @fixture_owner,@fixture_course,@fixture_room,'TEST-MYPAGE-USER1-20261113-3','[테스트] 예약자','000-0000-0000','2026-10-24','2026-10-25',2,180000,'COMPLETED','{"testData":true,"fixture":"MYPAGE-USER1-20261113-3","purpose":"마이페이지 화면 검증용. 실제 예약·결제 아님"}'
WHERE NOT EXISTS (SELECT 1 FROM room_reservation WHERE reservation_no='TEST-MYPAGE-USER1-20261113-3');
SET @fixture_reservation = (SELECT reservation_id FROM room_reservation WHERE reservation_no='TEST-MYPAGE-USER1-20261113-3');
INSERT INTO human_course_post(author_id,course_id,reservation_id,title,content,status,course_snapshot,companion_type,category,summary)
SELECT @fixture_owner,@fixture_course,@fixture_reservation,'[테스트] 제주 자연 여행 추천','[테스트 데이터 / MYPAGE-USER1-20261113-3] 실제 여행 후기나 결제 내역이 아닙니다.
성산일출봉 → 섭지코지 → 함덕해수욕장 순서로 둘러보는 1박 2일 추천 일정입니다.
마이페이지 목록·상세·공개 화면·글 수정·연결 예약 이동을 확인할 수 있습니다.','VISIBLE','{"region":"제주","startDate":"2026-10-24","endDate":"2026-10-25","version":1,"plan":[{"day":1,"stops":["성산일출봉","섭지코지","함덕해수욕장"]},{"day":2,"stops":["비자림","동문시장"]}]}','가족','힐링','[테스트 데이터] 마이페이지 기능 확인용 추천 코스'
WHERE NOT EXISTS (SELECT 1 FROM human_course_post WHERE reservation_id=@fixture_reservation);

COMMIT;
SELECT 'course' AS entity,c.id,c.title FROM course c WHERE c.member_no=@fixture_owner AND JSON_UNQUOTE(JSON_EXTRACT(c.condition_meta,'$.fixture')) LIKE 'MYPAGE-USER1-20261113-%';
SELECT 'reservation' AS entity,reservation_id,reservation_no,status FROM room_reservation WHERE member_id=@fixture_owner AND reservation_no LIKE 'TEST-MYPAGE-USER1-20261113-%';
SELECT 'post' AS entity,p.post_id,p.title,p.status FROM human_course_post p JOIN room_reservation r ON r.reservation_id=p.reservation_id WHERE p.author_id=@fixture_owner AND r.reservation_no LIKE 'TEST-MYPAGE-USER1-20261113-%';

