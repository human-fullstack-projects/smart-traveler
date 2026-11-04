import { useState } from 'react';

const navigation = [
  { label: '여행 시작', href: '#/' },
  { label: '조건검색', href: '#/conditions' },
  { label: '숙소 찾기' },
  { label: '내 여행 일정', href: '#/my-courses' },
  { label: '휴먼 추천 코스', href: '#/human-courses' },
  { label: '마이페이지', href: '#/mypage' },
];

export default function Header({ route, onUnavailable }) {
  const [menuOpen, setMenuOpen] = useState(false);
  const link = (item) => item.href ? (
    <a key={item.label} href={item.href} aria-current={route === item.href.slice(1) || (item.href === '#/human-courses' && route.startsWith('/human-courses/')) ? 'page' : undefined} onClick={() => setMenuOpen(false)}>{item.label}</a>
  ) : (
    <button key={item.label} type="button" className="nav-link" onClick={() => { setMenuOpen(false); onUnavailable(item.label); }}>{item.label}</button>
  );
  return <header className="site-header">
    <div className="header-inner">
      <div className="brand-wrap">
        <button className="menu-toggle" type="button" aria-label={menuOpen ? '메뉴 닫기' : '메뉴 열기'} aria-expanded={menuOpen} aria-controls="mobile-menu" onClick={() => setMenuOpen(!menuOpen)}>
          <span></span><span></span><span></span>
        </button>
        <a className="brand" href="#/">smart <span>travel</span></a>
      </div>
      <nav className="desktop-nav" aria-label="주 메뉴">{navigation.map(link)}<button className="account" onClick={() => onUnavailable('로그인')}>로그인</button></nav>
    </div>
    {menuOpen && <nav id="mobile-menu" className="menu-panel" aria-label="전체 메뉴">{navigation.map(link)}<button className="nav-link" onClick={() => { setMenuOpen(false); onUnavailable('로그인'); }}>로그인</button></nav>}
  </header>;
}

