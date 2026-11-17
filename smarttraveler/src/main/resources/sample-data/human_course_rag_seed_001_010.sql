-- Smart Traveler 휴먼 추천 코스 테스트 시드 (rag_seed_test_001 ~ rag_seed_test_010)
-- 대상 DB: MariaDB 12.3 / smart_travel_db
--
-- 실행 전제
--   1. rag_course_seed_1000_multiaccount.sql이 먼저 실행되어 있어야 한다.
--   2. 기존 DB에 lodging -> room 데이터가 있어야 한다.
--   3. 기존 RAG 코스의 condition_meta.itinerary가 유효해야 한다.
--   4. 시간 미사용 스키마 전환 전이라면 human_course_rag_remove_times.sql을 먼저 실행한다.
--
-- 생성 내용 (계정당 2세트, 총 10계정)
--   - OPEN   : 체크아웃이 지난 COMPLETED 예약 1건. 게시글이 없어 화면에서 새 글 작성 가능.
--   - POSTED : 체크아웃이 지난 COMPLETED 예약 1건 + VISIBLE 휴먼 추천 글 1건.
--
-- 기존 데이터 활용 원칙
--   - 기존 region/place/lodging/room 행은 수정하지 않는다.
--   - 각 계정의 기존 RAG 코스 중, 같은 region_id에 실제 숙소와 활성 객실이 있는 코스를
--     템플릿으로 골라 별도의 과거 여행 코스를 생성한다.
--   - 방문 장소는 템플릿 condition_meta.itinerary의 기존 place_id를 사용한다.
--   - 숙박업소와 객실은 기존 lodging/room 행을 참조한다.
--
-- 안전성
--   - 이 배치가 이미 존재하면 아무것도 추가하지 않고 오류로 종료한다.
--   - 모든 영구 INSERT는 하나의 트랜잭션으로 실행한다.
--   - 중간 오류 발생 시 전체 ROLLBACK한다.
--   - 원본 RAG 코스, 계정, 장소, 숙소, 객실은 UPDATE/DELETE하지 않는다.

USE smart_travel_db;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @human_course_seed_previous_sql_mode = @@SESSION.sql_mode;
SET SESSION sql_mode = 'STRICT_ALL_TABLES,NO_BACKSLASH_ESCAPES,NO_ENGINE_SUBSTITUTION';

