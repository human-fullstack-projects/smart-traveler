import PageHeading from '../components/PageHeading';
import '../human-course.css';
import { previewCourses } from '../data/previewData';

export default function HumanCourseModeration({ courses = previewCourses, execute }) {
  const reported = courses.filter((course) => course.reportedByMe || course.visibility === 'BLINDED');
  return <div className="human-courses"><div className="page">
    <PageHeading title="신고·블라인드 상태 시연" description="관리자 처리 결과를 확인하는 개발 검증 화면입니다."><a className="button" href="#/human-courses">← 코스 목록</a></PageHeading>
    <p className="note">실제 관리자 기능이나 권한 인증이 아닙니다. 신고 접수 자체는 게시글을 자동으로 숨기지 않습니다.</p>
    {reported.length ? reported.map((course) => <section className="panel" key={course.id}><h2>#{course.id} {course.title}</h2><p>상태: {course.visibility === 'BLINDED' ? '블라인드' : '공개 / 신고 접수'}</p><p>신고 사유: {course.reportReason || '부적절한 내용 (샘플)'}{course.reportDetails && ` / ${course.reportDetails}`}</p><div className="actions"><button onClick={() => execute?.({ type: 'moderate', id: course.id })}>{course.visibility === 'BLINDED' ? '블라인드 해제 시연' : '관리자 블라인드 시연'}</button><a href={`#/human-courses/${course.id}`}>일반 상세 화면 확인 →</a></div></section>) : <p className="panel">신고된 게시글이 없습니다. 상세 화면에서 신고를 접수해 보세요.</p>}
  </div></div>;
}
