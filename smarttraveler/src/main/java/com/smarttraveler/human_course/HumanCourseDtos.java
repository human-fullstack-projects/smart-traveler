package com.smarttraveler.human_course;

import java.math.BigDecimal;
import java.util.List;

public final class HumanCourseDtos {
    private HumanCourseDtos() {}

    public record MapPlace(String id, Long placeId, String name, String address,
                           BigDecimal latitude, BigDecimal longitude, String kind, List<String> imageUrls) {}
    public record Day(int day, List<String> stops, List<MapPlace> places, String visitDate) {}
    public record Photo(String name, String dataUrl) {}
    public record Comment(String id, String author, String text, boolean mine) {}
    public record PostInput(String title, String category, String companion, String summary,
                            String body, List<Photo> photoAssets, boolean replacePhotos) {}
    public record CommentInput(String text) {}
    public record ReportInput(String reason, String details) {}
    public record Reservation(String id, String name, String region, String dates, String status,
                              String checkoutAt, boolean itineraryFinal, int courseVersion,
                              List<Day> plan, Long existingPostId, String eligibilityError,
                              MapPlace stay) {}
    public record Post(Long id, String title, String author, String region, String category,
                       String companion, String summary, String body, List<String> photos,
                       List<Photo> photoAssets, List<Day> plan, long likes, boolean likedByMe,
                       boolean reportedByMe, List<Comment> comments, String createdAt,
                       String visibility, int courseVersion, String travelDates, boolean mine,
                       String reservationId, MapPlace stay, String coverPhotoUrl) {}
    public record IdResult(Long id) {}
}
