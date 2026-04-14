import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
  url: 'http://localhost:8080',
  realm: 'sso-realm',
  clientId: 'client3-react'
});

export default keycloak;
