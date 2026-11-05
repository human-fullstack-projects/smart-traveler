package com.smarttraveler.admin;

import com.smarttraveler.admin.repository.*;
import com.smarttraveler.admin.service.AdminDataService;
import com.smarttraveler.domain.entity.AdminLodgingActionHistory;
import com.smarttraveler.domain.entity.Lodging;
import com.smarttraveler.domain.entity.Room;
import com.smarttraveler.domain.entity.RoomReservation;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminDeletionTest {
    @Mock AdminMemberRepository members;
    @Mock OwnerProfileRepository owners;
    @Mock OwnerReviewHistoryRepository reviews;
    @Mock MemberSuspensionHistoryRepository suspensions;
    @Mock LodgingRepository lodgings;
    @Mock AdminLodgingActionHistoryRepository actions;
    @Mock RoomRepository rooms;
    @Mock ReservationRepository reservations;
    @Mock ReservationActionHistoryRepository reservationActions;
    @Mock PaymentRepository payments;
    @Mock CourseRepository courses;
    @Mock CourseTemplateRepository templates;
    @Mock HumanCourseRepository humanCourses;
    @Mock HumanReportRepository humanReports;
    @Mock HumanActionHistoryRepository humanActions;
    @Mock RecommendationCriteriaRepository criteria;
    @Mock RecommendationCriteriaHistoryRepository criteriaHistory;
    @Mock org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Mock jakarta.persistence.EntityManager entityManager;
    @InjectMocks AdminDataService data;

    @Test void ownerRejectionRequiresReason() {
        assertThrows(IllegalArgumentException.class, () -> data.reviewOwner(9L, "REJECTED", "  ", "admin"));
        verifyNoInteractions(reviews);
    }

    @Test void ongoingReservationStopsLodgingInsteadOfDeleting() {
        Lodging lodging = new Lodging(); lodging.setLodgingId(9L); lodging.setStatus("ACTIVE");
        Room room = new Room(); room.setRoomId(12L);
        RoomReservation reservation = new RoomReservation(); reservation.setStatus("CONFIRMED");
        when(lodgings.findById(9L)).thenReturn(Optional.of(lodging));
        when(rooms.findByLodgingId(9L)).thenReturn(List.of(room));
        when(reservations.findByRoomIdIn(List.of(12L))).thenReturn(List.of(reservation));
        assertEquals("SUSPENDED", data.deleteLodging(9L, "잘못된 정보", "admin"));
        assertEquals("SUSPENDED", lodging.getStatus());
        verify(lodgings, never()).delete(any());
        verify(actions).save(any(AdminLodgingActionHistory.class));
    }
}
