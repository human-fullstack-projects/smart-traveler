package com.smarttraveler.auth;

import com.smarttraveler.auth.controller.OwnerReviewController;
import com.smarttraveler.auth.repository.MemberRepository;
import com.smarttraveler.auth.repository.OwnerReviewRepository;
import com.smarttraveler.auth.repository.SignupOwnerRepository;
import com.smarttraveler.domain.entity.Member;
import com.smarttraveler.domain.entity.OwnerProfile;
import com.smarttraveler.domain.entity.OwnerReviewHistory;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OwnerReviewTest {
    @Test void rejectedOwnerReapplyCreatesNewPendingHistory() {
        var members = mock(MemberRepository.class);
        var owners = mock(SignupOwnerRepository.class);
        var reviews = mock(OwnerReviewRepository.class);
        var controller = new OwnerReviewController(members, owners, reviews);
        Member member = new Member(); member.setMemberId(7L); member.setRoleCode("OWNER");
        OwnerProfile owner = new OwnerProfile(); owner.setOwnerId(9L); owner.setReviewStatusCode("REJECTED");
        when(members.findByLoginId("owner")).thenReturn(Optional.of(member));
        when(owners.findByMemberId(7L)).thenReturn(Optional.of(owner));
        when(reviews.findByOwnerIdOrderByCreatedAtDesc(9L)).thenReturn(List.of());
        Principal principal = () -> "owner";
        var result = controller.reapply(principal,
            new OwnerReviewController.ReapplyRequest("123-45", "새 상호", "대표", "주소", "010-1234"));
        assertEquals("PENDING", result.status());
        assertEquals("새 상호", owner.getBusinessName());
        var saved = org.mockito.ArgumentCaptor.forClass(OwnerReviewHistory.class);
        verify(reviews).save(saved.capture());
        assertEquals("PENDING", saved.getValue().getReviewStatusCode());
        assertEquals(9L, saved.getValue().getOwnerId());
        assertNull(saved.getValue().getReviewerId());
    }
}
