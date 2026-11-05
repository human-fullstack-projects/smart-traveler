import { useState } from 'react';
import CourseForm from '../components/CourseForm';
import '../../../css/human-course.css';
import CoursePlan from '../components/CoursePlan';
import PageHeading from '../components/PageHeading';
import { previewCourses } from '../data/previewData';

export default function HumanCourseEdit({ courses = previewCourses, courseId = 2, execute }) {
  const [notice, setNotice] = useState('');
  function submit(fields) {
    const result = execute?.({ type: 'edit', id: courseId, fields });
    if (result?.error) setNotice(result.error);
    else if (result?.id) window.location.hash = `/human-courses/${result.id}`;
  }
  const course = courses.find((item) => String(item.id) === String(courseId));
  return <div className="human-courses"><div className="page">
    <PageHeading breadcrumb="작성 코스 수정" title="내 여행 코스 수정" description="게시글의 설명과 후기만 수정합니다. 원래 여행 일정은 수정되지 않습니다."><a className="button" href={course ? `#/human-courses/${course.id}` : '#/human-courses'}>← {course ? '상세로 돌아가기' : '목록으로'}</a></PageHeading>
    {notice && <p className="status" role="status">{notice}</p>}
    {!course?.mine ? <section className="panel"><h2>수정할 수 없습니다</h2><p>{course ? '다른 사람의 코스는 수정할 수 없습니다.' : '해당 코스를 찾지 못했습니다.'}</p></section> : <div className="layout wide-left">
      <section className="panel"><div className="panel-heading"><h2>기존 코스 정보 수정</h2><span className="badge">내 게시글</span></div><p className="note">게시글 ID: {course.id} | 연결 예약: {course.reservationId || '없음(샘플)'} | 최종 코스 v{course.courseVersion}</p>{course.visibility === 'BLINDED' && <p className="note">블라인드 상태: 수정해도 자동 공개되지 않습니다.</p>}<CourseForm key={course.id} course={course} mode="edit" onSubmit={submit} cancelHref={`#/human-courses/${course.id}`} /></section>
      <aside><section className="panel"><div className="panel-heading"><h2>연결된 여행 일정</h2><span className="badge badge-neutral">수정 불가</span></div><p className="form-help">게시글을 수정해도 여행 일정과 연결 예약 정보는 그대로 유지됩니다.</p><CoursePlan plan={course.plan} version={course.courseVersion} /></section><section className="panel sidebar-hint"><h3>수정 흐름</h3><ol><li>기존 글을 입력란에 표시</li><li>제목 / 후기 / 사진명 변경</li><li>기존 글 ID 그대로 유지</li><li>상세 페이지로 돌아가 확인</li></ol></section></aside>
    </div>}
  </div></div>;
}
