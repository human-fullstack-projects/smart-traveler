package com.smarttraveler.mypage;

import com.smarttraveler.auth.config.AuthSecurityConfig;
import com.smarttraveler.auth.controller.AuthController;
import com.smarttraveler.auth.repository.MemberRepository;
import com.smarttraveler.auth.repository.SuspensionRepository;
import com.smarttraveler.auth.service.MemberDetailsService;
import com.smarttraveler.auth.service.SuspensionReleaseService;
import com.smarttraveler.chat.ChatSecurity;
import com.smarttraveler.domain.entity.Member;
import com.smarttraveler.mypage.controller.MyPageController;
import com.smarttraveler.mypage.dto.MyPageDtos.*;
import com.smarttraveler.mypage.repository.MyPageRepository;
import com.smarttraveler.mypage.service.MyPageService;
import com.smarttraveler.tour.service.TourPhotoService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 실제 보안 체인과 컨트롤러를 검사하되 DB·외부 API는 사용하지 않는다.
@SpringJUnitConfig(MyPageAccessTest.Config.class)
@WebAppConfiguration
class MyPageAccessTest {
    @Configuration @EnableWebMvc
    @Import({AuthSecurityConfig.class, ChatSecurity.class, MemberDetailsService.class,
        SuspensionReleaseService.class, AuthController.class, MyPageController.class, MyPageService.class})
    static class Config {
        @Bean MemberRepository members() { return mock(MemberRepository.class); }
        @Bean SuspensionRepository suspensions() { return mock(SuspensionRepository.class); }
        @Bean MyPageRepository myPageRepository() { return mock(MyPageRepository.class); }
        @Bean TourPhotoService tourPhotoService() { return mock(TourPhotoService.class); }
        @Bean ObjectMapper mapper() { return new ObjectMapper(); }
    }
    @Autowired WebApplicationContext context;
    @Autowired MemberRepository members;
    @Autowired MyPageRepository data;
    @Autowired ObjectMapper mapper;
    private MockMvc mvc;
    private Member member;

