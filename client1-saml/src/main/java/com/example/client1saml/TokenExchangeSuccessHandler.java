package com.example.client1saml;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TokenExchangeSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(TokenExchangeSuccessHandler.class);

    @Value("${custom.token-exchange.client-id}")
    private String clientId;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {

        log.info("SAML Authentication successful! Bridging over to OIDC...");

        // Construct the Authorization Code flow URL to silently get OIDC tokens
        // Keycloak will seamlessly log the user in via their active SAML SSO session
        String authUrl = "http://localhost:8080/realms/sso-realm-a/protocol/openid-connect/auth" +
                "?client_id=" + clientId +
                "&response_type=code" +
                "&redirect_uri=http://localhost:8081/oidc-callback" +
                "&scope=openid";

        response.sendRedirect(authUrl);
    }
}
