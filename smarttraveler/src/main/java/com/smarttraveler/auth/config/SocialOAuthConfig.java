package com.smarttraveler.auth.config;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
    @Bean
    SocialProviders socialProviders(
            @Value("${GOOGLE_CLIENT_ID:}") String googleId,
            @Value("${GOOGLE_CLIENT_SECRET:}") String googleSecret,
            @Value("${KAKAO_CLIENT_ID:}") String kakaoId,
            @Value("${KAKAO_CLIENT_SECRET:}") String kakaoSecret) {
        return new SocialProviders(googleId, googleSecret, kakaoId, kakaoSecret);
    }

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
        private static boolean present(String value) { return value != null && !value.isBlank(); }
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
