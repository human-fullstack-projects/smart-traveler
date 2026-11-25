import { createContext, useContext, useEffect, useRef, useState } from 'react';
import { setAdminViewer } from '../api/adminReadOnly';
import { getCurrentMember, logout as logoutRequest } from '../api/auth';
import { chatApi } from '../chatApi';

const AuthContext = createContext(null); //전역 공유 저장소 만들기
const AUTH_CHANGE_KEY = 'smart-travel-auth-change';
function notifyAuthChange() {
  try { window.localStorage.setItem(AUTH_CHANGE_KEY, `${Date.now()}:${Math.random()}`); } catch { /* Storage may be disabled. */ }
}

export function AuthProvider({ children }) {
  const [member, setMember] = useState(null);
  const [authChecked, setAuthChecked] = useState(false);
  const [authCheckFailed, setAuthCheckFailed] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const [authRevision, setAuthRevision] = useState(0);
  const [checkRequest, setCheckRequest] = useState(0);
  const [guestCourse, setGuestCourse] = useState(null);
  const [courseTransfer, setCourseTransfer] = useState({ memberId: null, status: 'idle' });
  const [transferRetry, setTransferRetry] = useState(0);
  const authVersion = useRef(0);
  const logoutInFlight = useRef(false);

  useEffect(() => {
    const changed = (event) => { if (event.key === AUTH_CHANGE_KEY) retryAuth(); };
    window.addEventListener('storage', changed);
    return () => window.removeEventListener('storage', changed);
  }, []);

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

  useEffect(() => {
    const updated = () => setTransferRetry(value => value + 1);
    window.addEventListener('smart-travel-guest-course-updated', updated);
    return () => window.removeEventListener('smart-travel-guest-course-updated', updated);
  }, []);

  useEffect(() => {
    if (!authChecked || authCheckFailed) return;
    let cancelled = false;
    const version = authVersion.current;
    const current = () => !cancelled && version === authVersion.current;
    if (!member) {
      setCourseTransfer({ memberId: null, status: 'idle' });
      chatApi.guestCourse().then(result => { if (current()) setGuestCourse(result.course); })
        .catch(() => { if (current()) setGuestCourse(null); });
    } else if (member.roleCode === 'ADMIN') {
      // 관리자 계정은 둘러보기만 해서 비회원 임시 코스를 가져오지 않는다.
      setGuestCourse(null);
      setCourseTransfer({ memberId: member.memberId, status: 'idle' });
    } else {
      const memberId = member.memberId;
      setGuestCourse(null);
      setCourseTransfer({ memberId, status: 'saving' });
      chatApi.claimGuestCourse().then(result => {
        if (!current()) return;
        setCourseTransfer({ memberId, status: result.courseId ? 'saved' : 'idle' });
        if (result.courseId) window.dispatchEvent(new Event('smart-travel-courses-updated'));
      }).catch(() => { if (current()) setCourseTransfer({ memberId, status: 'error' }); });
    }
    return () => { cancelled = true; };
  }, [authChecked, authCheckFailed, member?.memberId, authRevision, transferRetry]);

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
    notifyAuthChange();
  }

  function updateMemberProfile(expectedMemberId, changes) {
    setMember(previous => previous?.memberId === expectedMemberId
      ? { ...previous, ...changes } : previous);
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
      notifyAuthChange();
    } finally {
      logoutInFlight.current = false;
      setLoggingOut(false);
    }
  }

  // AuthContext.Provider-전역 저장소에 연결하는 도구
  //value : 전역 저장소에 저장되는 데이터
  setAdminViewer(member?.roleCode === 'ADMIN');
  return (
    <AuthContext.Provider value={{ member, authChecked, authCheckFailed, loggingOut, authRevision, retryAuth, updateMemberProfile, login, logout,
      guestCourse: !member && authChecked && !authCheckFailed ? guestCourse : null }}>
      {member && courseTransfer.memberId === member.memberId && courseTransfer.status === 'error' && <div className="navigation-notice" role="alert">
        보관한 추천 코스를 이전하지 못했습니다. 코스는 세션에 보관 중입니다. 처음 이전을 시도한 계정으로 다시 시도해 주세요.
        <button type="button" onClick={() => setTransferRetry(value => value + 1)}>다시 저장</button>
      </div>}
      {member && courseTransfer.memberId === member.memberId && courseTransfer.status === 'saved' && <div className="navigation-notice" role="status">
        비회원으로 보관한 추천 코스를 저장했습니다. <a href="#/my-courses">내 코스 보기</a>
        <button type="button" onClick={() => setCourseTransfer({ memberId: member.memberId, status: 'idle' })}>닫기</button>
      </div>}
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