    @BeforeEach void setup() {
        reset(members, data);
        member = new Member(); member.setMemberId(7L); member.setLoginId("alice");
        member.setRoleCode("USER"); member.setStatus("ACTIVE"); member.setNickname("여행자");
        when(members.findByLoginId("alice")).thenReturn(Optional.of(member));
        when(data.courses(7)).thenReturn(List.of()); when(data.bookings(7)).thenReturn(List.of());
        when(data.posts(7)).thenReturn(List.of()); when(data.lockMember(7)).thenReturn(true);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test void anonymousCannotReadMyPage() throws Exception {
        mvc.perform(get("/api/mypage")).andExpect(status().isUnauthorized());
        verifyNoInteractions(data);
    }
    @Test void chatChainDoesNotShadowCustomAuthOrAdminPolicy() throws Exception {
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk());
        mvc.perform(get("/api/admin/dashboard").with(user("alice").roles("USER"))).andExpect(status().isForbidden());
    }
    @Test void ownerIsTakenFromLoginAndNotRequestHeader() throws Exception {
        mvc.perform(get("/api/mypage").with(user("alice")).header("X-MyPage-Member", "8"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(data);
        mvc.perform(get("/api/mypage").with(user("alice"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.memberId").value(7)).andExpect(jsonPath("$.courses").isEmpty());
        verify(data).courses(7); verify(data, never()).courses(8);
    }
    @Test void otherOwnersOrMissingResourcesHaveSameResponse() throws Exception {
        mvc.perform(get("/api/mypage/courses/course-99").with(user("alice"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/mypage/reservations/99").with(user("alice"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/mypage/posts/99").with(user("alice"))).andExpect(status().isNotFound());
        verify(data).course(7, "course-99");
    }
    @Test void suspendedSessionCannotReadData() throws Exception {
        member.setStatus("SUSPENDED");
        mvc.perform(get("/api/mypage").with(user("alice"))).andExpect(status().isUnauthorized());
        verifyNoInteractions(data);
    }
    @Test void importRequiresCsrfAndSameLoggedInMember() throws Exception {
        String json = mapper.writeValueAsString(request(7L));
        mvc.perform(post("/api/mypage/courses/import").with(user("alice"))
            .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isForbidden());
        mvc.perform(post("/api/mypage/courses/import").with(user("alice")).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request(8L))))
            .andExpect(status().isForbidden());
        verifyNoInteractions(data);
        mvc.perform(post("/api/mypage/courses/import").with(user("alice")).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isOk())
            .andExpect(jsonPath("$.importedClientIds[0]").value("test-client-id-0001"));
        verify(data).lockMember(7); verify(data).importDraft(eq(7L), any(Draft.class));
    }
    @Test void importRejectsReversedDatesAndDuplicateIdsBeforeWriting() throws Exception {
        Draft valid = request(7L).courses().getFirst();
        Draft invalid = new Draft(valid.clientId(), valid.title(), valid.region(), valid.endDate().plusDays(1), valid.startDate(), valid.days());
        for (var body : List.of(new ImportRequest(7L, List.of(invalid)), new ImportRequest(7L, List.of(valid, valid)))) {
            mvc.perform(post("/api/mypage/courses/import").with(user("alice")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body))).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(data);
    }
    @Test void invalidPayloadIsRejectedByValidation() throws Exception {
        for (String json : List.of(
            "{\"expectedMemberId\":7,\"courses\":[]}",
            "{\"expectedMemberId\":null,\"courses\":[]}",
            "{\"expectedMemberId\":7,\"courses\":[null]}")) {
            mvc.perform(post("/api/mypage/courses/import").with(user("alice")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(data);
    }
    @Test void bookingIncludesMatchedLodgingAndPhotoWithoutChangingReservationTerms() throws Exception {
        var date = LocalDate.of(2026, 12, 10);
        var stay = new StayInfo(18L, "부산 부산진구", "051-123-4567", "숙소 소개",
                "https://example.test/stay.jpg", "15:00:00", "11:00:00", true);
        when(data.bookings(7)).thenReturn(List.of(new BookingView("9", "TEST-BOOKING-9", "course-10",
                "부산 숙소", "더블룸", date, date.plusDays(1), 2, 120000, "CONFIRMED", stay)));
        mvc.perform(get("/api/mypage").with(user("alice"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.bookings[0].stay").value("부산 숙소"))
                .andExpect(jsonPath("$.bookings[0].stayInfo.imageUrl").value("https://example.test/stay.jpg"))
                .andExpect(jsonPath("$.bookings[0].stayInfo.regionMatched").value(true))
                .andExpect(jsonPath("$.bookings[0].date").value("2026-12-10"))
                .andExpect(jsonPath("$.bookings[0].price").value(120000));
    }
    @Test void placeDetailsRequireLoginAndReturnDatabasePhotoAndDescription() throws Exception {
        mvc.perform(get("/api/mypage/places/detail").param("placeId","15"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(data);
        var place = new PlaceDetails(15L,"경복궁","서울 종로구","DB 장소 설명",
                "https://example.test/palace.jpg","02-1234-5678","09:00~18:00");
        when(data.placeDetails(15L,"","서울")).thenReturn(place);
        mvc.perform(get("/api/mypage/places/detail").with(user("alice"))
                .param("placeId","15").param("region","서울"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placeId").value(15))
                .andExpect(jsonPath("$.description").value("DB 장소 설명"))
                .andExpect(jsonPath("$.imageUrl").value("https://example.test/palace.jpg"));
        mvc.perform(get("/api/mypage/places/detail").with(user("alice")))
                .andExpect(status().isBadRequest());
        when(data.placeDetails(null,"없는 장소","서울")).thenReturn(null);
        mvc.perform(get("/api/mypage/places/detail").with(user("alice"))
                .param("name","없는 장소").param("region","서울"))
                .andExpect(status().isNotFound());
    }
    private ImportRequest request(long owner) {
        var date = LocalDate.of(2026, 12, 10);
        return new ImportRequest(owner, List.of(new Draft("test-client-id-0001", "부산 여행", "부산", date, date,
            List.of(new Day(date, List.of("광안리"))))));
    }
}
