import Card from '../components/Card';
import PageHero from '../components/PageHero';
import AccessNotice from '../components/AccessNotice';
import { money } from '../utils/format';
import '../../../css/mypage.css';

export default function Reservations({ bookings = [], reservationId }) {
  const list = reservationId ? bookings.filter(b => b.id === reservationId) : bookings;
  if (reservationId && !list.length) return <AccessNotice />;
  return <div className="mypage"><PageHero eyebrow="MY RESERVATIONS" title="내 예약">본인 소유의 예약만 표시합니다.</PageHero>
    {reservationId && <p><a href="#/reservations">← 내 예약 목록</a></p>}
    {!list.length && <p className="empty">예약 내역이 없습니다.</p>}
    <div className="stack">{list.map(b => <Card key={b.id}><span className="pill">{b.status}</span>
      <h2>{b.stay || '숙소 정보 확인 필요'}</h2><p>예약번호: {b.reservationNo}</p><p>{b.date} ~ {b.endDate} · {b.guests}명 · {b.room}</p><strong>{money(b.price)}</strong>
      <div className="actions">{!reservationId && <a href={`#/reservations/${b.id}`}>예약 상세</a>}{b.status === 'PENDING_PAYMENT' && <a href={`#/payments/${b.id}`}>토스 테스트 결제</a>}{b.courseId && <a href={`#/my-courses/${b.courseId}`}>이 예약의 내 코스</a>}{b.status === 'COMPLETED' && <a href="#/human-courses/create">휴먼 추천 코스 작성·수정</a>}</div>
      <p className="note">취소·환불 기능은 서버 연결을 준비 중입니다.</p>
    </Card>)}</div></div>;
}
