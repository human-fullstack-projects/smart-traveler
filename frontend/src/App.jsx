import { useEffect, useRef, useState } from 'react';
import Home, { Intro, PanelHeader } from './pages/Home';
import Login from './pages/Login';
import Signup from './pages/Signup';
import AdminApp, { AdminGuard } from './pages/admin/AdminApp';
import { getCurrentMember, logout } from './api/auth';
import AiCoursePage from './ai-course/AiCoursePage.jsx';
import SearchPage from './search/SearchPage.jsx';
import MainPage from './main/MainPage.jsx';
import StaySearchPage from './stays/search/StaySearchPage.jsx';
import StayBookingPage from './stays/booking/StayBookingPage.jsx';
import OwnerPage from './owner/OwnerPage.jsx';

import Header from './common_components/Header';
import Footer from './common_components/Footer';
import MyPageMenu from './mypage/components/MyPageMenu';
import MyPage from './mypage/pages/MyPage';
import Reservations from './mypage/pages/Reservations';
import MyCourses from './mypage/pages/MyCourses';
import MyPosts from './mypage/pages/MyPosts';
import useMyPageStore from './mypage/hooks/useMyPageStore';
import HumanCourseList from './human_course/pages/HumanCourseList';
import HumanCourseDetail from './human_course/pages/HumanCourseDetail';
import HumanCourseCreate from './human_course/pages/HumanCourseCreate';
import HumanCourseEdit from './human_course/pages/HumanCourseEdit';
import HumanCourseModeration from './human_course/pages/HumanCourseModeration';
import useHumanCourses from './human_course/hooks/useHumanCourses';

const myPageRoutes = {
  '/mypage': MyPage,
  '/reservations': Reservations,
  '/my-courses': MyCourses,
  '/my-posts': MyPosts,
};

const humanCourseRoutes = {
  '/human-courses': HumanCourseList,
  '/human-courses/create': HumanCourseCreate,
  '/human-courses/moderation': HumanCourseModeration,
};

// function Intro({ conditions = false }) {
//   return <section className="intro">
//     <div className="kicker"><span />{conditions ? 'PLAN YOUR TRIP' : 'AI TRAVEL PLANNER'}</div>
//     {conditions ? <><h1>원하는 조건으로<br /><em>여행을 그려요.</em></h1><p>여행지와 일정을 직접 선택해 주세요.<br />선택한 조건에 맞는 여행을 찾아드릴게요.</p></> : <><h1>당신의 이야기가<br /><em>여행이 되는 곳.</em></h1><p>가고 싶은 곳부터 떠오르는 장면까지,<br />편하게 들려주세요. 함께 여행을 그려볼게요.</p></>}
//     <div className="intro-divider" />
//     <div className="intro-caption"><strong>{conditions ? '어떤 여행을 생각하고 있나요?' : '어디로 떠나볼까요?'}</strong><span>{conditions ? '조건은 다음 단계에서도 변경할 수 있어요.' : '지역, 일정, 동행을 알려주면 더 좋아요.'}</span></div>
//   </section>;
// }

// function PanelHeader({ conditions = false }) {
//   return <div className="panel-header"><div className="assistant-mark" aria-hidden="true">✳</div><div className="panel-title"><strong>{conditions ? '조건검색' : '여행 플래너'}</strong><span>{conditions ? '내게 맞는 여행을 찾아보세요' : '당신만의 여행을 함께 만들어요'}</span></div><span className="status-dot">{conditions ? '여행 계획' : '대화 준비'}</span></div>;
// }

// function Home() {
//   const [message, setMessage] = useState('');
//   const [notice, setNotice] = useState('');
//   function submit(event) {
//     event.preventDefault();
//     if (!message.trim()) { setNotice('여행 이야기를 입력해 주세요.'); return; }
//     setNotice('입력한 여행 이야기는 유지됩니다. 대화 추천 기능은 연결 준비 중입니다.');
//   }
//   return <div className="start-layout"><Intro /><section className="conversation" aria-labelledby="greeting-title"><PanelHeader /><div className="greeting"><span className="greeting-label">SMART TRAVEL</span><h2 id="greeting-title">안녕하세요!<br />어떤 여행을 계획하고 있나요?</h2><p>정해진 게 없어도 괜찮아요.<br />생각나는 것부터 이야기해 주세요.</p></div><form className="composer" onSubmit={submit}><label className="visually-hidden" htmlFor="travel-story">여행 이야기</label><textarea id="travel-story" value={message} onChange={(e) => { setMessage(e.target.value); setNotice(''); }} placeholder="예: 친구와 부산으로 1박 2일 여행을 가고 싶어요." /><div className="composer-bottom"><span>원하는 분위기나 장소도 함께 적어보세요</span><button type="submit" className="primary">대화 시작하기 <span aria-hidden="true">↗</span></button></div>{notice && <p role="status" className="form-notice">{notice}</p>}</form></section></div>;
// }

