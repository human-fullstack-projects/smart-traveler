import { useEffect, useRef, useState } from 'react';
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

import { chatApi } from './chatApi.js';
import TravelConditions from './TravelConditions.jsx';
import TourSearch from './TourSearch.jsx';
import { createEmptyConditions } from './travelConditions.js';
import StaySearchPage from './stays/search/StaySearchPage.jsx';
import StayBookingPage from './stays/booking/StayBookingPage.jsx';
import OwnerPage from './owner/OwnerPage.jsx';

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

// 한글 조합을 확정하는 Enter는 전송하지 않고, Shift+Enter는 줄바꿈에 사용한다.
function submitOnEnter(event) {
  if (event.key !== 'Enter' || event.shiftKey || event.ctrlKey || event.altKey || event.metaKey) return;
  if (event.nativeEvent.isComposing || event.nativeEvent.keyCode === 229) return;
  event.preventDefault();
  event.currentTarget.form?.requestSubmit();
}

/** AI가 추출한 여행 조건을 사용자가 확인한다. */
function ChatPage({ messages, onSend, conditions, conditionsConfirmed, onConditionsChange, onConfirmConditions, busy, saveStatus, onRetry }) {
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  async function submit(event) {
    event.preventDefault();
    const trimmed = message.trim();
    if (!trimmed) {
      setError('여행 이야기를 입력해 주세요.');
      return;
    }
    try { await onSend(trimmed); setMessage(''); setError(''); }
    catch { setError('전송하지 못했습니다. 서버와 OpenAI API 키를 확인해 주세요.'); }
  }

  return (
    <section className="chat-page" aria-labelledby="chat-title">
      <div className="chat-page-heading">
        <div>
          <span className="greeting-label">AI TRAVEL PLANNER</span>
          <h1 id="chat-title">여행 이야기를 들려주세요.</h1>
          <p>원하는 여행을 차근차근 함께 그려갈게요.</p>
        </div>
        <a href="#/" className="chat-home-link">여행 시작으로</a>
      </div>

      <div className="chat-layout">
      <div className="chat-window">
        <PanelHeader />
        <div className="chat-messages" aria-label="입력한 여행 이야기" aria-live="polite">
          {messages.length === 0 && (
            <p className="chat-empty">어떤 여행을 계획하고 있는지 이야기해 주세요.</p>
          )}
          {messages.map((item) => (
            <div className={`chat-message${item.role === 'USER' ? '' : ' assistant'}`} key={item.id}>
              <span className="visually-hidden">{item.role === 'USER' ? '나' : '안내'}: </span>{item.content}
            </div>
          ))}
        </div>
        <p className="chat-pending" role="status">
          {saveStatus === 'saving' ? '여행 조건을 저장하고 있습니다.' : saveStatus === 'error' ? '여행 조건을 저장하지 못했습니다. 다시 저장해 주세요.' : 'AI가 대화에서 여행 조건을 확인합니다. 지역 관광지도 함께 둘러볼 수 있어요.'}
          {saveStatus === 'error' && <button type="button" onClick={onRetry}>다시 저장</button>}
        </p>
        <form className="chat-send" onSubmit={submit}>
          <label className="visually-hidden" htmlFor="chat-message">여행 이야기 입력</label>
          <textarea
            id="chat-message"
            value={message}
            onKeyDown={submitOnEnter}
            onChange={(event) => { setMessage(event.target.value); setError(''); }}
            placeholder="여행에 대해 더 이야기해 주세요."
          />
          <div className="chat-send-bottom">
            <span>{error || '지역, 일정, 동행을 함께 알려주시면 좋아요.'}</span>
            <button className="primary" type="submit" disabled={busy}>보내기 <span aria-hidden="true">↗</span></button>
          </div>
        </form>
      </div>
      <fieldset className="condition-controls" disabled={busy}>
      <TravelConditions
        conditions={conditions}
        confirmed={conditionsConfirmed}
        onChange={onConditionsChange}
        onConfirm={onConfirmConditions}
      />
      <TourSearch conditions={conditions} onConditionsChange={onConditionsChange} />
      </fieldset>
      </div>
    </section>
  );
}

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
};

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
  '/stays': StaySearchPage,
  '/stays/booking': StayBookingPage,
  '/owner': OwnerPage,
};

