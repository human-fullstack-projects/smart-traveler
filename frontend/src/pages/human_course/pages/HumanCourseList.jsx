import CourseCard from '../components/CourseCard';
import '../../../css/human-course.css';
import CourseFilters, { emptyCourseFilters } from '../components/CourseFilters';
import PageHeading from '../components/PageHeading';
import { previewCourses } from '../data/previewData';
import { useEffect, useMemo, useRef, useState } from 'react';
import { filterCourses } from '../data/courseState';

// Keep these breakpoints in sync with the list grid so a page is at most three rows.
const columnBreakpoints = [[1200, 5], [1000, 4], [760, 3], [520, 2]];

function useCoursePageSize() {
  const [columns, setColumns] = useState(5);
  useEffect(() => {
    if (!window.matchMedia) return;
    const queries = columnBreakpoints.map(([width, count]) => [window.matchMedia(`(min-width: ${width}px)`), count]);
    const update = () => setColumns(queries.find(([query]) => query.matches)?.[1] ?? 1);
    update();
    queries.forEach(([query]) => query.addEventListener('change', update));
    return () => queries.forEach(([query]) => query.removeEventListener('change', update));
  }, []);
  return columns * 3;
}

export default function HumanCourseList({ courses = previewCourses, execute, serverMode, isAuthenticated = false }) {
  const [filters, setFilters] = useState(emptyCourseFilters);
  const [resetting, setResetting] = useState(false);
  const [page, setPage] = useState(1);
  const resultsHeading = useRef(null);
  const pageSize = useCoursePageSize();
  const visible = useMemo(() => filterCourses(courses, filters), [courses, filters]);
  const pageCount = Math.max(1, Math.ceil(visible.length / pageSize));
  const currentPage = Math.min(page, pageCount);
  const firstCard = (currentPage - 1) * pageSize;
  const pageCourses = visible.slice(firstCard, firstCard + pageSize);
  const firstPage = Math.min(Math.max(1, currentPage - 2), Math.max(1, pageCount - 4));
  const pageNumbers = Array.from({ length: Math.min(5, pageCount) }, (_, index) => firstPage + index);
  const regions = useMemo(() => [...new Set(courses.map((course) => course.region).filter(Boolean))].sort((a, b) => a.localeCompare(b, 'ko')), [courses]);
  const categories = useMemo(() => [...new Set(courses.map((course) => course.category).filter(Boolean))].sort((a, b) => a.localeCompare(b, 'ko')), [courses]);
  const activeFilters = [filters.region, filters.category, filters.companion, filters.keyword?.trim()].filter(Boolean);

  function changeFilters(nextFilters) {
    setFilters(nextFilters);
    setPage(1);
  }

  function changePage(nextPage) {
    setPage(nextPage);
    resultsHeading.current?.focus();
  }

  return <div className="human-courses human-course-list"><div className="page">
    <PageHeading kicker="휴먼 여행" title="다른 여행자의 추천 여행" description="조건을 선택하고, 목록에서 여행을 눌러 상세 내용을 확인합니다.">{isAuthenticated && <a className="button button-primary" href="#/human-courses/create">+ 저장된 여행 작성</a>}</PageHeading>
    <div className="layout human-course-list-layout">
      <aside className="panel human-course-filter-panel" aria-labelledby="human-course-filter-title"><div className="panel-heading"><h2 id="human-course-filter-title">검색 / 필터</h2>{activeFilters.length > 0 && <span className="badge">적용 조건 {activeFilters.length}개</span>}</div><CourseFilters filters={filters} regions={regions} categories={categories} onChange={changeFilters} onReset={() => changeFilters(emptyCourseFilters)} /></aside>
      <section className="panel" aria-labelledby="human-course-list-title"><div className="panel-heading human-course-results-heading"><div><h2 id="human-course-list-title" ref={resultsHeading} tabIndex={-1}>공유 여행 <span className="muted">({visible.length}건)</span></h2><p className="helper" role="status">전체 {courses.length}건 중 조건에 맞는 여행입니다.{pageCount > 1 && ` ${currentPage} / ${pageCount} 페이지`}</p></div><span className="helper">{filters.sort === 'likes' ? '좋아요 많은 순' : '최신 순'}</span></div>
        <div className="course-grid">{pageCourses.length ? pageCourses.map((course) => <CourseCard key={course.id} course={course} detailHref={`#/human-courses/${course.id}`} />) : <div className="empty-state">검색 결과가 없습니다. 다른 조건을 선택해 보세요.</div>}</div>
        {pageCount > 1 && <nav className="course-pagination" aria-label="공유 여행 페이지">
          <button type="button" onClick={() => changePage(currentPage - 1)} disabled={currentPage === 1}>이전</button>
          {pageNumbers.map((number) => <button key={number} type="button" aria-label={`${number} 페이지`}
            aria-current={number === currentPage ? 'page' : undefined} onClick={() => changePage(number)}>{number}</button>)}
          <button type="button" onClick={() => changePage(currentPage + 1)} disabled={currentPage === pageCount}>다음</button>
        </nav>}
        {!serverMode && <div className="footer-tools"><button onClick={() => setResetting(true)}>전체 샘플 데이터 초기화</button><a href="#/human-courses/moderation">신고·블라인드 상태 시연 →</a></div>}
        {!serverMode && resetting && <div className="note"><p>작성한 글·댓글·좋아요 등 모든 시연 변경을 초기화할까요?</p><div className="actions"><button onClick={async () => { await execute?.({ type: 'reset' }); setResetting(false); }}>초기화 확인</button><button onClick={() => setResetting(false)}>취소</button></div></div>}
      </section>
    </div>
  </div></div>;
}
