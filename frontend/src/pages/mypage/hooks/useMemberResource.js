import { useEffect, useRef, useState } from 'react';

// 계정이 바뀌면 effect 전에 이전 자료를 가리고, 이전 응답도 폐기한다.
export default function useMemberResource(memberId, load) {
  const [request, setRequest] = useState(0);
  const [state, setState] = useState({ memberId: null, status: 'loading', data: null, error: null });
  const latest = useRef(memberId);
  latest.current = memberId;
  useEffect(() => {
    if (!memberId) return;
    let cancelled = false;
    setState({ memberId, status: 'loading', data: null, error: null });
    load(memberId).then(data => {
      if (cancelled) return;
      if (data.memberId !== memberId) throw new Error('회원 정보가 달라 다시 로그인해야 합니다.');
      setState({ memberId, status: 'ready', data, error: null });
    }).catch(error => { if (!cancelled) setState({ memberId, status: 'error', data: null, error }); });
    return () => { cancelled = true; };
  }, [memberId, load, request]);
  return { ...(state.memberId === memberId ? state : { status: 'loading', data: null, error: null }),
    reload() { setRequest(value => value + 1); },
    accept(data) { if (data.memberId === latest.current) setState({ memberId: data.memberId, status: 'ready', data, error: null }); },
  };
}
