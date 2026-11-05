import Card from '../components/Card';
import '../../../css/mypage.css';
import Button from '../components/Button';
import PageHero from '../components/PageHero';
import OrderSummary from '../components/OrderSummary';
import { previewBookings } from '../data/previewBookings';
import { endDate, money } from '../utils/format';
import { canCancel } from '../data/store';
import { useState } from 'react';

export default function Reservations({ bookings = previewBookings, dispatch }) {
  const [notice, setNotice] = useState('');
  return <div className="mypage">
    <PageHero eyebrow="MY RESERVATIONS" title={<>기다리는 여행과 <em>지난 여행.</em></>}>예약 상태를 확인하고 상세 내역을 살펴보세요.</PageHero>
    {notice && <p role="status">{notice}</p>}
    {bookings.length === 0 && <div className="empty">예약 내역이 없습니다.</div>}
    <div className="stack">{bookings.map((booking) => <Card key={booking.id}>
      <div className="row"><div><span className="pill">{booking.status}</span><h2>{booking.stay}</h2><p className="muted">{booking.date} ~ {endDate(booking.date, booking.nights)} · {booking.guests}명</p></div><strong>{money(booking.price)}</strong></div>
      <details><summary>예약 상세</summary><OrderSummary booking={booking} /><p>{booking.status === '취소' ? '연결 코스 비활성화 · 작성 자격 해제 · 예정 알림 취소' : booking.status === '환불실패' ? '환불 실패 · 관리자 확인 및 재시도가 필요합니다.' : booking.status === '환불처리중' ? '환불 처리 중입니다. 완료되면 취소 상태로 변경됩니다.' : '예약에 연결된 코스는 내 코스에서 확인할 수 있습니다.'}</p></details>
      <div className="actions"><a className="secondary" href="#/my-courses">연결 코스 확인</a>{booking.status === '숙박완료' && <a className="secondary" href="#/my-posts">{booking.post ? '작성한 코스 수정' : '휴먼 추천 코스 작성'}</a>}</div>
      {canCancel(booking) && <details><summary>예약 취소 안내</summary><h3>예약 취소</h3><p>취소 수수료·환불 규정은 정책 확정이 필요합니다.</p><p className="note">샘플 예약만 변경됩니다. 취소 요청 후 연결 코스 변경과 여행 기록 작성을 제한합니다.</p><form onSubmit={(event) => { event.preventDefault(); const status = new FormData(event.currentTarget).get('refund'); dispatch?.({ type: 'cancel', id: booking.id, status }); setNotice(`샘플 예약의 상태를 ${status}(으)로 변경했습니다.`); }}><label>환불 결과 테스트<select name="refund" defaultValue="환불처리중"><option>환불처리중</option><option>취소</option><option>환불실패</option></select></label><Button type="submit">취소 요청 확인</Button></form></details>}
    </Card>)}</div>
  </div>;
}
