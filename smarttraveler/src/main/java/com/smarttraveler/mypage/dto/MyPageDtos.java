package com.smarttraveler.mypage.dto;

import com.smarttraveler.reservation.dto.ReservationItinerary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

/** 임시 코스와 확정 코스, 예약, 게시글을 서로 다른 자료로 전달한다. */
public final class MyPageDtos {
    public static final String COURSE_REFERENCE_PATTERN = "(course|draft|human)-[1-9][0-9]{0,17}";

    private MyPageDtos() {}

    public record MapPlace(Long placeId, String name, String address, Double latitude, Double longitude) {}
    public record PlaceDetails(Long placeId, String name, String address, String description,
                               String imageUrl, String phone, String openingHours) {}
    public record Day(@NotNull LocalDate date,
                      @NotNull @Size(max = 30) List<@NotBlank @Size(max = 200) String> places,
                      List<MapPlace> mapPlaces) {
        public Day(LocalDate date, List<String> places) { this(date, places, List.of()); }
    }
    public record Draft(@NotBlank @Pattern(regexp = "[a-zA-Z0-9-]{16,64}") String clientId,
                        @NotBlank @Size(max = 200) String title,
                        @NotBlank @Size(max = 100) String region,
                        @NotNull LocalDate startDate, @NotNull LocalDate endDate,
                        @NotEmpty @Size(max = 31) List<@NotNull @Valid Day> days) {}
    public record ImportRequest(@NotNull @Positive Long expectedMemberId,
                                @NotEmpty @Size(max = 20) List<@NotNull @Valid Draft> courses) {}
    public record CourseStay(Long lodgingId, String name, String address) {}
    public record CourseView(String id, String title, String region, LocalDate startDate,
                             LocalDate endDate, String state, List<Day> days, String coverImageUrl,
                             Long regionId, Integer adults, Integer children, CourseStay stay) {
        public CourseView(String id, String title, String region, LocalDate startDate,
                          LocalDate endDate, String state, List<Day> days, String coverImageUrl,
                          Long regionId, Integer adults, Integer children) {
            this(id, title, region, startDate, endDate, state, days, coverImageUrl, regionId, adults, children, null);
        }
        public CourseView(String id, String title, String region, LocalDate startDate,
                          LocalDate endDate, String state, List<Day> days, String coverImageUrl) {
            this(id, title, region, startDate, endDate, state, days, coverImageUrl, null, null, null);
        }
    }
    public record StayInfo(Long id, String address, String phone, String description,
                           String imageUrl, String checkinTime, String checkoutTime, boolean regionMatched,
                           Double latitude, Double longitude) {
        public StayInfo(Long id, String address, String phone, String description,
                        String imageUrl, String checkinTime, String checkoutTime, boolean regionMatched) {
            this(id, address, phone, description, imageUrl, checkinTime, checkoutTime, regionMatched, null, null);
        }
    }
    public record BookingView(String id, String reservationNo, String courseId, String stay,
                              String room, LocalDate date, LocalDate endDate, int guests,
                              int price, String status, StayInfo stayInfo, String coverImageUrl,
                              ReservationItinerary itinerary) {
        public BookingView(String id, String reservationNo, String courseId, String stay, String room,
                           LocalDate date, LocalDate endDate, int guests, int price, String status, StayInfo stayInfo) {
            this(id, reservationNo, courseId, stay, room, date, endDate, guests, price, status, stayInfo, null, null);
        }
    }
    public record PostView(String id, String courseId, String reservationId, String title,
                           String body, String status, String courseSnapshot, String coverImageUrl) {}
    public record Snapshot(long memberId, List<CourseView> courses,
                           List<BookingView> bookings, List<PostView> posts) {}
    public record ImportResult(List<String> importedClientIds) {}
}