export default function App() {
  const { member, authChecked, authCheckFailed, loggingOut, login, logout } = useAuth();
  const myPageStore = useMyPageStore();
  const humanCourses = useHumanCourses();
  const [route, setRoute] = useState(window.location.hash.slice(1) || '/');
  const [notice, setNotice] = useState('');

  const [messages, setMessages] = useState([]);
  const [conditions, setConditions] = useState(createEmptyConditions);
  const [conditionsConfirmed, setConditionsConfirmed] = useState(false);
  const [sessionId, setSessionId] = useState(null);
  const [busy, setBusy] = useState(false);
  const [chatLoading, setChatLoading] = useState(true);
  const [saveStatus, setSaveStatus] = useState('saved');
  const saveQueue = useRef(Promise.resolve());
  const saveVersion = useRef(0);
  const saveFailed = useRef(false);
  const currentConditions = useRef({ conditions: createEmptyConditions(), confirmed: false });

  useEffect(() => {
    if (authCheckFailed) setNotice('로그인 상태를 확인할 서버에 연결하지 못했어요. Spring Boot 실행 상태를 확인해주세요.');
  }, [authCheckFailed]);

  useEffect(() => {
    const update = () => { setRoute(window.location.hash.slice(1) || '/'); setNotice(''); window.scrollTo(0, 0); };
    window.addEventListener('hashchange', update);
    return () => window.removeEventListener('hashchange', update);
  }, []);

  useEffect(() => {
    let cancelled = false;
    const stored = window.localStorage.getItem('smart-travel-chat-session');
    if (!stored) { setChatLoading(false); return () => { cancelled = true; }; }
    chatApi.load(stored).then((chat) => {
      if (cancelled) return;
      const restored = chat.conditions ? JSON.parse(chat.conditions.conditionsJson) : createEmptyConditions();
      const confirmed = chat.conditions?.confirmed || false;
      setSessionId(chat.id); setMessages(chat.messages); setConditions(restored); setConditionsConfirmed(confirmed);
      currentConditions.current = { conditions: restored, confirmed };
    }).catch(() => {
      if (!cancelled) setNotice('저장된 대화를 불러오지 못했습니다. 서버 연결과 대화 세션을 확인해 주세요.');
    }).finally(() => { if (!cancelled) setChatLoading(false); });
    return () => { cancelled = true; };
  }, []);

  async function startChat(message) {
    setBusy(true);
    try {
      // 앞선 조건 저장이 끝난 다음 새 대화를 시작해 서로 섞이지 않게 한다.
      await saveQueue.current;
      const chat = await chatApi.create(message);
      window.localStorage.setItem('smart-travel-chat-session', String(chat.id));
      const next = chat.conditions ? JSON.parse(chat.conditions.conditionsJson) : createEmptyConditions();
      const confirmed = chat.conditions?.confirmed || false;
      setSessionId(chat.id); setMessages(chat.messages); setConditions(next); setConditionsConfirmed(confirmed);
      currentConditions.current = { conditions: next, confirmed };
      saveFailed.current = false; setSaveStatus('saved'); window.location.hash = '/chat';
    } finally { setBusy(false); }
  }

  async function sendMessage(message) {
    if (!sessionId) throw new Error('대화 세션이 없습니다.');
    setBusy(true);
    try {
      // 앞서 수정한 조건의 저장이 끝난 다음 서버에서 메시지를 처리한다.
      await saveQueue.current;
      if (saveFailed.current) throw new Error('조건 저장이 필요합니다.');
      const chat = await chatApi.send(sessionId, message);
      const next = chat.conditions ? JSON.parse(chat.conditions.conditionsJson) : createEmptyConditions();
      const confirmed = chat.conditions?.confirmed || false;
      setMessages(chat.messages); setConditions(next); setConditionsConfirmed(confirmed);
      currentConditions.current = { conditions: next, confirmed };
    } finally { setBusy(false); }
  }

  function queueConditions(next, confirmed) {
    currentConditions.current = { conditions: next, confirmed };
    setConditions(next); setConditionsConfirmed(confirmed);
    if (!sessionId) { setSaveStatus('error'); return; }
    const version = ++saveVersion.current;
    setSaveStatus('saving');
    // 요청 순서를 보장해 이전 입력이 나중 입력을 덮어쓰지 않도록 한다.
    saveQueue.current = saveQueue.current.catch(() => {}).then(() => chatApi.saveConditions(sessionId, next, confirmed))
      .then(() => { if (version === saveVersion.current) { saveFailed.current = false; setSaveStatus('saved'); } })
      .catch(() => { if (version === saveVersion.current) { saveFailed.current = true; setSaveStatus('error'); } });
  }

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

  const routePath = (route.split('?')[0].replace(/\/$/, '')) || '/';
  const MyPageScreen = myPageRoutes[routePath];
  const isHumanCourse = routePath === '/human-courses' || routePath.startsWith('/human-courses/');
  const courseMatch = routePath.match(/^\/human-courses\/([^/]+)(\/edit)?$/);
  const HumanCourseScreen = humanCourseRoutes[routePath] || (courseMatch ? (courseMatch[2] ? HumanCourseEdit : HumanCourseDetail) : null);
  const isChat = routePath === '/chat';

  let content;
  if (MyPageScreen) {
    content = <>
      <div className="mypage"><MyPageMenu currentPage={routePath.slice(1)} /><p className="note">샘플 데이터로 동작합니다. 실제 예약·환불·AI 추천·공개 게시는 처리되지 않습니다.</p>{myPageStore.storageError && <p className="warning" role="alert">{myPageStore.storageError}</p>}</div>
      <MyPageScreen key={routePath} {...myPageStore} />
    </>;
  } else if (isHumanCourse) {
    content = <>
      {humanCourses.storageNotice && <div className="human-courses"><p className="note" role="alert">{humanCourses.storageNotice}</p></div>}
      {HumanCourseScreen ? <HumanCourseScreen key={routePath} {...humanCourses} courseId={courseMatch?.[1]} /> : <div className="human-courses"><section className="panel"><h1>페이지를 찾을 수 없습니다.</h1><a className="button" href="#/human-courses">코스 목록으로</a></section></div>}
    </>;
  } else if (isChat) {
    content = chatLoading
      ? <p role="status">대화를 불러오는 중입니다.</p>
      : !sessionId
        ? <p role="status">대화를 불러올 수 없습니다. <a href="#/">여행 시작으로 돌아가기</a></p>
        : <ChatPage
            messages={messages}
            onSend={sendMessage}
            conditions={conditions}
            conditionsConfirmed={conditionsConfirmed}
            onConditionsChange={(next) => queueConditions(next, false)}
            onConfirmConditions={() => queueConditions(currentConditions.current.conditions, true)}
            busy={busy}
            saveStatus={saveStatus}
            onRetry={() => queueConditions(currentConditions.current.conditions, currentConditions.current.confirmed)}
          />;
  } else {
    const SidebarScreen = sidebarPageRoutes[routePath] || HomePage;
    content = <SidebarScreen key={routePath} onLogin={loggedIn} onStart={startChat} busy={busy} />;
  }

  return (
    <MainLayout member={member} loggingOut={loggingOut} onLogout={handleLogout} notice={notice} onDismissNotice={() => setNotice('')}>
      {content}
    </MainLayout>
  );
}
