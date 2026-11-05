import { endDate, money } from '../utils/format';

export default function OrderSummary({ booking }) {
  return <dl>
    <dt>지역</dt><dd>{booking.region}</dd>
    <dt>숙소 · 객실</dt><dd>{booking.stay} · {booking.room}</dd>
    <dt>체크인</dt><dd>{booking.date} 15:00</dd>
    <dt>체크아웃</dt><dd>{endDate(booking.date, booking.nights)} 11:00</dd>
    <dt>인원</dt><dd>{booking.guests}명 · {booking.companion}</dd>
    <dt>교통수단</dt><dd>{booking.transport}</dd>
    <dt>총 금액</dt><dd>{money(booking.price)}</dd>
  </dl>;
}
