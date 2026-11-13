package com.smarttraveler.mypage.preferences.service;

import com.smarttraveler.mypage.preferences.dto.PreferenceDtos.*;
import com.smarttraveler.mypage.preferences.repository.PreferenceRepository;
import com.smarttraveler.mypage.service.MyPageService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class PreferenceService {
    private final MyPageService owners;
    private final PreferenceRepository preferences;
    public PreferenceService(MyPageService owners, PreferenceRepository preferences) { this.owners = owners; this.preferences = preferences; }
    private static final List<String> SURVEY_PLACES = List.of("야외", "실내", "둘 다");
    private static final List<String> SURVEY_AREAS = List.of("도시", "시골", "호수·강", "섬", "산·숲", "바다");
    private static final List<TagOption> DEFAULT_TAGS = Stream.concat(List.of(
        new TagOption("interest", "자연"), new TagOption("interest", "역사·문화"), new TagOption("interest", "쇼핑"), new TagOption("interest", "액티비티"),
        new TagOption("food", "한식"), new TagOption("food", "해산물"), new TagOption("food", "채식"), new TagOption("food", "카페·디저트"),
        new TagOption("transport", "대중교통"), new TagOption("transport", "자동차"), new TagOption("transport", "도보"), new TagOption("transport", "자전거"),
        new TagOption("style", "여유로운 휴식"), new TagOption("style", "가성비"), new TagOption("style", "사진 여행"), new TagOption("style", "로컬 체험")
    ).stream(), Stream.concat(
        SURVEY_PLACES.stream().map(name -> new TagOption("style", "장소:" + name)),
        SURVEY_AREAS.stream().map(name -> new TagOption("style", "선호지역:" + name))
    )).toList();
    public Preferences get(Authentication auth, Long expected) {
        long owner = owners.owner(auth, expected);
        return preferences.find(owner, catalog());
    }
    private List<TagOption> catalog() {
        var all = new ArrayList<>(DEFAULT_TAGS);
        for (var option : preferences.tags()) if (!isOldSurveyTag(option) && !all.contains(option)) all.add(option);
        return List.copyOf(all);
    }
    private boolean isOldSurveyTag(TagOption option) {
        return "style".equals(option.type()) && (option.name().startsWith("지역:") || option.name().startsWith("분위기:"));
    }
    @Transactional
    public Preferences save(Authentication auth, Save request) {
        long owner = owners.owner(auth, request.expectedMemberId());
        if (Boolean.TRUE.equals(request.surveyOnly())) {
            validateSurvey(request.pace(), request.tags());
            if (!preferences.lockSurveyOwner(owner)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "여행자 회원만 선호도 조사를 저장할 수 있어요.");
            }
            preferences.saveSurvey(owner, request.pace(), request.tags());
            return preferences.find(owner, catalog());
        }
        var allowed = new HashSet<>(catalog());
        var selected = new HashSet<TagOption>();
        for (var tag : request.tags()) {
            // 일반 저장에 실려 온 조사 응답은 무시하고 기존 조사 결과를 보존한다.
            if (isSurveyTag(tag)) continue;
            var key = new TagOption(tag.type(), tag.name());
            if (!allowed.contains(key) || !selected.add(key)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "태그 목록을 새로 확인해 주세요. 같은 태그를 중복 선택할 수 없습니다.");
            }
        }
        if (!preferences.lockOwner(owner)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요.");
        preferences.save(owner, request);
        return preferences.find(owner, catalog());
    }
    private void validateSurvey(String pace, List<Selection> tags) {
        if (pace == null || !List.of("packed", "normal", "relaxed").contains(pace) || tags == null || tags.size() < 2 || tags.size() > 4) throw invalidSurvey();
        var areas = new HashSet<String>();
        int places = 0;
        for (var tag : tags) {
            if (tag == null || !"style".equals(tag.type()) || !"like".equals(tag.polarity())
                || !Integer.valueOf(1).equals(tag.weight())) throw invalidSurvey();
            if (tag.name() != null && tag.name().startsWith("선호지역:")
                && SURVEY_AREAS.contains(tag.name().substring("선호지역:".length()))) {
                if (!areas.add(tag.name())) throw invalidSurvey();
            } else if (tag.name() != null && tag.name().startsWith("장소:")
                && SURVEY_PLACES.contains(tag.name().substring("장소:".length()))) {
                places++;
            } else throw invalidSurvey();
        }
        if (areas.isEmpty() || areas.size() > 3 || places != 1) throw invalidSurvey();
    }
    private boolean isSurveyTag(Selection tag) {
        return "style".equals(tag.type()) && tag.name() != null
            && (tag.name().startsWith("지역:") || tag.name().startsWith("분위기:")
                || tag.name().startsWith("장소:") || tag.name().startsWith("선호지역:"));
    }
    private ResponseStatusException invalidSurvey() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "여행 스타일과 장소 1개, 선호 지역 1~3개를 골라 주세요. 태그는 좋아요·강도 1이어야 합니다.");
    }
}
