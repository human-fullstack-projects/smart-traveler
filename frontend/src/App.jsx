import { useEffect, useState } from 'react';
import Home, { Intro, PanelHeader } from './pages/Home';

const navigation = [
  { label: '여행 시작', href: '#/' },
  { label: '조건검색', href: '#/conditions' },
  { label: '숙소 찾기' },
  { label: '내 여행 일정' },
  { label: '마이페이지' },
];

function Header({ route, onUnavailable }) {
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
      <nav className="desktop-nav" aria-label="주 메뉴">{navigation.map(link)}<button className="account" onClick={() => onUnavailable('로그인')}>로그인</button></nav>
    </div>
    {menuOpen && <nav id="mobile-menu" className="menu-panel" aria-label="전체 메뉴">{navigation.map(link)}<button className="nav-link" onClick={() => { setMenuOpen(false); onUnavailable('로그인'); }}>로그인</button></nav>}
  </header>;
}


function Conditions() {
  const [notice, setNotice] = useState('');
  return <div className="start-layout"><Intro conditions /><section className="conversation" aria-labelledby="condition-title"><PanelHeader conditions /><div className="greeting condition-greeting"><span className="greeting-label">YOUR TRAVEL PLAN</span><h2 id="condition-title">여행의 시작을 정해볼까요?</h2><p>지역과 일정, 함께할 사람을 선택해 주세요.</p></div><form className="condition-form" onSubmit={(e) => { e.preventDefault(); setNotice('입력한 조건은 유지됩니다. 코스 추천 기능은 연결 준비 중입니다.'); }}><div className="fields"><label>여행 지역<input name="region" placeholder="예: 부산" required /></label><label>출발 날짜<input name="date" type="date" required /></label><label>여행 기간<select name="duration" defaultValue="1박 2일"><option>당일</option><option>1박 2일</option><option>2박 3일</option><option>3박 4일</option></select></label><label>동행 유형<select name="companion" defaultValue="친구와 함께"><option>혼자</option><option>친구와 함께</option><option>연인과 함께</option><option>가족과 함께</option></select></label><label>교통수단<select name="transport" defaultValue="대중교통"><option>대중교통</option><option>자동차</option></select></label><label>인원<input name="people" type="number" min="1" defaultValue="2" required /></label></div><button className="primary condition-submit" type="submit">추천 코스 보기 <span aria-hidden="true">↗</span></button>{notice && <p role="status" className="form-notice">{notice}</p>}</form></section></div>;
}

export default function App() {
  const [route, setRoute] = useState(window.location.hash.slice(1) || '/');
  const [notice, setNotice] = useState('');
  useEffect(() => { const update = () => { setRoute(window.location.hash.slice(1) || '/'); setNotice(''); window.scrollTo(0, 0); }; window.addEventListener('hashchange', update); return () => window.removeEventListener('hashchange', update); }, []);
  const isConditions = route === '/conditions';
  return <div className="app-shell"><Header route={route} onUnavailable={(label) => setNotice(`${label} 페이지는 연결 준비 중입니다.`)} />{notice && <div className="navigation-notice" role="status">{notice}<button onClick={() => setNotice('')} aria-label="알림 닫기">×</button></div>}<main className="main-content">{isConditions ? <Conditions /> : <Home />}</main><footer className="footer"><a href="#/">smart <strong>travel</strong></a><span>당신의 여행을 함께 그려요.</span></footer></div>;
}
