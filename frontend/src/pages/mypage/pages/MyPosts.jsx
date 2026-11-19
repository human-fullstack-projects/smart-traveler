import { useEffect, useState } from 'react';
import { getCourse } from '../../human_course/api/humanCourses';
import { readRecommendationDraft } from '../../human_course/data/recommendationDraft';
import AccessNotice from '../components/AccessNotice';
import Card from '../components/Card';
import MyCourseMap from '../components/MyCourseMap';
import { recommendationTrips } from '../data/visibleItems';
import useBookingDate from '../hooks/useBookingDate';
import tripCover from '../utils/tripCover';
import '../../../css/mypage.css';
import './my-courses.css';

const tabs = [
  ['all', '전체'], ['complete', '작성완료'], ['unwritten', '미작성'], ['draft', '작성중'],
];
const labels = Object.fromEntries(tabs);

function PostCover({ post, fallback, loading }) {
  const [loadedCover, setLoadedCover] = useState('');
  const { id, coverImageUrl } = post;

  useEffect(() => {
    // 마이페이지 응답에 대표 사진이 없으면 기존 추천 글 상세 API에서 사진을 확인한다.
    if (coverImageUrl) return;
    let active = true;
    getCourse(id).then(detail => {
      if (!active) return;
      const uploadedPhoto = detail.photoAssets?.find(photo => photo?.dataUrl)?.dataUrl;
      const places = (detail.plan || []).flatMap(day => day.places || []);
      const placePhoto = places.flatMap(place => place.imageUrls || []).find(Boolean);
      setLoadedCover(uploadedPhoto || placePhoto || '');
    }).catch(() => {
      if (active) setLoadedCover('');
    });
    return () => { active = false; };
  }, [id, coverImageUrl]);

  return <img src={coverImageUrl || loadedCover || fallback} alt="" loading={loading} />;
}

export default function MyPosts({ posts = [], bookings = [], courses = [], postId, member }) {
  const [tab, setTab] = useState('all');
  const [query, setQuery] = useState('');
  const today = useBookingDate();
  const { bookingsById, published: eligiblePosts, pending: pendingBookings } = recommendationTrips(bookings, posts, today);
  const coursesById = new Map(courses.map(course => [course.id, course]));

  if (postId) {
    const post = eligiblePosts.find(item => String(item.id) === String(postId));
    if (!post) return <AccessNotice />;
    const booking = bookingsById.get(String(post.reservationId));
    const course = coursesById.get(post.courseId);
    const id = encodeURIComponent(post.id);
    return <div className="mypage trips-page trip-detail-page">
      <a className="trips-back trips-back-pill" href="#/my-posts">← 내 여행 추천 목록</a>
      <header className="trip-detail-hero">
        <PostCover key={post.id} post={post} fallback={tripCover(course?.region || booking?.stay || post.title)} />
        <div><span className="trip-status">{labels.complete}</span><h1>{post.title}</h1>
          {booking && <p>{booking.date} ~ {booking.endDate}</p>}
        </div>
      </header>
      <Card>
        <h2>여행 후기</h2>
        <p className="chat-text">{post.body}</p>
        <div className="actions">
          <a href={`#/human-courses/${id}`}>공개 화면</a>
          <a href={`#/human-courses/${id}/edit?from=my-posts`}>글 수정</a>
          {booking && <a href={`#/reservations/${encodeURIComponent(booking.id)}`}>연결 예약</a>}
        </div>
      </Card>
      <MyCourseMap snapshot={post.courseSnapshot} courseId={post.courseId} memberId={member?.memberId} expanded />
      <div className="trip-detail-footer"><a className="trips-back trips-back-pill" href="#/my-posts">← 내 여행 추천 목록</a></div>
    </div>;
  }

  const published = eligiblePosts.map(post => ({
    key: `post-${post.id}`, status: 'complete', title: post.title,
    post,
    region: coursesById.get(post.courseId)?.region, booking: bookingsById.get(String(post.reservationId)),
    href: `#/my-posts/${encodeURIComponent(post.id)}`, action: '추천 글 자세히 보기',
  }));
  const pending = pendingBookings.map(booking => {
    const course = coursesById.get(booking.courseId);
    const draft = readRecommendationDraft(member?.memberId, booking.id);
    return {
      key: `booking-${booking.id}`, status: draft ? 'draft' : 'unwritten', booking,
      title: draft?.title.trim() || course?.title || booking.stay || '나의 여행',
      region: course?.region, href: `#/human-courses/create?reservationId=${encodeURIComponent(booking.id)}&from=my-posts`,
      action: draft ? '이어서 작성하기' : '여행 추천 작성하기',
    };
  });
  const search = query.trim().toLowerCase();
  const list = [...published, ...pending].filter(item =>
    (tab === 'all' || item.status === tab)
    && [item.title, item.region, item.booking?.stay, item.booking?.reservationNo].join(' ').toLowerCase().includes(search)
  );
  list.sort((first, second) => {
    const firstDate = first.booking?.date || '';
    const secondDate = second.booking?.date || '';
    return secondDate.localeCompare(firstDate);
  });

  return <div className="mypage trips-page my-posts-list">
    <a className="trips-back trips-back-pill" href="#/mypage">← 마이페이지</a>
    <header className="trips-heading">
      <div><h1>내 여행 추천</h1><p>완료한 여행의 추천 글을 작성하고 관리하세요.</p></div>
    </header>
    <div className="trips-list-toolbar">
      <nav className="trips-filters" aria-label="여행 추천 작성 상태">
        {tabs.map(([value, label]) => <button type="button" key={value}
          aria-pressed={tab === value} onClick={() => setTab(value)}>{label}</button>)}
      </nav>
      <div className="trips-tools">
        <label className="trips-search">
          <span className="trips-sr-only">제목, 지역, 숙소 또는 예약번호 검색</span>
          <span aria-hidden="true">⌕</span>
          <input type="search" placeholder="내 여행 추천 검색" value={query} onChange={event => setQuery(event.target.value)} />
        </label>
      </div>
    </div>
    <p className="trips-count" role="status">{list.length}개의 여행</p>
    {!list.length && <p className="empty">조건에 맞는 여행이 없습니다.</p>}
    <div className="trips-grid">
      {list.map(({ key, status, title, region, booking, href, action, post }) => {
        const fallback = tripCover(region || booking?.stay || title);
        return <a key={key} className="trip-cover-card" href={href}>
          {/* 작성 완료 카드도 상세 화면과 같은 순서로 대표 사진을 고른다. */}
          {post ? <PostCover post={post} fallback={fallback} loading="lazy" /> : <img src={fallback} alt="" loading="lazy" />}
          <span className="trip-status">{labels[status]}</span>
          <div className="trip-card-copy">
            <span className="trip-region">{region}</span>
            <h2>{title}</h2>
            {booking && <p>{booking.date} ~ {booking.endDate}<br />{booking.stay}<br />{booking.reservationNo}</p>}
            <span className="trip-card-open">{action} <span aria-hidden="true">↗</span></span>
          </div>
        </a>;
      })}
    </div>
  </div>;
}
