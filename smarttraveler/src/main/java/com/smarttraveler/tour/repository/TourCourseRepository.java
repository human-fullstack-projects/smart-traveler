package com.smarttraveler.tour.repository;

import com.smarttraveler.domain.entity.Course;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TourCourseRepository extends JpaRepository<Course, Long> {
    @Query("""
        select c from Course c
        left join c.region r
        where (c.regionId = :regionId or r.parentRegionId = :regionId)
          and c.status = 'done'
          and c.isActive = true
          and c.deletedAt is null
          and c.conditionMeta is not null
        order by c.id
        """)
    List<Course> findCandidates(@Param("regionId") Long regionId);
}
