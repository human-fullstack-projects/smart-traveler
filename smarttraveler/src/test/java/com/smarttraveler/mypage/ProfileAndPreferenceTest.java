package com.smarttraveler.mypage;

import com.smarttraveler.mypage.profile.dto.ProfileDtos.*;
import com.smarttraveler.mypage.profile.repository.ProfileRepository;
import com.smarttraveler.mypage.profile.service.ProfileService;
import com.smarttraveler.mypage.preferences.dto.PreferenceDtos;
import com.smarttraveler.mypage.preferences.repository.PreferenceRepository;
import com.smarttraveler.mypage.preferences.service.PreferenceService;
import com.smarttraveler.mypage.service.MyPageService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
}
