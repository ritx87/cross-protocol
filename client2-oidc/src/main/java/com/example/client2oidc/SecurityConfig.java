package com.example.client2oidc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

@Configuration
public class SecurityConfig {

    private final TokenCookieSuccessHandler tokenCookieSuccessHandler;

    public SecurityConfig(TokenCookieSuccessHandler tokenCookieSuccessHandler) {
        this.tokenCookieSuccessHandler = tokenCookieSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, org.springframework.security.oauth2.client.registration.ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/unauthorized", "/login/**", "/error").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.sendRedirect("/unauthorized");
                })
            )
            .oauth2Login(oauth2 -> {
                org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver resolver =
                    new org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository, "/oauth2/authorization");
                resolver.setAuthorizationRequestCustomizer(customizer -> 
                    customizer.additionalParameters(params -> params.put("kc_idp_hint", "keycloak-oidc")));
                
                oauth2.authorizationEndpoint(auth -> auth.authorizationRequestResolver(resolver))
                      .successHandler(tokenCookieSuccessHandler);
            });
        return http.build();
    }

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {

        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .authorizationCode()
                        .refreshToken()
                        .build();

        DefaultOAuth2AuthorizedClientManager authorizedClientManager =
                new DefaultOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientRepository);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }
}
