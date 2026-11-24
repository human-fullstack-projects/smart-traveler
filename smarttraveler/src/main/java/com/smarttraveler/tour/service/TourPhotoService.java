package com.smarttraveler.tour.service;

import com.smarttraveler.domain.entity.Lodging;
import com.smarttraveler.stay.dto.StayDetailResponse.Photo;
import com.smarttraveler.tour.client.TourApiClient;
import com.smarttraveler.tour.client.TourApiClient.Page;
import com.smarttraveler.tour.client.TourApiClient.TourApiException;
import java.net.URI;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

@Service
public class TourPhotoService {
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final int CACHE_LIMIT = 512;
    private final TourApiClient api;
    private final Map<Query, CachedPhotos> cache = Collections.synchronizedMap(new LinkedHashMap<>(32, .75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Query, CachedPhotos> eldest) {
            return size() > CACHE_LIMIT;
        }
    });

    public record Target(String contentId, String name, String address, String region, boolean lodging) {}
    public record RoomPhotos(String name, List<Photo> photos) {}
    public record Gallery(List<Photo> photos, List<RoomPhotos> rooms) {}
    private record Query(Target target, boolean fullGallery) {}
    private record CachedPhotos(Gallery gallery, Instant expiresAt) {}

    public TourPhotoService(TourApiClient api) {
        this.api = api;
    }

    public Gallery lodgingGallery(Lodging lodging) {
        String contentId = "TOURAPI".equals(lodging.getSourceProvider()) ? lodging.getSourceContentId() : null;
        return load(new Query(new Target(contentId, lodging.getName(), lodging.getAddress(), "", true), true));
    }

    public List<String> photos(Target target) {
        return load(new Query(target, false)).photos().stream()
                .flatMap(photo -> Stream.of(photo.imageUrl(), photo.thumbnailUrl()))
                .filter(java.util.Objects::nonNull).distinct().limit(8).toList();
    }

    private Gallery load(Query query) {
        CachedPhotos cached = cache.get(query);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) return cached.gallery();
        try {
            Gallery gallery = lookup(query.target(), query.fullGallery());
            cache.put(query, new CachedPhotos(gallery, Instant.now().plus(CACHE_TTL)));
            return gallery;
        } catch (TourApiException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "TourAPI 사진을 불러오지 못했습니다.");
        }
    }

    private Gallery lookup(Target target, boolean fullGallery) {
        var photos = new LinkedHashSet<Photo>();
        String contentId = target.contentId();
        if (contentId == null || !contentId.matches("[0-9]+")) {
            String name = target.name();
            if (name == null || name.isBlank()) return new Gallery(List.of(), List.of());
            var page = api.photoCandidates(name, target.lodging());
            // 전체 후보를 확인할 수 없으면 동명이인을 배제할 수 없다.
            if (page.totalCount() > page.items().size()) return new Gallery(List.of(), List.of());
            var matches = page.items().stream().filter(item -> samePlace(target, item)).toList();
            if (matches.size() != 1) return new Gallery(List.of(), List.of());
            JsonNode item = matches.getFirst();
            addPhoto(photos, item, "firstimage", "firstimage2");
            contentId = item.path("contentid").asString("");
            if (!contentId.matches("[0-9]+")) return new Gallery(List.copyOf(photos), List.of());
        }

        String id = contentId;
        boolean hasCover = !photos.isEmpty();
        TourApiException lookupError = null;
        var rooms = new ArrayList<RoomPhotos>();
        try {
            List<JsonNode> items = fullGallery ? allItems(page -> api.photoDetails(id, page)) : api.photoDetails(id).items();
            for (var item : items) addPhoto(photos, item, "originimgurl", "smallimageurl");
        } catch (TourApiException error) {
            lookupError = error;
        }
        if (!hasCover) {
            try {
                var coverPhotos = new LinkedHashSet<Photo>();
                api.photoCover(id).items().forEach(item -> addPhoto(coverPhotos, item, "firstimage", "firstimage2"));
                if (fullGallery) coverPhotos.reversed().forEach(photos::addFirst);
                else photos.addAll(coverPhotos);
            } catch (TourApiException error) {
                lookupError = error;
            }
        }
        if (fullGallery) {
            try {
                for (var item : allItems(page -> api.roomPhotos(id, page))) {
                    String name = item.path("roomtitle").asString("").strip();
                    if (name.isEmpty()) continue;
                    var roomPhotos = new LinkedHashSet<Photo>();
                    for (int index = 1; index <= 5; index++) addPhoto(roomPhotos, item, "roomimg" + index, "");
                    rooms.add(new RoomPhotos(name, List.copyOf(roomPhotos)));
                }
            } catch (TourApiException error) {
                lookupError = error;
            }
        }
        if (photos.isEmpty() && rooms.stream().allMatch(room -> room.photos().isEmpty()) && lookupError != null) throw lookupError;
        return new Gallery(List.copyOf(photos), List.copyOf(rooms));
    }

    private static List<JsonNode> allItems(IntFunction<Page> fetch) {
        var items = new ArrayList<JsonNode>();
        for (int index = 1; ; index++) {
            Page page;
            try {
                page = fetch.apply(index);
            } catch (TourApiException error) {
                if (items.isEmpty()) throw error;
                return items;
            }
            if (page.items().isEmpty() || items.containsAll(page.items())) return items;
            items.addAll(page.items());
            if (items.size() >= page.totalCount()) return items;
        }
    }

    private boolean samePlace(Target target, JsonNode item) {
        if (!key(target.name()).equals(key(item.path("title").asString("")))) return false;
        if (target.lodging() && !"32".equals(item.path("contenttypeid").asString(""))) return false;
        String candidate = addressKey(item.path("addr1").asString(""));
        String address = addressKey(target.address());
        if (!address.isEmpty()) return address.equals(candidate);
        String region = addressKey(target.region());
        return !region.isEmpty() && candidate.contains(region);
    }

    private static String addressKey(String value) {
        if (value == null) return "";
        return key(value.replaceAll("\\([^)]*\\)", "")
                .replace("충청북도", "충북").replace("충청남도", "충남")
                .replace("경상북도", "경북").replace("경상남도", "경남")
                .replace("전라남도", "전남").replace("전라북도", "전북")
                .replace("전북특별자치도", "전북").replace("강원특별자치도", "강원").replace("강원도", "강원")
                .replace("제주특별자치도", "제주").replace("제주도", "제주")
                .replace("특별시", "").replace("광역시", ""));
    }

    private static String key(String input) {
        return Normalizer.normalize(input == null ? "" : input, Normalizer.Form.NFKC)
                .replaceAll("[^\\p{L}\\p{N}]", "").toLowerCase(Locale.ROOT);
    }

    private static void addPhoto(Set<Photo> photos, JsonNode item, String imageField, String thumbnailField) {
        String imageUrl = imageUrl(item.path(imageField).asString(""));
        String thumbnailUrl = imageUrl(item.path(thumbnailField).asString(""));
        if (imageUrl != null || thumbnailUrl != null) photos.add(new Photo(imageUrl, thumbnailUrl));
    }

    private static String imageUrl(String value) {
        try {
            URI url = URI.create(value.strip());
            if (url.getHost() == null) return null;
            if ("https".equalsIgnoreCase(url.getScheme())) return url.toString();
            if ("http".equalsIgnoreCase(url.getScheme())) return "https:" + url.toString().substring(5);
        } catch (IllegalArgumentException ignored) {
            // 잘못된 URL은 건너뛰고 다음 사진을 확인한다.
        }
        return null;
    }
}
