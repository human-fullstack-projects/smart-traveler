package com.smarttraveler.chat;

import com.smarttraveler.mypage.preferences.dto.PreferenceDtos.*;
import com.smarttraveler.mypage.preferences.repository.PreferenceRepository;
import com.smarttraveler.mypage.service.MyPageService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatPersonalizationTest {
    MyPageService owners = mock(MyPageService.class);
    PreferenceRepository preferences = mock(PreferenceRepository.class);
    ChatPersonalization personalization = new ChatPersonalization(owners, preferences);

    void login(String name, long id) {
        var auth = UsernamePasswordAuthenticationToken.authenticated(name, "", List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(owners.owner(auth, null)).thenReturn(id);
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void guestSkipsMemberLookupAndUnconfiguredDefaultIsNotPersonalization() {
        assertEquals(Map.of(), personalization.savedPreferences());
        verifyNoInteractions(owners, preferences);
        login("unset", 7);
        when(preferences.find(7, List.of())).thenReturn(new Preferences(7, "normal", "normal", List.of(), List.of(), false));
        assertEquals(Map.of(), personalization.savedPreferences());
    }
    @Test void onlyPositiveSurveyTagsAreSentWithoutMemberId() {
        login("first", 7);
        when(preferences.find(7, List.of())).thenReturn(new Preferences(7, "relaxed", "early", List.of(
            new Selection("style", "장소:야외", "like", 1),
            new Selection("style", "선호지역:바다", "like", 1),
            new Selection("style", "선호지역:도시", "avoid", 1),
            new Selection("food", "해산물", "like", 1)), List.of(), true));
        assertEquals(Map.of("pace", "relaxed", "environment", "outdoor", "landscapes", List.of("sea")), personalization.savedPreferences());
        login("second", 8);
        when(preferences.find(8, List.of())).thenReturn(new Preferences(8, "packed", "normal", List.of(
            new Selection("style", "장소:둘 다", "like", 1),
            new Selection("style", "선호지역:산·숲", "like", 1)), List.of(), true));
        assertEquals(Map.of("pace", "packed", "landscapes", List.of("forest")), personalization.savedPreferences());
    }
    @Test @SuppressWarnings("unchecked")
    void accountSwitchRevokesReadWriteAccessToOldChat() {
        var jdbc = mock(JdbcTemplate.class);
        var dialogue = mock(ConditionDialogueService.class);
        var controller = new ChatController(jdbc, dialogue, personalization, mock(CourseRegionController.class), mock(RecommendedCourseService.class), mock(GuestRecommendedCourseService.class), new ChatMessageBuffer(jdbc, mock(org.springframework.transaction.PlatformTransactionManager.class)), mock(GuestChatQuota.class));
        var session = new MockHttpSession();
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(1L), anyString())).thenReturn(1);
        controller.load(1, session);
        String guest = (String) session.getAttribute(ChatController.class.getName() + ".guest");
        login("first", 7);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(1L), anyString())).thenReturn(0);
        assertThrows(ResponseStatusException.class, () -> controller.load(1, session));
        String first = (String) session.getAttribute(ChatController.class.getName() + ".guest");
        assertNotEquals(guest, first);
        login("second", 8);
        assertThrows(ResponseStatusException.class, () -> controller.load(1, session));
        String second = (String) session.getAttribute(ChatController.class.getName() + ".guest");
        assertNotEquals(first, second);
        assertThrows(ResponseStatusException.class, () -> controller.send(1, new ChatController.MessageInput("네"), session));
        assertThrows(ResponseStatusException.class, () -> controller.saveConditions(1, new ChatController.ConditionsInput("{}", false), session));
        verifyNoInteractions(dialogue);
    }
    @Test @SuppressWarnings("unchecked")
    void bothChatEntryPointsReadCurrentPreferences() {
        var jdbc = mock(JdbcTemplate.class);
        var dialogue = mock(ConditionDialogueService.class);
        var controller = new ChatController(jdbc, dialogue, personalization, mock(CourseRegionController.class), mock(RecommendedCourseService.class), mock(GuestRecommendedCourseService.class), new ChatMessageBuffer(jdbc, mock(org.springframework.transaction.PlatformTransactionManager.class)), mock(GuestChatQuota.class));
        var session = new MockHttpSession();
        login("first", 7);
        when(preferences.find(7, List.of())).thenReturn(new Preferences(7, "relaxed", "normal", List.of(), List.of(), true));
        when(dialogue.processResult(anyLong(), anyString(), anyMap())).thenReturn(new PythonAiClient.Result("다음 질문"));
        doAnswer(call -> { call.getArgument(1, KeyHolder.class).getKeyList().add(Map.of("id", 1L)); return 1; })
            .when(jdbc).update(any(PreparedStatementCreator.class), any(KeyHolder.class));
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(1L), anyString())).thenReturn(1);
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());
        controller.create(new ChatController.MessageInput("부산"), session, null);
        // A locked owned chat must exist for follow-up messages.
        when(jdbc.query(contains("FOR UPDATE"), any(RowMapper.class), eq(1L), anyString())).thenReturn(List.of(1L));
        controller.send(1, new ChatController.MessageInput("네"), session);
        verify(dialogue).processResult(1, "부산", Map.of("pace", "relaxed"));
        verify(dialogue).processResult(1, "네", Map.of("pace", "relaxed"));
        verify(preferences, times(2)).find(7, List.of());
    }

    @Test @SuppressWarnings("unchecked")
    void newChatIsBlockedWhenGuestQuotaIsExhausted() {
        var jdbc = mock(JdbcTemplate.class);
        var dialogue = mock(ConditionDialogueService.class);
        var quota = mock(GuestChatQuota.class);
        var controller = new ChatController(jdbc, dialogue, personalization, mock(CourseRegionController.class), mock(RecommendedCourseService.class), mock(GuestRecommendedCourseService.class), new ChatMessageBuffer(jdbc, mock(org.springframework.transaction.PlatformTransactionManager.class)), quota);
        var session = new MockHttpSession();
        doThrow(new GuestChatQuota.LimitReached()).when(quota).reserve(anyString(), eq(session), any());
        assertThrows(GuestChatQuota.LimitReached.class, () -> controller.create(new ChatController.MessageInput("부산"), session, null));
        verify(jdbc, never()).update(any(PreparedStatementCreator.class), any(KeyHolder.class));
    }
}
