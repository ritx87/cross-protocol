package com.example.client2oidc;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Instant;
import java.util.Map;

@Controller
public class Client2Controller {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public Client2Controller(OAuth2AuthorizedClientService authorizedClientService, OAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientService = authorizedClientService;
        this.authorizedClientManager = authorizedClientManager;
    }

    @GetMapping("/")
    public String index(@AuthenticationPrincipal OidcUser principal, Model model) {
        if (principal != null) {
            model.addAttribute("userName", principal.getFullName());
            model.addAttribute("email", principal.getEmail());
            model.addAttribute("claims", principal.getClaims());
        }
        return "index";
    }

    @GetMapping("/api/validate")
    @ResponseBody
    public ResponseEntity<?> validateToken(Authentication authentication, HttpServletResponse response) {
        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Not authenticated via OAuth2"));
        }

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(), oauthToken.getName());

        if (client == null || client.getAccessToken() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No access token found"));
        }

        Instant expiresAt = client.getAccessToken().getExpiresAt();
        
        if (expiresAt != null && expiresAt.isBefore(Instant.now().plusSeconds(30))) {
            
            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(oauthToken.getAuthorizedClientRegistrationId())
                    .principal(authentication)
                    .build();
            
            OAuth2AuthorizedClient refreshedClient = this.authorizedClientManager.authorize(authorizeRequest);
            
            if (refreshedClient != null) {
                Cookie accessTokenCookie = new Cookie("access_token", refreshedClient.getAccessToken().getTokenValue());
                accessTokenCookie.setHttpOnly(true);
                accessTokenCookie.setSecure(false);
                accessTokenCookie.setPath("/");
                accessTokenCookie.setMaxAge(3600);
                response.addCookie(accessTokenCookie);

                if (refreshedClient.getRefreshToken() != null) {
                    Cookie refreshTokenCookie = new Cookie("refresh_token", refreshedClient.getRefreshToken().getTokenValue());
                    refreshTokenCookie.setHttpOnly(true);
                    refreshTokenCookie.setSecure(false);
                    refreshTokenCookie.setPath("/");
                    refreshTokenCookie.setMaxAge(3600);
                    response.addCookie(refreshTokenCookie);
                }
                
                return ResponseEntity.ok(Map.of("message", "The token was expired so created a new token"));
            } else {
                return ResponseEntity.status(401).body(Map.of("message", "Refresh failed, please login again"));
            }
        }
        
        return ResponseEntity.ok(Map.of("message", "Token is valid"));
    }
}
