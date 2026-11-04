package com.smarttraveler.chat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** AI 서버의 지역·관광지 조회를 화면에 전달한다. 인증키는 전달하지 않는다. */
@RestController
@RequestMapping("/api/chat/tour")
public class TourController {
    private final String baseUrl;
    private final TourImportService importer;
    private final ObjectMapper mapper;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    public TourController(@Value("${AI_SERVICE_URL:http://127.0.0.1:8001}") String baseUrl, TourImportService importer, ObjectMapper mapper) {
        this.importer = importer; this.mapper = mapper;
        this.baseUrl = baseUrl.replaceAll("/+$", "") + "/internal/tour/";
    }
    @GetMapping("/regions")
    public ResponseEntity<String> regions(@RequestParam(required = false) String province) {
        if (province != null) validateCode(province);
        return forward("regions" + (province == null ? "" : "?province=" + province));
    }
    @PostMapping("/places")
    @Transactional
    public ResponseEntity<String> places(@RequestParam String province, @RequestParam(required = false) String district,
                                          @RequestParam(defaultValue = "1") int page) {
        validateCode(province);
        if (district != null) validateCode(district);
        if (page < 1 || page > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "페이지를 확인해 주세요.");
        ResponseEntity<String> response = forward("places?province=" + province + (district == null ? "" : "&district=" + district) + "&page=" + page);
        try {
            JsonNode body = mapper.readTree(response.getBody());
            if (!(body instanceof ObjectNode object)) throw new IllegalArgumentException("관광정보 응답이 올바르지 않습니다.");
            if (!province.equals(object.path("province").path("code").asText()) ||
                    (district != null && !district.equals(object.path("district").path("code").asText())))
                throw new IllegalArgumentException("조회한 지역 코드가 요청과 일치하지 않습니다.");
            importer.save(object);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(mapper.writeValueAsString(object));
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "관광정보를 저장하지 못했습니다. 다시 시도해 주세요.");
        }
    }
    private static void validateCode(String code) {
        if (code == null || !code.matches("[0-9]{1,5}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지역 코드를 확인해 주세요.");
    }
    private ResponseEntity<String> forward(String path) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(20)).GET().build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "관광정보를 불러오지 못했습니다.");
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "관광정보 요청이 중단되었습니다.");
        } catch (java.io.IOException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "관광정보 서버에 연결할 수 없습니다.");
        }
    }
}
