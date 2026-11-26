"""
RDB(MariaDB/MySQL) 읽기 전용 조회 모듈.

/internal/dialogue의 코스 상세 조회(fetch_course_detail)는 research/chat_test.py의
fetch_course_detail을 그대로 포팅했다 — condition_meta.itinerary에 이미 들어있는 place_name을
그대로 쓰고, place 테이블 조인이나 활성 여부·순서 검증은 하지 않는다(설계 기준이 chat_test.py이므로).

lodging_by_id/places_near_lodging은 /internal/lodging-itinerary(별도 API)에서 쓰는,
이번 교체와 무관한 기존 기능이라 그대로 둔다.
"""
import json
import os

import pymysql
import pymysql.cursors

DB_HOST = os.environ.get("DB_HOST", "127.0.0.1")
DB_PORT = int(os.environ.get("DB_PORT", "3306"))
DB_NAME = os.environ.get("DB_NAME", "smart_travel_db")
DB_USERNAME = os.environ.get("DB_USERNAME", "")
DB_PASSWORD = os.environ.get("DB_PASSWORD", "")


def connect():
    """운영 DB에는 읽기 전용 계정 사용을 권장한다."""
    return pymysql.connect(
        host=DB_HOST, port=DB_PORT, user=DB_USERNAME, password=DB_PASSWORD,
        database=DB_NAME, charset="utf8mb4", cursorclass=pymysql.cursors.DictCursor,
    )


def fetch_course_detail(conn, course_id):
    """research/chat_test.py의 fetch_course_detail과 동일. 없으면 None."""
    with conn.cursor() as cur:
        cur.execute(
            "SELECT destination, start_date, end_date, adults, children, "
            "companion_type, transport_type, condition_meta FROM course WHERE id = %s",
            (course_id,),
        )
        row = cur.fetchone()
    if not row:
        return None
    try:
        cm = json.loads(row["condition_meta"]) if row["condition_meta"] else {}
    except (json.JSONDecodeError, TypeError):
        cm = {}
    row["itinerary"] = cm.get("itinerary", [])
    return row


def lodging_by_id(conn, lodging_id):
    """StoredLodgingCandidates.findById와 동일한 조회. 없으면 None."""
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT l.lodging_id AS lodgingId, l.name AS name, COALESCE(l.address, '') AS address,
                   l.region_id AS regionId, l.latitude AS latitude, l.longitude AS longitude,
                   r.full_name AS regionName
            FROM lodging l LEFT JOIN region r ON r.region_id = l.region_id
            WHERE l.status = 'ACTIVE' AND l.lodging_id = %s
            """,
            (lodging_id,),
        )
        return cur.fetchone()


def _rough_distance_km(lat1, lng1, lat2, lng2):
    """정밀한 Haversine 대신 평면 근사식. StoredPlaceCandidates.roughDistanceKm과 동일(정렬용으로만 충분)."""
    d_lat = (lat1 - lat2) * 111.0
    d_lng = (lng1 - lng2) * 88.8
    return (d_lat * d_lat + d_lng * d_lng) ** 0.5


def places_near_lodging(conn, lat, lng, limit=30, range_deg=0.3):
    """숙소 좌표 주변(±0.3도, 30km대)의 활성 관광지를 거리순으로 최대 limit개.
    StoredPlaceCandidates.findNearLodging과 동일한 규칙."""
    lat, lng = float(lat), float(lng)
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT place_id AS placeId, name AS name, COALESCE(address, '') AS address,
                   latitude AS latitude, longitude AS longitude
            FROM place
            WHERE source_provider IN ('TOURAPI', 'KTO_TOURAPI') AND is_active = TRUE
              AND latitude BETWEEN %s AND %s AND longitude BETWEEN %s AND %s
            LIMIT 60
            """,
            (lat - range_deg, lat + range_deg, lng - range_deg, lng + range_deg),
        )
        rows = cur.fetchall()
    rows.sort(key=lambda row: _rough_distance_km(lat, lng, float(row["latitude"]), float(row["longitude"])))
    return rows[:limit]
