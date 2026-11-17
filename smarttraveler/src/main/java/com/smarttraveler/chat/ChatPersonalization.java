package com.smarttraveler.chat;

import com.smarttraveler.mypage.preferences.repository.PreferenceRepository;
import com.smarttraveler.mypage.service.MyPageService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolve the current active member on every request; never accept a client member ID. */
@Service
@Transactional(readOnly = true)
public class ChatPersonalization {
    private final MyPageService owners;
    private final PreferenceRepository preferences;
    private static final Map<String, String> LANDSCAPES = Map.of(
        "도시", "city", "시골", "countryside", "호수·강", "water",
        "섬", "island", "산·숲", "forest", "바다", "sea");

    public ChatPersonalization(MyPageService owners, PreferenceRepository preferences) {
        this.owners = owners;
        this.preferences = preferences;
    }

    public String ownerKey() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) return "guest";
        return "member:" + owners.owner(auth, null);
    }

    public Map<String, Object> savedPreferences() {
        String key = ownerKey();
        if ("guest".equals(key)) return Map.of();
        var saved = preferences.find(Long.parseLong(key.substring("member:".length())), List.of());
        if (!saved.saved()) return Map.of();
        Map<String, Object> result = new LinkedHashMap<>();
        if (List.of("relaxed", "normal", "packed").contains(saved.pace())) result.put("pace", saved.pace());
        var liked = saved.tags().stream().filter(t -> "style".equals(t.type()) && "like".equals(t.polarity()))
            .map(t -> t.name()).toList();
        if (!liked.contains("장소:둘 다")) {
            if (liked.contains("장소:실내") && !liked.contains("장소:야외")) result.put("environment", "indoor");
            if (liked.contains("장소:야외") && !liked.contains("장소:실내")) result.put("environment", "outdoor");
        }
        var landscapes = liked.stream().filter(t -> t.startsWith("선호지역:"))
            .map(t -> LANDSCAPES.get(t.substring("선호지역:".length())))
            .filter(java.util.Objects::nonNull).distinct().sorted().toList();
        if (!landscapes.isEmpty()) result.put("landscapes", landscapes);
        return result;
    }

    public Long memberId() {
        String key = ownerKey();
        return "guest".equals(key) ? null : Long.valueOf(key.substring("member:".length()));
    }
}
