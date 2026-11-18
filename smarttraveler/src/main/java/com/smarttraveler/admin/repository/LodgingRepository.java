package com.smarttraveler.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.smarttraveler.domain.entity.Lodging;
import java.util.List;

public interface LodgingRepository extends JpaRepository<Lodging, Long> {
    String FILTER = """
        FROM lodging l
        WHERE ((:status IS NULL AND l.status <> 'DELETED') OR l.status = :status)
          AND (:ownerId IS NULL OR l.owner_id = :ownerId)
          AND (:sidoId IS NULL OR l.region_id = :sidoId OR l.region_id IN
               (SELECT r.region_id FROM region r WHERE r.parent_region_id = :sidoId))
          AND (:region IS NULL OR LOWER(l.address) LIKE CONCAT('%', LOWER(:region), '%'))
          AND (:query IS NULL OR LOWER(l.name) LIKE CONCAT('%', LOWER(:query), '%')
               OR LOWER(l.address) LIKE CONCAT('%', LOWER(:query), '%'))
          AND (:ownerName IS NULL OR EXISTS (
               SELECT 1 FROM owner_profiles op LEFT JOIN members m ON m.member_id = op.member_id
               WHERE op.owner_id = l.owner_id AND (
                   LOWER(op.representative_name) LIKE CONCAT('%', LOWER(:ownerName), '%')
                   OR LOWER(op.business_name) LIKE CONCAT('%', LOWER(:ownerName), '%')
                   OR LOWER(m.nickname) LIKE CONCAT('%', LOWER(:ownerName), '%'))))
        """;

    @Query(value = "SELECT l.* " + FILTER + """
        ORDER BY
          CASE WHEN :sortField = 'name' AND :sortDirection = 'ASC' THEN l.name END ASC,
          CASE WHEN :sortField = 'name' AND :sortDirection = 'DESC' THEN l.name END DESC,
          CASE WHEN :sortField = 'lodgingId' AND :sortDirection = 'ASC' THEN l.lodging_id END ASC,
          l.lodging_id DESC
        """, countQuery = "SELECT COUNT(*) " + FILTER, nativeQuery = true)
    Page<Lodging> search(@Param("status") String status, @Param("ownerId") Long ownerId,
        @Param("sidoId") Long sidoId, @Param("region") String region,
        @Param("query") String query, @Param("ownerName") String ownerName,
        @Param("sortField") String sortField, @Param("sortDirection") String sortDirection,
        Pageable pageable);

    long countByStatusNot(String status);
    long countByStatus(String status);
    List<Lodging> findByOwnerIdOrderByLodgingIdDesc(Long ownerId);
}