function Conditions() {
  const [notice, setNotice] = useState('');
  return <div className="start-layout"><Intro conditions /><section className="conversation" aria-labelledby="condition-title"><PanelHeader conditions /><div className="greeting condition-greeting"><span className="greeting-label">YOUR TRAVEL PLAN</span><h2 id="condition-title">여행의 시작을 정해볼까요?</h2><p>지역과 일정, 함께할 사람을 선택해 주세요.</p></div><form className="condition-form" onSubmit={(e) => { e.preventDefault(); setNotice('입력한 조건은 유지됩니다. 코스 추천 기능은 연결 준비 중입니다.'); }}><div className="fields"><label>여행 지역<input name="region" placeholder="예: 부산" required /></label><label>출발 날짜<input name="date" type="date" required /></label><label>여행 기간<select name="duration" defaultValue="1박 2일"><option>당일</option><option>1박 2일</option><option>2박 3일</option><option>3박 4일</option></select></label><label>동행 유형<select name="companion" defaultValue="친구와 함께"><option>혼자</option><option>친구와 함께</option><option>연인과 함께</option><option>가족과 함께</option></select></label><label>교통수단<select name="transport" defaultValue="대중교통"><option>대중교통</option><option>자동차</option></select></label><label>인원<input name="people" type="number" min="1" defaultValue="2" required /></label></div><button className="primary condition-submit" type="submit">추천 코스 보기 <span aria-hidden="true">↗</span></button>{notice && <p role="status" className="form-notice">{notice}</p>}</form></section></div>;
}

export default function App() {
  const myPageStore = useMyPageStore();
  const humanCourses = useHumanCourses();
  const [member, setMember] = useState(null);
  const [authChecked, setAuthChecked] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const authVersion = useRef(0);
  const logoutInFlight = useRef(false);
  useEffect(() => {
    let cancelled = false;
    const version = authVersion.current;
    getCurrentMember().then((value) => {
      if (!cancelled && version === authVersion.current) { setMember(value); setAuthChecked(true); }
    }).catch(() => {
      if (!cancelled) setAuthChecked(true);
      if (!cancelled && version === authVersion.current) setNotice('로그인 상태를 확인할 서버에 연결하지 못했어요. Spring Boot 실행 상태를 확인해주세요.');
    });
    return () => { cancelled = true; };
  }, []);

  function loggedIn(value) {
    authVersion.current += 1;
    setMember(value);
    setNotice('');
    setAuthChecked(true);
    window.location.hash = value?.roleCode === 'ADMIN' ? '/admin' : '/';
  }

  async function handleLogout() {
    if (logoutInFlight.current) return;
    logoutInFlight.current = true;
    setLoggingOut(true);
    authVersion.current += 1;
    try {
      await logout();
      setMember(null);
      setNotice('로그아웃했어요.');
      if (window.location.hash.startsWith('#/admin')) window.location.hash = '/';
    } catch (error) {
      setNotice(error.message);
    } finally {
      logoutInFlight.current = false;
      setLoggingOut(false);
    }
  }

  const [route, setRoute] = useState(window.location.hash.slice(1) || '/');
  const [notice, setNotice] = useState('');
  useEffect(() => { const update = () => { setRoute(window.location.hash.slice(1) || '/'); setNotice(''); window.scrollTo(0, 0); }; window.addEventListener('hashchange', update); return () => window.removeEventListener('hashchange', update); }, []);
  const routePath = route.split('?')[0];
  const MyPageScreen = myPageRoutes[routePath];
  const humanCoursePath = routePath.replace(/\/$/, '');
  const isHumanCourse = humanCoursePath === '/human-courses' || humanCoursePath.startsWith('/human-courses/');
  const courseMatch = humanCoursePath.match(/^\/human-courses\/([^/]+)(\/edit)?$/);
  const HumanCourseScreen = humanCourseRoutes[humanCoursePath] || (courseMatch ? (courseMatch[2] ? HumanCourseEdit : HumanCourseDetail) : null);
  const isConditions = routePath === '/conditions';
  const isLogin = routePath === '/login';
  const isSignup = routePath === '/signup';
  const isAiCourse = routePath === '/ai-course';
  const isSearch = routePath === '/search';
  const isMain = routePath === '/main';
  const isStaySearch = routePath === '/stays';
  const isStayBooking = routePath === '/stays/booking';
  const isOwner = routePath === '/owner';
  if (routePath === '/admin' || routePath.startsWith('/admin/')) {
    if (member?.roleCode === 'ADMIN') return <AdminApp route={routePath} member={member} onLogout={handleLogout} />;
    return <AdminGuard member={member} checking={!authChecked} />;
  }
  if (isAiCourse) return <AiCoursePage />;
  if (isSearch) return <SearchPage />;
  if (isMain) return <MainPage />;
  if (isStaySearch) return <StaySearchPage />;
  if (isStayBooking) return <StayBookingPage />;
  if (isOwner) return <OwnerPage />;
  return <div className="app-shell"><Header route={routePath} member={member} onLogout={handleLogout} loggingOut={loggingOut} onUnavailable={(label) => setNotice(`${label} 페이지는 연결 준비 중입니다.`)} />{notice && <div className="navigation-notice" role="status">{notice}<button onClick={() => setNotice('')} aria-label="알림 닫기">×</button></div>}<main className="main-content">{MyPageScreen ? <>
      <div className="mypage"><MyPageMenu currentPage={routePath.slice(1)} /><p className="note">샘플 데이터로 동작합니다. 실제 예약·환불·AI 추천·공개 게시는 처리되지 않습니다.</p>{myPageStore.storageError && <p className="warning" role="alert">{myPageStore.storageError}</p>}</div>
      <MyPageScreen key={routePath} {...myPageStore} />
    </> : isHumanCourse ? <>
      {humanCourses.storageNotice && <div className="human-courses"><p className="note" role="alert">{humanCourses.storageNotice}</p></div>}
      {HumanCourseScreen ? <HumanCourseScreen key={humanCoursePath} {...humanCourses} courseId={courseMatch?.[1]} /> : <div className="human-courses"><section className="panel"><h1>페이지를 찾을 수 없습니다.</h1><a className="button" href="#/human-courses">코스 목록으로</a></section></div>}
    </> : isLogin ? <Login onLogin={loggedIn} /> : isSignup ? <Signup /> : isConditions ? <Conditions /> : <Home />}</main><Footer /></div>;
}
