# Keycloak SAML-to-OIDC Protocol Bridge

This document outlines the Keycloak configuration required to support the SAML-to-OIDC Protocol Bridge, an architecture that allows `client1-saml` to securely fetch and store OAuth 2.0 `ACCESS_TOKEN` and `REFRESH_TOKEN` cookies immediately after a user logs in via SAML 2.0.

> [!NOTE]
> Historically, applications used the Keycloak Token Exchange REST API for this. However, Keycloak > v21 dropped native support for exchanging internal SAML tokens. This setup instead relies on an industry-standard **Silent Protocol Bridge (OpenID Connect `authorization_code` flow)** using an already-active Keycloak browser session.

---

## Required Keycloak Configuration (`sso-realm-a`)

To properly execute the silent bridge, Keycloak must contain an OIDC client securely matched to the one requesting the bridge on the backend.

### 1. Configure the Bridge Client
Inside your `sso-realm-a` dashboard, navigate to **Clients** and create or modify the backend bridging client (e.g., `broker-client`):

*   **Client ID**: `broker-client`
*   **Protocol / Client Type**: `openid-connect`
*   **Authentication Flow**: Ensure **Standard Flow Enabled** is checked (this allows the `code` redirect flow).
*   **Access Type**: `Confidential` (or ensure "Client authentication" is turned ON), ensuring the client has a valid credentials secret.

### 2. Whitelist the Callback URI
> [!IMPORTANT]
> The single most critical step in this bridge is ensuring the final landing zone for the OIDC code is explicitly allowed in Keycloak's security settings.

1.  In Keycloak, open the `broker-client` settings.
2.  Scroll down to **Valid Redirect URIs**.
3.  Add the exact callback URL configured in your Spring Boot application:
    `http://localhost:8081/oidc-callback`
4.  Click **Save**.

---

## How The Bridge Works

Once the Keycloak client is properly configured, the Spring Boot application executes the following sequence:

1.  **SAML Login:** User completes a standard SAML 2.0 login.
2.  **SAML Success Handler:** `TokenExchangeSuccessHandler` intercepts the success, but instead of completing the request, it issues a 302 Redirect to Keycloak's OIDC Auth Endpoint requesting `response_type=code`.
3.  **Silent SSO Validation:** Because Keycloak just validated the user, their browser holds an active `KEYCLOAK_IDENTITY` SSO session. Keycloak instantly bypassing the login screen and issues an authorization code.
4.  **Callback Exchange:** Keycloak cleanly redirects back to `http://localhost:8081/oidc-callback` with the code.
5.  **Cookie Injection:** The backend `OidcCallbackController` intercepts the code, executes a secure Server-to-Server request to pull the exact Access/Refresh tokens, sets them as `HttpOnly` browser cookies, and drops the user silently onto the dashboard.
