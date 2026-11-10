package com.smarttraveler.tour.client;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 한국관광공사 공공데이터 호출 (EXT-07 중심 관광지 · EXT-01 연관 관광지 · TourAPI KorService2).
 * 서비스키는 환경변수 TOUR_API_KEY 또는 %USERPROFILE%\.tourapi_key.txt 첫 줄에서만 읽고, 로그·오류 문구에 넣지 않는다.
 */
@Component
public class TourApiClient {
    public static final String BASE = "https://apis.data.go.kr/B551011";
    public static final String HUB = BASE + "/LocgoHubTarService1/areaBasedList1";
    public static final String RELATED = BASE + "/TarRlteTarService1/areaBasedList1";
    public static final String KOR_AREA = BASE + "/KorService2/areaBasedList2";
    public static final String KOR_DETAIL_COMMON = BASE + "/KorService2/detailCommon2";
    public static final String KOR_DETAIL_INTRO = BASE + "/KorService2/detailIntro2";

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private String key;

    /** 한 페이지 결과. */
    public record Page(int totalCount, List<JsonNode> items) {}

    /** 호출 실패(키 미등록·시간 초과·HTTP 오류). message 에는 키가 들어가지 않는다. */
    public static class TourApiException extends RuntimeException {
        public TourApiException(String message) { super(message); }
    }

    public Page hubPage(String baseYm, String areaCd, String signguCd, int pageNo, int rows) {
        return call(HUB, Map.of("baseYm", baseYm, "areaCd", areaCd, "signguCd", signguCd,
                "pageNo", String.valueOf(pageNo), "numOfRows", String.valueOf(rows)));
    }

    public Page relatedPage(String baseYm, String areaCd, String signguCd, int pageNo, int rows) {
        return call(RELATED, Map.of("baseYm", baseYm, "areaCd", areaCd, "signguCd", signguCd,
                "pageNo", String.valueOf(pageNo), "numOfRows", String.valueOf(rows)));
    }

    /** 관광지(contentTypeId=12) 목록 · 새 법정동 코드 기준. */
    public Page attractionPage(String lDongRegnCd, String lDongSignguCd, int pageNo, int rows) {
        return areaPage(lDongRegnCd, lDongSignguCd, "12", pageNo, rows);
    }

    /** contentTypeId별 목록 (12 관광지, 14 문화시설, 28 레포츠, 38 쇼핑, 39 음식점) · 새 법정동 코드 기준. */
    public Page areaPage(String lDongRegnCd, String lDongSignguCd, String contentTypeId, int pageNo, int rows) {
        return call(KOR_AREA, Map.of("lDongRegnCd", lDongRegnCd, "lDongSignguCd", lDongSignguCd, "contentTypeId", contentTypeId,
                "arrange", "C", "pageNo", String.valueOf(pageNo), "numOfRows", String.valueOf(rows)));
    }

    /** 소개글(overview)·홈페이지. 응답 1건 고정이며 선택 플래그(overviewYN 등)는 이 API에서 받지 않음. */
    public Page detailCommon(String contentId) {
        return call(KOR_DETAIL_COMMON, Map.of("contentId", contentId));
    }

    /** 타입별 소개정보(이용시간, 휴무일, 대표메뉴 등). contentTypeId에 따라 필드명이 달라진다. */
    public Page detailIntro(String contentId, String contentTypeId) {
        return call(KOR_DETAIL_INTRO, Map.of("contentId", contentId, "contentTypeId", contentTypeId));
    }

    /** 실패하면 1번 더 시도(2초 뒤). 그래도 실패하면 TourApiException. */
    Page call(String url, Map<String, String> params) {
        try {
            return callOnce(url, params);
        } catch (TourApiException first) {
            try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw first; }
            return callOnce(url, params);
        }
    }

    private Page callOnce(String url, Map<String, String> params) {
        Map<String, String> all = new LinkedHashMap<>();
        all.put("serviceKey", key());
        all.put("MobileOS", "ETC");
        all.put("MobileApp", "smarttravel");
        all.put("_type", "json");
        all.putAll(params);
        String query = all.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        HttpResponse<String> res;
        try {
            res = http.send(HttpRequest.newBuilder(URI.create(url + "?" + query)).timeout(Duration.ofSeconds(20)).GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new TourApiException("연결 실패: " + e.getClass().getSimpleName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TourApiException("호출 중단");
        }
        String body = res.body() == null ? "" : res.body();
        if (body.contains("SERVICE_KEY_IS_NOT_REGISTERED")) throw new TourApiException("서비스키 미등록(활용신청 확인 필요)");
        if (body.contains("LIMITED_NUMBER_OF_SERVICE_REQUESTS")) throw new TourApiException("하루 호출 한도 초과");
        if (res.statusCode() != 200) throw new TourApiException("HTTP " + res.statusCode());
        JsonNode root;
        try {
            root = mapper.readTree(body);
        } catch (RuntimeException e) {
            throw new TourApiException("응답이 JSON이 아님");
        }
        JsonNode header = root.path("response").path("header");
        String code = header.path("resultCode").asString("");
        if (!"0000".equals(code)) throw new TourApiException("결과 코드 " + code + " " + header.path("resultMsg").asString(""));
        JsonNode bodyNode = root.path("response").path("body");
        int total = bodyNode.path("totalCount").asInt(0);
        List<JsonNode> items = new ArrayList<>();
        JsonNode item = bodyNode.path("items").path("item");
        if (item.isArray()) item.forEach(items::add);
        else if (item.isObject()) items.add(item);
        return new Page(total, items);
    }

    private synchronized String key() {
        if (key != null) return key;
        String env = System.getenv("TOUR_API_KEY");
        if (StringUtils.hasText(env)) return key = env.trim();
        Path file = Path.of(System.getProperty("user.home"), ".tourapi_key.txt");
        try {
            String first = Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                    .map(String::trim).filter(StringUtils::hasText).findFirst().orElse("");
            if (first.isEmpty()) throw new TourApiException("서비스키 파일이 비어 있음");
            return key = first;
        } catch (IOException e) {
            throw new TourApiException("서비스키 없음(TOUR_API_KEY 또는 .tourapi_key.txt)");
        }
    }
}
