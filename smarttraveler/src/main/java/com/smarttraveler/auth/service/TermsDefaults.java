package com.smarttraveler.auth.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** AUTH-03: 약관 표가 비어 있는 PC에서도 회원가입이 되도록, 종류별 약관이 없을 때만 기본 약관을 넣는다. */
@Component
public class TermsDefaults implements ApplicationRunner {
    private static final String[][] DEFAULTS = {
        {"SERVICE", "서비스 이용약관"},
        {"PRIVACY", "개인정보 수집·이용 동의"},
    };
    private final JdbcTemplate jdbc;

    public TermsDefaults(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void run(ApplicationArguments args) {
        for (String[] term : DEFAULTS) {
            try {
                jdbc.update("""
                    INSERT INTO terms(terms_type,terms_version,is_required,effective_date,content,created_at)
                    SELECT ?,'1.0',TRUE,CURRENT_DATE(),?,NOW() FROM DUAL
                    WHERE NOT EXISTS (SELECT 1 FROM terms WHERE terms_type=?)
                    """, term[0], term[1], term[0]);
            } catch (RuntimeException e) {
                // 기본 약관을 못 넣어도 서버는 계속 켜져야 한다.
                org.slf4j.LoggerFactory.getLogger(TermsDefaults.class).warn("기본 약관 추가 실패: {}", e.getMessage());
            }
        }
    }
}
