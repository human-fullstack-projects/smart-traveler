package com.smarttraveler.chat;

import com.smarttraveler.domain.entity.Course;
import com.smarttraveler.tour.repository.TourCourseDayRepository;
import com.smarttraveler.tour.repository.TourCourseRepository;
import com.smarttraveler.tour.repository.TourPlaceRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
public class StoredCourseCandidates {
    private static final Set<String> RAG_SEED_BATCHES = Set.of(
            "smart-traveler-rag-1000-v1", "smart-traveler-rag-2000-v1");

    private final TourCourseRepository courses;
    private final TourCourseDayRepository courseDays;
    private final TourPlaceRepository places;
    private final StoredPlaceCandidates regions;
    private final ObjectMapper mapper;

    public StoredCourseCandidates(TourCourseRepository courses, TourCourseDayRepository courseDays,
                                  TourPlaceRepository places, StoredPlaceCandidates regions,
                                  ObjectMapper mapper) {
        this.courses = courses;
        this.courseDays = courseDays;
        this.places = places;
        this.regions = regions;
        this.mapper = mapper;
    }

    /** 지역을 말하지 않았을 때 전국 후보 중 비교할 코스 수 상한 (임베딩 비용·시간). */
    static final int NATIONWIDE_LIMIT = 40;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> find(ObjectNode conditions) {
        return find(conditions, Set.of());
    }

