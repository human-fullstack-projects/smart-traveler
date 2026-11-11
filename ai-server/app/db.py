import os
import pymysql


def connect():
    """Spring과 같은 MariaDB(smart_travel_db)에 연결한다. 값은 ai-server/.env에 둔다.
    ai-server는 MariaDB를 읽기만 한다 (쓰기는 Spring 담당)."""
    if not os.getenv("DB_USERNAME", "").strip():
        raise RuntimeError("DB_USERNAME이 설정되지 않았습니다.")
    return pymysql.connect(
        host=os.getenv("DB_HOST", "127.0.0.1"),
        port=int(os.getenv("DB_PORT", "3306")),
        user=os.getenv("DB_USERNAME"),
        password=os.getenv("DB_PASSWORD", ""),
        database=os.getenv("DB_NAME", "smart_travel_db"),
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
    )


# Spring TourSyncService.placesForIndexing()과 같은 조건·같은 칸 (PlaceRow 형식)
PLACES_SQL = """
SELECT p.place_id, p.place_type, p.name, p.address, p.description, p.closed_days, p.is_market,
       r.region_code, p.latitude, p.longitude, rd.main_menu,
       (SELECT i.image_url FROM place_image i WHERE i.place_id = p.place_id AND i.sort_order = 0
         ORDER BY i.place_image_id LIMIT 1) AS image_url,
       (SELECT MIN(h.hub_rank) FROM place_hub_rank h WHERE h.place_id = p.place_id) AS hub_rank
FROM place p
JOIN region r ON r.region_id = p.region_id
LEFT JOIN restaurant_detail rd ON rd.place_id = p.place_id
WHERE r.region_code = %s AND p.is_active = TRUE AND p.latitude IS NOT NULL AND p.longitude IS NOT NULL
"""


def read_places(sigungu_code: str) -> list[dict]:
    """MariaDB에서 한 시군구의 활성·좌표 보유 장소를 읽는다."""
    with connect() as conn, conn.cursor() as cur:
        cur.execute(PLACES_SQL, (sigungu_code,))
        return cur.fetchall()
