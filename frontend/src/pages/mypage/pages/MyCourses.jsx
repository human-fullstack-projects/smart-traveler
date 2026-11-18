import { useEffect, useState } from 'react';
import Card from '../components/Card';
import AccessNotice from '../components/AccessNotice';
import MyCourseMap from '../components/MyCourseMap';
import { myPageApi } from '../api/myPageApi';
import { isSavedCourse as isDraft } from '../data/visibleItems';
import palace from '../profile/avatars/palace.png';
import beach from '../profile/avatars/beach.png';
import mountain from '../profile/avatars/mountain.png';
import village from '../profile/avatars/village.png';
import '../../../css/mypage.css';
import './my-courses.css';

const stateLabel = course => {
  const today = new Date().toLocaleDateString('sv-SE');
  if (!isDraft(course) && course.endDate && course.endDate < today) return '지난 여행';
  if (!isDraft(course) && course.endDate && course.endDate >= today) return '예정된 여행';
  return course.state === 'GUEST' ? '비회원 저장된 여행' : '저장된 여행';
};
const cover = course => /제주|부산|바다|해변/.test(`${course.region} ${course.title}`) ? beach : /서울|경주|문화|궁/.test(`${course.region} ${course.title}`) ? palace : /산|자연|숲/.test(`${course.region} ${course.title}`) ? mountain : village;
const period = course => `${course.startDate || '날짜 미정'} ~ ${course.endDate || '날짜 미정'}`;

export default function MyCourses({ courses = [], isGuest, member, courseId, guestRecommendedCourse }) {
  const [detail, setDetail] = useState(null);
  const [detailStatus, setDetailStatus] = useState('loading');
  const [query, setQuery] = useState('');
  const [latestCourses, setLatestCourses] = useState(null);
  const [listStatus, setListStatus] = useState('ready');
  useEffect(() => {
    let cancelled = false;
    setLatestCourses(null);
    if (isGuest || !member?.memberId || courseId) return;
    setListStatus('loading');
    myPageApi.load(member.memberId).then(result => {
      if (cancelled) return;
      if (result.memberId !== member.memberId) throw new Error('Member mismatch');
      setLatestCourses(result.courses);
      setListStatus('ready');
    }).catch(() => { if (!cancelled) setListStatus('error'); });
    return () => { cancelled = true; };
  }, [member?.memberId, isGuest, courseId]);
  useEffect(() => {
    let cancelled = false;
    if (!courseId) return;
    setDetailStatus('loading'); setDetail(null);
    if (isGuest) {
      const course = courses.find(c => String(c.id) === String(courseId));
      setDetail(course || null); setDetailStatus(course ? 'ready' : 'denied');
    } else if (member?.memberId) {
      myPageApi.course(member.memberId, courseId).then(course => {
        if (!cancelled) { setDetail(course); setDetailStatus('ready'); }
      }).catch(error => { if (!cancelled) setDetailStatus(error.status === 401 ? 'login' : error.status === 403 || error.status === 404 ? 'denied' : 'unavailable'); });
    } else setDetailStatus('login');
    return () => { cancelled = true; };
  }, [courseId, member?.memberId, isGuest, courses]);

  if (courseId && detailStatus !== 'ready') return <AccessNotice kind={detailStatus === 'loading' ? 'checking' : detailStatus} />;
  if (courseId) return <div className="mypage trips-page">
    <a className="trips-back trips-back-pill" href="#/my-courses">← 저장된 여행 목록</a>
    <header className="trip-detail-hero">
      <img src={cover(detail)} alt="" />
      <div><span className="trip-status">{stateLabel(detail)}</span><h1>{detail.title}</h1><p>{detail.region} · {period(detail)}</p></div>
    </header>
    <div className="trip-detail-summary"><strong>{detail.region || '여행 지역 미정'}</strong><span>{period(detail)}</span><span>총 {(detail.days || []).length}일 일정</span><span>방문 장소 {(detail.days || []).reduce((sum, day) => sum + (day.places || []).length, 0)}곳</span></div>
    <MyCourseMap key={detail.id} course={detail} expanded title="여행 일정" hideMapControls showPlaceDetails />
    <div className="trip-detail-footer"><a className="trips-back trips-back-pill" href="#/my-courses">← 저장된 여행 목록</a></div>
  </div>;

  const visibleCourses = latestCourses ?? courses;
  const filtered = visibleCourses.filter(course => {
    return isDraft(course) && `${course.title} ${course.region}`.toLowerCase().includes(query.trim().toLowerCase());
  });
  const shownGuestCourse = isGuest && guestRecommendedCourse
    && `${guestRecommendedCourse.title} ${guestRecommendedCourse.destination}`.toLowerCase().includes(query.trim().toLowerCase());
  return <div className="mypage trips-page">
    <a className="trips-back trips-back-pill" href="#/mypage">← 마이페이지</a>
    <header className="trips-heading trips-heading--saved"><h1>저장된 여행</h1><div className="trips-description-search"><p>{isGuest ? '비회원의 저장된 여행은 새로고침하거나 사이트를 떠나면 삭제됩니다.' : '저장된 여행을 모아보고, 여행을 예약해보세요.'}</p>
      <div className="trips-tools"><label className="trips-search"><span className="trips-sr-only">여행 제목 또는 지역 검색</span><span aria-hidden="true">⌕</span><input type="search" placeholder="내 여행 검색" value={query} onChange={event => setQuery(event.target.value)} /></label></div>
    </div></header>
    <p className="trips-count" role="status">{filtered.length + Number(Boolean(shownGuestCourse))}개의 여행</p>
    {!isGuest && listStatus === 'loading' && <p role="status" className="muted">최신 여행 일정을 불러오는 중입니다.</p>}
    {!isGuest && listStatus === 'error' && <p role="alert">최신 여행 일정을 불러오지 못했습니다. 서버 연결을 확인한 뒤 화면을 새로고침해 주세요.</p>}
    {shownGuestCourse && <Card>
      <span className="pill">비회원 추천 코스</span>
      <h2><a href="#/login">{guestRecommendedCourse.title}</a></h2>
      <p>{guestRecommendedCourse.destination} · {guestRecommendedCourse.startDate}부터 {guestRecommendedCourse.durationDays}일</p>
      <p><a href="#/login">로그인 후 내 코스에 저장</a> · <a href={`#/stays?destination=${encodeURIComponent(guestRecommendedCourse.destination ?? '')}`}>숙소 찾기</a></p>
    </Card>}
    {!shownGuestCourse && !filtered.length && (isGuest || listStatus === 'ready') && <p className="empty">{visibleCourses.length ? '조건에 맞는 여행 일정이 없습니다.' : '저장된 여행 일정이 없습니다.'}</p>}
    <div className="trips-grid">{filtered.map(course => <div key={course.id}><a className="trip-cover-card" href={`#/my-courses/${encodeURIComponent(course.id)}`}>
      <img src={cover(course)} alt="" loading="lazy" /><span className="trip-status">{stateLabel(course)}</span>
      <div className="trip-card-copy"><span className="trip-region">{course.region || '나의 여행'}</span><h2>{course.title}</h2><p>{period(course)}</p><span className="trip-card-open">일정 자세히 보기 <span aria-hidden="true">↗</span></span></div>
    </a></div>)}</div>
  </div>;
}
