const regions = ['부산', '강릉', '서울', '제주', '경주'];
const categories = ['맛집', '문화', '힐링', '액티비티'];
const companions = ['혼자', '친구', '연인', '가족'];

export default function CourseFilters({ onSearch, onReset }) {
  return <form onSubmit={(event) => { event.preventDefault(); onSearch?.(Object.fromEntries(new FormData(event.currentTarget))); }} onReset={() => onReset?.()}>
    <div className="field"><label>지역<select name="region" defaultValue=""><option value="">전체 지역</option>{regions.map((value) => <option key={value}>{value}</option>)}</select></label></div>
    <div className="field"><label>여행 유형<select name="category" defaultValue=""><option value="">전체 유형</option>{categories.map((value) => <option key={value}>{value}</option>)}</select></label></div>
    <div className="field"><label>동행 유형<select name="companion" defaultValue=""><option value="">전체 유형</option>{companions.map((value) => <option key={value}>{value}</option>)}</select></label></div>
    <div className="field"><label>검색어<input name="keyword" type="search" placeholder="제목 또는 설명" /></label></div>
    <div className="field"><label>정렬<select name="sort" defaultValue="newest"><option value="newest">최신 순</option><option value="likes">좋아요 많은 순</option></select></label></div>
    <div className="actions"><button className="primary" type="submit">검색하기</button><button type="reset">초기화</button></div>
  </form>;
}
