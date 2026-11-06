package com.smarttraveler.owner.service;

import com.smarttraveler.auth.repository.MemberRepository;
import com.smarttraveler.auth.repository.SignupOwnerRepository;
import com.smarttraveler.owner.dto.OwnerLodgingSummary;
import com.smarttraveler.owner.repository.OwnerLodgingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
        var member = members.findByLoginId(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (!"OWNER".equals(member.getRoleCode())
                || !"ACTIVE".equals(member.getStatus())
                || member.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        var owner = owners.findByMemberId(member.getMemberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));

        if (!"APPROVED".equals(owner.getReviewStatusCode())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return lodgings.findAllByOwnerIdOrderByLodgingId(owner.getOwnerId())
                .stream()
                .map(lodging -> new OwnerLodgingSummary(
                        lodging.getLodgingId(),
                        lodging.getName(),
                        lodging.getStatus()
                ))
                .toList();
    }
}