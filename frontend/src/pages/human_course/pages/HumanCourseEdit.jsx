import { useEffect, useState } from 'react';
import '../../../css/human-course.css';
import PageHeading from '../components/PageHeading';
import RecommendationEditor from '../components/RecommendationEditor';
import { findRecommendationPostId, isPastRecommendationTrip } from '../data/courseState';
import { previewCourses } from '../data/previewData';
import useBookingDate from '../../mypage/hooks/useBookingDate';

export default function HumanCourseEdit({ courses = previewCourses, courseId = 2, execute, serverMode, loadCourse, from, reservations = [] }) {
  const source = ['reservations', 'my-posts'].includes(from) ? from : 'human-courses';
  const back = `#/${source}`;
  const today = useBookingDate();
  const [loading, setLoading] = useState(Boolean(serverMode && loadCourse));
  useEffect(() => {
    if (!serverMode || !loadCourse) return;
    let active = true;
    setLoading(true);
    loadCourse(courseId).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [serverMode, courseId]);
  async function submit(fields) {
    const result = await execute?.({ type: 'edit', id: courseId, fields, republish: draft });
    if (result?.id && !result.error) window.location.hash = `/human-courses/${encodeURIComponent(result.id)}`;
    return result;
  }
  const course = courses.find((item) => String(item.id) === String(courseId));
  const draft = course?.visibility === 'DRAFT';
  const reservation = reservations.find(item => String(item.id) === String(course?.reservationId));
  const trip = { region: course?.region, dates: course?.travelDates || reservation?.dates };
  const tripLabel = [trip.region, course?.stay?.name || reservation?.name, trip.dates].filter(Boolean).join(' · ');
  const otherTrips = reservations.flatMap(item => {
    if (String(item.id) === String(course?.reservationId)) return [];
    const postId = findRecommendationPostId(item, courses);
    if (!postId && !isPastRecommendationTrip(item, today)) return [];
    const route = postId
      ? `/human-courses/${encodeURIComponent(postId)}/edit?from=${source}`
      : `/human-courses/create?reservationId=${encodeURIComponent(item.id)}&from=${source}`;
    return [{ id: item.id, route, label: [item.region, item.name, item.dates].filter(Boolean).join(' · ') }];
  });

  return <div className="human-courses human-course-editor"><div className="page">
    <PageHeading kicker="My Trips Post" title={draft ? '내 여행 추천하기' : '여행 추천 수정하기'} description={draft ? '여행 후기를 이어서 작성하고 다시 게시하세요.' : '여행 후기를 수정하고 변경 내용을 저장하세요.'}><a className="button" href={back}>← 목록으로</a></PageHeading>
    {!course && loading ? <p role="status">게시글을 불러오고 있습니다.</p> : !course?.mine ? <section className="panel"><h2>수정할 수 없습니다</h2><p>{course ? '다른 사람의 글은 수정할 수 없습니다.' : '해당 글을 찾지 못했습니다.'}</p></section> : <>
      {course.visibility === 'BLINDED' && <p className="note">수정 후에도 숨김 상태가 유지됩니다.</p>}
      <RecommendationEditor trip={trip} plan={course.plan}
        tripSelection={<section className="panel recommendation-trip-select"><h2>연결된 여행</h2>
          <label>후기를 작성하거나 수정할 여행<select value="" onChange={event => {
            if (event.target.value) window.location.hash = event.target.value;
          }}>
            <option value="">{tripLabel || '연결된 여행 정보 없음'}</option>
            {otherTrips.map(item => <option key={item.id} value={item.route}>{item.label}</option>)}
          </select></label>
        </section>}
        formKey={`${Boolean(serverMode)}-${course.id}`}
        formProps={{ course, mode: 'edit', republishing: draft, onSubmit: submit, cancelHref: back, serverMode }} />
    </>}
  </div></div>;
}
