import { useState } from 'react';
import './main.css';

const navigation = [
  { label: '여행 시작', href: '#/' },
  { label: '코스추천', href: '#/ai-course' },
  { label: '조건검색', href: '#/search' },
  { label: '숙소 찾기' },
  { label: '내 여행 일정' },
  { label: '마이페이지' },
];

const footerLinks = ['내 여행 일정', '조건검색', '숙소 찾기', '내 휴먼 추천 코스', '마이페이지'];

function SparkleIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <path d="M10 2v5M10 13v5M2 10h5M13 10h5M4.5 4.5l3 3M12.5 12.5l3 3M15.5 4.5l-3 3M7.5 12.5l-3 3" stroke="#07836a" strokeWidth="1.6" strokeLinecap="round" />
    </svg>
  );
}

export default function MainPage() {
  const [menuOpen, setMenuOpen] = useState(false);
  const [message, setMessage] = useState('');
  const [notice, setNotice] = useState('');
  const [helpOpen, setHelpOpen] = useState(false);
  const userName = '홍길동';

  function submit(event) {
    event.preventDefault();
    if (!message.trim()) { setNotice('여행 이야기를 입력해 주세요.'); return; }
    setNotice('입력한 여행 이야기는 유지됩니다. 대화 추천 기능은 연결 준비 중입니다.');
  }

  return (
    <div className="main-page">
      <header className="mp-header">
        <div className="mp-header-inner">
          <div className="mp-brand-wrap">
            <button type="button" className="mp-menu-toggle" aria-label={menuOpen ? '메뉴 닫기' : '메뉴 열기'} aria-expanded={menuOpen} onClick={() => setMenuOpen(!menuOpen)}>
              <span /><span /><span />
            </button>
            <a className="mp-brand" href="#/">smart <span>travel</span></a>
          </div>
          <nav className="mp-desktop-nav" aria-label="주 메뉴">
            {navigation.map((item) => item.href
              ? <a key={item.label} href={item.href} aria-current={item.label === '여행 시작' ? 'page' : undefined}>{item.label}</a>
              : <button key={item.label} type="button" className="mp-nav-link">{item.label}</button>)}
            <button type="button" className="mp-account">{userName}님</button>
          </nav>
        </div>
        {menuOpen && (
          <nav className="mp-menu-panel" aria-label="전체 메뉴">
            {navigation.map((item) => item.href
              ? <a key={item.label} href={item.href} onClick={() => setMenuOpen(false)}>{item.label}</a>
              : <button key={item.label} type="button" className="mp-nav-link" onClick={() => setMenuOpen(false)}>{item.label}</button>)}
            <button type="button" className="mp-nav-link" onClick={() => setMenuOpen(false)}>{userName}님</button>
          </nav>
        )}
      </header>

      <main className="mp-main">
        <div className="mp-layout">
          <section className="mp-intro">
            <div className="mp-kicker"><span />AI TRAVEL PLANNER</div>
            <h1>당신의 이야기가<br /><em>여행이 되는 곳.</em></h1>
            <p>가고 싶은 곳부터 떠오르는 장면까지,<br />편하게 들려주세요. 함께 여행을 그려볼게요.</p>
            <div className="mp-divider" />
            <div className="mp-caption"><strong>어디로 떠나볼까요?</strong><span>지역, 일정, 동행을 알려주면 더 좋아요.</span></div>
          </section>

          <section className="mp-conversation" aria-labelledby="mp-greeting-title">
            <div className="mp-panel-header">
              <div className="mp-assistant-mark"><SparkleIcon /></div>
              <div className="mp-panel-title"><strong>여행 플래너</strong><span>당신만의 여행을 함께 만들어요</span></div>
              <span className="mp-status-dot">대화 준비</span>
            </div>

            <div className="mp-greeting">
              <span className="mp-greeting-label">SMART TRAVEL</span>
              <h2 id="mp-greeting-title">안녕하세요!<br />어떤 여행을 계획하고 있나요?</h2>
              <p>정해진 게 없어도 괜찮아요.<br />생각나는 것부터 이야기해 주세요.</p>
            </div>

            <form className="mp-composer" onSubmit={submit}>
              <label className="mp-visually-hidden" htmlFor="mp-story">여행 이야기</label>
              <textarea
                id="mp-story"
                value={message}
                onChange={(e) => { setMessage(e.target.value); setNotice(''); }}
                placeholder="예: 친구와 부산으로 1박 2일 여행을 가고 싶어요."
              />
              <div className="mp-composer-bottom">
                <span>원하는 분위기나 장소도 함께 적어보세요</span>
                <button type="submit" className="mp-primary">대화 시작하기 <span aria-hidden="true">↗</span></button>
              </div>
              {notice && <p role="status" className="mp-notice">{notice}</p>}
            </form>

            <button type="button" className="mp-help-toggle" aria-expanded={helpOpen} onClick={() => setHelpOpen(!helpOpen)}>
              <span className={`mp-help-arrow${helpOpen ? ' open' : ''}`} aria-hidden="true">▶</span> 대화 응답에 문제가 있나요?
            </button>
            {helpOpen && <p className="mp-help-body">잠시 후 다시 시도하거나, 더 구체적인 문장으로 이야기해 주세요. 문제가 계속되면 고객센터로 문의해 주세요.</p>}
          </section>
        </div>

        <p className="mp-sample-note">지도형 ver9 화면 샘플 · 대화·추출·재고·결제·환불은 샘플 동작이며 실제 서비스와 연결되지 않습니다.</p>
      </main>

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
  );
}
