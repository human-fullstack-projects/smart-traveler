-- 일정용 category 5종 시드 (코스 템플릿 template_activity.category_id가 참조).
-- category.id는 PRIMARY KEY라 실제 유니크 제약이 있으므로 INSERT IGNORE로 여러 번 실행해도 안전함.
-- place_category(TourAPI 분류체계, lclsSystm 기반 ID 1~49대)와는 다른 체계이므로 ID를 겹치지 않게 썼다.
USE smart_travel_db;

INSERT IGNORE INTO category (id, name) VALUES
(1,'관광'),
(2,'문화'),
(3,'레포츠'),
(4,'쇼핑'),
(5,'식사');
