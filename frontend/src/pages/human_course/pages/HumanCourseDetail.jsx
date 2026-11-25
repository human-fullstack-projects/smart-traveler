import CourseActions from '../components/CourseActions';
import CourseComments from '../components/CourseComments';
import CoursePhotoGallery from '../components/CoursePhotoGallery';
import CourseMap from '../components/CourseMap';
import SaveCourseRoute from '../components/SaveCourseRoute';
import '../../../css/human-course.css';
import PageHeading from '../components/PageHeading';
import { previewCourses, previewReservations } from '../data/previewData';
import { useEffect, useState } from 'react';

export default function HumanCourseDetail({ courses = previewCourses, reservations = previewReservations, courseId = 1, execute, serverMode, loadCourse, member, isAuthenticated = false }) {
  const [detailError, setDetailError] = useState('');
  const [loadingDetail, setLoadingDetail] = useState(false);
  useEffect(() => {
    if (!serverMode) return;
    let active = true;
    setLoadingDetail(true);
    loadCourse?.(courseId).then((result) => { if (active) { setDetailError(result?.error || ''); setLoadingDetail(false); } });
    return () => { active = false; };
  }, [serverMode, courseId]);
  const course = courses.find((item) => String(item.id) === String(courseId));
  const blocked = course?.visibility === 'BLINDED';
  const photoAssets = course?.photoAssets || [];
  const photos = photoAssets.length ? photoAssets : (course?.photos || []).map((name) => ({ name, dataUrl: '' }));
  const reservation = serverMode ? null : reservations.find((item) => item.id === course?.reservationId);
  const stay = course?.stay || reservation?.stay || (reservation && { name: reservation.name });
  const listLink = <a className="button" href="#/human-courses">← 여행자 추천 트립으로</a>;
  const canOpenMyPosts = isAuthenticated && member?.roleCode === 'USER';
  const canSave = isAuthenticated && serverMode && !blocked && course?.plan?.length > 0;
  return <div className="human-courses human-course-detail"><div className="page">
    <PageHeading kicker="휴먼 여행" listLabel="여행 목록" breadcrumb="여행 상세" title="여행 상세 보기" description="게시글 내용, 실제 방문 순서, 댓글과 좋아요를 확인합니다.">
      {canSave || canOpenMyPosts ? <div className="actions course-heading-actions">
        {canSave && <SaveCourseRoute key={course.id} postId={course.id} />}
        {listLink}
        {canOpenMyPosts && <a className="button" href="#/my-posts">{'<- 내 추천 여행으로'}</a>}
      </div> : listLink}
    </PageHeading>
    {loadingDetail && <p role="status">게시글을 확인하고 있습니다.</p>}
    {detailError && <p className="note" role="alert">{detailError} <button type="button" onClick={async () => { setLoadingDetail(true); const result = await loadCourse?.(courseId); setDetailError(result?.error || ''); setLoadingDetail(false); }}>다시 시도</button></p>}
    {!loadingDetail && (!course || blocked) ? <section className="panel"><h2>{blocked ? '블라인드 처리된 여행입니다.' : '여행을 찾을 수 없습니다.'}</h2><p>{blocked ? '관리자 검토로 숨김 처리되어 게시글·사진·일정·댓글을 볼 수 없습니다.' : '존재하지 않는 여행입니다.'}</p>{isAuthenticated && course?.mine && <a className="button" href={`#/human-courses/${course.id}/edit`}>수정하기</a>}</section> : course && !blocked ? <>
      <article className="panel"><div className="article-intro"><p className="small-kicker">여행 정보</p><h2>{course.title}</h2><dl className="detail-meta"><div><dt>작성자</dt><dd>{course.author}</dd></div><div><dt>지역</dt><dd>{course.region}</dd></div><div><dt>여행 기간</dt><dd>{course.travelDates || '미등록'}</dd></div><div><dt>여행 유형</dt><dd>{course.category}</dd></div><div><dt>동행</dt><dd>{course.companion}</dd></div><div><dt>작성일</dt><dd><time dateTime={course.createdAt}>{course.createdAt}</time></dd></div></dl></div>
        <CoursePhotoGallery key={course.id} title={course.title} photos={photos} />
        <hr className="divider" /><h3>여행 후기</h3><p className="course-review-text">{course.body}</p>
      </article>
      <section className="panel course-detail-map"><div className="panel-heading"><h2>지역 지도</h2></div>
        <CourseMap region={course.region} plan={course.plan} stay={stay} travelDates={course.travelDates} allDaysLabel="전체 일정" height="clamp(350px, 40vw, 380px)" />
      </section>
      <CourseComments course={course} execute={execute} isAuthenticated={isAuthenticated} />
      {isAuthenticated && <CourseActions key={course.id} course={course} execute={execute} />}
    </> : null}
  </div></div>;
}
