const PAGE_SIZE = 10;

export function tripPage(total, requestedPage) {
  const pageCount = Math.ceil(total / PAGE_SIZE);
  const currentPage = Math.min(requestedPage, Math.max(1, pageCount));
  const start = (currentPage - 1) * PAGE_SIZE;
  return { pageCount, currentPage, start, end: start + PAGE_SIZE };
}

export default function TripPagination({ pageCount, currentPage, onChange }) {
  if (pageCount <= 1) return null;
  return <nav className="trips-pagination" aria-label="목록 페이지">
    <button type="button" disabled={currentPage === 1} onClick={() => onChange(currentPage - 1)}>이전</button>
    <span role="status">{currentPage} / {pageCount} 페이지</span>
    <button type="button" disabled={currentPage === pageCount} onClick={() => onChange(currentPage + 1)}>다음</button>
  </nav>;
}
