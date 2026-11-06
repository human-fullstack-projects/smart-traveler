const items = [
  { page: 'mypage', label: '마이페이지' },
  { page: 'reservations', label: '내 예약' },
  { page: 'my-courses', label: '내 여행 코스' },
  { page: 'my-posts', label: '내 여행 추천' },
  { page: 'my-profile', label: '내 정보관리' },
  { page: 'my-travel-style', label: '내 여행 스타일 관리' },
];

export default function MyPageMenu({ currentPage, isGuest = false }) {
  return (
    <nav className="mypage-menu" aria-label="마이페이지 메뉴">
      {items.filter(({ page }) => !isGuest || ['mypage', 'my-courses'].includes(page)).map(({ page, label }) => (
        <a
          key={page}
          className="nav-link"
          href={`#/${page}`}
          aria-current={currentPage === page ? 'page' : undefined}
        >
          {label}
        </a>
      ))}
    </nav>
  );
}
