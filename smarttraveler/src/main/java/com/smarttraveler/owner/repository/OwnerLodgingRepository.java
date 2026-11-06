package com.smarttraveler.owner.repository;

import com.smarttraveler.domain.entity.Lodging;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface OwnerLodgingRepository extends Repository<Lodging, Long> {
    List<Lodging> findAllByOwnerIdOrderByLodgingId(Long ownerId);
}