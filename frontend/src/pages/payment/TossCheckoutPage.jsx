import { useEffect, useId, useRef, useState } from 'react';
import { request } from '../../api/auth.js';
import { confirmPayment, loadTossSdk, preparePayment, syncPayment } from './tossPayment.js';
import './toss-checkout.css';

const paymentStatusLabels = {
  READY: '결제 대기',
  IN_PROGRESS: '결제 결과 확인 중',
  WAITING_FOR_DEPOSIT: '입금 대기',
  DONE: '승인 완료',
  ABORTED: '결제 실패',
  EXPIRED: '결제 만료',
  CANCELED: '결제 취소',
  PARTIAL_CANCELED: '부분 취소',
};

export default function TossCheckoutPage({ reservationId, result, onConfirmed, embedded = false, visible = true }) {
  const id = useId();
  const widgetId = id.replace(/[^a-zA-Z0-9_-]/g, '');
  const methodsId = `toss-payment-methods-${widgetId}`;
  const agreementId = `toss-payment-agreement-${widgetId}`;
  const [checkout, setCheckout] = useState(null);
  const [error, setError] = useState('');
  const [setupFailed, setSetupFailed] = useState(false);
  const [setupAttempt, setSetupAttempt] = useState(0);
  const [holdExpiresAt, setHoldExpiresAt] = useState(null);
  const [busy, setBusy] = useState(true);
  const widgets = useRef(null);
  const widgetSetup = useRef(null);
  const confirmation = useRef(null);
  const reportedConfirmation = useRef(false);
  const completed = result !== 'failure' && checkout?.status === 'DONE'
    && String(checkout.reservationId) === String(reservationId)
    && ['CONFIRMED', 'COMPLETED'].includes(checkout.reservationStatus);

  useEffect(() => {
    if (completed && !reportedConfirmation.current) {
      reportedConfirmation.current = true;
      onConfirmed?.();
    }
  }, [completed, onConfirmed]);

  useEffect(() => {
    if (!visible) return undefined;
    let active = true;
    const renderedWidgets = [];
    if (result === 'failure') {
      const query = new URLSearchParams(window.location.hash.split('?')[1] || '');
      setError(query.get('message') || '결제 요청이 취소되거나 실패했습니다.');
      setBusy(false);
      return undefined;
    }
    if (result === 'success') {
      const query = new URLSearchParams(window.location.hash.split('?')[1] || '');
      const orderId = query.get('orderId');
      const paymentKey = query.get('paymentKey');
      const amount = Number(query.get('amount'));
      if (!orderId || !paymentKey || !Number.isSafeInteger(amount)) {
        setError('결제 승인에 필요한 정보가 없습니다.'); setBusy(false); return undefined;
      }
      confirmation.current ||= confirmPayment({ orderId, paymentKey, amount });
      confirmation.current
        .then((value) => { if (active) setCheckout(value); })
        .catch((reason) => { if (active) setError(reason.message); })
        .finally(() => { if (active) setBusy(false); });
      return () => { active = false; };
    }
    function destroyWidgets() {
      renderedWidgets.splice(0).forEach(widget => widget?.destroy?.());
    }

    async function rememberWidget(rendering) {
      const widget = await rendering;
      if (active) renderedWidgets.push(widget);
      else widget?.destroy?.();
    }

    async function initialize(previousSetup) {
      // Toss allows only one payment-method widget and one agreement widget on the page.
      await previousSetup;
      if (!active) return;
      const [value, reservation] = await Promise.all([
        preparePayment(reservationId), request('/api/reservations/' + reservationId),
      ]);
      if (!active) return;
      setCheckout(value);
      setHoldExpiresAt(reservation.holdExpiresAt);
      if (value.status !== 'READY') return;

      const TossPayments = await loadTossSdk();
      if (!active) return;
      const instance = TossPayments(value.clientKey).widgets({ customerKey: 'ANONYMOUS' });
      await instance.setAmount({ currency: 'KRW', value: value.amount });
      if (!active) return;

      await rememberWidget(instance.renderPaymentMethods({ selector: `#${methodsId}` }));
      if (!active) return;
      await rememberWidget(instance.renderAgreement({ selector: `#${agreementId}` }));
      if (active) widgets.current = instance;
    }

    setBusy(true);
    setError('');
    setSetupFailed(false);
    widgetSetup.current = initialize(widgetSetup.current)
      .catch(reason => {
        destroyWidgets();
        if (!active) return;
        setError(reason?.message || '토스 결제 정보를 불러오지 못했습니다.');
        setSetupFailed(true);
      })
      .finally(() => { if (active) setBusy(false); });
    return () => {
      active = false;
      widgets.current = null;
      destroyWidgets();
    };
  }, [reservationId, result, visible, methodsId, agreementId, setupAttempt]);

  async function pay() {
    if (!widgets.current || !checkout) return;
    setBusy(true); setError('');
    try {
      const base = `${window.location.origin}${window.location.pathname}`;
      await widgets.current.requestPayment({
        orderId: checkout.orderId,
        orderName: checkout.orderName,
        ...(embedded ? { windowTarget: 'self' } : {}),
        successUrl: `${base}?stayReservationId=${reservationId}&stayPaymentResult=success`,
        failUrl: `${base}?stayReservationId=${reservationId}&stayPaymentResult=failure`,
      });
    } catch (reason) { setError(reason.message || '결제창을 열지 못했습니다.'); setBusy(false); }
  }

  async function refresh() {
    setBusy(true); setError('');
    try { setCheckout(await syncPayment(reservationId)); }
    catch (reason) { setError(reason.message); }
    finally { setBusy(false); }
  }

  const Heading = embedded ? 'h3' : 'h1';
  return <section className={embedded ? 'toss-checkout toss-checkout--embedded' : 'mypage toss-checkout'} aria-labelledby={id}>
    <Heading id={id}>토스 테스트 결제</Heading>
    <p>테스트 키로만 실행됩니다. 실제 금액은 청구되지 않습니다.</p>
    {!result && holdExpiresAt && <p>결제 기한 {new Date(holdExpiresAt).toLocaleTimeString('ko-KR')}</p>}
    {checkout && <div className="toss-checkout__summary">
      <dl className="toss-checkout__details">
        <dt>주문번호</dt>
        <dd>{checkout.orderId}</dd>
        <dt>결제금액</dt>
        <dd><strong className="toss-checkout__amount">{checkout.amount.toLocaleString('ko-KR')}원</strong></dd>
        <dt>결제상태</dt>
        <dd className="toss-checkout__state">
          <span className="toss-checkout__badge" data-status={checkout.status}
            role={['ABORTED', 'EXPIRED'].includes(checkout.status) ? 'alert' : 'status'}>{paymentStatusLabels[checkout.status] || '상태 확인 필요'}</span>
          {checkout.status === 'DONE' && <span>{completed ? '예약 완료' : '예약 확인 필요'}</span>}
        </dd>
      </dl>
      {checkout.status === 'DONE' && checkout.receiptUrl && <a className="toss-checkout__receipt"
        href={checkout.receiptUrl} target="_blank" rel="noreferrer">테스트 영수증 보기 <span aria-hidden="true">↗</span></a>}
    </div>}
    {!result && <><div id={methodsId} /><div id={agreementId} /></>}
    {checkout?.status === 'IN_PROGRESS' && <p>잠시 후 상태를 다시 조회해 주세요.</p>}
    {error && <p role="alert">{error}</p>}
    {setupFailed && <div className="toss-checkout__actions">
      <button type="button" className="toss-checkout__refresh" onClick={() => setSetupAttempt(attempt => attempt + 1)}>결제 정보 다시 불러오기</button>
      {embedded && <a className="toss-checkout__reservation" href={`#/payments/${encodeURIComponent(reservationId)}`}>결제 페이지에서 계속하기</a>}
    </div>}
    {result === 'failure' && <p><a href={'#/payments/' + reservationId}>결제 다시 시도</a></p>}
    {!result && !setupFailed && checkout?.status === 'READY' && <button type="button" className={embedded ? 'booking-pay-button' : undefined}
      disabled={busy || !widgets.current} onClick={pay}>토스로 테스트 결제</button>}
    {(result === 'success' || ['DONE', 'IN_PROGRESS', 'WAITING_FOR_DEPOSIT'].includes(checkout?.status)) && <div className="toss-checkout__actions">
      <button type="button" className="toss-checkout__refresh" disabled={busy} onClick={refresh}>결제 상태 다시 조회</button>
      {completed && <a className="toss-checkout__reservation" href={`#/reservations/${encodeURIComponent(reservationId)}`}>예약 내용으로</a>}
    </div>}
    {busy && <p role="status">결제 정보를 확인하고 있습니다.</p>}
  </section>;
}
