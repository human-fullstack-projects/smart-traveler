import Card from '../components/Card';
import '../mypage.css';
import PageHero from '../components/PageHero';
import { previewBookings } from '../data/previewBookings';

export default function MyPage({ bookings = previewBookings, notify = true, dispatch }) {
  const active = bookings.filter((booking) => booking.status !== '취소' && !booking.cancelRequested);
  const summaries = [
    ['reservations', '예약 내역', bookings.length, '예약 상태와 결제·환불 내역'],
    ['my-courses', '저장된 코스', active.filter((booking) => booking.versions.length).length, '예약에 연결된 확정 코스와 변경 이력'],
    ['my-posts', '내 휴먼 추천 코스', bookings.filter((booking) => booking.post).length, '내가 작성한 여행 기록과 최종 코스'],
  ];
  return <div className="mypage">
    <PageHero eyebrow="MY TRAVEL" title={<>나의 여행, <em>한곳에서.</em></>}>예약부터 여행 일정, 직접 남긴 이야기까지.</PageHero>
    <Card className="row"><div><span className="pill">샘플 사용자</span><h2>홍길동님, 안녕하세요.</h2><p className="muted">다음 여행도 나만의 취향으로 채워보세요.</p></div><a className="primary" href="#/">새 여행 만들기 →</a></Card>
    <div className="grid three" style={{ marginTop: 23 }}>{summaries.map(([page, title, count, description]) => <Card key={title}>
      <span className="pill">MY TRAVEL</span><h2><a className="card-title-link" href={`#/${page}`}>{title}</a></h2><p className="stats">{count}<span className="note"> 건</span></p><p className="muted">{description}</p><div className="actions"><a className="text-button" href={`#/${page}`} aria-label={`${title} 자세히 보기`}>자세히 보기 →</a></div>
    </Card>)}</div>
    <Card style={{ marginTop: 23 }}><h2>알림 설정</h2><label className="checkline"><input type="checkbox" checked={notify} onChange={(event) => dispatch?.({ type: 'notify', value: event.target.checked })} /> 여행 전 일정과 체크아웃 후 코스 작성 요청 알림 받기</label><p className="note">수신 거부 시 알림 대상에서 제외되며, 예약 취소 시 해당 예약의 예정 알림도 취소됩니다.</p></Card>
  </div>;
}
