import { useEffect, useRef, useSyncExternalStore } from 'react';
import { createMyPageSession, emptyData } from '../data/store';
import { myPageApi } from '../api/myPageApi';

export default function useMyPageStore({ member, authChecked, authCheckFailed, authRevision }) {
  const session = useRef(null);
  if (!session.current) session.current = createMyPageSession(myPageApi);
  const store = session.current;
  const state = useSyncExternalStore(store.subscribe, store.getSnapshot, store.getSnapshot);
  useEffect(() => { store.setIdentity(member, authChecked, authCheckFailed); }, [store, member?.memberId, authChecked, authCheckFailed, authRevision]);
  useEffect(() => {
    const leave = () => store.clear();
    const returnToSite = event => { if (event.persisted) store.setIdentity(member, authChecked, authCheckFailed); };
    window.addEventListener('pagehide', leave);
    window.addEventListener('pageshow', returnToSite);
    return () => { window.removeEventListener('pagehide', leave); window.removeEventListener('pageshow', returnToSite); };
  }, [store, member, authChecked, authCheckFailed]);
  const identity = !authChecked ? 'checking' : authCheckFailed ? 'unavailable' : member?.memberId || 'guest';
  // effect가 실행되기 전의 렌더에서도 이전 회원 자료를 가린다.
  const visible = state.identity === identity ? state : { identity, status: 'checking', error: '', ...emptyData(), pendingCount: 0 };
  return { ...visible, member, isGuest: identity === 'guest', addCourse: store.addCourse, saveTrialCourse: store.saveTrialCourse, retry: store.refresh };
}