    /**
     * 추천 후보 코스의 방문 행. 지역이 없으면 전국에서(동행·교통이 맞는 코스 먼저, 최대 NATIONWIDE_LIMIT개) 고르고,
     * excluded(이 대화에서 이미 추천한 코스)는 뺀다. 지역 이름을 말했는데 찾을 수 없으면 빈 목록.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> find(ObjectNode conditions, Set<Long> excluded) {
        Integer duration = conditions.path("durationDays").isIntegralNumber() ? conditions.path("durationDays").asInt() : null;
        if (duration != null && (duration < 1 || duration > 4)) {
            return List.of();
        }
        Long region = regions.regionId(conditions);
        boolean namedRegion = !conditions.path("regionName").asText("").isBlank();
        if (region == null && namedRegion) {
            return List.of();
        }
        List<Course> pool = region != null ? courses.findCandidates(region) : nationwide(conditions, duration, excluded);
        return rows(pool, duration, excluded);
    }

    /** 추천한 코스 한 개의 방문 행 (저장용 스냅샷을 만들 때 다시 읽는다). duration이 없으면 코스의 원래 일수를 그대로 쓴다. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> findCourse(long courseId, Integer duration) {
        return courses.findById(courseId).map(course -> rows(List.of(course), duration, Set.<Long>of())).orElse(List.of());
    }

    private List<Map<String, Object>> rows(List<Course> pool, Integer duration, Set<Long> excluded) {
        Map<Long, Course> selected = new HashMap<>();
        Map<Long, JsonNode> itineraries = new HashMap<>();
        Set<Long> placeIds = new HashSet<>();
        for (Course course : pool) {
            if (excluded.contains(course.getId())) {
                continue;
            }
            if (duration != null && !course.getStartDate().plusDays(duration - 1L).equals(course.getEndDate())) {
                continue;
            }
            JsonNode metadata = mapper.readTree(course.getConditionMeta());
            if (!RAG_SEED_BATCHES.contains(metadata.path("seed_batch").asText())) {
                continue;
            }
            JsonNode itinerary = metadata.path("itinerary");
            selected.put(course.getId(), course);
            itineraries.put(course.getId(), itinerary);
            for (JsonNode day : itinerary) {
                for (JsonNode visit : day.path("visits")) {
                    placeIds.add(visit.path("place_id").asLong());
                }
            }
        }
        if (selected.isEmpty()) {
            return List.of();
        }

        Map<Long, String> placeNames = new HashMap<>();
        Map<Long, String> placeSummaries = new HashMap<>();
        for (var place : places.findAllById(placeIds)) {
            if (Boolean.TRUE.equals(place.getIsActive())) {
                placeNames.put(place.getPlaceId(), place.getName());
                placeSummaries.put(place.getPlaceId(), summary(place.getDescription()));
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (var day : courseDays.findByCourseIdInOrderByCourseIdAscDayNoAsc(selected.keySet())) {
            Course course = selected.get(day.getCourseId());
            JsonNode prefs = mapper.readTree(course.getPreferencesSnapshot());
            JsonNode itineraryDay = itineraries.get(course.getId()).path(day.getDayNo() - 1);
            JsonNode visits = itineraryDay.path("visits");
            if (itineraryDay.path("day_no").asInt() != day.getDayNo() || visits.isEmpty()) {
                continue;
            }
            for (int i = 0; i < visits.size(); i++) {
                JsonNode visit = visits.path(i);
                long placeId = visit.path("place_id").asLong();
                String placeName = placeNames.get(placeId);
                Map<String, Object> row = new HashMap<>();
                row.put("courseId", course.getId());
                row.put("title", course.getTitle());
                row.put("destination", course.getDestination());
                row.put("regionId", course.getRegionId());
                row.put("companion", course.getCompanionType());
                row.put("transport", course.getTransportType());
                row.put("theme", prefs.path("theme_name").asText());
                row.put("pace", prefs.path("travel_style").asText());
                row.put("environment", prefs.path("place_preference").asText());
                List<String> landscapes = new ArrayList<>();
                if (prefs.path("preferred_region").isArray()) {
                    for (JsonNode value : prefs.path("preferred_region")) {
                        String normalized = switch (value.asText()) {
                            case "도시" -> "city";
                            case "시골" -> "countryside";
                            case "호수·강" -> "water";
                            case "섬" -> "island";
                            case "산·숲" -> "forest";
                            case "바다", "바다·해변" -> "sea";
                            default -> "";
                        };
                        if (!normalized.isEmpty() && !landscapes.contains(normalized)) landscapes.add(normalized);
                    }
                }
                row.put("landscapes", landscapes);
                row.put("dayNo", day.getDayNo());
                row.put("dayTheme", day.getTheme());
                row.put("seq", visit.path("seq").asInt() == i + 1 ? i + 1 : null);
                row.put("placeId", placeName == null ? null : placeId);
                row.put("placeName", placeName);
                row.put("placeSummary", placeName == null ? "" : placeSummaries.getOrDefault(placeId, ""));
                rows.add(row);
            }
        }
        return rows;
    }

    /** 전국 후보: 일수가 맞는 RAG 코스 중 동행·교통이 맞는 것을 먼저, 최대 NATIONWIDE_LIMIT개. duration이 없으면 일수로 거르지 않는다. */
    private List<Course> nationwide(ObjectNode conditions, Integer duration, Set<Long> excluded) {
        String companion = conditions.path("companionType").asText("");
        String transport = conditions.path("transportType").asText("");
        List<Course> fitting = new ArrayList<>();
        for (Course course : courses.findAllCandidates()) {
            if (excluded.contains(course.getId()) || course.getStartDate() == null
                    || (duration != null && !course.getStartDate().plusDays(duration - 1L).equals(course.getEndDate()))) {
                continue;
            }
            if (!RAG_SEED_BATCHES.contains(mapper.readTree(course.getConditionMeta()).path("seed_batch").asText())) {
                continue;
            }
            fitting.add(course);
        }
        fitting.sort(java.util.Comparator.comparingInt((Course course) ->
                (companion.isEmpty() || companion.equals(course.getCompanionType()) ? 0 : 1)
                + (transport.isEmpty() || transport.equals(course.getTransportType()) ? 0 : 1))
            .thenComparing(Course::getId));
        return fitting.subList(0, Math.min(NATIONWIDE_LIMIT, fitting.size()));
    }

    /** 장소 설명의 첫 문장(최대 60자). 설명이 없으면 빈 문자열. */
    static String summary(String description) {
        if (description == null) return "";
        String text = description.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        if (text.isEmpty()) return "";
        int end = text.indexOf(". ");
        if (end < 0 && text.endsWith(".")) end = text.length() - 1;
        String first = end > 0 ? text.substring(0, end + 1) : text;
        return first.length() > 60 ? first.substring(0, 59).trim() + "…" : first;
    }
}
