import Card from '../components/Card';
import '../mypage.css';
import CourseEditor from '../components/CourseEditor';
import PageHero from '../components/PageHero';
import TripTimeline from '../components/TripTimeline';
import { previewBookings } from '../data/previewBookings';
import { endDate } from '../utils/format';

export default function MyCourses({ bookings = previewBookings, dispatch }) {
  return <div className="mypage">
    <PageHero eyebrow="MY COURSES" title={<>일정은 편하게, <em>변경은 신중하게.</em></>}>확정된 예약의 코스를 확인하고 변경안을 승인하세요.</PageHero>
    {bookings.length === 0 && <div className="empty">저장된 코스가 없습니다.</div>}
    <div className="stack">{bookings.map((booking) => {
      const inactive = booking.status === '취소' || booking.cancelRequested;
      const current = booking.versions.at(-1);
      return <Card key={booking.id}>
        <span className="pill">{inactive ? '비활성' : current ? '저장됨' : '생성 대기'}</span><h2>{booking.stay} 여행 코스</h2><p className="muted">{booking.date} ~ {endDate(booking.date, booking.nights)}</p>
        {current ? <><details open><summary>현재 일정 · 버전 {current.v}</summary><TripTimeline course={current} nights={booking.nights} /><p className="note">체크인 15:00 · 체크아웃 11:00 기준의 샘플 일정입니다.</p></details><details><summary>버전별 변경 이력</summary>{booking.versions.map((version) => <div className="version" key={version.v}><strong>버전 {version.v} · {version.name}</strong><p>{version.note}</p><p className="note">{version.time}</p></div>)}</details></> : <p>연결된 코스가 없습니다. 예약은 유지한 채 코스를 생성할 수 있습니다.</p>}
        {inactive ? <p className="warning">취소 요청된 예약의 코스는 변경·작성에 사용할 수 없습니다.</p> : <CourseEditor booking={booking} dispatch={dispatch} />}
      </Card>;
    })}</div>
  </div>;
}
