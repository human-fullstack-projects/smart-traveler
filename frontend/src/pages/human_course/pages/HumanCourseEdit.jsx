import { useEffect, useState } from 'react';
import '../../../css/human-course.css';
import PageHeading from '../components/PageHeading';
import RecommendationEditor from '../components/RecommendationEditor';
import { previewCourses } from '../data/previewData';
import { creationError, finalCourse } from '../data/courseState';
import { clearRecommendationDraft, readRecommendationDraft } from '../data/recommendationDraft';

export default function HumanCourseEdit({ courses = previewCourses, courseId = 2, execute, serverMode, loadCourse, from, reservations = [], member }) {
  const back = ['reservations', 'my-posts'].includes(from) ? `#/${from}` : '#/human-courses';
  const [loading, setLoading] = useState(Boolean(serverMode && loadCourse));
  const [selectedReservationId, setSelectedReservationId] = useState(null);
  useEffect(() => {
    if (!serverMode || !loadCourse) return;
    let active = true;
    setLoading(true);
    loadCourse(courseId).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [serverMode, courseId]);
  async function submit(fields) {
    const result = await execute?.({ type: 'edit', id: courseId,
      fields: replacement ? { ...fields, replacePhotos: true } : fields,
      republish: draft, reservationId: replacement?.id });
    if (result?.id && !result.error) {
      if (replacement && member?.memberId) clearRecommendationDraft(member.memberId, replacement.id);
      window.location.hash = `/human-courses/${encodeURIComponent(result.id)}`;
    }
    return result;
  }
  const course = courses.find((item) => String(item.id) === String(courseId));
  const draft = course?.visibility === 'DRAFT';
  const currentId = String(course?.reservationId ?? '');
  const reservation = reservations.find(item => String(item.id) === currentId);
  const alternatives = reservations.filter(item => String(item.id) !== currentId && finalCourse(item)?.plan?.length
    && (serverMode ? item.eligibilityError === '' : !creationError({ courses }, item)));
  const replacement = alternatives.find(item => String(item.id) === selectedReservationId);
  const currentTrip = { region: course?.region, dates: course?.travelDates || reservation?.dates };
  const trip = replacement ?? currentTrip;
  const plan = replacement ? finalCourse(replacement).plan : course?.plan;
  const formCourse = replacement
    ? readRecommendationDraft(member?.memberId, replacement.id) ?? { title: `${replacement.region} 여행 추천` }
    : course;
  const tripLabel = [currentTrip.region, course?.stay?.name || reservation?.name, currentTrip.dates].filter(Boolean).join(' · ');

  return <div className="human-courses human-course-editor"><div className="page">
    <PageHeading kicker="My Trips Post" title={draft ? '내 여행 추천하기' : '여행 추천 수정하기'} description={draft ? '여행 후기를 이어서 작성하고 다시 게시하세요.' : '여행 후기를 수정하고 변경 내용을 저장하세요.'}><a className="button" href={back}>← 목록으로</a></PageHeading>
    {!course && loading ? <p role="status">게시글을 불러오고 있습니다.</p> : !course?.mine ? <section className="panel"><h2>수정할 수 없습니다</h2><p>{course ? '다른 사람의 글은 수정할 수 없습니다.' : '해당 글을 찾지 못했습니다.'}</p></section> : <>
      {course.visibility === 'BLINDED' && <p className="note">수정 후에도 숨김 상태가 유지됩니다.</p>}
      <RecommendationEditor trip={trip} plan={plan}
        tripSelection={<section className="panel recommendation-trip-select"><h2>연결된 여행</h2>
          <label>후기를 수정할 여행<select value={replacement ? String(replacement.id) : currentId} onChange={event => setSelectedReservationId(event.target.value)}>
            <option value={currentId}>{tripLabel || '연결된 여행 정보 없음'}</option>
            {alternatives.map(item => <option key={item.id} value={item.id}>{[item.region, item.name, item.dates].filter(Boolean).join(' · ')}</option>)}
          </select></label>
        </section>}
        formKey={`${Boolean(serverMode)}-${course.id}-${replacement?.id ?? currentId}`}
        formProps={{ course: formCourse, mode: 'edit', republishing: draft, onSubmit: submit, cancelHref: back, serverMode }} />
    </>}
  </div></div>;
}
