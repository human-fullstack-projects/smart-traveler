package com.smarttraveler.auth.service;

import com.smarttraveler.auth.dto.SignupRequest;
import com.smarttraveler.auth.dto.SignupResult;
import com.smarttraveler.auth.repository.MemberRepository;
import com.smarttraveler.auth.repository.SignupOwnerRepository;
import com.smarttraveler.domain.entity.Member;
import com.smarttraveler.domain.entity.OwnerProfile;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SignupService {
    private final MemberRepository members;
    private final SignupOwnerRepository owners;
    private final PasswordEncoder passwords;
    private final JdbcTemplate jdbc;
    public SignupService(MemberRepository members, SignupOwnerRepository owners, PasswordEncoder passwords, JdbcTemplate jdbc) {
        this.members = members; this.owners = owners; this.passwords = passwords; this.jdbc = jdbc;
    }

    public List<Map<String,Object>> activeTerms() {
        return jdbc.queryForList("""
            SELECT t.terms_id AS termsId, t.terms_type AS type, t.terms_version AS version,
                   (t.is_required <> 0) AS required, t.content AS content
            FROM terms t
            WHERE t.effective_date <= CURRENT_DATE()
              AND NOT EXISTS (SELECT 1 FROM terms newer WHERE newer.terms_type=t.terms_type
                AND newer.effective_date<=CURRENT_DATE() AND
                (newer.effective_date>t.effective_date OR
                 (newer.effective_date=t.effective_date AND newer.terms_id>t.terms_id)))
            ORDER BY t.terms_id
            """);
    }

    @Transactional
    public SignupResult create(SignupRequest request) {
        if (request == null) throw bad("가입 정보가 없습니다.");
        String role = request.role() == null ? "USER" : request.role().trim().toUpperCase();
        if (!Set.of("USER", "OWNER").contains(role)) throw bad("USER 또는 OWNER로 가입할 수 있습니다.");
        if (blank(request.loginId()) || !request.loginId().matches("[A-Za-z0-9_]{4,50}"))
            throw bad("아이디는 영문, 숫자, 밑줄 4~50자로 입력하세요.");
        if (request.password() == null || request.password().length() < 8 || request.password().length() > 72
            || !request.password().matches("(?=.*[A-Za-z])(?=.*[0-9]).*"))
            throw bad("비밀번호는 영문과 숫자를 포함한 8~72자로 입력하세요.");
        if (members.existsByLoginId(request.loginId())) throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다.");
        if (blank(request.nickname()) || request.nickname().length() > 50) throw bad("닉네임을 입력하세요.");
        if (blank(request.email()) || request.email().length() > 255 || !request.email().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))
            throw bad("올바른 이메일을 입력하세요.");
        if ("OWNER".equals(role) && (blank(request.businessNumber()) || blank(request.businessName())
            || blank(request.representativeName()) || blank(request.businessAddress())
            || blank(request.businessPhone()))) throw bad("업주 사업자 정보 5개 항목은 필수입니다.");
        if ("OWNER".equals(role) && (request.businessNumber().length()>20 || request.businessName().length()>100
            || request.representativeName().length()>50 || request.businessAddress().length()>255
            || request.businessPhone().length()>20)) throw bad("업주 사업자 정보 길이를 확인하세요.");

        List<Map<String,Object>> activeTerms = jdbc.queryForList("""
            SELECT t.terms_id, t.is_required FROM terms t
            WHERE t.effective_date <= CURRENT_DATE()
              AND NOT EXISTS (SELECT 1 FROM terms newer WHERE newer.terms_type=t.terms_type
                AND newer.effective_date<=CURRENT_DATE() AND
                (newer.effective_date>t.effective_date OR
                 (newer.effective_date=t.effective_date AND newer.terms_id>t.terms_id)))
            """);
        if (activeTerms.isEmpty()) throw bad("가입 약관이 아직 등록되지 않았습니다.");
        Map<Long,Boolean> consents = new java.util.HashMap<>();
        if (request.consents() != null) for (var consent : request.consents())
            if (consent.termsId() != null) consents.put(consent.termsId(), consent.agreed());
        for (var term : activeTerms) {
            Long id = ((Number)term.get("terms_id")).longValue();
            Object required = term.get("is_required");
            if ((Boolean.TRUE.equals(required) || required instanceof Number n && n.intValue() != 0)
                && !Boolean.TRUE.equals(consents.get(id)))
                throw bad("필수 약관에 동의해야 합니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        Member member = new Member();
        member.setLoginId(request.loginId());
        member.setPassword(passwords.encode(request.password()));
        member.setNickname(request.nickname().trim());
        member.setEmail(request.email().trim());
        member.setRoleCode(role);
        member.setStatus("ACTIVE");
        member.setCreatedAt(now); member.setUpdatedAt(now);
        try { member = members.saveAndFlush(member); }
        catch (DataIntegrityViolationException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, "아이디 또는 이메일이 이미 사용 중입니다."); }
        for (var term : activeTerms) {
            Long id = ((Number)term.get("terms_id")).longValue();
            jdbc.update("INSERT INTO member_consents(member_id,terms_id,is_agreed,consented_at) VALUES (?,?,?,?)",
                member.getMemberId(), id, Boolean.TRUE.equals(consents.get(id)), now);
        }
        if ("OWNER".equals(role)) {
            OwnerProfile owner = new OwnerProfile();
            owner.setMemberId(member.getMemberId());
            owner.setBusinessNumber(request.businessNumber().trim());
            owner.setBusinessName(request.businessName().trim());
            owner.setRepresentativeName(request.representativeName().trim());
            owner.setBusinessAddress(request.businessAddress());
            owner.setBusinessPhone(request.businessPhone());
            owner.setReviewStatusCode("PENDING");
            owner.setCreatedAt(now); owner.setUpdatedAt(now);
            try { owners.saveAndFlush(owner); }
            catch (DataIntegrityViolationException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, "사업자번호가 이미 사용 중입니다."); }
        }
        return new SignupResult(member.getMemberId(), role, "OWNER".equals(role) ? "PENDING" : null);
    }
    private static boolean blank(String s) { return s == null || s.isBlank(); }
    private static ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
