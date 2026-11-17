import { createContext, useContext, useEffect, useRef, useState } from 'react';
import { getCurrentMember, logout as logoutRequest } from '../api/auth';

const AuthContext = createContext(null); //전역 공유 저장소 만들기

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

  // 페이지 이동·탭 복귀·API 401 때 서버 로그인 상태를 다시 확인한다.
  // (처음 한 번만 확인하면 30분 미사용·서버 재시작·다른 탭 로그아웃 뒤에도 메뉴가 「로그아웃」으로 남음)
  const memberRef = useRef(null);
  memberRef.current = member;
  useEffect(() => {
    if (!authChecked) return undefined;
    let lastCheck = 0;
    function recheck() {
      const now = Date.now();
      if (logoutInFlight.current || now - lastCheck < 2000) return;
      lastCheck = now;
      const version = authVersion.current;
      getCurrentMember()
        .then((value) => {
          if (version !== authVersion.current || logoutInFlight.current) return;
          const before = memberRef.current;
          if ((before?.memberId ?? null) === (value?.memberId ?? null)) return;
          authVersion.current += 1;
          setMember(value);
          setAuthRevision(revision => revision + 1);
        })
        .catch(() => {}); // 서버 연결 실패는 로그인 상태를 바꾸지 않음
    }
    function onVisible() { if (document.visibilityState === 'visible') recheck(); }
    window.addEventListener('hashchange', recheck);
    window.addEventListener('focus', recheck);
    window.addEventListener('auth:unauthorized', recheck);
    document.addEventListener('visibilitychange', onVisible);
    return () => {
      window.removeEventListener('hashchange', recheck);
      window.removeEventListener('focus', recheck);
      window.removeEventListener('auth:unauthorized', recheck);
      document.removeEventListener('visibilitychange', onVisible);
    };
  }, [authChecked]);

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

  // AuthContext.Provider-전역 저장소에 연결하는 도구
  //value : 전역 저장소에 저장되는 데이터
  return (
    <AuthContext.Provider value={{ member, authChecked, authCheckFailed, loggingOut, authRevision, retryAuth, updateMemberProfile, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

//custom hook : AuthContext를 쉽게 사용하기 위한 재사용 모듈 또는 컴포넌트
export function useAuth() {
  const ctx = useContext(AuthContext); // 전역저장소 가져오기
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}
