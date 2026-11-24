import { useEffect, useRef, useState } from 'react';
import HomePage, { Intro, PanelHeader } from './pages/home/HomePage';
import LoginPage from './pages/auth/LoginPage';
import SignupPage from './pages/auth/SignupPage';
import SocialSignupPage from './pages/auth/SocialSignupPage';
import { createReturnPage } from './pages/auth/returnPage';
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
  const { member, authChecked, authCheckFailed, loggingOut, authRevision, retryAuth, login, logout, guestCourse } = useAuth();
  const myPageStore = useMyPageStore({ member, authChecked, authCheckFailed, authRevision });
  const humanCourses = useHumanCourses(member);
  //window.location.hash : 현재 url의 해시부분 (예:#/mypage)
  const [route, setRoute] = useState(window.location.hash.slice(1) || '/');
  const returnPage = useRef(null);
  if (!returnPage.current) returnPage.current = createReturnPage(route);
  const [notice, setNotice] = useState('');
  const [survey, setSurvey] = useState({ memberId: null, status: 'idle', data: null });
  const chat = useAiChat({ ownerKey: member?.memberId ? `member:${member.memberId}` : 'guest', ready: authChecked && !authCheckFailed, authRevision });
  // 로그인·회원가입 화면에 '비로그인 상태로' 들어온 주소. 이 화면 안에서 로그인한 경우(업주 안내 화면 등)는 이동시키지 않는다.
  const guestAuthRoute = useRef(null);
  const isAuthRoute = Boolean(topNavPageRoutes[route]);
  if (isAuthRoute && authChecked && !member) guestAuthRoute.current = route;
  // 이미 로그인한 상태로 로그인·회원가입 주소에 들어오면 홈으로 보낸다.
  const redirectLoggedIn = isAuthRoute && authChecked && Boolean(member) && guestAuthRoute.current !== route;

  useEffect(() => {
    const memberId = member?.memberId;
    if (!authChecked || authCheckFailed || member?.roleCode !== 'USER' || !memberId) {
      setSurvey({ memberId: null, status: 'idle', data: null });
      return;
    }
    let cancelled = false;
    setSurvey({ memberId, status: 'loading', data: null });
    settingsApi.preferences(memberId).then(data => {
      if (cancelled || data.memberId !== memberId) return;
      setSurvey({ memberId, status: surveyDone(data.tags) ? 'done' : 'needed', data });
    }).catch(() => { if (!cancelled) setSurvey({ memberId, status: 'error', data: null }); });
    return () => { cancelled = true; };
  }, [authChecked, authCheckFailed, member?.memberId, member?.roleCode, authRevision]);

  useEffect(() => {
    if (member && /^\/reservations(?:\/|$)/.test(route.split('?')[0])) myPageStore.retry();
  }, [member?.memberId, route, myPageStore.retry]);

  useEffect(() => {
    if (authCheckFailed) setNotice('로그인 상태를 확인할 서버에 연결하지 못했어요. Spring Boot 실행 상태를 확인해주세요.');
  }, [authCheckFailed]);

  useEffect(() => {
    const update = () => {
      const next = window.location.hash.slice(1) || '/';
      guestAuthRoute.current = null;
      returnPage.current.remember(next);
      setRoute(next); setNotice(''); window.scrollTo(0, 0);
    };
    window.addEventListener('hashchange', update);
    return () => window.removeEventListener('hashchange', update);
  }, []);

  useEffect(() => {
    // replace: 뒤로가기 때 로그인 주소로 돌아가 다시 이동되는 반복을 막는다.
    if (redirectLoggedIn) window.location.replace('#/');
  }, [redirectLoggedIn]);

  function loggedIn(value) {
    login(value);
    setNotice('');
    window.location.hash = value.roleCode === 'OWNER' ? '/owner' : returnPage.current.get();
  }

  async function handleLogout() {
    try {
      await logout();
      setNotice('로그아웃했어요.');
      if (window.location.hash.startsWith('#/admin') || window.location.hash.startsWith('#/owner')) window.location.hash = '/';
    } catch (error) {
      setNotice(error.message);
    }
  }

  if (route === '/admin' || route.startsWith('/admin/')) {
    return <>
      {member?.roleCode === 'ADMIN'
        ? <AdminApp route={route} member={member} onLogout={handleLogout} />
        : <AdminGuard member={member} checking={!authChecked} />}
      <SiteFooter />
    </>;
  }

  if (route === '/owner' || route === '/owner/reapply') {
    return (
      <>
        <OwnerAccess isReapplyPage={route === '/owner/reapply'}>
          <OwnerPage member={member} onLogout={handleLogout} loggingOut={loggingOut} />
        </OwnerAccess>
        <SiteFooter />
      </>
    );
  }

  const TopNavScreen = topNavPageRoutes[route];
  if (TopNavScreen) {
    return <div className="app-shell">
      <SiteHeader member={member} onLogout={handleLogout} loggingOut={loggingOut} />
      {notice && <div className="navigation-notice" role="status">{notice}<button onClick={() => setNotice('')} aria-label="알림 닫기">×</button></div>}
      <main className="main-content">{!authChecked || redirectLoggedIn
        ? <p className="auth-checking" role="status">로그인 상태를 확인하고 있어요.</p>
        : <TopNavScreen onLogin={loggedIn} guestCourseCount={myPageStore.isGuest ? myPageStore.courses.length + (guestCourse ? 1 : 0) : 0} />}</main>
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
      <div className="mypage">
        {member && myPageStore.pendingCount > 0 && <p role="status">회원 연결을 기다리는 임시 코스가 {myPageStore.pendingCount}개 있습니다. 이 페이지를 새로고침하지 말고 다시 시도해 주세요.</p>}
        {member && myPageStore.error && <p className="warning" role="alert">{myPageStore.error} <button type="button" onClick={myPageStore.retry}>다시 불러오기·연결</button></p>}
      </div>
      {blocked ? <AccessNotice kind={blocked} onRetry={blocked === 'unavailable' ? retryAuth : undefined} /> : needsTravelData && ['checking', 'loading'].includes(myPageStore.status)
        ? <p role="status">내 자료를 불러오고 있습니다.</p>
        : needsTravelData && myPageStore.status === 'error' ? <p role="alert">자료를 불러오지 못했습니다. 위의 다시 불러오기 버튼으로 재시도해 주세요.</p>
        : <MyPageScreen key={`${member?.memberId || 'guest'}-${routePath}`} {...myPageStore} guestRecommendedCourse={guestCourse} courseId={myCourseMatch?.[1]} reservationId={reservationMatch?.[1]} postId={myPostMatch?.[1]}
            onSaved={routePath === '/my-travel-style' ? () => setSurvey({ memberId: member.memberId, status: 'done', data: null }) : undefined} />}
    </>;
  } else if (routePath === '/payments/demo' || new URLSearchParams(window.location.search).has('tossDemo')) {
    content = <TossDemoPage />;
  } else if (paymentMatch) {
    content = member ? <TossCheckoutPage key={routePath} reservationId={paymentMatch[1]} result={paymentMatch[2]} /> : <AccessNotice kind="login" />;
  } else if (isHumanCourse) {
    content = <>
      <div className="human-courses storage-notice">
        {humanCourses.loading && <p className="note" role="status">여행 게시글을 불러오고 있습니다.</p>}
        {!humanCourses.serverMode && !humanCourses.loading && <p className="note" role="alert">서버에 연결하지 못해 샘플 데이터로 시연 중입니다. {humanCourses.serverError} <button type="button" onClick={humanCourses.refresh}>서버 다시 연결</button></p>}
        {humanCourses.serverMode && humanCourses.serverError && <p className="note" role="alert">서버 자료를 다시 불러오지 못했습니다. {humanCourses.serverError} <button type="button" onClick={humanCourses.refresh}>다시 시도</button></p>}
      </div>
      {humanCourses.storageNotice && <div className="human-courses storage-notice"><p className="note" role="alert">{humanCourses.storageNotice}</p>{humanCourses.saveFailed && <button type="button" onClick={humanCourses.retrySave}>브라우저 저장 다시 시도</button>}</div>}
      {(routePath === '/human-courses/create' || Boolean(courseMatch?.[2])) && !member
        ? <AccessNotice kind="login" />
        : HumanCourseScreen ? <HumanCourseScreen key={route} {...humanCourses} courseId={courseMatch?.[1]}
          member={member}
          isAuthenticated={Boolean(member)}
          reservationId={new URLSearchParams(route.split('?')[1] || '').get('reservationId') || ''}
          from={new URLSearchParams(route.split('?')[1] || '').get('from') || ''}
          execute={async action => { const result = await humanCourses.execute(action); if (!result?.error && ['create', 'edit', 'delete'].includes(action.type)) await myPageStore.retry(); return result; }} /> : <div className="human-courses"><section className="panel"><h1>페이지를 찾을 수 없습니다.</h1><a className="button" href="#/human-courses">여행 목록으로</a></section></div>}
    </>;
  } else if (isChat) {
    content = <AiChatPage {...chat} />;
  } else {
    const SidebarScreen = sidebarPageRoutes[routePath] || HomePage;
         content = (
      <SidebarScreen key={routePath === '/stays/booking' ? route : routePath}//undefined
        // key={routePath}
        onLogin={loggedIn}
        onStart={chat.startChat}
        busy={chat.busy}
        reviewKey={`${member?.memberId ?? 'guest'}-${authRevision}`}
      />
    );
  }

  return (
    <MainLayout member={member} loggingOut={loggingOut} onLogout={handleLogout} notice={notice} onDismissNotice={() => setNotice('')}
      pastelBackground={Boolean(MyPageScreen) || routePath === '/stays' || routePath === '/stays/booking'
        || routePath === '/human-courses' || HumanCourseScreen === HumanCourseDetail}
      isHome={(routePath === '/' || routePath === '/main') && !(authChecked && member?.roleCode === 'USER' && routePath !== '/my-travel-style' && survey.memberId === member.memberId && survey.status === 'needed')}>
      {authChecked && member?.roleCode === 'USER' && routePath !== '/my-travel-style' && survey.memberId === member.memberId && survey.status === 'needed'
        ? <TravelStylePage key={`survey-${member.memberId}`} member={member} surveyMode initialData={survey.data}
            onSaved={() => setSurvey({ memberId: member.memberId, status: 'done', data: null })} />
        : content}
    </MainLayout>
  );
}