DROP TEMPORARY TABLE IF EXISTS human_course_seed_target;
CREATE TEMPORARY TABLE human_course_seed_target (
  account_no INT NOT NULL,
  slot_no INT NOT NULL,
  member_id BIGINT NOT NULL,
  login_id VARCHAR(100) NOT NULL,
  template_course_id BIGINT NOT NULL,
  room_id BIGINT NOT NULL,
  checkin_date DATE NOT NULL,
  checkout_date DATE NOT NULL,
  new_course_id BIGINT NULL,
  reservation_id BIGINT NULL,
  PRIMARY KEY (account_no, slot_no),
  UNIQUE KEY uq_human_course_seed_course (new_course_id),
  UNIQUE KEY uq_human_course_seed_reservation (reservation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DELIMITER //
BEGIN NOT ATOMIC
  DECLARE seed_lock INT DEFAULT 0;
  DECLARE finished INT DEFAULT 0;
  DECLARE target_count INT DEFAULT 0;
  DECLARE account_count INT DEFAULT 0;
  DECLARE created_course_count INT DEFAULT 0;
  DECLARE created_reservation_count INT DEFAULT 0;
  DECLARE created_post_count INT DEFAULT 0;

  DECLARE v_account_no INT;
  DECLARE v_slot_no INT;
  DECLARE v_member_id BIGINT;
  DECLARE v_login_id VARCHAR(100);
  DECLARE v_template_course_id BIGINT;
  DECLARE v_room_id BIGINT;
  DECLARE v_checkin_date DATE;
  DECLARE v_checkout_date DATE;
  DECLARE v_new_course_id BIGINT;
  DECLARE v_reservation_id BIGINT;
  DECLARE v_snapshot LONGTEXT;
  DECLARE v_activity_count INT;

  DECLARE target_cursor CURSOR FOR
    SELECT account_no, slot_no, member_id, login_id, template_course_id,
           room_id, checkin_date, checkout_date
    FROM human_course_seed_target
    ORDER BY account_no, slot_no;

  DECLARE CONTINUE HANDLER FOR NOT FOUND SET finished = 1;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF seed_lock = 1 THEN
      DO RELEASE_LOCK('smart_travel_db.human_course_rag_seed_001_010');
    END IF;
    RESIGNAL;
  END;

  IF @@session.foreign_key_checks <> 1 OR @@session.check_constraint_checks <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Enable foreign key and check constraints before import.';
  END IF;

  SELECT GET_LOCK('smart_travel_db.human_course_rag_seed_001_010', 10)
    INTO seed_lock;
  IF COALESCE(seed_lock, 0) <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Another human-course seed import holds the lock.';
  END IF;

  SELECT COUNT(*) INTO account_count
  FROM members
  WHERE login_id REGEXP '^rag_seed_test_00[1-9]$|^rag_seed_test_010$'
    AND status = 'ACTIVE'
    AND deleted_at IS NULL;

  IF account_count <> 10 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Expected 10 active accounts: rag_seed_test_001 through rag_seed_test_010.';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM course
    WHERE JSON_VALUE(condition_meta, '$.seed_batch') = 'smart-traveler-human-course-demo-v1'
  ) OR EXISTS (
    SELECT 1
    FROM room_reservation
    WHERE reservation_no LIKE 'HC-RAG-%'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'The human-course RAG demo seed already exists. No rows were added.';
  END IF;

  IF (SELECT COUNT(*) FROM category WHERE id IN (1, 2, 3, 4, 5)) <> 5 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Required activity categories 1 through 5 are missing.';
  END IF;

  -- 계정마다 동일 지역의 기존 숙소/활성 객실이 있는 RAG 코스 2개를 고른다.
  -- 여러 숙소가 매칭되면 가장 작은 room_id를 사용하고, 코스는 id 순서로 고정한다.
  INSERT INTO human_course_seed_target (
    account_no, slot_no, member_id, login_id, template_course_id,
    room_id, checkin_date, checkout_date
  )
  SELECT ranked.account_no,
         ranked.target_rank,
         ranked.member_id,
         ranked.login_id,
         ranked.template_course_id,
         ranked.room_id,
         DATE_SUB(
           DATE_SUB(CURDATE(), INTERVAL 75 DAY),
           INTERVAL (ranked.account_no * 3 + ranked.target_rank * 7) DAY
         ) AS checkin_date,
         DATE_ADD(
           DATE_SUB(
             DATE_SUB(CURDATE(), INTERVAL 75 DAY),
             INTERVAL (ranked.account_no * 3 + ranked.target_rank * 7) DAY
           ),
           INTERVAL 1 DAY
         ) AS checkout_date
  FROM (
    SELECT candidate.*,
           ROW_NUMBER() OVER (
             PARTITION BY candidate.member_id
             ORDER BY candidate.template_course_id
           ) AS target_rank
    FROM (
      SELECT CAST(RIGHT(m.login_id, 3) AS UNSIGNED) AS account_no,
             m.member_id,
             m.login_id,
             c.id AS template_course_id,
             MIN(ro.room_id) AS room_id
      FROM members m
      JOIN course c
        ON c.member_no = m.member_id
      JOIN course_day source_day
        ON source_day.course_id = c.id
      JOIN lodging l
        ON l.region_id = c.region_id
      JOIN room ro
        ON ro.lodging_id = l.lodging_id
       AND ro.is_active = TRUE
      WHERE m.login_id REGEXP '^rag_seed_test_00[1-9]$|^rag_seed_test_010$'
        AND m.status = 'ACTIVE'
        AND m.deleted_at IS NULL
        AND c.status = 'done'
        AND c.is_active = TRUE
        AND c.deleted_at IS NULL
        AND JSON_VALUE(c.condition_meta, '$.seed_batch') = 'smart-traveler-rag-1000-v1'
        AND JSON_LENGTH(JSON_QUERY(c.condition_meta, '$.itinerary')) > 0
      GROUP BY m.member_id, m.login_id, c.id
    ) candidate
  ) ranked
  WHERE ranked.target_rank <= 2;

  SELECT COUNT(*) INTO target_count FROM human_course_seed_target;
  IF target_count <> 20 OR EXISTS (
    SELECT 1
    FROM (
      SELECT member_id, COUNT(*) AS target_per_member
      FROM human_course_seed_target
      GROUP BY member_id
      HAVING COUNT(*) <> 2
    ) invalid_target
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Each test account needs two RAG courses with an existing lodging and active room in the same region.';
  END IF;

  START TRANSACTION;
  OPEN target_cursor;

  seed_loop: LOOP
    FETCH target_cursor INTO
      v_account_no, v_slot_no, v_member_id, v_login_id,
      v_template_course_id, v_room_id, v_checkin_date, v_checkout_date;

    IF finished = 1 THEN
      LEAVE seed_loop;
    END IF;

    -- 원본 RAG 코스는 수정하지 않고, 과거 숙박용 코스를 별도로 복제한다.
    INSERT INTO course (
      member_no, destination, start_date, end_date, adults, children,
      fixed_events, preferences_snapshot, title, summary, tips, assumptions,
      revision_request, origin_template_id, share_consent, generation_method_id,
      status, error_message, model, prompt_version, is_active,
      created_at, updated_at, deleted_at, companion_type, transport_type,
      flow_type, condition_state, condition_meta, conditions_confirmed_at,
      plan_state, lock_version, region_code, region_id
    )
    SELECT c.member_no,
           c.destination,
           v_checkin_date,
           v_checkout_date,
           c.adults,
           c.children,
           c.fixed_events,
           c.preferences_snapshot,
           LEFT(CONCAT('[휴먼코스 시드] ', c.title), 200),
           c.summary,
           c.tips,
           c.assumptions,
           NULL,
           c.origin_template_id,
           FALSE,
           c.generation_method_id,
           'done',
           NULL,
           c.model,
           c.prompt_version,
           TRUE,
           TIMESTAMP(v_checkin_date, '08:00:00'),
           TIMESTAMP(v_checkout_date, '20:00:00'),
           NULL,
           c.companion_type,
           c.transport_type,
           'COURSE_FIRST',
           'CONFIRMED',
           JSON_SET(
             c.condition_meta,
             '$.seed_batch', 'smart-traveler-human-course-demo-v1',
             '$.seed_account', v_login_id,
             '$.seed_slot', IF(v_slot_no = 1, 'OPEN', 'POSTED'),
             '$.template_course_id', v_template_course_id
           ),
           TIMESTAMP(v_checkin_date, '08:00:00'),
           'FINAL',
           1,
           c.region_code,
           c.region_id
    FROM course c
    WHERE c.id = v_template_course_id
      AND c.member_no = v_member_id;

    IF ROW_COUNT() <> 1 THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Failed to clone exactly one RAG course.';
    END IF;

    SET v_new_course_id = LAST_INSERT_ID();
    SET created_course_count = created_course_count + 1;

    UPDATE human_course_seed_target
    SET new_course_id = v_new_course_id
    WHERE account_no = v_account_no
      AND slot_no = v_slot_no;

    INSERT INTO course_day (course_id, day_no, theme, visit_date)
    SELECT v_new_course_id,
           source_day.day_no,
           source_day.theme,
           DATE_ADD(v_checkin_date, INTERVAL (source_day.day_no - 1) DAY)
    FROM course_day source_day
    WHERE source_day.course_id = v_template_course_id
    ORDER BY source_day.day_no;

    -- condition_meta.itinerary에 저장된 기존 place를 실제 activity 행으로 만든다.
    -- 휴먼 코스 작성 가능 판정은 activity가 최소 1개 있어야 통과한다.
    INSERT INTO activity (
      course_day_id, seq, start_time, end_time, title, category_id,
      place_name, place_id, note, reason, address_snapshot,
      latitude_snapshot, longitude_snapshot
    )
    SELECT new_day.id,
           visit_row.seq,
           NULL,
           NULL,
           p.name,
           CASE p.place_type
             WHEN 'CULTURE' THEN 2
             WHEN 'LEISURE' THEN 3
             WHEN 'SHOPPING' THEN 4
             WHEN 'RESTAURANT' THEN 5
             ELSE 1
           END,
           p.name,
           p.place_id,
           '기존 RAG 코스 방문지를 활용한 휴먼 추천 코스 테스트 일정',
           '기존 여행지 데이터를 활용한 시드 일정입니다.',
           p.address,
           p.latitude,
           p.longitude
    FROM course template_course
    JOIN JSON_TABLE(
      template_course.condition_meta,
      '$.itinerary[*]' COLUMNS (
        day_no INT PATH '$.day_no',
        visits JSON PATH '$.visits'
      )
    ) day_row
    JOIN JSON_TABLE(
      day_row.visits,
      '$[*]' COLUMNS (
        seq INT PATH '$.seq',
        place_id BIGINT PATH '$.place_id'
      )
    ) visit_row
    JOIN course_day new_day
      ON new_day.course_id = v_new_course_id
     AND new_day.day_no = day_row.day_no
    JOIN place p
      ON p.place_id = visit_row.place_id
     AND p.is_active = TRUE
    WHERE template_course.id = v_template_course_id
    ORDER BY day_row.day_no, visit_row.seq;

    SET v_activity_count = ROW_COUNT();
    IF v_activity_count < 1 THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'A cloned course has no valid activity rows.';
    END IF;

    INSERT INTO room_reservation (
      reservation_no, member_id, room_id, course_id,
      checkin_date, checkout_date, guest_count,
      booker_name, booker_phone, request_memo,
      total_price, status, version, room_quantity,
      created_at, updated_at
    )
    SELECT CONCAT(
             'HC-RAG-', LPAD(v_account_no, 3, '0'), '-',
             IF(v_slot_no = 1, 'OPEN', 'POSTED')
           ),
           v_member_id,
           v_room_id,
           v_new_course_id,
           v_checkin_date,
           v_checkout_date,
           GREATEST(1, COALESCE(c.adults, 1) + COALESCE(c.children, 0)),
           COALESCE(NULLIF(m.nickname, ''), m.login_id),
           CONCAT('010-9000-', LPAD(v_account_no, 4, '0')),
           IF(
             v_slot_no = 1,
             '휴먼 추천 코스 신규 작성 테스트용 완료 예약',
             '휴먼 추천 코스 기존 게시글 테스트용 완료 예약'
           ),
           GREATEST(1, COALESCE(ro.price_off_weekday, ro.price_off_weekend, 100000)),
           'COMPLETED',
           1,
           1,
           TIMESTAMP(DATE_SUB(v_checkin_date, INTERVAL 14 DAY), '10:00:00'),
           TIMESTAMP(v_checkout_date, '18:00:00')
    FROM members m
    JOIN course c
      ON c.id = v_new_course_id
     AND c.member_no = m.member_id
    JOIN room ro
      ON ro.room_id = v_room_id
    WHERE m.member_id = v_member_id;

    IF ROW_COUNT() <> 1 THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Failed to create exactly one completed reservation.';
    END IF;

    SET v_reservation_id = LAST_INSERT_ID();
    SET created_reservation_count = created_reservation_count + 1;

    UPDATE human_course_seed_target
    SET reservation_id = v_reservation_id
    WHERE account_no = v_account_no
      AND slot_no = v_slot_no;

    -- 두 번째 예약에는 이미 작성된 휴먼 추천 코스를 생성한다.
    IF v_slot_no = 2 THEN
      SELECT JSON_OBJECT(
               'region', c.destination,
               'startDate', DATE_FORMAT(v_checkin_date, '%Y-%m-%d'),
               'endDate', DATE_FORMAT(v_checkout_date, '%Y-%m-%d'),
               'version', GREATEST(1, c.lock_version),
               'plan', JSON_QUERY(COALESCE((
                 SELECT JSON_ARRAYAGG(
                          JSON_OBJECT(
                            'day', plan_day.day_no,
                            'stops', JSON_QUERY(plan_day.stops, '$')
                          ) ORDER BY plan_day.day_no
                        )
                 FROM (
                   SELECT d.day_no,
                          JSON_ARRAYAGG(a.place_name ORDER BY a.seq) AS stops
                   FROM course_day d
                   JOIN activity a
                     ON a.course_day_id = d.id
                   WHERE d.course_id = v_new_course_id
                   GROUP BY d.id, d.day_no
                 ) plan_day
               ), JSON_ARRAY()), '$')
             )
        INTO v_snapshot
      FROM course c
      WHERE c.id = v_new_course_id;

      INSERT INTO human_course_post (
        reservation_id, author_id, course_id,
        title, category, summary, companion_type,
        content, status, course_snapshot,
        created_at, updated_at, deleted_at
      )
      SELECT v_reservation_id,
             v_member_id,
             v_new_course_id,
             LEFT(CONCAT(c.destination, '에서 보낸 기억에 남는 하루'), 100),
             CASE MOD(v_account_no - 1, 4)
               WHEN 0 THEN '문화'
               WHEN 1 THEN '힐링'
               WHEN 2 THEN '액티비티'
               ELSE '맛집'
             END,
             LEFT(
               CONCAT(
                 l.name, '에 머물며 기존 여행지 코스를 직접 둘러본 후기입니다. ',
                 '일정과 숙소가 잘 연결되어 편안하게 여행했습니다.'
               ),
               150
             ),
             CASE c.companion_type
               WHEN 'SOLO' THEN '혼자'
               WHEN 'FRIENDS' THEN '친구'
               WHEN 'COUPLE' THEN '연인'
               WHEN 'FAMILY' THEN '가족'
               ELSE '친구'
             END,
             CONCAT(
               c.destination, ' 여행을 마치고 남기는 후기입니다. ',
               l.name, '에서 숙박했고, 저장해 둔 최종 코스를 따라 지역의 장소들을 차례로 방문했습니다. ',
               '이동 순서가 자연스러워 여유롭게 둘러볼 수 있었고, 다음에 같은 지역을 찾는 분께도 추천하고 싶은 일정입니다. ',
               '이 글은 rag_seed_test_', LPAD(v_account_no, 3, '0'),
               ' 계정의 휴먼 추천 코스 화면을 확인하기 위한 테스트 데이터입니다.'
             ),
             'VISIBLE',
             v_snapshot,
             TIMESTAMP(DATE_ADD(v_checkout_date, INTERVAL 1 DAY), '18:30:00'),
             TIMESTAMP(DATE_ADD(v_checkout_date, INTERVAL 1 DAY), '18:30:00'),
             NULL
      FROM course c
      JOIN room ro
        ON ro.room_id = v_room_id
      JOIN lodging l
        ON l.lodging_id = ro.lodging_id
      WHERE c.id = v_new_course_id;

      IF ROW_COUNT() <> 1 THEN
        SIGNAL SQLSTATE '45000'
          SET MESSAGE_TEXT = 'Failed to create exactly one human-course post.';
      END IF;

      SET created_post_count = created_post_count + 1;
    END IF;
  END LOOP;

  CLOSE target_cursor;

  IF created_course_count <> 20
     OR created_reservation_count <> 20
     OR created_post_count <> 10 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Unexpected result count; rolling back the human-course seed.';
  END IF;

  IF (
    SELECT COUNT(*)
    FROM room_reservation r
    LEFT JOIN human_course_post p
      ON p.reservation_id = r.reservation_id
    WHERE r.reservation_no LIKE 'HC-RAG-%-OPEN'
      AND r.status = 'COMPLETED'
      AND r.checkout_date < CURDATE()
      AND p.post_id IS NULL
  ) <> 10 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Expected 10 writable completed reservations without posts.';
  END IF;

  IF (
    SELECT COUNT(*)
    FROM human_course_post p
    JOIN room_reservation r
      ON r.reservation_id = p.reservation_id
    WHERE r.reservation_no LIKE 'HC-RAG-%-POSTED'
      AND p.status = 'VISIBLE'
      AND p.deleted_at IS NULL
  ) <> 10 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Expected 10 visible human-course posts.';
  END IF;

  COMMIT;
  DO RELEASE_LOCK('smart_travel_db.human_course_rag_seed_001_010');
  SET seed_lock = 0;

  SELECT created_course_count AS courses_inserted,
         created_reservation_count AS completed_reservations_inserted,
         created_post_count AS human_course_posts_inserted;

  SELECT m.login_id,
         r.reservation_no,
         r.checkin_date,
         r.checkout_date,
         r.status,
         l.name AS lodging_name,
         c.destination,
         p.post_id,
         p.title AS post_title
  FROM room_reservation r
  JOIN members m
    ON m.member_id = r.member_id
  JOIN room ro
    ON ro.room_id = r.room_id
  JOIN lodging l
    ON l.lodging_id = ro.lodging_id
  JOIN course c
    ON c.id = r.course_id
  LEFT JOIN human_course_post p
    ON p.reservation_id = r.reservation_id
  WHERE r.reservation_no LIKE 'HC-RAG-%'
  ORDER BY m.login_id, r.reservation_no;
END//
DELIMITER ;

DROP TEMPORARY TABLE human_course_seed_target;
SET SESSION sql_mode = @human_course_seed_previous_sql_mode;

