import { useEffect, useState } from 'react';
import HomePage, { Intro, PanelHeader } from './pages/home/HomePage';
import LoginPage from './pages/auth/LoginPage';
import SignupPage from './pages/auth/SignupPage';
import AdminApp, { AdminGuard } from './pages/admin/AdminApp';
import AiCoursePage from './pages/ai-course/AiCoursePage';
import SearchPage from './pages/search/SearchPage';
import MainPage from './pages/main/MainPage';
import SiteHeader from './components/layout/SiteHeader';
import SiteFooter from './components/layout/SiteFooter';
import MainLayout from './components/layout/MainLayout';
import { useAuth } from './contexts/AuthContext';

function Conditions() {
  const [notice, setNotice] = useState('');
  return <div className="start-layout"><Intro conditions /><section className="conversation" aria-labelledby="condition-title"><PanelHeader conditions /><div className="greeting condition-greeting"><span className="greeting-label">YOUR TRAVEL PLAN</span><h2 id="condition-title">여행의 시작을 정해볼까요?</h2><p>지역과 일정, 함께할 사람을 선택해 주세요.</p></div><form className="condition-form" onSubmit={(e) => { e.preventDefault(); setNotice('입력한 조건은 유지됩니다. 코스 추천 기능은 연결 준비 중입니다.'); }}><div className="fields"><label>여행 지역<input name="region" placeholder="예: 부산" required /></label><label>출발 날짜<input name="date" type="date" required /></label><label>여행 기간<select name="duration" defaultValue="1박 2일"><option>당일</option><option>1박 2일</option><option>2박 3일</option><option>3박 4일</option></select></label><label>동행 유형<select name="companion" defaultValue="친구와 함께"><option>혼자</option><option>친구와 함께</option><option>연인과 함께</option><option>가족과 함께</option></select></label><label>교통수단<select name="transport" defaultValue="대중교통"><option>대중교통</option><option>자동차</option></select></label><label>인원<input name="people" type="number" min="1" defaultValue="2" required /></label></div><button className="primary condition-submit" type="submit">추천 코스 보기 <span aria-hidden="true">↗</span></button>{notice && <p role="status" className="form-notice">{notice}</p>}</form></section></div>;
}

const topNavPageRoutes = {
  '/login': LoginPage,
  '/signup': SignupPage,
};

const sidebarPageRoutes = {
  '/': MainPage,
  '/main': MainPage,
  '/conditions': Conditions,
  '/ai-course': AiCoursePage,
  '/search': SearchPage,
};

export default function App() {
  const { member, authChecked, authCheckFailed, loggingOut, login, logout } = useAuth();
  const [route, setRoute] = useState(window.location.hash.slice(1) || '/');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    if (authCheckFailed) setNotice('로그인 상태를 확인할 서버에 연결하지 못했어요. Spring Boot 실행 상태를 확인해주세요.');
  }, [authCheckFailed]);

  useEffect(() => {
    const update = () => { setRoute(window.location.hash.slice(1) || '/'); setNotice(''); window.scrollTo(0, 0); };
    window.addEventListener('hashchange', update);
    return () => window.removeEventListener('hashchange', update);
  }, []);

  function loggedIn(value) {
    login(value);
    setNotice('');
    window.location.hash = value?.roleCode === 'ADMIN' ? '/admin' : '/';
  }

  async function handleLogout() {
    try {
      await logout();
      setNotice('로그아웃했어요.');
      if (window.location.hash.startsWith('#/admin')) window.location.hash = '/';
    } catch (error) {
      setNotice(error.message);
    }
  }

  if (route === '/admin' || route.startsWith('/admin/')) {
    if (member?.roleCode === 'ADMIN') return <AdminApp route={route} member={member} onLogout={handleLogout} />;
    return <AdminGuard member={member} checking={!authChecked} />;
  }

  const TopNavScreen = topNavPageRoutes[route];
  if (TopNavScreen) {
    return <div className="app-shell">
      <SiteHeader route={route} member={member} onLogout={handleLogout} loggingOut={loggingOut} onUnavailable={(label) => setNotice(`${label} 페이지는 연결 준비 중입니다.`)} />
      {notice && <div className="navigation-notice" role="status">{notice}<button onClick={() => setNotice('')} aria-label="알림 닫기">×</button></div>}
      <main className="main-content"><TopNavScreen onLogin={loggedIn} /></main>
      <SiteFooter />
    </div>;
  }

  const SidebarScreen = sidebarPageRoutes[route] || HomePage;
  return (
    <MainLayout member={member} loggingOut={loggingOut} onLogout={handleLogout} notice={notice} onDismissNotice={() => setNotice('')}>
      <SidebarScreen key={route} onLogin={loggedIn} />
    </MainLayout>
  );
}
