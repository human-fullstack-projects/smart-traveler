-- Read-only audit for the exact batch used by StoredCourseCandidates.
WITH batch AS (
  SELECT c.*, DATEDIFF(end_date, start_date) + 1 AS duration
  FROM course c
  WHERE JSON_UNQUOTE(JSON_EXTRACT(condition_meta, '$.seed_batch')) = 'smart-traveler-rag-1000-v1'
), audited AS (
  SELECT c.id, c.region_id, c.destination, c.duration,
    c.status = 'done' AND c.is_active = 1 AND c.deleted_at IS NULL
    AND c.duration BETWEEN 1 AND 4
    AND (SELECT COUNT(*) FROM course_day d WHERE d.course_id=c.id) = c.duration
    AND NOT EXISTS (
      SELECT 1 FROM JSON_TABLE(c.condition_meta, '$.itinerary[*]' COLUMNS (
        day_position FOR ORDINALITY, day_no INT PATH '$.day_no', visits JSON PATH '$.visits'
      )) days
      LEFT JOIN course_day d ON d.course_id=c.id AND d.day_no=days.day_no
      WHERE d.id IS NULL OR days.day_no NOT BETWEEN 1 AND c.duration OR days.day_position <> days.day_no
         OR JSON_LENGTH(days.visits) = 0
    )
    AND JSON_LENGTH(JSON_EXTRACT(c.condition_meta, '$.itinerary')) = c.duration
    AND NOT EXISTS (
      SELECT 1 FROM JSON_TABLE(c.condition_meta, '$.itinerary[*]' COLUMNS (
        NESTED PATH '$.visits[*]' COLUMNS (
          visit_position FOR ORDINALITY, seq INT PATH '$.seq', place_id BIGINT PATH '$.place_id'
        )
      )) visits LEFT JOIN place p ON p.place_id=visits.place_id
      WHERE p.place_id IS NULL OR p.is_active <> 1 OR visits.seq IS NULL OR visits.seq <> visits.visit_position
    ) AS valid
  FROM batch c
)
SELECT region_id, destination, duration, COUNT(*) AS batch_courses,
       SUM(valid) AS valid_courses, COUNT(*) - SUM(valid) AS rejected_courses
FROM audited GROUP BY region_id, destination, duration
ORDER BY region_id, duration;
