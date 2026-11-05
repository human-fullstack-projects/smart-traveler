package com.smarttraveler.auth.dto;

import java.util.List;

public record SignupRequest(String role, String loginId, String password, String nickname, String email,
        String businessNumber, String businessName, String representativeName,
        String businessAddress, String businessPhone, List<TermConsent> consents) {}
