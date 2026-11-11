import { useEffect, useState } from 'react';
import HomePage, { Intro, PanelHeader } from './pages/home/HomePage';
import LoginPage from './pages/auth/LoginPage';
import SignupPage from './pages/auth/SignupPage';
import SocialSignupPage from './pages/auth/SocialSignupPage';
import AdminApp, { AdminGuard } from './pages/admin/AdminApp';
import AiCoursePage from './pages/ai-course/AiCoursePage';
import AiChatPage from './pages/ai-chat/AiChatPage';
import useAiChat from './pages/ai-chat/hooks/useAiChat';
import SearchPage from './pages/search/SearchPage';
import MainPage from './pages/main/MainPage';
import SiteHeader from './components/layout/SiteHeader';
import SiteFooter from './components/layout/SiteFooter';
import MainLayout from './components/layout/MainLayout';
import { useAuth } from './contexts/AuthContext';


import StaySearchPage from './pages/stays/search/StaySearchPage.jsx';
import StayBookingPage from './pages/stays/booking/StayBookingPage.jsx';
import TossCheckoutPage from './pages/payment/TossCheckoutPage.jsx';
import TossDemoPage from './pages/payment/TossDemoPage.jsx';
import OwnerPage from './pages/owner/OwnerPage.jsx';
import OwnerAccess from './pages/owner/OwnerAccess.jsx';

import MyPageMenu from './pages/mypage/components/MyPageMenu';
import MyPage from './pages/mypage/pages/MyPage';
import Reservations from './pages/mypage/pages/Reservations';
import MyCourses from './pages/mypage/pages/MyCourses';
import MyPosts from './pages/mypage/pages/MyPosts';
import ProfilePage from './pages/mypage/profile/ProfilePage';
import TravelStylePage from './pages/mypage/preferences/TravelStylePage';
import { settingsApi } from './pages/mypage/api/settingsApi';
import { surveyDone } from './pages/mypage/data/settings';
import useMyPageStore from './pages/mypage/hooks/useMyPageStore';
import AccessNotice from './pages/mypage/components/AccessNotice';
import { routeAccess } from './pages/mypage/data/store';
import HumanCourseList from './pages/human_course/pages/HumanCourseList';
import HumanCourseDetail from './pages/human_course/pages/HumanCourseDetail';
import HumanCourseCreate from './pages/human_course/pages/HumanCourseCreate';
import HumanCourseEdit from './pages/human_course/pages/HumanCourseEdit';
import HumanCourseModeration from './pages/human_course/pages/HumanCourseModeration';
import MapDemoPage from './pages/map/MapDemoPage';
import useHumanCourses from './pages/human_course/hooks/useHumanCourses';

function Conditions() {
  const [notice, setNotice] = useState('');
  return <div className="start-layout"><Intro conditions /><section className="conversation" aria-labelledby="condition-title"><PanelHeader conditions /><div className="greeting condition-greeting"><span className="greeting-label">YOUR TRAVEL PLAN</span><h2 id="condition-title">여행의 시작을 정해볼까요?</h2><p>지역과 일정, 함께할 사람을 선택해 주세요.</p></div><form className="condition-form" onSubmit={(e) => { e.preventDefault(); setNotice('입력한 조건은 유지됩니다. 코스 추천 기능은 연결 준비 중입니다.'); }}><div className="fields"><label>여행 지역<input name="region" placeholder="예: 부산" required /></label><label>출발 날짜<input name="date" type="date" required /></label><label>여행 기간<select name="duration" defaultValue="1박 2일"><option>당일</option><option>1박 2일</option><option>2박 3일</option><option>3박 4일</option></select></label><label>동행 유형<select name="companion" defaultValue="친구와 함께"><option>혼자</option><option>친구와 함께</option><option>연인과 함께</option><option>가족과 함께</option></select></label><label>교통수단<select name="transport" defaultValue="대중교통"><option>대중교통</option><option>자동차</option></select></label><label>인원<input name="people" type="number" min="1" defaultValue="2" required /></label></div><button className="primary condition-submit" type="submit">추천 코스 보기 <span aria-hidden="true">↗</span></button>{notice && <p role="status" className="form-notice">{notice}</p>}</form></section></div>;
}

const humanCourseRoutes = {
  '/human-courses': HumanCourseList,
  '/human-courses/create': HumanCourseCreate,
  '/human-courses/moderation': HumanCourseModeration,
};

const myPageRoutes = {
  '/mypage': MyPage,
  '/reservations': Reservations,
  '/my-courses': MyCourses,
  '/my-posts': MyPosts,
  '/my-profile': ProfilePage,
  '/my-travel-style': TravelStylePage,
};

