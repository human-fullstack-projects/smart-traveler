const items = [
  { page: 'mypage', label: '마이페이지' },
  { page: 'reservations', label: '내 예약' },
  { page: 'my-courses', label: '내 코스' },
  { page: 'my-posts', label: '내 휴먼 추천 코스' },
];

export default function MyPageMenu({ currentPage }) {
  return (
    <nav className="mypage-menu" aria-label="마이페이지 메뉴">
      {items.map(({ page, label }) => (
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
