import { useEffect, useRef } from 'react';
import './owner-action-notice.css';

export default function OwnerActionNotice({ action = '예약', onClose }) {
  const dialog = useRef(null);
  const goHome = () => { window.location.hash = '/main'; };
  const finish = onClose || goHome;

  useEffect(() => {
    const element = dialog.current;
    const trigger = document.activeElement;
    element.showModal();
    return () => {
      element.close();
      if (trigger instanceof HTMLElement && trigger.isConnected) trigger.focus();
    };
  }, []);

  return <dialog ref={dialog} className="owner-action-dialog" aria-labelledby="owner-action-title"
    aria-describedby="owner-action-description" onCancel={event => { event.preventDefault(); finish(); }}>
    <h2 id="owner-action-title">일반 사용자 계정으로 이용해주세요</h2>
    <p id="owner-action-description">숙박업주 계정에서는 {action} 기능을 이용할 수 없어요.<br />일반 사용자 계정으로 다시 로그인해주세요.</p>
    <button type="button" autoFocus onClick={finish}>{onClose ? '확인' : '확인 · 홈으로 이동'}</button>
  </dialog>;
}
