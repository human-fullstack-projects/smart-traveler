import { useEffect, useRef, useState } from 'react';
import { confirmPayment, loadTossSdk, preparePayment, syncPayment } from './tossPayment.js';

export default function TossCheckoutPage({ reservationId, result }) {
  const [checkout, setCheckout] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(true);
  const widgets = useRef(null);
  const resultStarted = useRef(false);

  useEffect(() => {
    let active = true;
    if (result === 'failure') {
      const query = new URLSearchParams(window.location.hash.split('?')[1] || '');
      setError(query.get('message') || '결제 요청이 취소되거나 실패했습니다.');
      setBusy(false);
      return undefined;
    }
    if (result === 'success') {
      if (resultStarted.current) return undefined;
      resultStarted.current = true;
      const query = new URLSearchParams(window.location.hash.split('?')[1] || '');
      const orderId = query.get('orderId');
      const paymentKey = query.get('paymentKey');
      const amount = Number(query.get('amount'));
      if (!orderId || !paymentKey || !Number.isSafeInteger(amount)) {
        setError('결제 승인에 필요한 정보가 없습니다.'); setBusy(false); return undefined;
      }
      confirmPayment({ orderId, paymentKey, amount })
        .then((value) => { if (active) setCheckout(value); })
        .catch((reason) => { if (active) setError(reason.message); })
        .finally(() => { if (active) setBusy(false); });
      return () => { active = false; };
    }
    preparePayment(reservationId).then(async (value) => {
      if (!active) return;
      setCheckout(value);
      if (value.status === 'DONE') { setCheckout(value); return; }
      const TossPayments = await loadTossSdk();
      if (!active) return;
      await new Promise((resolve) => window.requestAnimationFrame(resolve));
      const instance = TossPayments(value.clientKey).widgets({ customerKey: 'ANONYMOUS' });
      await instance.setAmount({ currency: 'KRW', value: value.amount });
      await Promise.all([
        instance.renderPaymentMethods({ selector: '#toss-payment-methods' }),
        instance.renderAgreement({ selector: '#toss-payment-agreement' }),
      ]);
      if (active) { widgets.current = instance; setCheckout(value); }
    }).catch((reason) => { if (active) setError(reason.message); })
      .finally(() => { if (active) setBusy(false); });
    return () => { active = false; };
  }, [reservationId, result]);

  async function pay() {
    if (!widgets.current || !checkout) return;
    setBusy(true); setError('');
    try {
      const base = `${window.location.origin}${window.location.pathname}`;
      await widgets.current.requestPayment({
        orderId: checkout.orderId,
        orderName: checkout.orderName,
        successUrl: `${base}#/payments/${reservationId}/success`,
        failUrl: `${base}#/payments/${reservationId}/failure`,
      });
    } catch (reason) { setError(reason.message || '결제창을 열지 못했습니다.'); setBusy(false); }
  }

  async function refresh() {
    setBusy(true); setError('');
    try { setCheckout(await syncPayment(reservationId)); }
    catch (reason) { setError(reason.message); }
    finally { setBusy(false); }
  }

  return <section className="mypage" aria-labelledby="payment-title">
    <p><a href={`#/reservations/${reservationId}`}>← 예약 상세</a></p>
    <h1 id="payment-title">토스 테스트 결제</h1>
    <p>테스트 키로만 실행됩니다. 실제 금액은 청구되지 않습니다.</p>
    {checkout && <p>주문번호 {checkout.orderId} · {checkout.amount.toLocaleString('ko-KR')}원</p>}
    {!result && <><div id="toss-payment-methods" /><div id="toss-payment-agreement" /></>}
    {checkout?.status === 'DONE' && <p role="status">결제 승인 완료 · 예약 {checkout.reservationStatus}{checkout.receiptUrl && <> · <a href={checkout.receiptUrl} target="_blank" rel="noreferrer">테스트 영수증</a></>}</p>}
    {checkout?.status === 'IN_PROGRESS' && <p role="status">결제 결과를 확인 중입니다. 잠시 후 상태를 다시 조회해 주세요.</p>}
    {error && <p role="alert">{error}</p>}
    {!result && checkout?.status === 'READY' && <button type="button" disabled={busy || !widgets.current} onClick={pay}>토스로 테스트 결제</button>}
    {(result || checkout?.status === 'IN_PROGRESS') && <button type="button" disabled={busy} onClick={refresh}>결제 상태 다시 조회</button>}
    {busy && <p role="status">결제 정보를 확인하고 있습니다.</p>}
  </section>;
}
