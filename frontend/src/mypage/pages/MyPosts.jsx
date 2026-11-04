import Card from '../components/Card';
import '../mypage.css';
import PostEditor from '../components/PostEditor';
import PageHero from '../components/PageHero';
import TripTimeline from '../components/TripTimeline';
import { previewBookings } from '../data/previewBookings';

export default function MyPosts({ bookings = previewBookings, dispatch }) {
  const eligible = bookings.filter((booking) => booking.post || booking.status === '숙박완료');
  return <div className="mypage">
    <PageHero eyebrow="MY HUMAN COURSES" title={<>다녀온 여행을 <em>이야기로.</em></>}>숙박완료 예약 한 건에 하나의 코스를 작성하고 수정할 수 있어요.</PageHero>
    <div className="stack">{eligible.map((booking) => <Card key={booking.id}>
      <span className="pill">{booking.post ? '작성한 코스' : '작성 가능'}</span><h2>{booking.post?.title || `${booking.stay} 여행 기록`}</h2><p className="muted">{booking.region} · {booking.date} · {booking.companion}</p>
      {booking.post && <><p className="chat-text">{booking.post.body}</p>{booking.post.photo && <img className="photo-preview" alt="첨부한 여행 사진" src={booking.post.photo} />}<details><summary>첨부된 최종 코스 · 버전 {booking.post.version}</summary><TripTimeline course={booking.versions.find((version) => version.v === booking.post.version)} nights={booking.nights} /></details></>}
      {booking.status === '숙박완료' && !booking.cancelRequested && booking.versions.length > 0 ? <PostEditor booking={booking} dispatch={dispatch} /> : !booking.versions.length ? <><p>먼저 예약에 연결된 코스를 생성해주세요.</p><a className="secondary" href="#/my-courses">내 코스로 이동</a></> : <p className="warning">취소된 예약은 작성·수정할 수 없습니다.</p>}
    </Card>)}{eligible.length === 0 && <div className="empty">숙박완료 예약 또는 작성한 코스가 없습니다.</div>}</div>
  </div>;
}
