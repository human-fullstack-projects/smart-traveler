import { useState } from 'react';
import '../../css/MainLayout.css';

const navigation = [
  { label: '여행 시작', href: '#/' },
  { label: '코스추천', href: '#/ai-course' },
  { label: '조건검색', href: '#/conditions' },
  { label: '숙소 찾기', href: '#/stays' },
  { label: '내 여행 일정', href: '#/my-courses' },
  { label: '휴먼 추천 코스', href: '#/human-courses' },
  { label: '마이페이지', href: '#/mypage' },
];

const footerLinks = ['내 여행 일정', '조건검색', '숙소 찾기', '내 휴먼 추천 코스', '마이페이지'];

export default function MainLayout({ children, member, loggingOut, onLogout, notice, onDismissNotice }) {
  const [globalNotice, setGlobalNotice] = useState('');
  const [menuOpen, setMenuOpen] = useState(false);

  const banner = notice || globalNotice;

  return (
    <div className="main-page">
      <header className="mp-topbar">
        <div className="mp-topbar-left">
          <button
            type="button"
            className="menu-toggle"
            aria-label={menuOpen ? '메뉴 닫기' : '메뉴 열기'}
            aria-expanded={menuOpen}
            aria-controls="mobile-menu"
            onClick={() => setMenuOpen(!menuOpen)}
          >
            <span /><span /><span />
          </button>
          <a className="mp-brand st-brand" href="#/main">
            SMART TRAVEL
          </a>
        </div>

        <div className="st-header-actions">
          <nav className="st-top-links" aria-label="주 메뉴">
            {navigation.map((item) => (
              <a key={item.label} href={item.href}>{item.label}</a>
            ))}
          </nav>

          {member ? (
            <span className="auth-account">
              <button
                type="button"
                className="mp-account"
                disabled={loggingOut}
                onClick={onLogout}
              >
                {loggingOut ? '로그아웃 중…' : '로그아웃'}
              </button>
            </span>
          ) : (
            <a className="mp-account" href="#/login">
              로그인
            </a>
          )}
        </div>

        {menuOpen && (
          <nav id="mobile-menu" className="menu-panel" aria-label="전체 메뉴">
            {navigation.map((item) => (
              <a key={item.label} href={item.href} onClick={() => setMenuOpen(false)}>{item.label}</a>
            ))}
          </nav>
        )}
      </header>

      {banner && (
        <div className="mp-global-notice" role="status">
          {banner}
          <button type="button" onClick={() => (notice ? onDismissNotice?.() : setGlobalNotice(''))} aria-label="알림 닫기">×</button>
        </div>
      )}

      <div className="mp-body">
        <div className="mp-content">
          <main className="mp-main">{children}</main>

          <footer className="mp-footer">
            <nav aria-label="바로가기 메뉴">
              {footerLinks.map((label, i) => (
                <span key={label} className="mp-footer-item">
                  <a href="#">{label}</a>
                  {i < footerLinks.length - 1 && <span className="mp-footer-sep" aria-hidden="true">|</span>}
                </span>
              ))}
            </nav>
            <p className="mp-footer-fine">smart travel · 지도형 ver9 화면 시안</p>
          </footer>
        </div>
      </div>
    </div>
  );
}
