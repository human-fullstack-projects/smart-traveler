package com.smarttraveler.auth.service;

import com.smarttraveler.auth.repository.MemberRepository;
import com.smarttraveler.domain.entity.Member;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** 회원가입 휴대폰 본인인증(시연용) 이름·번호 확인과 저장. 번호는 숫자만 저장해 하이픈 차이로 중복 가입되지 않게 한다. */
public final class IdentityRules {
    private IdentityRules() {}

    /** 숫자만 남긴 휴대폰 번호. 형식이 틀리면 null. */
    public static String normalizePhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("\\D", "");
        return digits.matches("01[016789]\\d{7,8}") ? digits : null;
    }

    /** 이름·번호를 확인해 회원에 넣는다. 저장 전에 부른다. */
    public static void apply(Member member, String name, String phone, MemberRepository members, LocalDateTime now) {
        if (name == null || name.isBlank() || name.trim().length() > 50)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "본인인증 이름을 확인해주세요.");
        String digits = normalizePhone(phone);
        if (digits == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "본인인증 휴대폰 번호를 확인해주세요.");
        if (members.existsByPhoneNumber(digits))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 휴대폰 번호예요.");
        member.setRealName(name.trim());
        member.setPhoneNumber(digits);
        member.setIdentityVerifiedAt(now);
    }

    /** 저장 중 유일 키 오류가 휴대폰 번호 때문인지. */
    public static boolean isPhoneConflict(String details) {
        String text = details == null ? "" : details.toLowerCase(java.util.Locale.ROOT);
        return text.contains("uk_members_phone") || text.contains("members.phone_number");
    }
}
