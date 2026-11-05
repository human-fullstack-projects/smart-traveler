import CourseCard from '../components/CourseCard';
import '../../../css/human-course.css';
import CourseFilters from '../components/CourseFilters';
import PageHeading from '../components/PageHeading';
import { previewCourses } from '../data/previewData';
import { useState } from 'react';
import { filterCourses } from '../data/courseState';

export default function HumanCourseList({ courses = previewCourses, execute }) {
  const [filters, setFilters] = useState({});
  const [resetting, setResetting] = useState(false);
  const visible = filterCourses(courses, filters);
  return <div className="human-courses"><div className="page">
    <PageHeading title="다른 여행자의 추천 코스" description="조건을 선택하고, 목록에서 코스를 눌러 상세 내용을 확인합니다."><a className="button button-primary" href="#/human-courses/create">+ 내 코스 작성</a></PageHeading>
    <div className="layout">
      <aside className="panel"><div className="panel-heading"><h2>검색 / 필터</h2></div><CourseFilters onSearch={setFilters} onReset={() => setFilters({})} /></aside>
      <section className="panel"><div className="panel-heading"><h2>공유 코스 <span className="muted">({visible.length}건)</span></h2><span className="helper">공유된 여행 코스</span></div>
        <div className="course-grid" aria-live="polite">{visible.length ? visible.map((course) => <CourseCard key={course.id} course={course} detailHref={`#/human-courses/${course.id}`} />) : <div className="empty-state">검색 결과가 없습니다. 다른 조건을 선택해 보세요.</div>}</div>
        <div className="footer-tools"><button onClick={() => setResetting(true)}>전체 샘플 데이터 초기화</button><a href="#/human-courses/moderation">신고·블라인드 상태 시연 →</a></div>
        {resetting && <div className="note"><p>작성한 글·댓글·좋아요 등 모든 시연 변경을 초기화할까요?</p><div className="actions"><button onClick={() => { execute?.({ type: 'reset' }); setResetting(false); }}>초기화 확인</button><button onClick={() => setResetting(false)}>취소</button></div></div>}
      </section>
    </div>
  </div></div>;
}
