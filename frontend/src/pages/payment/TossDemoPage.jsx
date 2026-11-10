import { useEffect, useRef, useState } from 'react';
import { confirmDemoOrder, createDemoOrder, getDemoOrder, loadTossSdk } from './tossPayment.js';
import './toss-demo.css';

const ORDER_NAME = '스마트 트래블 결제 연동 테스트';
const AMOUNT = 1000;

export default function TossDemoPage() {
  const params = new URLSearchParams(window.location.search);
  const result = params.get('tossDemo');
  const [order, setOrder] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(Boolean(result));
  const widgets = useRef(null);

  useEffect(() => {
    if (!result) return undefined;
    let active = true;
    if (result === 'failure') {
      setError(params.get('message') || '결제가 취소되거나 실패했습니다.');
      setBusy(false);
      return undefined;
    }
    const orderId = params.get('orderId');
    const paymentKey = params.get('paymentKey');
    const amount = Number(params.get('amount'));
    if (!orderId || !paymentKey || !Number.isSafeInteger(amount)) {
      setError('토스에서 받은 결제 정보가 부족합니다.');
      setBusy(false);
      return undefined;
    }
    confirmDemoOrder({ orderId, paymentKey, amount })
      .then((value) => { if (active) setOrder(value); })
      .catch((reason) => { if (active) setError(reason.message); })
      .finally(() => { if (active) setBusy(false); });
    return () => { active = false; };
  }, [result]);

  async function start() {
    setBusy(true); setError('');
    try {
      const next = await createDemoOrder(ORDER_NAME, AMOUNT);
      setOrder(next);
      const TossPayments = await loadTossSdk();
      await new Promise((resolve) => window.requestAnimationFrame(resolve));
      const instance = TossPayments(next.clientKey).widgets({ customerKey: 'ANONYMOUS' });
      await instance.setAmount({ currency: 'KRW', value: next.amount });
      await Promise.all([
        instance.renderPaymentMethods({ selector: '#toss-demo-methods' }),
        instance.renderAgreement({ selector: '#toss-demo-agreement' }),
      ]);
      widgets.current = instance;
    } catch (reason) { setOrder(null); widgets.current = null; setError(reason.message || '테스트 주문을 만들지 못했습니다.'); }
    finally { setBusy(false); }
  }

  async function pay() {
    if (!order || !widgets.current) return;
    setBusy(true); setError('');
    try {
      const base = `${window.location.origin}${window.location.pathname}`;
      await widgets.current.requestPayment({
        orderId: order.orderId,
        orderName: order.orderName,
        successUrl: `${base}?tossDemo=success`,
        failUrl: `${base}?tossDemo=failure`,
      });
    } catch (reason) { setError(reason.message || '토스 결제창을 열지 못했습니다.'); setBusy(false); }
  }

  async function refresh() {
    if (!order?.orderId) return;
    setBusy(true); setError('');
    try { setOrder(await getDemoOrder(order.orderId)); }
    catch (reason) { setError(reason.message); }
    finally { setBusy(false); }
  }

  return <section className="toss-demo" aria-labelledby="toss-demo-title">
    <a className="toss-demo__back" href={`${window.location.origin}${window.location.pathname}#/`}>← 메인으로</a>
    <p className="toss-demo__eyebrow">TOSS PAYMENTS · TEST</p>
    <h1 id="toss-demo-title">1,000원 테스트 결제</h1>
    <p>토스 주문서형 결제 동작을 확인하는 임시 화면입니다. 테스트 키를 사용하므로 실제 금액은 청구되지 않습니다.</p>
    <div className="toss-demo__summary"><span>{ORDER_NAME}</span><strong>{AMOUNT.toLocaleString('ko-KR')}원</strong></div>
    {!result && <><div id="toss-demo-methods" /><div id="toss-demo-agreement" /></>}
    {!order && !result && <button type="button" onClick={start} disabled={busy}>테스트 주문 만들기</button>}
    {order?.status === 'READY' && !result && <button type="button" onClick={pay} disabled={busy || !widgets.current}>토스로 테스트 결제</button>}
    {order?.status === 'DONE' && <p role="status" className="toss-demo__success">테스트 결제가 승인됐습니다. 주문번호: {order.orderId}{order.receiptUrl && <> · <a href={order.receiptUrl} target="_blank" rel="noreferrer">영수증 보기</a></>}</p>}
    {order?.status === 'IN_PROGRESS' && <p role="status">승인 결과를 확인 중입니다. 상태를 다시 조회해 주세요.</p>}
    {order?.status === 'IN_PROGRESS' && <button type="button" onClick={refresh} disabled={busy}>결제 상태 다시 조회</button>}
    {error && <p role="alert" className="toss-demo__error">{error}</p>}
    {busy && <p role="status">토스 결제 정보를 확인 중입니다.</p>}
    {result && <a href={`${window.location.origin}${window.location.pathname}#/payments/demo`}>새 테스트 시작</a>}
  </section>;
}
