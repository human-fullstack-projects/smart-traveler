import Card from '../components/Card';
import PageHero from '../components/PageHero';
import AccessNotice from '../components/AccessNotice';
import '../../../css/mypage.css';

export default function MyPosts({ posts = [], postId }) {
  const list = postId ? posts.filter(p => p.id === postId) : posts;
  if (postId && !list.length) return <AccessNotice />;
  return <div className="mypage"><PageHero eyebrow="MY POSTS" title="내 휴먼 추천 코스">본인이 작성한 게시글만 표시합니다.</PageHero>
    {postId && <p><a href="#/my-posts">← 내 게시글 목록</a></p>}
    {!list.length && <p className="empty">작성한 게시글이 없습니다.</p>}
    <div className="stack">{list.map(p => <Card key={p.id}><span className="pill">{p.status}</span><h2>{p.title}</h2><p className="chat-text">{p.body}</p>
      <div className="actions">{!postId && <a href={`#/my-posts/${p.id}`}>게시글 상세</a>}<a href={`#/human-courses/${p.id}`}>공개 화면</a><a href={`#/human-courses/${p.id}/edit`}>글 수정</a><a href={`#/reservations/${p.reservationId}`}>연결 예약</a></div>
    </Card>)}</div></div>;
}
