//package com.smarttraveler.owner.service;
//
//import com.smarttraveler.auth.repository.MemberRepository;
//import com.smarttraveler.auth.repository.SignupOwnerRepository;
//import com.smarttraveler.owner.dto.OwnerLodgingSummary;
//import com.smarttraveler.owner.repository.OwnerLodgingRepository;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.server.ResponseStatusException;
//
//import java.util.List;
//
//@Service
//public class OwnerLodgingService {
//    private final MemberRepository members;
//    private final SignupOwnerRepository owners;
//    private final OwnerLodgingRepository lodgings;
//
//    public OwnerLodgingService(
//            MemberRepository members,
//            SignupOwnerRepository owners,
//            OwnerLodgingRepository lodgings
//    ) {
//        this.members = members;
//        this.owners = owners;
//        this.lodgings = lodgings;
//    }
//
//    @Transactional(readOnly = true)
//    public List<OwnerLodgingSummary> findMyLodgings(String loginId) {
//        var member = members.findByLoginId(loginId)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
//
//        if (!"OWNER".equals(member.getRoleCode())
//                || !"ACTIVE".equals(member.getStatus())
//                || member.getDeletedAt() != null) {
//            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
//        }
//
//        var owner = owners.findByMemberId(member.getMemberId())
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
//
//        if (!"APPROVED".equals(owner.getReviewStatusCode())) {
//            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
//        }
//
//        return lodgings.findAllByOwnerIdOrderByLodgingId(owner.getOwnerId())
//                .stream()
//                .map(lodging -> new OwnerLodgingSummary(
//                        lodging.getLodgingId(),
//                        lodging.getName(),
//                        lodging.getStatus()
//                ))
//                .toList();
//    }
//}

package com.smarttraveler.owner.service;

import com.smarttraveler.auth.repository.MemberRepository;
import com.smarttraveler.auth.repository.SignupOwnerRepository;
import com.smarttraveler.domain.entity.Lodging;
import com.smarttraveler.domain.entity.Member;
import com.smarttraveler.domain.entity.OwnerProfile;
import com.smarttraveler.owner.dto.OwnerLodgingCreateRequest;
import com.smarttraveler.owner.dto.OwnerLodgingSummary;
import com.smarttraveler.owner.repository.OwnerLodgingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.smarttraveler.owner.dto.OwnerLodgingDetail;

import java.util.List;

@Service
public class OwnerLodgingService {
    private final MemberRepository members;
    private final SignupOwnerRepository owners;
    private final OwnerLodgingRepository lodgings;

    public OwnerLodgingService(
            MemberRepository members,
            SignupOwnerRepository owners,
            OwnerLodgingRepository lodgings
    ) {
        this.members = members;
        this.owners = owners;
        this.lodgings = lodgings;
    }

    @Transactional(readOnly = true)
    public List<OwnerLodgingSummary> findMyLodgings(String loginId) {
        Long ownerId = requireApprovedOwnerId(loginId);

        return lodgings.findAllByOwnerIdOrderByLodgingId(ownerId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public OwnerLodgingSummary create(
            String loginId,
            OwnerLodgingCreateRequest request
    ) {
        Long ownerId = requireApprovedOwnerId(loginId);

        Lodging lodging = new Lodging();
        lodging.setOwnerId(ownerId);
        lodging.setSourceProvider("OWNER");
        lodging.setStatus("PENDING");
        // lodging.setName(request.name().trim());
        // lodging.setLodgingType(request.lodgingType().trim());
        // lodging.setAddress(request.address().trim());
        // lodging.setLatitude(request.latitude());
        // lodging.setLongitude(request.longitude());
        // lodging.setCheckinTime(request.checkinTime());
        // lodging.setCheckoutTime(request.checkoutTime());
        applyLodgingInfo(lodging, request);

        return toSummary(lodgings.save(lodging));
    }

    @Transactional(readOnly = true)
    public OwnerLodgingDetail findMyLodging(String loginId, Long lodgingId) {
        Long ownerId = requireApprovedOwnerId(loginId);
        Lodging lodging = lodgings.findByLodgingIdAndOwnerId(lodgingId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "숙소를 찾을 수 없습니다."));

        return new OwnerLodgingDetail(
                lodging.getLodgingId(),
                lodging.getName(),
                lodging.getLodgingType(),
                lodging.getAddress(),
                lodging.getLatitude(),
                lodging.getLongitude(),
                lodging.getCheckinTime(),
                lodging.getCheckoutTime(),
                lodging.getStatus(),
                lodging.getRejectReason()
        );
    }

        @Transactional
    public OwnerLodgingSummary update(
            String loginId,
            Long lodgingId,
            OwnerLodgingCreateRequest request
    ) {
        Long ownerId = requireApprovedOwnerId(loginId);
        Lodging lodging = lodgings.findForUpdateByLodgingIdAndOwnerId(lodgingId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "숙소를 찾을 수 없습니다."));

        if (!"PENDING".equals(lodging.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "현재 승인 대기 중인 숙소만 수정할 수 있습니다.");
        }

        applyLodgingInfo(lodging, request);
        return toSummary(lodgings.save(lodging));
    }

    private void applyLodgingInfo(Lodging lodging, OwnerLodgingCreateRequest request) {
        lodging.setName(request.name().trim());
        lodging.setLodgingType(request.lodgingType().trim());
        lodging.setAddress(request.address().trim());
        lodging.setLatitude(request.latitude());
        lodging.setLongitude(request.longitude());
        lodging.setCheckinTime(request.checkinTime());
        lodging.setCheckoutTime(request.checkoutTime());
    }


    private Long requireApprovedOwnerId(String loginId) {
        Member member = members.findByLoginId(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (!"OWNER".equals(member.getRoleCode())
                || !"ACTIVE".equals(member.getStatus())
                || member.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        OwnerProfile owner = owners.findByMemberId(member.getMemberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));

        if (!"APPROVED".equals(owner.getReviewStatusCode())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return owner.getOwnerId();
    }

    private OwnerLodgingSummary toSummary(Lodging lodging) {
        return new OwnerLodgingSummary(
                lodging.getLodgingId(),
                lodging.getName(),
                lodging.getStatus()
        );
    }
}