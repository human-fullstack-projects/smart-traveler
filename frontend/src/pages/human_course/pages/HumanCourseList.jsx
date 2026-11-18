import CourseCard from '../components/CourseCard';
import '../../../css/human-course.css';
import CourseFilters, { emptyCourseFilters } from '../components/CourseFilters';
import PageHeading from '../components/PageHeading';
import { previewCourses } from '../data/previewData';
import { useMemo, useState } from 'react';
import { filterCourses } from '../data/courseState';

export default function HumanCourseList({ courses = previewCourses, execute, serverMode, isAuthenticated = false }) {
  const [filters, setFilters] = useState(emptyCourseFilters);
  const [resetting, setResetting] = useState(false);
  const visible = useMemo(() => filterCourses(courses, filters), [courses, filters]);
  const regions = useMemo(() => [...new Set(courses.map((course) => course.region).filter(Boolean))].sort((a, b) => a.localeCompare(b, 'ko')), [courses]);
  const categories = useMemo(() => [...new Set(courses.map((course) => course.category).filter(Boolean))].sort((a, b) => a.localeCompare(b, 'ko')), [courses]);
  const activeFilters = [filters.region, filters.category, filters.companion, filters.keyword?.trim()].filter(Boolean);
  return <div className="human-courses"><div className="page">
    <PageHeading title="다른 여행자의 추천 코스" description="조건을 선택하고, 목록에서 코스를 눌러 상세 내용을 확인합니다.">{isAuthenticated && <a className="button button-primary" href="#/human-courses/create">+ 저장된 여행 작성</a>}</PageHeading>
    <div className="layout human-course-list-layout">
      <aside className="panel human-course-filter-panel" aria-labelledby="human-course-filter-title"><div className="panel-heading"><h2 id="human-course-filter-title">검색 / 필터</h2>{activeFilters.length > 0 && <span className="badge">적용 조건 {activeFilters.length}개</span>}</div><CourseFilters filters={filters} regions={regions} categories={categories} onChange={setFilters} onReset={() => setFilters(emptyCourseFilters)} /></aside>
      <section className="panel" aria-labelledby="human-course-list-title"><div className="panel-heading human-course-results-heading"><div><h2 id="human-course-list-title">공유 코스 <span className="muted">({visible.length}건)</span></h2><p className="helper" role="status" aria-live="polite">전체 {courses.length}건 중 조건에 맞는 코스입니다.</p></div><span className="helper">{filters.sort === 'likes' ? '좋아요 많은 순' : '최신 순'}</span></div>
        <div className="course-grid" aria-live="polite">{visible.length ? visible.map((course) => <CourseCard key={course.id} course={course} detailHref={`#/human-courses/${course.id}`} />) : <div className="empty-state">검색 결과가 없습니다. 다른 조건을 선택해 보세요.</div>}</div>
        {!serverMode && <div className="footer-tools"><button onClick={() => setResetting(true)}>전체 샘플 데이터 초기화</button><a href="#/human-courses/moderation">신고·블라인드 상태 시연 →</a></div>}
        {!serverMode && resetting && <div className="note"><p>작성한 글·댓글·좋아요 등 모든 시연 변경을 초기화할까요?</p><div className="actions"><button onClick={async () => { await execute?.({ type: 'reset' }); setResetting(false); }}>초기화 확인</button><button onClick={() => setResetting(false)}>취소</button></div></div>}
      </section>
    </div>
  </div></div>;
}
