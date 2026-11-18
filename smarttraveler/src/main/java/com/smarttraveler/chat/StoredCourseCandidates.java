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

    @Transactional(readOnly = true)
    public List<Map<String, Object>> find(ObjectNode conditions) {
        int duration = conditions.path("durationDays").asInt();
        if (duration < 1 || duration > 4) {
            return List.of();
        }
        Long region = regions.regionId(conditions);
        if (region == null) {
            return List.of();
        }
        Map<Long, Course> selected = new HashMap<>();
        Map<Long, JsonNode> itineraries = new HashMap<>();
        Set<Long> placeIds = new HashSet<>();
        for (Course course : courses.findCandidates(region)) {
            if (!course.getStartDate().plusDays(duration - 1L).equals(course.getEndDate())) {
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
        for (var place : places.findAllById(placeIds)) {
            if (Boolean.TRUE.equals(place.getIsActive())) {
                placeNames.put(place.getPlaceId(), place.getName());
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
                rows.add(row);
            }
        }
        return rows;
    }
}
