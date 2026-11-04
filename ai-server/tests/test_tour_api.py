import httpx
import pytest
from fastapi.testclient import TestClient
from app.tour_api import TourApiClient, TourApiError
from app import main


def envelope(items, total=None):
    return {"response": {"header": {"resultCode": "0000"}, "body": {"items": {"item": items}, "totalCount": total if total is not None else (len(items) if isinstance(items, list) else 1)}}}


def test_지역코드_페이지_조회와_캐시():
    calls = []
    def handler(request):
        calls.append(request)
        assert request.url.params["serviceKey"] == "key+/="
        assert request.url.path.endswith("/ldongCode2")
        if request.url.params["pageNo"] == "1":
            return httpx.Response(200, json=envelope([{"code":"26", "name":"부산광역시"}], 2))
        return httpx.Response(200, json=envelope({"code":"11", "name":"서울특별시"}, 2))
    client = TourApiClient(httpx.Client(transport=httpx.MockTransport(handler)), key="key+/=")
    assert client.regions() == [{"code":"26", "name":"부산광역시"}, {"code":"11", "name":"서울특별시"}]
    assert client.regions() == [{"code":"26", "name":"부산광역시"}, {"code":"11", "name":"서울특별시"}]
    assert len(calls) == 2


def test_시군구와_관광지_파라미터_및_응답():
    def handler(request):
        if request.url.path.endswith("/ldongCode2"):
            if not request.url.params.get("lDongRegnCd"):
                return httpx.Response(200, json=envelope({"code":"26", "name":"부산광역시"}))
            assert request.url.params["lDongRegnCd"] == "26"
            return httpx.Response(200, json=envelope({"code":"350", "name":"해운대구"}))
        assert request.url.params["lDongRegnCd"] == "26"
        assert request.url.params["lDongSignguCd"] == "350"
        assert request.url.params["contentTypeId"] == "12"
        assert request.url.params["pageNo"] == "2"
        return httpx.Response(200, json=envelope({"contentid":"100", "title":"관광지", "addr1":"부산", "mapx":"129.1", "mapy":"35.1"}, 11))
    client = TourApiClient(httpx.Client(transport=httpx.MockTransport(handler)), key="test")
    assert client.regions("26") == [{"code":"350", "name":"해운대구"}]
    result = client.places("26", "350", 2)
    assert result["totalCount"] == 11 and result["page"] == 2
    assert result["items"][0]["name"] == "관광지" and result["items"][0]["longitude"] == "129.1"


def test_빈목록과_불완전한_페이지():
    client = TourApiClient(httpx.Client(transport=httpx.MockTransport(lambda request: httpx.Response(200, json={"response":{"header":{"resultCode":"0000"},"body":{"items":"", "totalCount":0}}}))), key="test")
    assert client.regions() == []
    client = TourApiClient(httpx.Client(transport=httpx.MockTransport(lambda request: httpx.Response(200, json=envelope([], 1)))), key="test")
    with pytest.raises(TourApiError): client.regions()


def test_키가_없거나_API_오류면_오류로_처리(monkeypatch):
    monkeypatch.delenv("TOUR_API_KEY", raising=False)
    with pytest.raises(TourApiError, match="인증키"):
        TourApiClient(key="").regions()
    client = TourApiClient(httpx.Client(transport=httpx.MockTransport(lambda request: httpx.Response(200, json={"response":{"header":{"resultCode":"30", "resultMsg":"bad"}}}))), key="secret")
    with pytest.raises(TourApiError) as error: client.regions()
    assert "secret" not in str(error.value)
    client = TourApiClient(httpx.Client(transport=httpx.MockTransport(lambda request: httpx.Response(200, text="<error>bad</error>"))), key="secret")
    with pytest.raises(TourApiError) as error: client.regions()
    assert "secret" not in str(error.value)


def test_내부_API_입력_검증과_오류_응답(monkeypatch):
    class FakeTour:
        def regions(self, province): return [{"code":"26", "name":"부산"}]
        def places(self, province, district, page): return {"items":[], "page":page, "totalCount":0, "pageSize":10}
    monkeypatch.setattr(main, "tour", FakeTour())
    client = TestClient(main.app)
    assert client.get('/internal/tour/regions').json()['items'][0]['name'] == '부산'
    assert client.get('/internal/tour/places', params={'province':'26'}).status_code == 200
    assert client.get('/internal/tour/places', params={'province':'abc'}).status_code == 422
