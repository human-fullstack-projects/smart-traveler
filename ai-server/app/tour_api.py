import logging
import os
import threading
import time
from pathlib import Path
from typing import Any
import httpx

# 인증키가 포함된 외부 요청 주소가 로그에 남지 않도록 한다.
logging.getLogger("httpx").setLevel(logging.WARNING)
BASE_URL = "https://apis.data.go.kr/B551011/KorService2"
_KEY_FILE = Path.home() / ".tourapi_key.txt"


class TourApiError(Exception):
    """외부 서비스 설정 또는 응답 오류다."""


def _read_key_file() -> str:
    """환경변수 TOUR_API_KEY가 없을 때 %USERPROFILE%\\.tourapi_key.txt 첫 줄을 읽는다."""
    try:
        with _KEY_FILE.open("r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line:
                    return line
    except OSError:
        pass
    return ""


class TourApiClient:
    def __init__(self, http_client: httpx.Client | None = None, key: str | None = None):
        self._client = http_client or httpx.Client(timeout=10.0, follow_redirects=False)
        self._key = key
        self._region_cache: dict[str, tuple[float, list[dict[str, str]]]] = {}
        self._lock = threading.Lock()

    def _request(self, operation: str, params: dict[str, Any]) -> dict[str, Any]:
        # 공공데이터포털에서 제공하는 일반 인증키(Decoding)를 사용한다.
        # 우선순위: 생성자에 직접 넘긴 키 > 환경변수 TOUR_API_KEY > .tourapi_key.txt 첫 줄 (Spring TourApiClient와 동일한 규칙).
        if self._key is not None:
            key = self._key.strip()
        else:
            key = os.getenv("TOUR_API_KEY", "").strip() or _read_key_file()
        if not key:
            raise TourApiError("관광정보 API 인증키가 설정되지 않았습니다.")
        query = {"serviceKey": key, "MobileOS": "ETC", "MobileApp": "SmartTraveler", "_type": "json", **params}
        try:
            response = self._client.get(f"{BASE_URL}/{operation}", params=query)
            response.raise_for_status()
            result = response.json()
        except (httpx.HTTPError, ValueError):
            # 오류의 원본 URL에는 인증키가 포함될 수 있으므로 전달하지 않는다.
            raise TourApiError("관광정보 API에 연결할 수 없거나 응답 형식이 올바르지 않습니다.") from None
        if not isinstance(result, dict):
            raise TourApiError("관광정보 API 응답 형식이 올바르지 않습니다.")
        envelope = result.get("response")
        if not isinstance(envelope, dict):
            raise TourApiError("관광정보 API 응답을 확인할 수 없습니다. 인증키와 서비스 승인을 확인해 주세요.")
        header = envelope.get("header")
        if not isinstance(header, dict) or str(header.get("resultCode", "")) not in {"0", "00", "0000"}:
            raise TourApiError("관광정보 API가 요청을 거절했습니다. 인증키와 서비스 승인을 확인해 주세요.")
        body = envelope.get("body")
        if not isinstance(body, dict):
            raise TourApiError("관광정보 API의 조회 결과가 올바르지 않습니다.")
        return body

    @staticmethod
    def _items(body: dict[str, Any]) -> list[dict[str, Any]]:
        container = body.get("items")
        if container is None or container == "" or container == []:
            return []
        if not isinstance(container, dict):
            raise TourApiError("관광정보 API 목록 형식이 올바르지 않습니다.")
        items = container.get("item", [])
        if isinstance(items, dict):
            return [items]
        if isinstance(items, list) and all(isinstance(item, dict) for item in items):
            return items
        if items is None or items == "":
            return []
        raise TourApiError("관광정보 API 목록 형식이 올바르지 않습니다.")

    def regions(self, province: str | None = None) -> list[dict[str, str]]:
        """시도 또는 선택한 시도의 시군구 코드를 읽는다."""
        cache_key = province or "all"
        with self._lock:
            cached = self._region_cache.get(cache_key)
            if cached and time.monotonic() - cached[0] < 3600:
                return [dict(item) for item in cached[1]]
        results: list[dict[str, str]] = []
        page = 1
        received = 0
        while page <= 20:
            params: dict[str, Any] = {"numOfRows": 100, "pageNo": page}
            if province:
                params["lDongRegnCd"] = province
            body = self._request("ldongCode2", params)
            items = self._items(body)
            received += len(items)
            for item in items:
                code = str(item.get("code") or (item.get("lDongSignguCd") if province else item.get("lDongRegnCd")) or "").strip()
                name = str(item.get("name") or (item.get("lDongSignguNm") if province else item.get("lDongRegnNm")) or "").strip()
                if not code.isdigit() or not name or len(code) > 5 or len(name) > 100:
                    raise TourApiError("지역 코드 또는 이름이 올바르지 않습니다.")
                if any(existing["code"] == code for existing in results):
                    raise TourApiError("중복된 지역 코드가 반환되었습니다.")
                results.append({"code": code, "name": name})
            try:
                total = int(body.get("totalCount", len(items)))
            except (ValueError, TypeError):
                raise TourApiError("지역 목록의 전체 건수를 확인할 수 없습니다.") from None
            if received >= total:
                break
            if not items:
                raise TourApiError("지역 목록을 모두 읽지 못했습니다.")
            page += 1
        if page > 20:
            raise TourApiError("지역 목록을 모두 읽지 못했습니다.")
        with self._lock:
            self._region_cache[cache_key] = (time.monotonic(), results)
        return [dict(item) for item in results]

    def places(self, province: str, district: str | None, page: int) -> dict[str, Any]:
        """지역 관광지 목록을 한 페이지씩 조회한다."""
        provinces = [item for item in self.regions() if item["code"] == province]
        if len(provinces) != 1:
            raise TourApiError("선택한 시도 코드를 확인할 수 없습니다.")
        districts = {item["code"]: item["name"] for item in self.regions(province)}
        if district and district not in districts:
            raise TourApiError("선택한 시군구 코드를 확인할 수 없습니다.")
        params: dict[str, Any] = {"lDongRegnCd": province, "contentTypeId": "12", "numOfRows": 10, "pageNo": page, "arrange": "A"}
        if district:
            params["lDongSignguCd"] = district
        body = self._request("areaBasedList2", params)
        results = []
        for item in self._items(body):
            identifier, name = str(item.get("contentid") or "").strip(), str(item.get("title") or "").strip()
            place_province = str(item.get("lDongRegnCd") or "").strip()
            place_district = str(item.get("lDongSignguCd") or "").strip()
            if place_province and place_province != province:
                raise TourApiError("관광지의 지역 코드가 조회 지역과 일치하지 않습니다.")
            if district and place_district and place_district != district:
                raise TourApiError("관광지의 시군구 코드가 조회 지역과 일치하지 않습니다.")
            if not identifier or not name or len(identifier) > 100 or len(name) > 200:
                raise TourApiError("관광지 식별 정보가 올바르지 않습니다.")
            # 목록에서 시군구가 빠진 경우 해당 지역 전체에 소속시킨다.
            resolved_district = place_district or district or ""
            if resolved_district and resolved_district not in districts:
                raise TourApiError("관광지의 시군구 코드를 확인할 수 없습니다.")
            results.append({"id": identifier, "name": name, "address": str(item.get("addr1") or ""), "longitude": str(item.get("mapx") or ""), "latitude": str(item.get("mapy") or ""), "phone": str(item.get("tel") or ""), "districtCode": resolved_district, "districtName": districts.get(resolved_district, "")})
        try:
            total = int(body.get("totalCount", len(results)))
        except (ValueError, TypeError):
            raise TourApiError("관광지 목록의 전체 건수를 확인할 수 없습니다.") from None
        return {"items": results, "page": page, "totalCount": total, "pageSize": 10, "province": provinces[0], "district": {"code": district, "name": districts[district]} if district else None}
