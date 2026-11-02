import { useState } from 'react';

export function Intro({ conditions = false }) {
  return <section className="intro">
    <div className="kicker"><span />{conditions ? 'PLAN YOUR TRIP' : 'AI TRAVEL PLANNER'}</div>
    {conditions ? <><h1>원하는 조건으로<br /><em>여행을 그려요.</em></h1><p>여행지와 일정을 직접 선택해 주세요.<br />선택한 조건에 맞는 여행을 찾아드릴게요.</p></> : <><h1>당신의 이야기가<br /><em>여행이 되는 곳.</em></h1><p>가고 싶은 곳부터 떠오르는 장면까지,<br />편하게 들려주세요. 함께 여행을 그려볼게요.</p></>}
    <div className="intro-divider" />
    <div className="intro-caption"><strong>{conditions ? '어떤 여행을 생각하고 있나요?' : '어디로 떠나볼까요?'}</strong><span>{conditions ? '조건은 다음 단계에서도 변경할 수 있어요.' : '지역, 일정, 동행을 알려주면 더 좋아요.'}</span></div>
  </section>;
}

export function PanelHeader({ conditions = false }) {
  return <div className="panel-header"><div className="assistant-mark" aria-hidden="true">✳</div><div className="panel-title"><strong>{conditions ? '조건검색' : '여행 플래너'}</strong><span>{conditions ? '내게 맞는 여행을 찾아보세요' : '당신만의 여행을 함께 만들어요'}</span></div><span className="status-dot">{conditions ? '여행 계획' : '대화 준비'}</span></div>;
}

export default function Home() {
  const [message, setMessage] = useState('');
  const [notice, setNotice] = useState('');
  function submit(event) {
    event.preventDefault();
    if (!message.trim()) { setNotice('여행 이야기를 입력해 주세요.'); return; }
    setNotice('입력한 여행 이야기는 유지됩니다. 대화 추천 기능은 연결 준비 중입니다.');
  }
  return <div className="start-layout"><Intro /><section className="conversation" aria-labelledby="greeting-title"><PanelHeader /><div className="greeting"><span className="greeting-label">SMART TRAVEL</span><h2 id="greeting-title">안녕하세요!<br />어떤 여행을 계획하고 있나요?</h2><p>정해진 게 없어도 괜찮아요.<br />생각나는 것부터 이야기해 주세요.</p></div><form className="composer" onSubmit={submit}><label className="visually-hidden" htmlFor="travel-story">여행 이야기</label><textarea id="travel-story" value={message} onChange={(e) => { setMessage(e.target.value); setNotice(''); }} placeholder="예: 친구와 부산으로 1박 2일 여행을 가고 싶어요." /><div className="composer-bottom"><span>원하는 분위기나 장소도 함께 적어보세요</span><button type="submit" className="primary">대화 시작하기 <span aria-hidden="true">↗</span></button></div>{notice && <p role="status" className="form-notice">{notice}</p>}</form></section></div>;
}