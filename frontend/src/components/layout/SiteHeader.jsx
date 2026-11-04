import { useState } from 'react';

const navigation = [
  { label: '여행 시작', href: '#/' },
  { label: '코스추천', href: '#/ai-course' },
  { label: '조건검색', href: '#/conditions' },
  { label: '숙소 찾기' },
  { label: '내 여행 일정' },
  { label: '마이페이지' },
];

export default function SiteHeader({ route, onUnavailable, member, onLogout, loggingOut }) {
  const [menuOpen, setMenuOpen] = useState(false);
  const link = (item) => item.href ? (
    <a key={item.label} href={item.href} aria-current={route === item.href.slice(1) ? 'page' : undefined} onClick={() => setMenuOpen(false)}>{item.label}</a>
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
      <nav className="desktop-nav" aria-label="주 메뉴">{navigation.map(link)}{member ? <span className="auth-account"><span className="auth-account__name">{member.nickname || member.loginId}님</span><button className="account" type="button" disabled={loggingOut} onClick={() => { setMenuOpen(false); onLogout(); }}>{loggingOut ? '로그아웃 중…' : '로그아웃'}</button></span> : <a className="account" href="#/login" aria-current={route === '/login' ? 'page' : undefined}>로그인</a>}</nav>
    </div>
    {menuOpen && <nav id="mobile-menu" className="menu-panel" aria-label="전체 메뉴">{navigation.map(link)}{member ? <span className="auth-account"><span className="auth-account__name">{member.nickname || member.loginId}님</span><button className="account" type="button" disabled={loggingOut} onClick={() => { setMenuOpen(false); onLogout(); }}>{loggingOut ? '로그아웃 중…' : '로그아웃'}</button></span> : <a className="nav-link" href="#/login" aria-current={route === '/login' ? 'page' : undefined} onClick={() => setMenuOpen(false)}>로그인</a>}</nav>}
  </header>;
}
