import { useEffect, useRef, useState } from 'react';
import { chatApi } from './chatApi.js';
import TravelConditions from './TravelConditions.jsx';
import TourSearch from './TourSearch.jsx';
import { createEmptyConditions } from './travelConditions.js';

const navigation = [
  { label: '여행 시작', href: '#/' },
  { label: '조건검색', href: '#/conditions' },
  { label: '숙소 찾기' },
  { label: '내 여행 일정' },
  { label: '마이페이지' },
];

// 한글 조합을 확정하는 Enter는 전송하지 않고, Shift+Enter는 줄바꿈에 사용한다.
function submitOnEnter(event) {
  if (event.key !== 'Enter' || event.shiftKey || event.ctrlKey || event.altKey || event.metaKey) return;
  if (event.nativeEvent.isComposing || event.nativeEvent.keyCode === 229) return;
  event.preventDefault();
  event.currentTarget.form?.requestSubmit();
}

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

function Intro({ conditions = false }) {
  return <section className="intro">
    <div className="kicker"><span />{conditions ? 'PLAN YOUR TRIP' : 'AI TRAVEL PLANNER'}</div>
    {conditions ? <><h1>원하는 조건으로<br /><em>여행을 그려요.</em></h1><p>여행지와 일정을 직접 선택해 주세요.<br />선택한 조건에 맞는 여행을 찾아드릴게요.</p></> : <><h1>당신의 이야기가<br /><em>여행이 되는 곳.</em></h1><p>가고 싶은 곳부터 떠오르는 장면까지,<br />편하게 들려주세요. 함께 여행을 그려볼게요.</p></>}
    <div className="intro-divider" />
    <div className="intro-caption"><strong>{conditions ? '어떤 여행을 생각하고 있나요?' : '어디로 떠나볼까요?'}</strong><span>{conditions ? '조건은 다음 단계에서도 변경할 수 있어요.' : '지역, 일정, 동행을 알려주면 더 좋아요.'}</span></div>
  </section>;
}

function PanelHeader({ conditions = false }) {
  return <div className="panel-header"><div className="assistant-mark" aria-hidden="true">✳</div><div className="panel-title"><strong>{conditions ? '조건검색' : '여행 플래너'}</strong><span>{conditions ? '내게 맞는 여행을 찾아보세요' : '당신만의 여행을 함께 만들어요'}</span></div><span className="status-dot">{conditions ? '여행 계획' : '대화 준비'}</span></div>;
}

function Home({ onStart, busy }) {
  const [message, setMessage] = useState('');
  const [notice, setNotice] = useState('');
  async function submit(event) {
    event.preventDefault();
    if (!message.trim()) { setNotice('여행 이야기를 입력해 주세요.'); return; }
    try { await onStart(message.trim()); } catch { setNotice('대화를 시작하지 못했습니다. 서버와 OpenAI API 키를 확인해 주세요.'); }
  }
  return <div className="start-layout"><Intro /><section className="conversation" aria-labelledby="greeting-title"><PanelHeader /><div className="greeting"><span className="greeting-label">SMART TRAVEL</span><h2 id="greeting-title">안녕하세요!<br />어떤 여행을 계획하고 있나요?</h2><p>정해진 게 없어도 괜찮아요.<br />생각나는 것부터 이야기해 주세요.</p></div><form className="composer" onSubmit={submit}><label className="visually-hidden" htmlFor="travel-story">여행 이야기</label><textarea id="travel-story" value={message} onKeyDown={submitOnEnter} onChange={(e) => { setMessage(e.target.value); setNotice(''); }} placeholder="예: 친구와 부산으로 1박 2일 여행을 가고 싶어요." /><div className="composer-bottom"><span>원하는 분위기나 장소도 함께 적어보세요</span><button type="submit" className="primary" disabled={busy}>대화 시작하기 <span aria-hidden="true">↗</span></button></div>{notice && <p role="status" className="form-notice">{notice}</p>}</form></section></div>;
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

export default function App() {
  const [route, setRoute] = useState(window.location.hash.slice(1) || '/');
  const [notice, setNotice] = useState('');
  const [messages, setMessages] = useState([]);
  const [conditions, setConditions] = useState(createEmptyConditions);
  const [conditionsConfirmed, setConditionsConfirmed] = useState(false);
  const [sessionId, setSessionId] = useState(null);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saveStatus, setSaveStatus] = useState('saved');
  const saveQueue = useRef(Promise.resolve());
  const saveVersion = useRef(0);
  const saveFailed = useRef(false);
  const currentConditions = useRef({ conditions: createEmptyConditions(), confirmed: false });

  useEffect(() => {
    const update = () => { setRoute(window.location.hash.slice(1) || '/'); setNotice(''); window.scrollTo(0, 0); };
    window.addEventListener('hashchange', update); return () => window.removeEventListener('hashchange', update);
  }, []);

  useEffect(() => {
    let cancelled = false;
    const stored = window.localStorage.getItem('smart-travel-chat-session');
    if (!stored) { setLoading(false); return () => { cancelled = true; }; }
    chatApi.load(stored).then((chat) => {
      if (cancelled) return;
      const restored = chat.conditions ? JSON.parse(chat.conditions.conditionsJson) : createEmptyConditions();
      const confirmed = chat.conditions?.confirmed || false;
      setSessionId(chat.id); setMessages(chat.messages); setConditions(restored); setConditionsConfirmed(confirmed);
      currentConditions.current = { conditions: restored, confirmed };
    }).catch(() => {
      if (!cancelled) setNotice('저장된 대화를 불러오지 못했습니다. 서버 연결과 대화 세션을 확인해 주세요.');
    }).finally(() => { if (!cancelled) setLoading(false); });
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

  const isConditions = route === '/conditions';
  return <div className="app-shell"><Header route={route} onUnavailable={(label) => setNotice(`${label} 페이지는 연결 준비 중입니다.`)} />{notice && <div className="navigation-notice" role="status">{notice}<button onClick={() => setNotice('')} aria-label="알림 닫기">×</button></div>}<main className="main-content">{isConditions ? <Conditions /> : route === '/chat' ? loading ? <p role="status">대화를 불러오는 중입니다.</p> : !sessionId ? <p role="status">대화를 불러올 수 없습니다. <a href="#/">여행 시작으로 돌아가기</a></p> : <ChatPage messages={messages} onSend={sendMessage} conditions={conditions} conditionsConfirmed={conditionsConfirmed} onConditionsChange={(next) => queueConditions(next, false)} onConfirmConditions={() => queueConditions(currentConditions.current.conditions, true)} busy={busy} saveStatus={saveStatus} onRetry={() => queueConditions(currentConditions.current.conditions, currentConditions.current.confirmed)} /> : <Home onStart={startChat} busy={busy || loading} />}</main><footer className="footer"><a href="#/">smart <strong>travel</strong></a><span>당신의 여행을 함께 그려요.</span></footer></div>;
}
