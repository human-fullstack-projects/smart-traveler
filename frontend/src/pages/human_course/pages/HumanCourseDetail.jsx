import CourseActions from '../components/CourseActions';
import CourseComments from '../components/CourseComments';
import CoursePlan from '../components/CoursePlan';
import '../../../css/human-course.css';
import PageHeading from '../components/PageHeading';
import { previewCourses } from '../data/previewData';

export default function HumanCourseDetail({ courses = previewCourses, courseId = 1, execute }) {
  const course = courses.find((item) => String(item.id) === String(courseId));
  const blocked = course?.visibility === 'BLINDED';
  return <div className="human-courses"><div className="page">
    <PageHeading breadcrumb="코스 상세" title="코스 상세 보기" description="게시글 내용, 실제 방문 순서, 댓글과 좋아요를 확인합니다."><a className="button" href="#/human-courses">← 목록으로</a></PageHeading>
    {!course || blocked ? <section className="panel"><h2>{blocked ? '블라인드 처리된 코스입니다.' : '코스를 찾을 수 없습니다.'}</h2><p>{blocked ? '관리자 검토로 숨김 처리되어 게시글·사진·일정·댓글을 볼 수 없습니다.' : '존재하지 않는 코스입니다.'}</p>{course?.mine && <a className="button" href={`#/human-courses/${course.id}/edit`}>내 게시글 수정하기 (숨김 상태 유지)</a>}</section> : <>
      <div className="layout wide-left"><div>
        <article className="panel"><div className="article-intro"><p className="small-kicker">코스 정보</p><h2>{course.title}</h2><p className="course-meta">작성자: {course.author} | 지역: {course.region} | 여행 유형: {course.category} | 동행: {course.companion} | 작성일: {course.createdAt}</p><p className="summary">{course.summary}</p></div>
          <h3>등록 사진 <span className="helper">· 실제 이미지는 표시하지 않음</span></h3><ul className="photo-boxes">{(course.photos.length ? course.photos : ['첨부된 사진 없음']).map((photo, index) => <li key={index}>{photo}</li>)}</ul>
          <hr className="divider" /><h3>여행 후기</h3><p className="writing-sample">{course.body}</p>
        </article>
        <section className="panel"><div className="panel-heading"><h2>방문 코스 / 일정</h2></div><CoursePlan plan={course.plan} version={course.courseVersion} /></section>
      </div><aside>
        <CourseActions course={course} execute={execute} />
        <section className="panel sidebar-hint"><h3>상세 화면에서 확인할 것</h3><ol><li>일정과 후기 표시</li><li>좋아요 누르기 / 취소</li><li>게시글 신고</li><li>댓글 작성 및 삭제</li><li>내 게시글 수정 화면 이동</li></ol></section>
      </aside></div>
      <CourseComments course={course} execute={execute} />
    </>}
  </div></div>;
}
