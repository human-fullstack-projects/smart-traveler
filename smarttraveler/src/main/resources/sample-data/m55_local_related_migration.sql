-- Local smart_travel_db data migration; keeps all old rows for rollback.
USE smart_travel_db;

INSERT IGNORE INTO related_attraction_rank(
  base_ym, attraction_code, attraction_name, area_code, area_name,
  sigungu_code, sigungu_name, related_code, related_name,
  related_region_name, related_sigungu_name, category_large, related_rank,
  fetched_at, base_place_id, source_record_hash)
SELECT p.base_ym, p.source_code, p.source_name, LEFT(r.region_code,2), r.full_name,
       RIGHT(r.region_code,3), r.region_name, p.related_code, p.related_name,
       p.related_area_name, p.related_signgu_name, LEFT(p.related_category,100),
       p.related_rank, p.synced_at, p.source_place_id,
       SHA2(CONCAT_WS('|',p.base_ym,p.source_code,p.related_code),256)
FROM place_related p JOIN region r ON r.region_id=p.region_id;
