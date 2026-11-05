package com.smarttraveler.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.smarttraveler.domain.entity.HumanCoursePost;

public interface HumanCourseRepository extends JpaRepository<HumanCoursePost, Long> {
    @Query("select p from HumanCoursePost p left join fetch p.course")
    java.util.List<HumanCoursePost> findAllWithCourse();
}

