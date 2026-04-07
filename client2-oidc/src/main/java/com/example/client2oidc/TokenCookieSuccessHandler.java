package com.example.client2oidc;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TokenCookieSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuth2AuthorizedClientService authorizedClientService;

    public TokenCookieSuccessHandler(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
        setDefaultTargetUrl("/");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                    oauthToken.getAuthorizedClientRegistrationId(),
                    oauthToken.getName());

            if (authorizedClient != null) {
                if (authorizedClient.getAccessToken() != null) {
                    Cookie accessTokenCookie = new Cookie("access_token", authorizedClient.getAccessToken().getTokenValue());
                    accessTokenCookie.setHttpOnly(true);
                    accessTokenCookie.setSecure(false);
                    accessTokenCookie.setPath("/");
                    accessTokenCookie.setMaxAge(3600);
                    response.addCookie(accessTokenCookie);
                }

                if (authorizedClient.getRefreshToken() != null) {
                    Cookie refreshTokenCookie = new Cookie("refresh_token", authorizedClient.getRefreshToken().getTokenValue());
                    refreshTokenCookie.setHttpOnly(true);
                    refreshTokenCookie.setSecure(false);
                    refreshTokenCookie.setPath("/");
                    refreshTokenCookie.setMaxAge(3600);
                    response.addCookie(refreshTokenCookie);
                }
            }
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
