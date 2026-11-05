import { useState } from 'react';
import '../../css/MainLayout.css';

const sidebarMenu = [
  { key: 'schedule', label: '내 여행 일정', icon: CalendarIcon, subLinks: [{ label: '예약 내역', href: '#/reservations' }, { label: '저장한 일정', href: '#/my-courses' }] },
  { key: 'stay', label: '숙소 찾기', icon: LodgeIcon, href: '#/stays' },
  { key: 'owner', label: '업주센터', icon: UserIcon, href: '#/owner' },
  { key: 'human-course', label: '휴먼 추천 코스', icon: MapPinIcon, href: '#/human-courses' },
  { key: 'mypage', label: '마이페이지', icon: UserIcon, href: '#/mypage' },
];

function PlusIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M8 2v12M2 8h12" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  );
}

function CalendarIcon() {
  return (
    <svg width="17" height="17" viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <rect x="2.5" y="4" width="15" height="13.5" rx="2.4" stroke="currentColor" strokeWidth="1.5" />
      <path d="M2.5 8h15M6.5 2.5v3M13.5 2.5v3" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  );
}

function LodgeIcon() {
  return (
    <svg width="17" height="17" viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <path d="M2.5 9.5 10 3l7.5 6.5" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round" />
      <path d="M4 8.5v8h12v-8" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round" />
      <path d="M8 16.5v-4.5h4v4.5" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round" />
    </svg>
  );
}

function MapPinIcon() {
  return (
    <svg width="17" height="17" viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <path d="M10 18s6-5.6 6-10.4A6 6 0 0 0 4 7.6C4 12.4 10 18 10 18Z" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round" />
      <circle cx="10" cy="7.6" r="2.1" stroke="currentColor" strokeWidth="1.5" />
    </svg>
  );
}

function UserIcon() {
  return (
    <svg width="17" height="17" viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <circle cx="10" cy="6.5" r="3.3" stroke="currentColor" strokeWidth="1.5" />
      <path d="M3.5 17c1.2-3.6 4-5.3 6.5-5.3S15.3 13.4 16.5 17" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  );
}

function HelpIcon() {
  return (
    <svg width="17" height="17" viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <circle cx="10" cy="10" r="7.5" stroke="currentColor" strokeWidth="1.5" />
      <path d="M7.8 7.7a2.2 2.2 0 1 1 3.3 1.9c-.9.5-1.1.9-1.1 1.9" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
      <circle cx="10" cy="14.2" r=".9" fill="currentColor" />
    </svg>
  );
}

const footerLinks = ['내 여행 일정', '조건검색', '숙소 찾기', '내 휴먼 추천 코스', '마이페이지'];

