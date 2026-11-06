import { createContext, useContext, useEffect, useRef, useState } from 'react';
import { getCurrentMember, logout as logoutRequest } from '../api/auth';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [member, setMember] = useState(null);
  const [authChecked, setAuthChecked] = useState(false);
  const [authCheckFailed, setAuthCheckFailed] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const [authRevision, setAuthRevision] = useState(0);
  const [checkRequest, setCheckRequest] = useState(0);
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
        setAuthCheckFailed(false);
      })
      .catch(() => {
        if (cancelled || version !== authVersion.current) return;
        setAuthChecked(true);
        setAuthCheckFailed(true);
      });
    return () => { cancelled = true; };
  }, [checkRequest]);

  function retryAuth() {
    authVersion.current += 1;
    setAuthChecked(false);
    setAuthCheckFailed(false);
    setCheckRequest(value => value + 1);
  }

  function login(value) {
    authVersion.current += 1;
    setAuthRevision(version => version + 1);
    setMember(value);
    setAuthChecked(true);
    setAuthCheckFailed(false);
  }

  function updateMemberProfile(expectedMemberId, changes) {
    setMember(previous => previous?.memberId === expectedMemberId
      ? { ...previous, nickname: changes.nickname } : previous);
  }

  async function logout() {
    if (logoutInFlight.current) return;
    logoutInFlight.current = true;
    setLoggingOut(true);
    authVersion.current += 1;
    try {
      await logoutRequest();
      setMember(null);
      setAuthRevision(version => version + 1);
    } finally {
      logoutInFlight.current = false;
      setLoggingOut(false);
    }
  }

  return (
    <AuthContext.Provider value={{ member, authChecked, authCheckFailed, loggingOut, authRevision, retryAuth, updateMemberProfile, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}
