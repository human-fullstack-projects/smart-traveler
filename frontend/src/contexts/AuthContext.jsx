import { createContext, useContext, useEffect, useRef, useState } from 'react';
import { getCurrentMember, logout as logoutRequest } from '../api/auth';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [member, setMember] = useState(null);
  const [authChecked, setAuthChecked] = useState(false);
  const [authCheckFailed, setAuthCheckFailed] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const authVersion = useRef(0);
  const logoutInFlight = useRef(false);

  useEffect(() => {
    let cancelled = false;
    const version = authVersion.current;
    getCurrentMember()
      .then((value) => {
        if (cancelled || version !== authVersion.current) return;
        setMember(value);
        setAuthChecked(true);
      })
      .catch(() => {
        if (cancelled) return;
        setAuthChecked(true);
        if (version === authVersion.current) setAuthCheckFailed(true);
      });
    return () => { cancelled = true; };
  }, []);

  function login(value) {
    authVersion.current += 1;
    setMember(value);
    setAuthChecked(true);
    setAuthCheckFailed(false);
  }

  async function logout() {
    if (logoutInFlight.current) return;
    logoutInFlight.current = true;
    setLoggingOut(true);
    authVersion.current += 1;
    try {
      await logoutRequest();
      setMember(null);
    } finally {
      logoutInFlight.current = false;
      setLoggingOut(false);
    }
  }

  return (
    <AuthContext.Provider value={{ member, authChecked, authCheckFailed, loggingOut, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}