export default function MainLayout({ children, member, loggingOut, onLogout, onNewTrip, notice, onDismissNotice }) {
  // 기존: 사이드바를 처음부터 표시
  // const [sidebarOpen, setSidebarOpen] = useState(true);

  // 변경: 메뉴 버튼을 눌렀을 때 표시
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [activeMenu, setActiveMenu] = useState(null);
  const [globalNotice, setGlobalNotice] = useState('');

  function toggleMenu(key) {
    setActiveMenu((prev) => (prev === key ? null : key));
  }

  function handleNewTrip() {
    setActiveMenu(null);
    setGlobalNotice('');
    (onNewTrip || (() => { window.location.hash = '/'; }))();
  }

  const banner = notice || globalNotice;

  return (
    <div className="main-page">
      {/* <header className="mp-topbar">
        <div className="mp-topbar-left">
          <button type="button" className="mp-menu-toggle" aria-label={sidebarOpen ? '메뉴 닫기' : '메뉴 열기'} aria-expanded={sidebarOpen} onClick={() => setSidebarOpen(!sidebarOpen)}>
            <span /><span /><span />
          </button>
          <a className="mp-brand" href="#/main">smart <span>travel</span></a>
        </div>
        {member ? (
          <span className="auth-account">
            <span className="auth-account__name">{member.nickname || member.loginId}님</span>
            <button type="button" className="mp-account" disabled={loggingOut} onClick={onLogout}>{loggingOut ? '로그아웃 중…' : '로그아웃'}</button>
          </span>
        ) : (
          <a className="mp-account" href="#/login">로그인</a>
        )}
      </header> */}
      <header className="mp-topbar">
  <div className="mp-topbar-left">
    <button
      type="button"
      className="mp-menu-toggle"
      aria-label={sidebarOpen ? '메뉴 닫기' : '메뉴 열기'}
      aria-expanded={sidebarOpen}
      onClick={() => setSidebarOpen(!sidebarOpen)}
    >
      <span />
      <span />
      <span />
    </button>

    <a className="mp-brand st-brand" href="#/main">
      <svg
        width="42"
        height="30"
        viewBox="0 0 42 30"
        fill="none"
        aria-hidden="true"
      >
        <path
          d="M3 25L14 14L22 20L34 7"
          stroke="currentColor"
          strokeWidth="5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <circle cx="39" cy="3" r="3" fill="currentColor" />
      </svg>
      SMART TRAVEL
    </a>
  </div>

  <div className="st-header-actions">
     {/* <nav className="st-top-links" aria-label="주요 메뉴">
      <a href="#/human-courses">둘러보기</a>
      <a href="#/stays">숙소</a>
      <a href="#/mypage">내 여행</a> 
    </nav> */}

    {member ? (
      <span className="auth-account">
        {/* <span className="auth-account__name">
          {member.nickname || member.loginId}님
        </span> */}
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
</header>

      {banner && (
        <div className="mp-global-notice" role="status">
          {banner}
          <button type="button" onClick={() => (notice ? onDismissNotice?.() : setGlobalNotice(''))} aria-label="알림 닫기">×</button>
        </div>
      )}

      <div className="mp-body">
                {sidebarOpen && (
          <button
            type="button"
            className="mp-sidebar-backdrop"
            aria-label="메뉴 닫기"
            onClick={() => setSidebarOpen(false)}
          />
        )}
        <aside
          className={`mp-sidebar${sidebarOpen ? '' : ' mp-sidebar-closed'}`}
          aria-hidden={!sidebarOpen}
          inert={!sidebarOpen}
        >
          <div className="mp-sidebar-inner">
            <button type="button" className="mp-new-trip" onClick={handleNewTrip}>
              <PlusIcon /> 새 여행 만들기
            </button>
            <nav className="mp-sidebar-nav" aria-label="사이드 메뉴">
              {sidebarMenu.map(({ key, label, icon: Icon, href, subLinks }) => (
                <div key={key} className="mp-sidebar-item">
                  {href ? (
                    <a className="mp-sidebar-link" href={href} onClick={() => setActiveMenu(null)}>
                      <Icon /> {label}
                    </a>
                  ) : (
                    <button type="button" className="mp-sidebar-link" aria-expanded={activeMenu === key} onClick={() => toggleMenu(key)}>
                      <Icon /> {label}
                    </button>
                  )}
                  {activeMenu === key && (
                    <div className="mp-sidebar-submenu">
                      {subLinks.map((sub) => typeof sub === 'string' ? (
                        <button key={sub} type="button" onClick={() => setGlobalNotice(`${sub} 페이지는 연결 준비 중입니다.`)}>{sub}</button>
                      ) : (
                        <a key={sub.label} href={sub.href} onClick={() => setActiveMenu(null)}>{sub.label}</a>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </nav>
            <div className="mp-sidebar-bottom">
              <button type="button" className="mp-sidebar-link" onClick={() => setGlobalNotice('이용 안내 페이지는 연결 준비 중입니다.')}>
                <HelpIcon /> 이용 안내
              </button>
            </div>
          </div>
        </aside>

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
