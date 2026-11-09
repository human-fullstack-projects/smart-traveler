import { useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';

export default function TermsModal({ term, title, onAgree, onClose, returnFocus }) {
  const dialogRef = useRef(null);
  const closeRef = useRef(onClose);
  closeRef.current = onClose;

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    dialogRef.current?.focus();

    function onKeyDown(event) {
      if (event.key === 'Escape') { event.preventDefault(); closeRef.current(); return; }
      if (event.key !== 'Tab') return;
      const focusable = [...dialogRef.current.querySelectorAll('button:not(:disabled), [href], input:not(:disabled), [tabindex]:not([tabindex="-1"])')];
      if (!focusable.length) { event.preventDefault(); return; }
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && (document.activeElement === first || document.activeElement === dialogRef.current)) {
        event.preventDefault(); last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault(); first.focus();
      }
    }
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('keydown', onKeyDown);
      document.body.style.overflow = previousOverflow;
      returnFocus?.focus();
    };
  }, [returnFocus]);

  return createPortal(
    <div className="auth-modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
      <section className="auth-modal" ref={dialogRef} role="dialog" aria-modal="true" aria-labelledby="auth-modal-title" tabIndex={-1}>
        <div className="auth-modal-head">
          <h2 id="auth-modal-title">{title} <span className={term.required ? 'auth-req' : 'st-note'}>{term.required ? '필수' : '선택'}</span></h2>
          <p className="st-note">버전 {term.version}</p>
        </div>
        <div className="auth-modal-body" tabIndex={0}>{term.content || '약관 내용이 없어요.'}</div>
        <div className="auth-modal-actions">
          <button type="button" className="st-btn" onClick={onAgree}>동의하고 닫기</button>
          <button type="button" className="st-btn st-btn-line" onClick={onClose}>닫기</button>
        </div>
      </section>
    </div>, document.body,
  );
}
