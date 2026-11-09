package com.smarttraveler.mypage.preferences.service;

import com.smarttraveler.mypage.preferences.dto.PreferenceDtos.*;
import com.smarttraveler.mypage.preferences.repository.PreferenceRepository;
import com.smarttraveler.mypage.service.MyPageService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    private static final List<TagOption> DEFAULT_TAGS = List.of(
        new TagOption("interest", "자연"), new TagOption("interest", "역사·문화"), new TagOption("interest", "쇼핑"), new TagOption("interest", "액티비티"),
        new TagOption("food", "한식"), new TagOption("food", "해산물"), new TagOption("food", "채식"), new TagOption("food", "카페·디저트"),
        new TagOption("transport", "대중교통"), new TagOption("transport", "자동차"), new TagOption("transport", "도보"), new TagOption("transport", "자전거"),
        new TagOption("style", "여유로운 휴식"), new TagOption("style", "가성비"), new TagOption("style", "사진 여행"), new TagOption("style", "로컬 체험")
    );
    public Preferences get(Authentication auth, Long expected) {
        long owner = owners.owner(auth, expected);
        return preferences.find(owner, catalog());
    }
    private List<TagOption> catalog() {
        var all = new ArrayList<>(DEFAULT_TAGS);
        for (var option : preferences.tags()) if (!all.contains(option)) all.add(option);
        return List.copyOf(all);
    }
    @Transactional
    public Preferences save(Authentication auth, Save request) {
        long owner = owners.owner(auth, request.expectedMemberId());
        var allowed = new HashSet<>(catalog());
        var selected = new HashSet<TagOption>();
        for (var tag : request.tags()) {
            var key = new TagOption(tag.type(), tag.name());
            if (!allowed.contains(key) || !selected.add(key)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "태그 목록을 새로 확인해 주세요. 같은 태그를 중복 선택할 수 없습니다.");
            }
        }
        if (!preferences.lockOwner(owner)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요.");
        preferences.save(owner, request);
        return preferences.find(owner, catalog());
    }
}
