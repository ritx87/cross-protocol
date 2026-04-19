# Keycloak Cross-Protocol SSO Configuration Guide

This guide provides step-by-step instructions for configuring two Keycloak realms (`sso-realm-a` and `sso-realm-b`) to achieve cross-protocol Single Sign-On (SSO) between a SAML client and an OIDC client using keycloak Identity Brokering.

## Overview
- **Keycloak Instance 1 (Port 8080)**: Hosts `sso-realm-a`. It acts as the primary Identity Provider (IdP) containing the users. It has the SAML Client (`client1-saml`) and an OIDC Client (`broker-client`) used for identity brokering.
- **Keycloak Instance 2 (Port 8090)**: Hosts `sso-realm-b`. It acts as a Service Provider leveraging `sso-realm-a` for authentication. It contains the OIDC Client (`client2-oidc`) and an Identity Provider configuration that links back to `sso-realm-a` via the `broker-client`.

---

## Part 1: Configuring Realm A (`sso-realm-a`) on Keycloak 1 (8080)

### 1. Create the Realm
1. Log in to the Keycloak Admin Console (`http://localhost:8080/admin`).
2. Hover over the top-left realm dropdown and click **Create Realm**.
3. Set **Realm name** to `sso-realm-a`.
4. Click **Create**.

### 2. Create the User
1. In the left navigation pane under **Manage**, click **Users**.
2. Click **Add user**.
3. Set **Username** to `user` (Email: `user@example.com`, First Name: `John`, Last Name: `Doe`).
4. Click **Create**.
5. Go to the **Credentials** tab for the user.
6. Click **Set Password** and set it to `password` (turn off "Temporary").
7. Click **Save**.

### 3. Configure SAML Client (`client1-saml`)
1. In the left navigation pane, click **Clients**.
2. Click **Create client**.
3. **General Settings**:
   - Client type: `SAML`
   - Client ID: `client1-saml`
   - Name: Client 1 SAML
   - Click **Next**.
4. **Login Settings**:
   - Valid Redirect URIs: `http://localhost:8081/login/saml2/sso/keycloak`
   - Click **Save**.
5. Once saved, scroll down to **Advanced Settings** -> **SAML capabilities**:
   - Ensure the following are set/toggled based on your requirements (e.g., turn off client signatures if not using mutual TLS/certs in local env).
   - Expected SAML Attributes:
     - `saml.authnstatement`: `true`
     - `saml.server.signature`: `false`
     - `saml.assertion.signature`: `false`
     - `saml.client.signature`: `false`
     - `saml.force.post.binding`: `true`
     - `saml_name_id_format`: `username`

### 4. Configure Broker Client (`broker-client`)
This client is used by Realm B to connect to Realm A.
1. In the left navigation pane, click **Clients**.
2. Click **Create client**.
3. **General Settings**:
   - Client type: `OpenID Connect`
   - Client ID: `broker-client`
   - Click **Next**.
4. **Capability Config**:
   - Client authentication: **ON** (To enable client secret verification)
   - Standard flow: **ON** (Enabled by default)
   - Click **Next**.
5. **Login Settings**:
   - Valid Redirect URIs: `http://localhost:8090/realms/sso-realm-b/broker/keycloak-oidc/endpoint/*`
   - Click **Save**.
6. Navigate to the **Credentials** tab of the saved client to obtain or set the client secret. 
   - Ensure the secret is `broker-secret` (or copy the generated one to use in Realm B).

---

## Part 2: Configuring Realm B (`sso-realm-b`) on Keycloak 2 (8090)

### 1. Create the Realm
1. Log in to the Keycloak Admin Console (`http://localhost:8090/admin`).
2. Hover over the top-left realm dropdown and click **Create Realm**.
3. Set **Realm name** to `sso-realm-b`.
4. Click **Create**.

### 2. Configure the OIDC Client (`client2-oidc`)
1. In the left navigation pane, click **Clients**.
2. Click **Create client**.
3. **General Settings**:
   - Client type: `OpenID Connect`
   - Client ID: `client2-oidc`
   - Click **Next**.
4. **Capability Config**:
   - Client authentication: **ON** (Private client / Non-public client)
   - Standard flow: **ON** 
   - Click **Next**.
5. **Login Settings**:
   - Valid Redirect URIs: `http://localhost:8082/login/oauth2/code/keycloak`
   - Click **Save**.
6. Navigate to the **Credentials** tab and configure your secret.
   - For consistency with your code: Set or use `GzW8o3kHqH48e9a2XU`.

### 3. Configure the Identity Provider (IdP Brokering)
By setting this up, Keycloak 2 will redirect unauthorized users to Keycloak 1 to authenticate.

1. In the left navigation pane, click **Identity Providers**.
2. Select **Keycloak OpenID Connect** from the list of providers.
3. Fill in the Alias and Display Name:
   - Alias: `keycloak-oidc`
   - Display Name: `Keycloak A`
4. Scroll down to **OpenID Connect Settings** and configure the endpoints pointing to `sso-realm-a`:
   - **Authorization URL**: `http://localhost:8080/realms/sso-realm-a/protocol/openid-connect/auth`
   - **Token URL**: `http://localhost:8080/realms/sso-realm-a/protocol/openid-connect/token`
   - **Logout URL**: `http://localhost:8080/realms/sso-realm-a/protocol/openid-connect/logout`
   - **UserInfo URL**: `http://localhost:8080/realms/sso-realm-a/protocol/openid-connect/userinfo`
   - **Issuer**: `http://localhost:8080/realms/sso-realm-a`
5. Map the Broker Client credentials:
   - **Client ID**: `broker-client`
   - **Client Secret**: `broker-secret`
   - Client Authentication: `Client secret sent as post` (or standard `basic`)
6. Ensure **Sync Mode** is set to `IMPORT` to sync user profiles accurately from Realm A to Realm B upon login.
7. Advanced settings:
   - Trust Email: `ON`
   - Use JWKS URL: `ON`
8. Click **Save**.

---

## Part 3: Import/Export Alternative (Automated Setup)

Instead of manual setup, you can import the configuration directly on server startup or via the Keycloak UI:

1. **Realm A**: Use the provided `realm-export-a.json`.
   - Admin Console > **Create Realm** > Browse & Upload `realm-export-a.json`.
2. **Realm B**: Use the provided `realm-export-b.json`.
   - Admin Console > **Create Realm** > Browse & Upload `realm-export-b.json`.

> **Note**: Importing is generally faster and ensures precision for complex setups such as SAML toggles or broker endpoints compared to exact UI navigation.
