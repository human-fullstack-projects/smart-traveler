import { useEffect, useRef } from 'react';
import './guest-ai-notice.css';

export default function GuestAiNotice({ onClose }) {
  const dialog = useRef(null);
  useEffect(() => {
    const element = dialog.current;
    element.showModal();
    return () => element.close();
  }, []);
  return <dialog ref={dialog} className="guest-ai-notice" aria-labelledby="guest-ai-title" onCancel={(event) => { event.preventDefault(); onClose(); }}>
      <h2 id="guest-ai-title">계속 이용하려면 로그인해 주세요.</h2>
      <p>비회원 AI 대화 생성 1회를 모두 사용했습니다. 회원가입 후 로그인하거나 기존 계정으로 로그인해 새 여행을 만들어 보세요.</p>
      <div className="actions">
        <a className="primary" href="#/signup" onClick={onClose} autoFocus>회원가입</a>
        <a className="secondary" href="#/login" onClick={onClose}>로그인</a>
        <button type="button" onClick={onClose}>닫기</button>
      </div>
  </dialog>;
}
