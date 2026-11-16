import { useEffect, useRef, useState } from 'react';
import { getPushTest, pushSupportMessage, resetExpiredSubscription, sendPushTest } from './browserPush.js';
import './BrowserPushTest.css';

const storageKey = 'smart-traveler-push-test';
function readPending() {
  try {
    const job = JSON.parse(sessionStorage.getItem(storageKey));
    return job?.id && Date.parse(job.scheduledAt) > Date.now() - 600000 ? job : null;
  } catch { return null; }
}
function savePending(job) {
  try {
    if (job) sessionStorage.setItem(storageKey, JSON.stringify(job));
    else sessionStorage.removeItem(storageKey);
  } catch { /* Notifications still work when browser storage is disabled. */ }
}

export default function BrowserPushTest() {
  const [busy, setBusy] = useState(null);
  const [notice, setNotice] = useState('');
  const [failed, setFailed] = useState(false);
  const [pending, setPending] = useState(readPending);
  const [remaining, setRemaining] = useState(0);
  const submitting = useRef(false);
  const unsupported = pushSupportMessage();

  useEffect(() => {
    if (!pending) return undefined;
    let disposed = false;
    let pollTimer;
    const tick = () => setRemaining(Math.max(0, Math.ceil((Date.parse(pending.scheduledAt) - Date.now()) / 1000)));
    const poll = async () => {
      try {
        const result = await getPushTest(pending.id);
        if (disposed) return;
        if (result.status === 'SCHEDULED') {
          pollTimer = setTimeout(poll, 3000);
          return;
        }
        setNotice(result.message);
        setFailed(result.status !== 'SENT');
        setPending(null);
        savePending(null);
        if (result.status === 'EXPIRED') resetExpiredSubscription().catch(() => {});
      } catch (error) {
        if (disposed) return;
        setNotice(`예약 상태를 확인하지 못했어요. ${error.message}`);
        setFailed(true);
        if (error.status === 404 || Date.now() > Date.parse(pending.scheduledAt) + 120000) {
          setPending(null);
          savePending(null);
        } else pollTimer = setTimeout(poll, 5000);
      }
    };
    tick();
    const countdown = setInterval(tick, 1000);
    pollTimer = setTimeout(poll, Math.max(0, Date.parse(pending.scheduledAt) - Date.now()) + 1000);
    return () => { disposed = true; clearInterval(countdown); clearTimeout(pollTimer); };
  }, [pending]);

  async function test(delaySeconds) {
    if (submitting.current) return;
    submitting.current = true;
    setBusy(delaySeconds);
    setFailed(false);
    setNotice('알림 권한과 연결 상태를 확인하고 있어요…');
    try {
      const result = await sendPushTest(delaySeconds);
      setNotice(result.message);
      setFailed(result.status === 'FAILED' || result.status === 'EXPIRED');
      if (result.status === 'SCHEDULED') {
        savePending(result);
        setRemaining(60);
        setPending(result);
      }
    } catch (error) {
      setFailed(true);
      setNotice(error.message || '알림을 요청하지 못했어요. 잠시 후 다시 시도해주세요.');
    } finally {
      submitting.current = false;
      setBusy(null);
    }
  }

  return <section className="browser-push-test" aria-labelledby="browser-push-title">
    <h2 id="browser-push-title">브라우저 알림 테스트</h2>
    <p>알림을 허용하면 사이트 탭을 닫아도 받을 수 있어요.</p>
    <div className="browser-push-actions">
      <button type="button" disabled={busy !== null || !!unsupported} onClick={() => test(0)}>
        {busy === 0 ? '발송 준비 중…' : '즉시 알림 받기'}
      </button>
      <button type="button" disabled={busy !== null || !!unsupported || !!pending} onClick={() => test(60)}>
        {busy === 60 ? '예약 중…' : pending ? '1분 후 알림 예약됨' : '1분 후 알림 받기'}
      </button>
    </div>
    <p className={`browser-push-status${failed ? ' is-error' : ''}`} role="status" aria-live="polite">
      {unsupported || notice || '버튼을 눌러 브라우저 알림을 테스트해보세요.'}
    </p>
    {pending && <p className="browser-push-countdown">
      {remaining > 0 ? `예약 알림 발송까지 약 ${remaining}초` : '예약 알림의 발송 결과를 확인하고 있어요…'}
    </p>}
    <p className="browser-push-help">브라우저 완전 종료·기기 절전·알림 차단 상태에서는 수신이 지연될 수 있어요. 서버 재시작 시 테스트 예약은 취소돼요.</p>
  </section>;
}
