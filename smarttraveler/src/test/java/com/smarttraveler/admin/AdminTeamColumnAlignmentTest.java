package com.smarttraveler.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Transient;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminTeamColumnAlignmentTest {
    @Test
    void adminMappingsDoNotAddColumnsToTeamTables() {
        var pairs = List.of(
            Map.entry(com.smarttraveler.admin.data.Course.class, com.smarttraveler.domain.entity.Course.class),
            Map.entry(com.smarttraveler.admin.data.CourseTemplate.class, com.smarttraveler.domain.entity.CourseTemplate.class),
            Map.entry(com.smarttraveler.admin.data.GenerationMethod.class, com.smarttraveler.domain.entity.GenerationMethod.class),
            Map.entry(com.smarttraveler.admin.data.Lodging.class, com.smarttraveler.domain.entity.Lodging.class),
            Map.entry(com.smarttraveler.admin.data.Member.class, com.smarttraveler.domain.entity.Member.class),
            Map.entry(com.smarttraveler.admin.data.MemberSuspensionHistory.class, com.smarttraveler.domain.entity.MemberSuspensionHistory.class),
            Map.entry(com.smarttraveler.admin.data.OwnerProfile.class, com.smarttraveler.domain.entity.OwnerProfile.class),
            Map.entry(com.smarttraveler.admin.data.OwnerReviewHistory.class, com.smarttraveler.domain.entity.OwnerReviewHistory.class),
            Map.entry(com.smarttraveler.admin.data.OwnerReviewStatus.class, com.smarttraveler.domain.entity.OwnerReviewStatus.class),
            Map.entry(com.smarttraveler.admin.data.Payment.class, com.smarttraveler.domain.entity.RoomPayment.class),
            Map.entry(com.smarttraveler.admin.data.Reservation.class, com.smarttraveler.domain.entity.RoomReservation.class),
            Map.entry(com.smarttraveler.admin.data.Room.class, com.smarttraveler.domain.entity.Room.class),
            Map.entry(com.smarttraveler.admin.data.RoomInventory.class, com.smarttraveler.domain.entity.RoomInventory.class),
            Map.entry(com.smarttraveler.admin.data.HumanCourse.class, com.smarttraveler.domain.entity.HumanCoursePost.class),
            Map.entry(com.smarttraveler.admin.data.HumanReport.class, com.smarttraveler.domain.entity.HumanCourseReport.class)
        );
        for (var pair : pairs) {
            Set<String> team = columns(pair.getValue());
            Set<String> admin = columns(pair.getKey());
            admin.removeAll(team);
            assertTrue(admin.isEmpty(), pair.getKey().getSimpleName() + " adds " + admin);
        }
    }

    private static Set<String> columns(Class<?> type) {
        return java.util.Arrays.stream(type.getDeclaredFields())
            .filter(f -> !Modifier.isStatic(f.getModifiers()))
            .filter(f -> !f.isAnnotationPresent(Transient.class))
            .filter(f -> !f.isAnnotationPresent(jakarta.persistence.ManyToOne.class))
            .filter(f -> !f.isAnnotationPresent(jakarta.persistence.OneToMany.class))
            .map(AdminTeamColumnAlignmentTest::column)
            .collect(Collectors.toSet());
    }
    private static String column(Field field) {
        Column c = field.getAnnotation(Column.class);
        if (c != null && !c.name().isEmpty()) return c.name();
        return field.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}
