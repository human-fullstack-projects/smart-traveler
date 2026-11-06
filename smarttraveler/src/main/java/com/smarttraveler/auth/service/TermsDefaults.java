package com.smarttraveler.auth.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * AUTH-03: 약관 표가 비어 있는 PC에서도 회원가입이 되도록, 종류별 약관이 없을 때만 기본 약관을 넣는다.
 * 예전에 넣은 제목 한 줄짜리 임시 내용은 예시 본문으로 바꾼다(다른 내용은 건드리지 않음).
 */
@Component
public class TermsDefaults implements ApplicationRunner {
    private static final String SERVICE = """
        [예시] 스마트 트래블러 서비스 이용약관 (1.0)

        제1조 (목적)
        이 약관은 스마트 트래블러(이하 「서비스」)가 제공하는 AI 여행 코스 추천, 숙소 검색·예약, 여행 코스 공유 기능의 이용 조건과 절차를 정합니다.

        제2조 (회원 가입)
        ① 회원은 일반 여행자와 숙박업소 회원(업주)으로 나뉩니다.
        ② 업주 회원은 관리자 승인을 받은 뒤 숙소를 등록할 수 있고, 숙소는 숙소별 승인을 받은 뒤 노출됩니다.

        제3조 (회원의 의무)
        ① 회원은 다른 사람의 정보를 도용하거나 사실과 다른 정보를 등록해서는 안 됩니다.
        ② 여행 코스·댓글에 욕설, 광고, 남의 권리를 침해하는 내용을 올리면 블라인드 또는 이용 정지될 수 있습니다.

        제4조 (예약과 결제)
        숙소 예약은 결제가 확인된 뒤 확정되며, 취소·환불은 숙소별 환불 규정을 따릅니다.

        제5조 (서비스 변경)
        AI가 추천한 코스는 참고용이며, 실제 운영 시간·요금은 방문 전에 확인해 주세요.
        """;
    private static final String PRIVACY = """
        [예시] 개인정보 수집·이용 동의 (1.0)

        1. 수집 항목
        - 공통: 아이디, 비밀번호(암호화 저장), 닉네임, 이메일
        - 업주 회원: 사업자 등록번호, 상호명, 대표자명, 사업장 주소, 연락처
        - 예약 시: 예약자 이름, 연락처

        2. 이용 목적
        회원 식별, 숙소 예약·결제 처리, 업주 심사, 부정 이용 방지, 고객 문의 응대

        3. 보관 기간
        회원 탈퇴 시 지체 없이 파기합니다. 단, 관계 법령에 따라 거래 기록은 정해진 기간 동안 보관합니다.

        4. 동의 거부 권리
        동의를 거부할 수 있으나, 필수 항목에 동의하지 않으면 회원 가입이 제한됩니다.
        """;
    private static final String[][] DEFAULTS = {
        {"SERVICE", "서비스 이용약관", SERVICE, "로컬 테스트 서비스 약관"},
        {"PRIVACY", "개인정보 수집·이용 동의", PRIVACY, "로컬 테스트 개인정보 약관"},
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
                    """, term[0], term[2], term[0]);
                jdbc.update("UPDATE terms SET content=? WHERE terms_type=? AND (content IS NULL OR content IN (?,?))",
                    term[2], term[0], term[1], term[3]);
            } catch (RuntimeException e) {
                // 기본 약관을 못 넣어도 서버는 계속 켜져야 한다.
                org.slf4j.LoggerFactory.getLogger(TermsDefaults.class).warn("기본 약관 추가 실패: {}", e.getMessage());
            }
        }
    }
}
