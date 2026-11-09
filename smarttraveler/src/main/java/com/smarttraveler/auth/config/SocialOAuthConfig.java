package com.smarttraveler.auth.config;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@Configuration
public class SocialOAuthConfig {
    private static final Logger log = LoggerFactory.getLogger(SocialOAuthConfig.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    @Bean
    SocialProviders socialProviders(
            @Value("${GOOGLE_CLIENT_ID:}") String googleId,
            @Value("${GOOGLE_CLIENT_SECRET:}") String googleSecret,
            @Value("${KAKAO_CLIENT_ID:}") String kakaoId,
            @Value("${KAKAO_CLIENT_SECRET:}") String kakaoSecret) {
        String profile = System.getenv("USERPROFILE");
        Path userProfile = Path.of(profile == null || profile.isBlank()
                ? System.getProperty("user.home") : profile);
        return resolveProviders(googleId, googleSecret, kakaoId, kakaoSecret,
                Path.of(System.getProperty("user.dir")), userProfile);
    }

    public static SocialProviders resolveProviders(String googleId, String googleSecret,
            String kakaoId, String kakaoSecret, Path workingDirectory, Path userProfile) {
        Selected google = select("google", googleId, googleSecret, workingDirectory, userProfile);
        Selected kakao = select("kakao", kakaoId, kakaoSecret, workingDirectory, userProfile);
        log.info("소셜 로그인: google={}, kakao={}", google.source(), kakao.source());
        return new SocialProviders(google.id(), google.secret(), kakao.id(), kakao.secret());
    }

    private static Selected select(String provider, String envId, String envSecret,
            Path workingDirectory, Path userProfile) {
        if (present(envId) && present(envSecret)) return new Selected(envId, envSecret, "env");
        String filename = provider + ".json";
        Path[] paths = {workingDirectory.resolve("secrets").resolve(filename),
                workingDirectory.resolve("..").resolve("secrets").resolve(filename),
                userProfile.resolve(".smart_travel_oauth").resolve(filename)};
        Path problemPath = null;
        String problem = null;
        for (int i = 0; i < paths.length; i++) {
            Path path = paths[i].toAbsolutePath().normalize();
            if (!Files.exists(path)) continue;
            try {
                JsonNode root = JSON.readTree(path.toFile());
                JsonNode values = root;
                if (provider.equals("google")) {
                    values = root == null ? null : root.get("web");
                    if (values == null && root != null) values = root.get("installed");
                }
                String id = text(values, provider.equals("google") ? "client_id" : "rest_api_key");
                String secret = text(values, "client_secret");
                if (present(id) && present(secret)) {
                    if (problemPath != null) log.warn("소셜 로그인 설정 파일 {}: {}", problemPath, problem);
                    return new Selected(id, secret, i < 2 ? "secrets 폴더" : "사용자 폴더");
                }
                if (problemPath == null) {
                    problemPath = path;
                    problem = "필수 ID 또는 비밀값이 비어 있음";
                }
            } catch (JacksonException | SecurityException exception) {
                if (problemPath == null) {
                    problemPath = path;
                    problem = "JSON을 읽거나 해석할 수 없음";
                }
            }
        }
        if (problemPath == null) {
            problemPath = paths[0].toAbsolutePath().normalize();
            problem = "설정 파일 없음";
        }
        log.warn("소셜 로그인 설정 파일 {}: {}", problemPath, problem);
        return new Selected("", "", "없음");
    }

    private static String text(JsonNode values, String field) {
        JsonNode value = values == null ? null : values.get(field);
        return value != null && value.isTextual() ? value.asText() : "";
    }

    private static boolean present(String value) { return value != null && !value.isBlank(); }

    private record Selected(String id, String secret, String source) { }

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(SocialProviders providers) {
        List<ClientRegistration> registrations = new ArrayList<>();
        if (providers.google()) registrations.add(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(providers.googleId()).clientSecret(providers.googleSecret())
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email").build());
        if (providers.kakao()) registrations.add(ClientRegistration.withRegistrationId("kakao")
                .clientId(providers.kakaoId()).clientSecret(providers.kakaoSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("profile_nickname")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .clientName("Kakao").build());
        return new AvailableRegistrations(registrations);
    }

    public record SocialProviders(String googleId, String googleSecret, String kakaoId, String kakaoSecret) {
        public boolean google() { return present(googleId) && present(googleSecret); }
        public boolean kakao() { return present(kakaoId) && present(kakaoSecret); }
        public boolean any() { return google() || kakao(); }
    }

    private record AvailableRegistrations(List<ClientRegistration> registrations)
            implements ClientRegistrationRepository, Iterable<ClientRegistration> {
        @Override public ClientRegistration findByRegistrationId(String id) {
            return registrations.stream().filter(registration -> registration.getRegistrationId().equals(id))
                    .findFirst().orElse(null);
        }
        @Override public Iterator<ClientRegistration> iterator() { return registrations.iterator(); }
    }
}
