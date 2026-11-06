package com.smarttraveler.mypage.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

/** 임시 코스와 확정 코스, 예약, 게시글을 서로 다른 자료로 전달한다. */
public final class MyPageDtos {
    private MyPageDtos() {}

    public record Day(@NotNull LocalDate date,
                      @NotNull @Size(max = 30) List<@NotBlank @Size(max = 200) String> places) {}
    public record Draft(@NotBlank @Pattern(regexp = "[a-zA-Z0-9-]{16,64}") String clientId,
                        @NotBlank @Size(max = 200) String title,
                        @NotBlank @Size(max = 100) String region,
                        @NotNull LocalDate startDate, @NotNull LocalDate endDate,
                        @NotEmpty @Size(max = 31) List<@NotNull @Valid Day> days) {}
    public record ImportRequest(@NotNull @Positive Long expectedMemberId,
                                @NotEmpty @Size(max = 20) List<@NotNull @Valid Draft> courses) {}
    public record CourseView(String id, String title, String region, LocalDate startDate,
                             LocalDate endDate, String state, List<Day> days) {}
    public record BookingView(String id, String reservationNo, String courseId, String stay,
                              String room, LocalDate date, LocalDate endDate, int guests,
                              int price, String status) {}
    public record PostView(String id, String courseId, String reservationId, String title,
                           String body, String status, String courseSnapshot) {}
    public record Snapshot(long memberId, List<CourseView> courses,
                           List<BookingView> bookings, List<PostView> posts) {}
    public record ImportResult(List<String> importedClientIds) {}
}
