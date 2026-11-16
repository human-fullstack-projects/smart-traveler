package com.smarttraveler.tour.repository;

import com.smarttraveler.domain.entity.CourseDay;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TourCourseDayRepository extends JpaRepository<CourseDay, Long> {
    List<CourseDay> findByCourseIdInOrderByCourseIdAscDayNoAsc(Collection<Long> courseIds);
}
