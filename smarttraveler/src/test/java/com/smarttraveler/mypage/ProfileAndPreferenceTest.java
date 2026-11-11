package com.smarttraveler.mypage;

import com.smarttraveler.mypage.profile.dto.ProfileDtos.*;
import com.smarttraveler.mypage.profile.repository.ProfileRepository;
import com.smarttraveler.mypage.profile.service.ProfileService;
import com.smarttraveler.mypage.preferences.dto.PreferenceDtos;
import com.smarttraveler.mypage.preferences.repository.PreferenceRepository;
import com.smarttraveler.mypage.preferences.service.PreferenceService;
import com.smarttraveler.mypage.service.MyPageService;
import java.util.List;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// DB·외부 API 없이 잘못된 비밀번호·태그 요청에서 쓰기가 발생하지 않는지 검사한다.
class ProfileAndPreferenceTest {
    MyPageService owners;
    ProfileRepository profiles;
    PreferenceRepository preferences;
    PasswordEncoder encoder;
    ProfileService profileService;
    PreferenceService preferenceService;
    Authentication auth = UsernamePasswordAuthenticationToken.authenticated("test", "", List.of());
    @BeforeEach void setup() {
        owners = mock(MyPageService.class); profiles = mock(ProfileRepository.class);
        preferences = mock(PreferenceRepository.class); encoder = mock(PasswordEncoder.class);
        when(owners.owner(auth, 7L)).thenReturn(7L);
        when(profiles.lockPassword(7)).thenReturn("stored-hash");
        when(profiles.find(7)).thenReturn(new Profile(7, "test", "여행자", "a@example.invalid", null));
        when(preferences.tags()).thenReturn(List.of()); when(preferences.lockOwner(7)).thenReturn(true);
        when(preferences.lockSurveyOwner(7)).thenReturn(true);
        profileService = new ProfileService(owners, profiles, encoder);
        preferenceService = new PreferenceService(owners, preferences);
    }
    @Test void wrongPasswordDoesNotModifyProfileOrImage() {
        var input = new SaveProfile(7L, "변경", "a@example.invalid", null, true, "wrong", "New123456", "New123456");
        assertThrows(ResponseStatusException.class, () -> profileService.save(auth, input));
        verify(profiles, never()).update(anyLong(), anyString(), anyString(), any(), any());
        verify(encoder, never()).encode(any());
    }
    @Test void unselectedPasswordChangeDoesNotOverwritePassword() {
        var input = new SaveProfile(7L, " 여행자 ", "a@example.invalid", null, false, null, null, null);
        profileService.save(auth, input);
        verify(profiles).update(7, "여행자", "a@example.invalid", null, null);
        verifyNoInteractions(encoder);
    }
    @Test void changedPasswordIsEncodedAndNeverReturned() {
        when(encoder.matches("Old12345", "stored-hash")).thenReturn(true);
        when(encoder.encode("New123456")).thenReturn("new-hash");
        var result = profileService.save(auth, new SaveProfile(7L, "여행자", "a@example.invalid", null, true, "Old12345", "New123456", "New123456"));
        verify(profiles).update(7, "여행자", "a@example.invalid", "new-hash", null);
        assertTrue(result.passwordChanged());
        assertFalse(result.toString().contains("New123456"));
    }
    @Test void unsupportedImageCannotReachDatabaseWrite() {
        var input = new SaveProfile(7L, "여행자", "a@example.invalid", "data:image/svg+xml;base64,AA==", false, null, null, null);
        assertThrows(ResponseStatusException.class, () -> profileService.save(auth, input));
        verifyNoInteractions(profiles);
    }
    @Test void accountMismatchStopsBothFeaturesBeforeDataLookup() {
        when(owners.owner(auth, 8L)).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN));
        assertThrows(ResponseStatusException.class, () -> profileService.get(auth, 8L));
        assertThrows(ResponseStatusException.class, () -> preferenceService.get(auth, 8L));
        verifyNoInteractions(profiles, preferences);
    }
    @Test void unknownAndDuplicatePreferenceTagsNeverWrite() {
        var known = new PreferenceDtos.Selection("interest", "자연", "like", 2);
        var unknown = new PreferenceDtos.Selection("interest", "임의 태그", "like", 2);
        for (var tags : List.of(List.of(unknown), List.of(known, known))) {
            assertThrows(ResponseStatusException.class, () -> preferenceService.save(auth, new PreferenceDtos.Save(7L, "normal", "normal", tags)));
        }
        verify(preferences, never()).save(anyLong(), any());
    }
    @Test void clearPreferencesOnlyWritesCurrentOwnersSettings() {
        var input = new PreferenceDtos.Save(7L, "relaxed", "late", List.of());
        preferenceService.save(auth, input);
        verify(preferences).save(7, input);
        verify(preferences, never()).save(eq(8L), any());
    }
    private PreferenceDtos.Selection region(String name) {
        return new PreferenceDtos.Selection("style", "지역:" + name, "like", 1);
    }
    private PreferenceDtos.Selection mood(String name) {
        return new PreferenceDtos.Selection("style", "분위기:" + name, "like", 1);
    }
    private List<PreferenceDtos.Selection> surveyTags() {
        return List.of(region("명동"), region("홍대"), region("경주"), mood("조용한 여행"));
    }
    private PreferenceDtos.Save survey(List<PreferenceDtos.Selection> tags) {
        return new PreferenceDtos.Save(7L, "normal", "normal", tags, true);
    }
    private void rejectsSurvey(List<PreferenceDtos.Selection> tags) {
        var error = assertThrows(ResponseStatusException.class, () -> preferenceService.save(auth, survey(tags)));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(preferences, never()).saveSurvey(anyLong(), any());
    }
    @Test void surveySavesThreeRegionsAndOneMood() {
        var tags = surveyTags();
        preferenceService.save(auth, survey(tags));
        verify(preferences).lockSurveyOwner(7);
        verify(preferences).saveSurvey(7, tags);
        verify(preferences, never()).save(anyLong(), any());
    }
    @Test void surveyRejectsTwoOrFourRegions() {
        rejectsSurvey(List.of(region("명동"), region("홍대"), mood("조용한 여행")));
        rejectsSurvey(List.of(region("명동"), region("홍대"), region("경주"), region("제주"), mood("조용한 여행")));
    }
    @Test void surveyRejectsUnknownRegionAndTwoMoods() {
        rejectsSurvey(List.of(region("없는지역"), region("홍대"), region("경주"), mood("조용한 여행")));
        rejectsSurvey(List.of(region("명동"), region("홍대"), mood("조용한 여행"), mood("활발한 여행")));
    }
    @Test void surveyRejectsDuplicatesAvoidAndWeightAboveOne() {
        rejectsSurvey(List.of(region("명동"), region("명동"), region("경주"), mood("조용한 여행")));
        rejectsSurvey(List.of(region("명동"), region("홍대"), region("경주"), new PreferenceDtos.Selection("style", "분위기:조용한 여행", "avoid", 1)));
        rejectsSurvey(List.of(region("명동"), region("홍대"), region("경주"), new PreferenceDtos.Selection("style", "분위기:조용한 여행", "like", 2)));
    }
    @Test void ownerAndAdminCannotSaveSurvey() {
        when(preferences.lockSurveyOwner(7)).thenReturn(false);
        for (var role : List.of("OWNER", "ADMIN")) {
            var error = assertThrows(ResponseStatusException.class, () -> preferenceService.save(auth, survey(surveyTags())));
            assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode(), role);
            assertEquals("여행자 회원만 선호도 조사를 저장할 수 있어요.", error.getReason());
        }
        verify(preferences, never()).saveSurvey(anyLong(), any());
    }
    @Test void normalSaveIgnoresSurveyTagsEvenWhenUnknownAndKeepsExistingAnswers() {
        var normal = new PreferenceDtos.Selection("interest", "자연", "like", 2);
        var input = new PreferenceDtos.Save(7L, "relaxed", "late", List.of(normal, region("명동"), region("없는지역"), mood("조용한 여행")));
        preferenceService.save(auth, input);
        verify(preferences).save(7, input);
        verify(preferences, never()).saveSurvey(anyLong(), any());
    }
    @Test void surveyTagsAreIncludedInCatalog() {
        preferenceService.get(auth, 7L);
        @SuppressWarnings("unchecked") ArgumentCaptor<List<PreferenceDtos.TagOption>> catalog = ArgumentCaptor.forClass(List.class);
        verify(preferences).find(eq(7L), catalog.capture());
        assertEquals(38, catalog.getValue().size());
        assertTrue(catalog.getValue().contains(new PreferenceDtos.TagOption("style", "지역:태안")));
        assertTrue(catalog.getValue().contains(new PreferenceDtos.TagOption("style", "분위기:활발한 여행")));
    }
    @Test void repositorySurveyWritePreservesPaceAndNormalTags() {
        var jdbc = mock(JdbcTemplate.class);
        new PreferenceRepository(jdbc).saveSurvey(7, List.of());
        verify(jdbc).update(argThat(sql -> sql.contains("ON DUPLICATE KEY UPDATE member_no=member_no")), eq(7L));
        verify(jdbc).update(argThat(sql -> sql.contains("DELETE p FROM member_preference_tag")
            && sql.contains("t.tag_type='style'") && !sql.contains("NOT (")), eq(7L));
    }
    @Test void repositoryNormalWritePreservesSurveyTagsAndIgnoresSubmittedSurveyTags() {
        var jdbc = mock(JdbcTemplate.class);
        new PreferenceRepository(jdbc).save(7, new PreferenceDtos.Save(7L, "relaxed", "late", List.of(region("명동"))));
        verify(jdbc).update(argThat(sql -> sql.contains("ON DUPLICATE KEY UPDATE pace=VALUES(pace)")), eq(7L), eq("relaxed"), eq("late"));
        verify(jdbc).update(argThat(sql -> sql.contains("DELETE p FROM member_preference_tag")
            && sql.contains("NOT (t.tag_type='style'")), eq(7L));
        verifyNoMoreInteractions(jdbc);
    }
    @Test void reSurveyUsesScopedReplacement() {
        var first = surveyTags();
        var second = List.of(region("제주"), region("통영"), region("태안"), mood("활발한 여행"));
        preferenceService.save(auth, survey(first));
        preferenceService.save(auth, survey(second));
        verify(preferences).saveSurvey(7, first);
        verify(preferences).saveSurvey(7, second);
    }
}
