import { useEffect, useRef } from 'react';
import './owner-action-notice.css';

export default function OwnerActionNotice({ action = '예약' }) {
  const dialog = useRef(null);
  const goHome = () => { window.location.hash = '/main'; };

  useEffect(() => {
    dialog.current.showModal();
    return () => dialog.current?.close();
  }, []);

  return <dialog ref={dialog} className="owner-action-dialog" aria-labelledby="owner-action-title"
    aria-describedby="owner-action-description" onCancel={event => { event.preventDefault(); goHome(); }}>
    <h2 id="owner-action-title">일반 사용자 계정으로 이용해주세요</h2>
    <p id="owner-action-description">숙박업주 계정으로는 {action}을 진행할 수 없어요.<br />일반 사용자 계정으로 다시 로그인해주세요.</p>
    <button type="button" autoFocus onClick={goHome}>확인 · 홈으로 이동</button>
  </dialog>;
}
