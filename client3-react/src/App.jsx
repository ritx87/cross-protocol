import React, { useState, useEffect, useRef } from 'react';
import keycloak from './keycloak';
import { jwtDecode } from 'jwt-decode';
import './index.css';

function App() {
  const [authenticated, setAuthenticated] = useState(null);
  const [userProfile, setUserProfile] = useState(null);
  const [tokenDecoded, setTokenDecoded] = useState(null);
  const [tokenStatus, setTokenStatus] = useState('');
  const isRun = useRef(false);

  useEffect(() => {
    if (isRun.current) return;
    isRun.current = true;

    keycloak.init({ onLoad: 'check-sso', checkLoginIframe: false })
      .then(auth => {
        setAuthenticated(auth);
        if (auth) {
          keycloak.loadUserProfile().then(profile => {
            setUserProfile(profile);
          });
          setTokenDecoded(jwtDecode(keycloak.token));
        }
      })
      .catch(err => {
        console.error("Failed to initialize Keycloak", err);
        setAuthenticated(false);
      });
  }, []);

  const handleValidation = () => {
    if (!keycloak.tokenParsed) return;
    
    // In Keycloak JS, updateToken(minValidity) returns a refreshed token if it expires in minValidity seconds
    // To explicitly test expiration and force a refresh, we can check if it's expired first
    const isExpired = keycloak.isTokenExpired();
    
    if (isExpired || keycloak.tokenParsed.exp * 1000 < Date.now() + 30000) {
       // Force refresh by passing -1
       keycloak.updateToken(-1).then((refreshed) => {
         if (refreshed) {
           setTokenStatus('The token was expired so created a new token');
           setTokenDecoded(jwtDecode(keycloak.token));
         } else {
           setTokenStatus('Token refresh failed');
         }
       }).catch(() => {
         setTokenStatus('Failure refreshing token. Please login again.');
       });
    } else {
       setTokenStatus('Token is valid');
    }
  };

  const handleLogout = () => {
    keycloak.logout();
  };

  if (authenticated === null) {
    return <div className="loading">Loading Authentication State...</div>;
  }

  if (!authenticated) {
    return (
      <div className="container centered">
        <div className="glass-card error-card">
          <h1>Access Denied</h1>
          <p>Direct access to Client 3 is strictly forbidden for unauthenticated users.</p>
          <p>You must authenticate via Client 1 first!</p>
          <a href="http://localhost:8081" className="btn btn-primary">Go to Client 1</a>
        </div>
      </div>
    );
  }

  return (
    <div className="container centered">
      <div className="glass-card">
        <header className="header">
          <h1>Client 3 (React OIDC)</h1>
          <button onClick={handleLogout} className="btn btn-danger">Logout</button>
        </header>
        
        <div className="content">
          <p className="subtitle">Authenticated seamlessly via Keycloak OIDC without typing credentials!</p>
          
          <div className="user-info">
            <p><strong>Name:</strong> {userProfile?.firstName} {userProfile?.lastName}</p>
            <p><strong>Email:</strong> {userProfile?.email}</p>
            <p className="security-notice">
               The access token is securely managed directly in the browser's memory via <code>keycloak-js</code> instead of backend cookies!
            </p>
          </div>

          <div className="actions">
            <button onClick={handleValidation} className="btn btn-primary">Validate & Refresh Token</button>
            {tokenStatus && (
              <div className={`status-message ${tokenStatus.includes('expired') ? 'warning' : 'success'}`}>
                {tokenStatus}
              </div>
            )}
          </div>

          <div className="claims-container">
            <h3>OIDC Profile Claims:</h3>
            <pre className="claims-json">
              {JSON.stringify(tokenDecoded, null, 2)}
            </pre>
          </div>
        </div>
      </div>
    </div>
  );
}

export default App;
