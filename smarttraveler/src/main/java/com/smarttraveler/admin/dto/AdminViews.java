package com.smarttraveler.admin.dto;

import com.smarttraveler.domain.entity.HumanCoursePost;
import com.smarttraveler.domain.entity.HumanCourseReport;
import com.smarttraveler.domain.entity.Member;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.BeanWrapperImpl;
import lombok.Getter;

/** The public admin fields from before the domain entity consolidation. */
public final class AdminViews {
    private AdminViews() {}

    public static Map<String, Object> of(Object entity) {
        var fields = switch (entity.getClass().getSimpleName()) {
            case "OwnerProfile" -> "ownerId memberId businessNumber businessName representativeName businessAddress businessPhone reviewStatusCode createdAt updatedAt";
            case "OwnerReviewHistory" -> "reviewId ownerId reviewerId reviewStatusCode reviewComment reviewedAt createdAt";
            case "MemberSuspensionHistory" -> "suspensionId memberId reasonCode reasonDetail startedAt suspendedUntil releasedAt processedBy createdAt";
            case "Lodging" -> "lodgingId ownerId sourceProvider sourceContentId lodgingType name address addressDetail zipcode latitude longitude phone homepageUrl description checkinTime checkoutTime parkingInfo amenities refundPolicy status rejectReason approvedAt createdAt updatedAt";
            case "Room" -> "roomId lodgingId name baseCapacity maxCapacity priceOffWeekday priceOffWeekend isActive createdAt updatedAt";
            case "RoomReservation" -> "reservationId reservationNo memberId roomId courseId checkinDate checkoutDate guestCount roomQuantity bookerName bookerPhone requestMemo totalPrice status version cancelReason cancelledAt createdAt updatedAt";
            case "RoomPayment" -> "paymentId reservationId idempotencyKey pgProvider paymentKey method amount status failReason approvedAt refundAmount refundedAt receiptUrl createdAt updatedAt";
            case "Course" -> "id memberNo destination startDate endDate adults children fixedEvents preferencesSnapshot tips assumptions title summary revisionRequest originTemplateId shareConsent generationMethodId status errorMessage model promptVersion isActive createdAt updatedAt deletedAt";
            default -> throw new IllegalArgumentException("No admin view for " + entity.getClass());
        };
        var result = new LinkedHashMap<String, Object>();
        var bean = new BeanWrapperImpl(entity);
        for (String field : fields.split(" ")) result.put(field, bean.getPropertyValue(field));
        return result;
    }

    public static List<Map<String, Object>> list(List<?> entities) {
        return entities.stream().map(AdminViews::of).toList();
    }

    private static String displayName(Member member) {
        if (member == null) return null;
        if (member.getNickname() != null && !member.getNickname().isBlank()) return member.getNickname();
        return member.getLoginId() != null && !member.getLoginId().isBlank() ? member.getLoginId() : null;
    }

    @Getter
    public static final class HumanCourseView {
        private final Long id, memberId, courseId, reservationId;
        private final String title, content, courseSnapshot, status, destination, region, summary, authorNickname;
        private final Boolean featured;
        private final LocalDateTime createdAt, updatedAt, deletedAt;
        public HumanCourseView(HumanCoursePost post, String destination, Boolean featured, Member author) {
            this.id = post.getPostId(); this.memberId = post.getAuthorId();
            this.courseId = post.getCourseId(); this.reservationId = post.getReservationId();
            this.title = post.getTitle(); this.content = post.getContent();
            this.courseSnapshot = post.getCourseSnapshot(); this.status = post.getStatus();
            this.destination = destination; this.region = destination; this.featured = featured; this.summary = post.getContent();
            this.authorNickname = displayName(author);
            this.createdAt = post.getCreatedAt(); this.updatedAt = post.getUpdatedAt(); this.deletedAt = post.getDeletedAt();
        }
    }

    @Getter
    public static final class HumanReportView {
        private final Long id, courseId, reporterId, processedBy;
        private final String reason, status, resolution, reporterNickname;
        private final LocalDateTime createdAt, resolvedAt;
        public HumanReportView(HumanCourseReport report, Member reporter) {
            this.id = report.getReportId(); this.courseId = report.getPostId();
            this.reporterId = report.getReporterId(); this.processedBy = report.getProcessedBy();
            this.reason = report.getReason(); this.status = report.getStatus(); this.reporterNickname = displayName(reporter);
            this.resolution = report.getResolution(); this.createdAt = report.getCreatedAt();
            this.resolvedAt = report.getProcessedAt();
        }
    }
}
