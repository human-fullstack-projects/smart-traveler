import { useEffect, useState } from 'react';
import { getCourse } from '../../human_course/api/humanCourses';
import { readRecommendationDraft } from '../../human_course/data/recommendationDraft';
import TripPagination, { tripPage } from '../components/TripPagination';
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

export default function MyPosts({ posts = [], bookings = [], courses = [], member }) {
  const [tab, setTab] = useState('all');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(1);
  const today = useBookingDate();
  const { bookingsById, existingPosts, pending: pendingBookings } = recommendationTrips(bookings, posts, today);
  const coursesById = new Map(courses.map(course => [course.id, course]));

  const postCards = existingPosts.map(post => {
    const draft = post.status === 'DRAFT';
    const detailHref = `#/human-courses/${encodeURIComponent(post.id)}`;
    return {
      key: `post-${post.id}`, status: draft ? 'draft' : 'complete', title: post.title,
      post, region: coursesById.get(post.courseId)?.region, booking: bookingsById.get(String(post.reservationId)),
      href: draft ? `${detailHref}/edit?from=my-posts` : detailHref,
      action: draft ? '이어서 작성하기' : '추천 글 자세히 보기',
    };
  });
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
  const list = [...postCards, ...pending].filter(item =>
    (tab === 'all' || item.status === tab)
    && [item.title, item.region, item.booking?.stay, item.booking?.reservationNo].join(' ').toLowerCase().includes(search)
  );
  list.sort((first, second) => {
    const firstDate = first.booking?.date || '';
    const secondDate = second.booking?.date || '';
    return secondDate.localeCompare(firstDate);
  });

  const pagination = tripPage(list.length, page);
  return <div className="mypage trips-page my-posts-list">
    <header className="trips-heading">
      <div><h1>내 여행 추천</h1><p>완료한 여행의 추천 글을 작성하고 관리하세요.</p></div>
    </header>
    <div className="trips-list-toolbar">
      <nav className="trips-filters" aria-label="여행 추천 작성 상태">
        {tabs.map(([value, label]) => <button type="button" key={value}
          aria-pressed={tab === value} onClick={() => { setTab(value); setPage(1); }}>{label}</button>)}
      </nav>
      <div className="trips-tools">
        <label className="trips-search">
          <span className="trips-sr-only">제목, 지역, 숙소 또는 예약번호 검색</span>
          <span aria-hidden="true">⌕</span>
          <input type="search" placeholder="내 여행 추천 검색" value={query} onChange={event => { setQuery(event.target.value); setPage(1); }} />
        </label>
        <a className="trips-back trips-back-pill trips-list-back" href="#/mypage">&lt;- 마이트립페이지</a>
      </div>
    </div>
    <p className="trips-count" role="status">{list.length}개의 여행</p>
    {!list.length && <p className="empty">조건에 맞는 여행이 없습니다.</p>}
    <div className="trips-grid">
      {list.slice(pagination.start, pagination.end).map(({ key, status, title, region, booking, href, action, post }) => {
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
    <TripPagination {...pagination} onChange={setPage} />
  </div>;
}
