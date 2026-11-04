import './stay-shared.css';

export function StayHeader({ active = 'search' }) {
  return (
    <header className="stay-header">
      <div className="stay-header__inner">
        <a className="stay-brand" href="#/">smart <span>travel</span></a>
        <nav className="stay-nav" aria-label="숙소 메뉴">
          <a href="#/stays" aria-current={active === 'search' ? 'page' : undefined}>숙소 검색</a>
          <a href="#/stays/booking" aria-current={active === 'booking' ? 'page' : undefined}>예약</a>
          <a href="#/owner">업주센터</a>
          <a className="stay-nav__home" href="#/">여행 홈</a>
        </nav>
      </div>
    </header>
  );
}

export function StayFooter() {
  return (
    <footer className="stay-footer">
      <a href="#/">smart <strong>travel</strong></a>
      <span>판매 가능 여부와 최종 금액은 예약 요청 시 다시 확인합니다.</span>
    </footer>
  );
}

export function StayArtwork({ theme, label, compact = false }) {
  return (
    <div className={`stay-art stay-art--${theme}${compact ? ' stay-art--compact' : ''}`} role="img" aria-label={label}>
      <span className="stay-art__sun" />
      <span className="stay-art__ridge stay-art__ridge--back" />
      <span className="stay-art__ridge stay-art__ridge--front" />
      <span className="stay-art__building"><i /><i /><i /></span>
    </div>
  );
}

export function StatusPill({ tone = 'neutral', children }) {
  return <span className={`stay-status stay-status--${tone}`}>{children}</span>;
}
