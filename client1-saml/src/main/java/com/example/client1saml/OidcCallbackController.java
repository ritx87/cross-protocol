package com.example.client1saml;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Controller
public class OidcCallbackController {

    private static final Logger log = LoggerFactory.getLogger(OidcCallbackController.class);

    @Value("${custom.token-exchange.client-id}")
    private String clientId;

    @Value("${custom.token-exchange.client-secret}")
    private String clientSecret;

    @Value("${custom.token-exchange.token-url}")
    private String tokenUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/oidc-callback")
    public void oidcCallback(@RequestParam("code") String code, HttpServletResponse response) throws IOException {
        log.info("Received OIDC callback with code, exchanging for tokens...");

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "authorization_code");
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("code", code);
        requestBody.add("redirect_uri", "http://localhost:8081/oidc-callback");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<TokenResponse> tokenResponse = restTemplate.postForEntity(tokenUrl, entity, TokenResponse.class);

            if (tokenResponse.getStatusCode() == HttpStatus.OK && tokenResponse.getBody() != null) {
                TokenResponse body = tokenResponse.getBody();

                if (body.getAccessToken() != null) {
                    Cookie accessTokenCookie = new Cookie("ACCESS_TOKEN", body.getAccessToken());
                    accessTokenCookie.setHttpOnly(true);
                    accessTokenCookie.setPath("/");
                    accessTokenCookie.setMaxAge(body.getExpiresIn() != null ? body.getExpiresIn().intValue() : 3600);
                    response.addCookie(accessTokenCookie);
                }

                if (body.getRefreshToken() != null) {
                    Cookie refreshTokenCookie = new Cookie("REFRESH_TOKEN", body.getRefreshToken());
                    refreshTokenCookie.setHttpOnly(true);
                    refreshTokenCookie.setPath("/");
                    refreshTokenCookie.setMaxAge(body.getRefreshExpiresIn() != null ? body.getRefreshExpiresIn().intValue() : 18000);
                    response.addCookie(refreshTokenCookie);
                }
                log.info("Tokens successfully generated and saved to cookies!");
            } else {
                log.error("Failed to exchange code. Status code: {}", tokenResponse.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Exception occurred during token exchange: {}", e.getMessage());
        }

        response.sendRedirect("/");
    }
}
