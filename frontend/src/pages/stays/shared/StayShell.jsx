import '../../../css/stay-shared.css';

export function StayHeader({ active = 'search', searchHref = '#/stays', bookingHref = '#/stays/booking' }) {
  return (
    <header className="stay-header">
      <div className="stay-header__inner">
        <a className="stay-brand brand-mark is-small" href="#/" aria-label="AI TRIP 홈"><img className="brand-logo" src="/images/brand/ait_pin.svg" alt="" /><span className="brand-word"><b>AI</b> TRIP</span></a>
        <nav className="stay-nav" aria-label="숙소 메뉴">
          <a href={searchHref} aria-current={active === 'search' ? 'page' : undefined}>숙소 검색</a>
          <a href={bookingHref} aria-current={active === 'booking' ? 'page' : undefined}>예약</a>
        </nav>
      </div>
    </header>
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