const topNavPageRoutes = {
  '/login': LoginPage,
  '/signup': SignupPage,
  '/signup/social': SocialSignupPage,
};

const sidebarPageRoutes = {
  '/': MainPage,
  '/main': MainPage,
  '/conditions': Conditions,
  '/ai-course': AiCoursePage,
  '/search': SearchPage,
  '/stays': StaySearchPage,
  '/stays/booking': StayBookingPage,
  '/map-demo': MapDemoPage,
};

export default function App() {
  const { member, authChecked, authCheckFailed, loggingOut, authRevision, retryAuth, login, logout } = useAuth();
  const myPageStore = useMyPageStore({ member, authChecked, authCheckFailed, authRevision });
  const humanCourses = useHumanCourses(member);
  const [route, setRoute] = useState(window.location.hash.slice(1) || '/');
  const [notice, setNotice] = useState('');
  const [survey, setSurvey] = useState({ memberId: null, status: 'idle', data: null });
  const chat = useAiChat();

  useEffect(() => {
    const memberId = member?.memberId;
    if (!authChecked || authCheckFailed || member?.roleCode !== 'USER' || !memberId) {
      setSurvey({ memberId: null, status: 'idle', data: null });
      return;
    }
    let cancelled = false;
    let skipped = false;
    try { skipped = sessionStorage.getItem(`st-survey-skip-${memberId}`) === '1'; } catch { /* storage unavailable */ }
    if (skipped) { setSurvey({ memberId, status: 'skipped', data: null }); return; }
    setSurvey({ memberId, status: 'loading', data: null });
    settingsApi.preferences(memberId).then(data => {
      if (cancelled || data.memberId !== memberId) return;
      setSurvey({ memberId, status: surveyDone(data.tags) ? 'done' : 'needed', data });
    }).catch(() => { if (!cancelled) setSurvey({ memberId, status: 'error', data: null }); });
    return () => { cancelled = true; };
  }, [authChecked, authCheckFailed, member?.memberId, member?.roleCode, authRevision]);

  useEffect(() => {
    if (authCheckFailed) setNotice('로그인 상태를 확인할 서버에 연결하지 못했어요. Spring Boot 실행 상태를 확인해주세요.');
  }, [authCheckFailed]);

  useEffect(() => {
    const update = () => { setRoute(window.location.hash.slice(1) || '/'); setNotice(''); window.scrollTo(0, 0); };
    window.addEventListener('hashchange', update);
    return () => window.removeEventListener('hashchange', update);
  }, []);

  function loggedIn(value) {
    const hasGuestCourses = myPageStore.isGuest && myPageStore.courses.length > 0;
    try { sessionStorage.removeItem(`st-survey-skip-${value?.memberId}`); } catch { /* storage unavailable */ }
    login(value);
    setNotice('');
    window.location.hash = value?.roleCode === 'ADMIN' ? '/admin' : hasGuestCourses ? '/my-courses' : '/';
  }

  async function handleLogout() {
    try {
      await logout();
      try { sessionStorage.removeItem(`st-survey-skip-${member?.memberId}`); } catch { /* storage unavailable */ }
      setNotice('로그아웃했어요.');
      if (window.location.hash.startsWith('#/admin') || window.location.hash.startsWith('#/owner')) window.location.hash = '/';
    } catch (error) {
      setNotice(error.message);
    }
  }

  if (route === '/admin' || route.startsWith('/admin/')) {
    if (member?.roleCode === 'ADMIN') return <AdminApp route={route} member={member} onLogout={handleLogout} />;
    return <AdminGuard member={member} checking={!authChecked} />;
  }

  if (route === '/owner') {
  // return <OwnerPage member={member} onLogout={handleLogout} loggingOut={loggingOut} />;
  return (
    <OwnerAccess>
      <OwnerPage member={member} onLogout={handleLogout} loggingOut={loggingOut} />
    </OwnerAccess>
  );
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

  const routePath = (route.split('?')[0].replace(/\/$/, '')) || '/';
  const myCourseMatch = routePath.match(/^\/my-courses\/([^/]+)$/);
  const reservationMatch = routePath.match(/^\/reservations\/([^/]+)$/);
  const paymentMatch = routePath.match(/^\/payments\/(\d+)(?:\/(success|failure))?$/);
  const myPostMatch = routePath.match(/^\/my-posts\/([^/]+)$/);
  const MyPageScreen = myPageRoutes[routePath] || (myCourseMatch ? MyCourses : reservationMatch ? Reservations : myPostMatch ? MyPosts : null);
  const isHumanCourse = routePath === '/human-courses' || routePath.startsWith('/human-courses/');
  const courseMatch = routePath.match(/^\/human-courses\/([^/]+)(\/edit)?$/);
  const HumanCourseScreen = humanCourseRoutes[routePath] || (courseMatch ? (courseMatch[2] ? HumanCourseEdit : HumanCourseDetail) : null);
  const isChat = routePath === '/chat';

  let content;
  if (MyPageScreen) {
    const access = routeAccess(routePath, { member, authChecked, authCheckFailed });
    const needsTravelData = !['/mypage', '/my-profile', '/my-travel-style'].includes(routePath);
    const blocked = access !== 'allowed' ? access : needsTravelData && ['login', 'denied', 'unavailable'].includes(myPageStore.status) ? myPageStore.status : null;
    content = <>
      <div className="mypage"><MyPageMenu currentPage={myCourseMatch ? 'my-courses' : routePath.slice(1)} isGuest={!member} />
        {myPageStore.pendingCount > 0 && <p role="status">회원 연결을 기다리는 임시 코스가 {myPageStore.pendingCount}개 있습니다. 이 페이지를 새로고침하지 말고 다시 시도해 주세요.</p>}
        {myPageStore.error && <p className="warning" role="alert">{myPageStore.error} <button type="button" onClick={myPageStore.retry}>다시 불러오기·연결</button></p>}
      </div>
      {blocked ? <AccessNotice kind={blocked} onRetry={blocked === 'unavailable' ? retryAuth : undefined} /> : needsTravelData && ['checking', 'loading'].includes(myPageStore.status)
        ? <p role="status">내 자료를 불러오고 있습니다.</p>
        : needsTravelData && myPageStore.status === 'error' ? <p role="alert">자료를 불러오지 못했습니다. 위의 다시 불러오기 버튼으로 재시도해 주세요.</p>
        : <MyPageScreen key={`${member?.memberId || 'guest'}-${routePath}`} {...myPageStore} courseId={myCourseMatch?.[1]} reservationId={reservationMatch?.[1]} postId={myPostMatch?.[1]} />}
    </>;
  } else if (routePath === '/payments/demo' || new URLSearchParams(window.location.search).has('tossDemo')) {
    content = <TossDemoPage />;
  } else if (paymentMatch) {
    content = member ? <TossCheckoutPage key={routePath} reservationId={paymentMatch[1]} result={paymentMatch[2]} /> : <AccessNotice kind="login" />;
  } else if (isHumanCourse) {
    content = <>
      <div className="human-courses storage-notice">
        {humanCourses.loading && <p className="note" role="status">휴먼 코스를 서버에서 불러오고 있습니다.</p>}
        {!humanCourses.serverMode && !humanCourses.loading && <p className="note" role="alert">서버에 연결하지 못해 샘플 데이터로 시연 중입니다. {humanCourses.serverError} <button type="button" onClick={humanCourses.refresh}>서버 다시 연결</button></p>}
        {humanCourses.serverMode && humanCourses.serverError && <p className="note" role="alert">서버 자료를 다시 불러오지 못했습니다. {humanCourses.serverError} <button type="button" onClick={humanCourses.refresh}>다시 시도</button></p>}
      </div>
      {humanCourses.storageNotice && <div className="human-courses storage-notice"><p className="note" role="alert">{humanCourses.storageNotice}</p>{humanCourses.saveFailed && <button type="button" onClick={humanCourses.retrySave}>브라우저 저장 다시 시도</button>}</div>}
      {HumanCourseScreen ? <HumanCourseScreen key={routePath} {...humanCourses} courseId={courseMatch?.[1]} /> : <div className="human-courses"><section className="panel"><h1>페이지를 찾을 수 없습니다.</h1><a className="button" href="#/human-courses">코스 목록으로</a></section></div>}
    </>;
  } else if (isChat) {
    content = <AiChatPage {...chat} />;
  } else {
    const SidebarScreen = sidebarPageRoutes[routePath] || HomePage;
    content = <SidebarScreen key={routePath} onLogin={loggedIn} onStart={chat.startChat} busy={chat.busy} />;
  }

  return (
    <MainLayout member={member} loggingOut={loggingOut} onLogout={handleLogout} notice={notice} onDismissNotice={() => setNotice('')}>
      {authChecked && member?.roleCode === 'USER' && routePath !== '/my-travel-style' && survey.memberId === member.memberId && survey.status === 'needed'
        ? <TravelStylePage key={`survey-${member.memberId}`} member={member} surveyMode initialData={survey.data}
            onSkip={() => { try { sessionStorage.setItem(`st-survey-skip-${member.memberId}`, '1'); } catch { /* storage unavailable */ } setSurvey({ memberId: member.memberId, status: 'skipped', data: null }); }}
            onSaved={() => setSurvey({ memberId: member.memberId, status: 'done', data: null })} />
        : content}
    </MainLayout>
  );
}
