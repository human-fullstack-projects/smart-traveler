package com.smarttraveler.auth;

import com.smarttraveler.auth.dto.SignupRequest;
import com.smarttraveler.auth.dto.SignupResult;
import com.smarttraveler.auth.dto.TermConsent;
import com.smarttraveler.auth.repository.MemberRepository;
import com.smarttraveler.auth.repository.SignupOwnerRepository;
import com.smarttraveler.auth.service.SignupService;
import com.smarttraveler.domain.entity.Member;
import com.smarttraveler.domain.entity.OwnerProfile;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SignupServiceTest {
    private final MemberRepository members = mock(MemberRepository.class);
    private final SignupOwnerRepository owners = mock(SignupOwnerRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private SignupService signup;

    @BeforeEach void setUp() {
        reset(members, owners, passwords, jdbc);
        signup = new SignupService(members, owners, passwords, jdbc);
        when(passwords.encode(anyString())).thenReturn("encoded");
        when(jdbc.queryForList(anyString())).thenReturn(List.of(Map.of("terms_id", 1L, "is_required", 1)));
        when(members.saveAndFlush(any(Member.class))).thenAnswer(call -> {
            Member member = call.getArgument(0);
            member.setMemberId(42L);
            return member;
        });
        when(owners.saveAndFlush(any(OwnerProfile.class))).thenAnswer(call -> call.getArgument(0));
    }

    private SignupRequest owner(String loginId, String email, String businessNumber) {
        return new SignupRequest("OWNER", loginId, "password123", "새 업주", email,
                businessNumber, "상호", "대표", "주소", "02-1234-5678", List.of(new TermConsent(1L, true)), null, null);
    }

    private SignupRequest traveler(String name, String phone) {
        return new SignupRequest("USER", "newuser", "password123", "새 여행자", "user@example.com",
                null, null, null, null, null, List.of(new TermConsent(1L, true)), name, phone);
    }

    @Test void travelerSavesIdentityNameAndDigitsOnlyPhone() {
        signup.create(traveler(" 홍길동 ", "010-1234-5678"));
        verify(members).saveAndFlush(argThat(member -> "홍길동".equals(member.getRealName())
                && "01012345678".equals(member.getPhoneNumber()) && member.getIdentityVerifiedAt() != null));
    }

    @Test void travelerNeedsIdentity() {
        expect(traveler(null, "010-1234-5678"), HttpStatus.BAD_REQUEST, "본인인증 이름을 확인해주세요.");
        expect(traveler("홍길동", "02-123-4567"), HttpStatus.BAD_REQUEST, "본인인증 휴대폰 번호를 확인해주세요.");
        verify(members, never()).saveAndFlush(any());
    }

    @Test void samePhoneCannotSignUpTwice() {
        when(members.existsByPhoneNumber("01012345678")).thenReturn(true);
        expect(traveler("홍길동", "01012345678"), HttpStatus.CONFLICT, "이미 가입된 휴대폰 번호예요.");
        assertFalse(signup.phoneAvailable("010-1234-5678"));
        assertTrue(signup.phoneAvailable("010-9999-0000"));
        verify(members, never()).saveAndFlush(any());
    }

    @Test void phoneConstraintRaceNamesPhone() {
        when(members.saveAndFlush(any(Member.class))).thenThrow(new DataIntegrityViolationException(
                "insert failed", new IllegalStateException("Duplicate entry for key 'UK_members_phone'")));
        expect(traveler("홍길동", "010-1234-5678"), HttpStatus.CONFLICT, "이미 가입된 휴대폰 번호예요.");
    }

    @Test void ownerSignupDoesNotNeedIdentity() {
        signup.create(owner("newowner", "new@example.com", "123-45-67890"));
        verify(members).saveAndFlush(argThat(member -> member.getPhoneNumber() == null));
    }

    private void expect(SignupRequest request, HttpStatus status, String message) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> signup.create(request));
        assertEquals(status, error.getStatusCode());
        assertEquals(message, error.getReason());
    }

    @Test void newOwnerCreatesMemberAndProfile() {
        SignupResult result = signup.create(owner("newowner", "new@example.com", "123-45-67890"));
        assertEquals(42L, result.memberId());
        assertEquals("OWNER", result.role());
        verify(owners).saveAndFlush(argThat(profile -> profile.getMemberId() == 42L
                && profile.getBusinessNumber().equals("123-45-67890")));
    }

    @Test void eachExistingUniqueValueHasItsOwnMessage() {
        when(members.existsByLoginId("usedowner")).thenReturn(true);
        expect(owner("usedowner", "new@example.com", "123-45-67890"), HttpStatus.CONFLICT,
                "이미 사용 중인 아이디입니다.");
        when(members.existsByEmail("traveler@example.com")).thenReturn(true);
        expect(owner("newowner", "traveler@example.com", "123-45-67890"), HttpStatus.CONFLICT,
                "이미 사용 중인 이메일입니다.");
        when(owners.existsByBusinessNumber("123-45-67890")).thenReturn(true);
        expect(owner("newowner", "new@example.com", "123-45-67890"), HttpStatus.CONFLICT,
                "사업자번호가 이미 사용 중입니다.");
        verify(members, never()).saveAndFlush(any());
    }

    @Test void longBusinessNumberIsInputError() {
        expect(owner("newowner", "new@example.com", "123456789012345678901"), HttpStatus.BAD_REQUEST,
                "업주 사업자 정보 길이를 확인하세요.");
        verify(members, never()).saveAndFlush(any());
    }

    @Test void databaseConstraintRaceUsesOnlyMatchingDuplicateMessage() {
        when(members.saveAndFlush(any(Member.class))).thenThrow(new DataIntegrityViolationException(
                "insert failed", new IllegalStateException("Duplicate entry for key 'UK_members2'")));
        expect(owner("newowner", "new@example.com", "123-45-67890"), HttpStatus.CONFLICT,
                "이미 사용 중인 이메일입니다.");
    }

    @Test void ownerConstraintRaceNamesBusinessNumber() {
        when(owners.saveAndFlush(any(OwnerProfile.class))).thenThrow(new DataIntegrityViolationException(
                "insert failed", new IllegalStateException("Duplicate entry for key 'UK_owner_profiles2'")));
        expect(owner("newowner", "new@example.com", "123-45-67890"), HttpStatus.CONFLICT,
                "사업자번호가 이미 사용 중입니다.");
    }

    @Test void ownerSaveFailureRollsBackWholeSignupTransaction() {
        when(owners.saveAndFlush(any(OwnerProfile.class))).thenThrow(new DataIntegrityViolationException(
                "insert failed", new IllegalStateException("Data too long for column 'business_phone'")));
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        TransactionStatus status = new SimpleTransactionStatus();
        when(transactions.getTransaction(any(TransactionDefinition.class))).thenReturn(status);
        ProxyFactory proxy = new ProxyFactory(signup);
        proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        SignupService transactional = (SignupService) proxy.getProxy();

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> transactional.create(owner("newowner", "new@example.com", "123-45-67890")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(members).saveAndFlush(any(Member.class));
        verify(transactions).rollback(status);
        verify(transactions, never()).commit(any());
    }
}
