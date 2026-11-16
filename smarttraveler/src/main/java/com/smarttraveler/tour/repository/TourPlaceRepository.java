package com.smarttraveler.tour.repository;

import com.smarttraveler.domain.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TourPlaceRepository extends JpaRepository<Place, Long> {
}
