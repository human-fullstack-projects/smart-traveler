package com.smarttraveler.auth.service;

import com.smarttraveler.auth.entity.MemberSocialAccount;
import com.smarttraveler.auth.repository.MemberRepository;
import com.smarttraveler.auth.repository.MemberSocialAccountRepository;
import com.smarttraveler.auth.dto.TermConsent;
import com.smarttraveler.domain.entity.Member;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SocialSignupService {
    private final MemberRepository members;
    private final MemberSocialAccountRepository links;
    private final PasswordEncoder passwords;
    private final JdbcTemplate jdbc;

    public SocialSignupService(MemberRepository members, MemberSocialAccountRepository links,
            PasswordEncoder passwords, JdbcTemplate jdbc) {
        this.members = members; this.links = links; this.passwords = passwords; this.jdbc = jdbc;
    }

    @Transactional
    public Member create(PendingSocialProfile profile, String nickname, List<TermConsent> selected) {
        if (profile == null) throw bad("소셜 로그인부터 다시 진행해주세요.");
        if (nickname == null || nickname.isBlank() || nickname.trim().length() > 50)
            throw bad("닉네임은 1~50자로 입력해주세요.");
        if (members.existsByNickname(nickname.trim()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
        if (links.findByProviderAndProviderUserId(profile.provider(), profile.providerUserId()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 소셜 계정입니다. 다시 로그인해주세요.");
        List<Map<String, Object>> activeTerms = jdbc.queryForList("""
            SELECT t.terms_id, t.is_required FROM terms t
            WHERE t.effective_date <= CURRENT_DATE()
              AND NOT EXISTS (SELECT 1 FROM terms newer WHERE newer.terms_type=t.terms_type
                AND newer.effective_date<=CURRENT_DATE() AND
                (newer.effective_date>t.effective_date OR
                 (newer.effective_date=t.effective_date AND newer.terms_id>t.terms_id)))
            """);
        if (activeTerms.isEmpty()) throw bad("가입 약관이 아직 등록되지 않았습니다.");
        Map<Long, Boolean> consents = new HashMap<>();
        if (selected != null) for (TermConsent consent : selected)
            if (consent.termsId() != null) consents.put(consent.termsId(), consent.agreed());
        for (Map<String, Object> term : activeTerms) {
            Long id = ((Number) term.get("terms_id")).longValue();
            Object required = term.get("is_required");
            if ((Boolean.TRUE.equals(required) || required instanceof Number number && number.intValue() != 0)
                    && !Boolean.TRUE.equals(consents.get(id))) throw bad("필수 약관에 동의해야 합니다.");
        }
        LocalDateTime now = LocalDateTime.now();
        Member member = new Member();
        member.setLoginId(loginId(profile));
        member.setPassword(passwords.encode(UUID.randomUUID().toString()));
        member.setNickname(nickname.trim());
        member.setEmail(profile.email());
        member.setRoleCode("USER");
        member.setStatus("ACTIVE");
        member.setCreatedAt(now); member.setUpdatedAt(now);
        try { member = members.saveAndFlush(member); }
        catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 이메일 또는 계정입니다.");
        }
        for (Map<String, Object> term : activeTerms) {
            Long id = ((Number) term.get("terms_id")).longValue();
            jdbc.update("INSERT INTO member_consents(member_id,terms_id,is_agreed,consented_at) VALUES (?,?,?,?)",
                    member.getMemberId(), id, Boolean.TRUE.equals(consents.get(id)), now);
        }
        MemberSocialAccount link = new MemberSocialAccount();
        link.setMemberId(member.getMemberId());
        link.setProvider(profile.provider());
        link.setProviderUserId(profile.providerUserId());
        link.setEmail(profile.email());
        link.setCreatedAt(now);
        try { links.saveAndFlush(link); }
        catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 소셜 계정입니다. 다시 로그인해주세요.");
        }
        return member;
    }

    private static String loginId(PendingSocialProfile profile) {
        String prefix = profile.provider().toLowerCase() + "_";
        String raw = profile.providerUserId();
        if (raw.matches("[A-Za-z0-9_]+") && prefix.length() + raw.length() <= 50) return prefix + raw;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte value : digest) hex.append(String.format("%02x", value & 0xff));
            return prefix + hex.substring(0, 50 - prefix.length());
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    public record PendingSocialProfile(String provider, String providerUserId, String nickname, String email)
            implements java.io.Serializable {}
}
