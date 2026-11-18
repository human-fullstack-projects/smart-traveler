const companions = ['혼자', '친구', '연인', '가족'];

export const emptyCourseFilters = Object.freeze({ region: '', category: '', companion: '', keyword: '', sort: 'newest' });

export default function CourseFilters({ filters = emptyCourseFilters, regions = [], categories = [], onChange, onReset }) {
  const update = (name, value) => onChange?.({ ...emptyCourseFilters, ...filters, [name]: value });
  return <form className="human-course-filter-form" onSubmit={(event) => event.preventDefault()} onReset={(event) => { event.preventDefault(); onReset?.(); }}>
    <div className="human-course-filter-grid">
      <div className="field"><label htmlFor="human-course-region">지역</label><select id="human-course-region" name="region" value={filters.region || ''} onChange={(event) => update('region', event.target.value)}><option value="">전체 지역</option>{regions.map((value) => <option key={value}>{value}</option>)}</select></div>
      <div className="field"><label htmlFor="human-course-category">여행 유형</label><select id="human-course-category" name="category" value={filters.category || ''} onChange={(event) => update('category', event.target.value)}><option value="">전체 유형</option>{categories.map((value) => <option key={value}>{value}</option>)}</select></div>
      <div className="field"><label htmlFor="human-course-keyword">검색어</label><input id="human-course-keyword" name="keyword" type="search" value={filters.keyword || ''} onChange={(event) => update('keyword', event.target.value)} placeholder="제목 또는 여행 이야기" /></div>
      <div className="field"><label htmlFor="human-course-sort">정렬</label><select id="human-course-sort" name="sort" value={filters.sort || 'newest'} onChange={(event) => update('sort', event.target.value)}><option value="newest">최신 순</option><option value="likes">좋아요 많은 순</option></select></div>
    </div>
    <fieldset className="companion-filter"><legend>동행 유형</legend><div className="filter-chip-group">
      {['', ...companions].map((value) => <button key={value || 'all'} className="filter-chip" type="button" aria-pressed={(filters.companion || '') === value} onClick={() => update('companion', value)}>{value || '전체'}</button>)}
    </div></fieldset>
    <div className="filter-actions"><p className="helper">조건을 바꾸면 목록에 바로 반영됩니다.</p><button type="reset">조건 초기화</button></div>
  </form>;
}
