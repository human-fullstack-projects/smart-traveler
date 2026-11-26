import { useEffect, useRef, useState } from 'react';
import { settingsApi } from '../../pages/mypage/api/settingsApi';
import useMemberResource from '../../pages/mypage/hooks/useMemberResource';
import '../../css/MainLayout.css';

const navigationItems = [
  { label: '여행 생성', href: '#/chat' },
  { label: '숙소 검색', href: '#/stays' },
  { label: '저장된 여행', href: '#/my-courses' },
  { label: '여행자추천트립', href: '#/human-courses' },
  { label: '마이트립페이지', href: '#/mypage' },
];

const accountLinks = [
  { label: '내 예약', href: '#/reservations' },
  { label: '내 여행 스타일', href: '#/my-travel-style' },
  { label: '내 여행 추천', href: '#/my-posts' },
];

export default function SiteHeader({ member, loggingOut, onLogout }) {
  const navigation = navigationItems.map((item) =>
    member?.roleCode === 'OWNER' && item.href === '#/mypage'
      ? { label: '업주 센터', href: '#/owner', className: 'header-owner-dashboard' }
      : item
  );
  const [menuOpen, setMenuOpen] = useState(false);
  const account = useRef(null);
  const toggle = useRef(null);
  const accountName = member?.loginId || member?.nickname || '내 계정';
  const profileMemberId = member?.roleCode === 'USER' && member.avatarData === undefined ? member.memberId : null;
  const profile = useMemberResource(profileMemberId, settingsApi.profile);
  const avatarData = member?.avatarData === undefined ? profile.data?.avatarData : member.avatarData;

  useEffect(() => {
    if (!menuOpen) return;
    const closeMenu = () => setMenuOpen(false);
    const onPointerDown = event => {
      if (!account.current?.contains(event.target)) closeMenu();
    };
    const onKeyDown = event => {
      if (event.key !== 'Escape') return;
      closeMenu();
      toggle.current?.focus();
    };
    document.addEventListener('pointerdown', onPointerDown);
    document.addEventListener('keydown', onKeyDown);
    window.addEventListener('hashchange', closeMenu);
    return () => {
      document.removeEventListener('pointerdown', onPointerDown);
      document.removeEventListener('keydown', onKeyDown);
      window.removeEventListener('hashchange', closeMenu);
    };
  }, [menuOpen]);

  const renderLink = item => (
    <a key={item.href} href={item.href} className={item.className} onClick={() => setMenuOpen(false)}>{item.label}</a>
  );

  return (
    <>
    <header className="mp-topbar">
      <div className="mp-topbar-left">
        <a className="mp-brand st-brand brand-mark" href="#/main" aria-label="AI TRIP 홈">
          <img className="brand-logo" src="/images/brand/ait_pin.svg" alt="" /><span className="brand-word"><b>AI</b> TRIP</span>
        </a>
      </div>

      <div className="st-header-actions">
        <nav className="st-top-links" aria-label="주 메뉴">
          {navigation.map((item) => (
            <a key={item.label} href={item.href} className={item.className}>{item.label}</a>
          ))}
        </nav>

        {!member && <a className="mp-account header-desktop-login" href="#/login">로그인</a>}
        <div className={member ? 'header-account' : 'header-account header-account-guest'} ref={account}>
          <button
            ref={toggle}
            type="button"
            className="mp-account"
            aria-label={member ? `${accountName} 계정 메뉴` : '전체 메뉴'}
            aria-expanded={menuOpen}
            aria-controls="header-account-menu"
            onClick={() => setMenuOpen(open => !open)}
          >
            {member && <span className="header-account-avatar" aria-hidden="true">
              {avatarData ? <img src={avatarData} alt="" /> : Array.from(accountName)[0]}
            </span>}
            <span className="header-account-label">{member ? accountName : '메뉴'}</span>
            <span className="header-account-bars" aria-hidden="true" />
          </button>
          <div id="header-account-menu" className="header-account-panel" hidden={!menuOpen}>
            {member && <a className="header-account-profile" href="#/my-profile" onClick={() => setMenuOpen(false)}>
              <span>{accountName}</span><span aria-hidden="true">›</span>
            </a>}
            <nav className="header-account-mobile-nav" aria-label="주 메뉴">{navigation.map(renderLink)}</nav>
            {member ? <>
              <nav className="header-account-links" aria-label="내 계정">
                {accountLinks.map(renderLink)}
                {member.roleCode === 'ADMIN' && (
                  <a href="#/admin" onClick={() => setMenuOpen(false)}>관리자 페이지</a>
                )}
              </nav>
              <button className="header-account-logout" type="button" disabled={loggingOut} onClick={onLogout}>
                {loggingOut ? '로그아웃 중…' : '로그아웃'}
              </button>
            </> : <a href="#/login" onClick={() => setMenuOpen(false)}>로그인</a>}
          </div>
        </div>
      </div>
    </header>
    {member?.roleCode === 'ADMIN' && (
      <p className="admin-readonly-banner" role="status">
        관리자 계정으로 둘러보는 중이에요 · 예약·결제, 휴먼 추천 코스 좋아요·댓글·신고·글쓰기, 코스 저장, 취향 저장은 할 수 없어요.
      </p>
    )}
    </>
  );
}
