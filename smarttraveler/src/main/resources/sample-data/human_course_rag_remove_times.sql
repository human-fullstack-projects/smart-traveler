-- Smart Traveler 휴먼 추천 코스 RAG 시드 시간 제거
-- 대상: human_course_rag_seed_001_010.sql로 생성한
--       smart-traveler-human-course-demo-v1 배치만 정리한다.
--
-- 수행 내용
--   1. 시간 미사용 코스를 지원하도록 activity.start_time/end_time을 NULL 허용으로 변경
--   2. 해당 배치 activity의 시작/종료 시간 제거
--   3. 이미 작성된 human_course_post.course_snapshot의 "HH:mm 장소명"에서 시간 접두사 제거
--
-- 기존 일반 코스의 시간값은 변경하지 않는다. 여러 번 실행해도 같은 결과가 된다.

USE smart_travel_db;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE activity
  MODIFY COLUMN start_time TIME NULL,
  MODIFY COLUMN end_time TIME NULL;

START TRANSACTION;

UPDATE activity a
JOIN course_day d ON d.id = a.course_day_id
JOIN course c ON c.id = d.course_id
SET a.start_time = NULL,
    a.end_time = NULL
WHERE JSON_VALUE(c.condition_meta, '$.seed_batch') = 'smart-traveler-human-course-demo-v1';

SET @human_course_seed_activity_times_removed = ROW_COUNT();

UPDATE human_course_post p
JOIN course c ON c.id = p.course_id
SET p.course_snapshot = REGEXP_REPLACE(
      p.course_snapshot,
      '[0-2][0-9]:[0-5][0-9][[:space:]]+',
      ''
    ),
    p.updated_at = p.updated_at
WHERE JSON_VALUE(c.condition_meta, '$.seed_batch') = 'smart-traveler-human-course-demo-v1'
  AND p.course_snapshot REGEXP '[0-2][0-9]:[0-5][0-9][[:space:]]+';

SET @human_course_seed_snapshots_cleaned = ROW_COUNT();

COMMIT;

SELECT @human_course_seed_activity_times_removed AS activity_rows_cleaned,
       @human_course_seed_snapshots_cleaned AS post_snapshots_cleaned;

SELECT m.login_id,
       r.reservation_no,
       SUM(a.start_time IS NOT NULL OR a.end_time IS NOT NULL) AS remaining_activity_times,
       MAX(p.course_snapshot REGEXP '[0-2][0-9]:[0-5][0-9][[:space:]]+') AS snapshot_still_has_time
FROM room_reservation r
JOIN members m ON m.member_id = r.member_id
JOIN course c ON c.id = r.course_id
JOIN course_day d ON d.course_id = c.id
JOIN activity a ON a.course_day_id = d.id
LEFT JOIN human_course_post p ON p.reservation_id = r.reservation_id
WHERE JSON_VALUE(c.condition_meta, '$.seed_batch') = 'smart-traveler-human-course-demo-v1'
GROUP BY m.login_id, r.reservation_no
ORDER BY m.login_id, r.reservation_no;

