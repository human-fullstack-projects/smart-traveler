import CourseActions from '../components/CourseActions';
import CourseComments from '../components/CourseComments';
import CoursePlan from '../components/CoursePlan';
import CourseMap from '../components/CourseMap';
import '../../../css/human-course.css';
import PageHeading from '../components/PageHeading';
import { previewCourses } from '../data/previewData';
import { useEffect, useState } from 'react';

export default function HumanCourseDetail({ courses = previewCourses, courseId = 1, execute, serverMode, loadCourse, isAuthenticated = false }) {
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
  return <div className="human-courses"><div className="page">
    <PageHeading breadcrumb="코스 상세" title="코스 상세 보기" description="게시글 내용, 실제 방문 순서, 댓글과 좋아요를 확인합니다."><a className="button" href="#/human-courses">← 목록으로</a></PageHeading>
    {loadingDetail && <p role="status">게시글을 확인하고 있습니다.</p>}
    {detailError && <p className="note" role="alert">{detailError} <button type="button" onClick={async () => { setLoadingDetail(true); const result = await loadCourse?.(courseId); setDetailError(result?.error || ''); setLoadingDetail(false); }}>다시 시도</button></p>}
    {!loadingDetail && (!course || blocked) ? <section className="panel"><h2>{blocked ? '블라인드 처리된 코스입니다.' : '코스를 찾을 수 없습니다.'}</h2><p>{blocked ? '관리자 검토로 숨김 처리되어 게시글·사진·일정·댓글을 볼 수 없습니다.' : '존재하지 않는 코스입니다.'}</p>{isAuthenticated && course?.mine && <a className="button" href={`#/human-courses/${course.id}/edit`}>내 게시글 수정하기 (숨김 상태 유지)</a>}</section> : course && !blocked ? <>
      <div className={`layout human-course-detail-layout${isAuthenticated ? ' wide-left' : ' is-read-only'}`}><div>
        <article className="panel"><div className="article-intro"><p className="small-kicker">코스 정보</p><h2>{course.title}</h2><dl className="detail-meta"><div><dt>작성자</dt><dd>{course.author}</dd></div><div><dt>지역</dt><dd>{course.region}</dd></div><div><dt>여행 기간</dt><dd>{course.travelDates || '미등록'}</dd></div><div><dt>여행 유형</dt><dd>{course.category}</dd></div><div><dt>동행</dt><dd>{course.companion}</dd></div><div><dt>작성일</dt><dd><time dateTime={course.createdAt}>{course.createdAt}</time></dd></div></dl><p className="summary">{course.summary}</p></div>
          <h3>등록 사진</h3><ul className="photo-boxes">{photos.length ? photos.map((photo, index) => <li key={`${photo.name}-${index}`}>{photo.dataUrl ? <img src={photo.dataUrl} alt={`${course.title} 사진 ${index + 1}: ${photo.name}`} loading="lazy" /> : <span>{photo.name} (기존 샘플 파일명)</span>}</li>) : <li><span>첨부된 사진이 없습니다.</span></li>}</ul>
          <hr className="divider" /><h3>여행 후기</h3><p className="writing-sample">{course.body}</p>
        </article>
        <section className="panel"><div className="panel-heading"><h2>방문 코스 / 일정</h2></div><CoursePlan plan={course.plan} version={course.courseVersion} /></section>
        <section className="panel"><div className="panel-heading"><h2>지역 지도</h2></div><CourseMap region={course.region} plan={course.plan} stay={course.stay} showDemoStays={!serverMode} /></section>
      </div>{isAuthenticated && <aside>
        <CourseActions course={course} execute={execute} />
        {!serverMode && <section className="panel sidebar-hint"><h3>상세 화면에서 확인할 것</h3><ol><li>일정과 후기 표시</li><li>좋아요 누르기 / 취소</li><li>게시글 신고</li><li>댓글 작성 및 삭제</li><li>내 게시글 수정 화면 이동</li></ol></section>}
      </aside>}</div>
      <CourseComments course={course} execute={execute} isAuthenticated={isAuthenticated} />
    </> : null}
  </div></div>;
}
