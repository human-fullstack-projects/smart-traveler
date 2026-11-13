//package com.smarttraveler.owner.repository;
//
//import com.smarttraveler.domain.entity.Lodging;
//import org.springframework.data.repository.Repository;
//
//import java.util.List;
//
//public interface OwnerLodgingRepository extends Repository<Lodging, Long> {
//    List<Lodging> findAllByOwnerIdOrderByLodgingId(Long ownerId);
//}

package com.smarttraveler.owner.repository;

import com.smarttraveler.domain.entity.Lodging;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface OwnerLodgingRepository extends Repository<Lodging, Long> {
    List<Lodging> findAllByOwnerIdOrderByLodgingId(Long ownerId);

    Lodging save(Lodging lodging);
    Optional<Lodging> findByLodgingIdAndOwnerId(Long lodgingId, Long ownerId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Lodging> findForUpdateByLodgingIdAndOwnerId(Long lodgingId, Long ownerId);

}