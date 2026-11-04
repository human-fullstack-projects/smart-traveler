import { useEffect, useRef, useState } from 'react';
import './Home.css';

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
  const [step, setStep] = useState('compose');
  const stepTitle = useRef(null);
  const input = useRef(null);
  const previousStep = useRef(step);

  useEffect(() => {
    if (previousStep.current !== step) {
      if (step === 'compose') input.current?.focus();
      else stepTitle.current?.focus();
      previousStep.current = step;
    }
  }, [step]);

  function submit(event) {
    event.preventDefault();
    if (!message.trim()) {
      setNotice('여행 이야기를 입력해 주세요.');
      input.current?.focus();
      return;
    }
    setNotice('');
    setStep('choose');
  }

  return (
    <div className="start-layout">
      <Intro />
      <section className="conversation home-planner" aria-labelledby="greeting-title">
        <PanelHeader />
        {step === 'compose' ? (
          <>
            <div className="greeting">
              <span className="greeting-label">SMART TRAVEL</span>
              <h2 id="greeting-title">안녕하세요!<br />어떤 여행을 계획하고 있나요?</h2>
              <p>정해진 게 없어도 괜찮아요.<br />생각나는 것부터 이야기해 주세요.</p>
            </div>
            <form className="composer" onSubmit={submit}>
              <label className="visually-hidden" htmlFor="travel-story">여행 이야기</label>
              <textarea
                ref={input}
                id="travel-story"
                value={message}
                aria-invalid={Boolean(notice)}
                aria-describedby={notice ? 'home-story-error' : undefined}
                onChange={(event) => { setMessage(event.target.value); setNotice(''); }}
                placeholder="예: 친구와 부산으로 1박 2일 여행을 가고 싶어요."
              />
              <div className="composer-bottom">
                <span>원하는 분위기나 장소도 함께 적어보세요</span>
                <button type="submit" className="primary">대화 시작하기 <span aria-hidden="true">↗</span></button>
              </div>
              {notice && <p id="home-story-error" role="alert" className="form-notice">{notice}</p>}
            </form>
          </>
        ) : (
          <>
            <div className="greeting">
              <span className="greeting-label">{step === 'choose' ? 'CHOOSE YOUR WAY' : 'CHAT WITH AI'}</span>
              <h2 id="greeting-title" ref={stepTitle} tabIndex={-1}>
                {step === 'choose' ? '어떤 방식으로 추천받을까요?' : '대화로 추천받기'}
              </h2>
              <p>{step === 'choose' ? '편한 방식을 선택해서 여행 계획을 이어가세요.' : 'AI 대화 기능을 연결 준비 중이에요.'}</p>
            </div>
            <div className="home-story-summary">
              <strong>내가 입력한 여행 이야기</strong>
              <p>{message.trim()}</p>
            </div>
            {step === 'choose' ? (
              <>
                <div className="home-methods">
                  <button className="home-method" type="button" onClick={() => setStep('chat')}>
                    <span className="home-method__title">대화로 추천받기 <span aria-hidden="true">↗</span></span>
                    <span className="home-method__description">AI와 이야기하며 취향에 맞는 코스를 찾아요.</span>
                    <span className="home-method__status">AI 연결 준비 중</span>
                  </button>
                  <a className="home-method" href="#/conditions">
                    <span className="home-method__title">조건 선택으로 추천받기 <span aria-hidden="true">↗</span></span>
                    <span className="home-method__description">지역·날짜·동행 등을 직접 선택해요.</span>
                    <span className="home-method__status">조건 입력 화면으로 이동</span>
                  </a>
                </div>
                <p className="home-method-note">조건 선택 화면에는 여행 조건을 직접 입력해주세요. 입력한 문장을 자동으로 분석하는 기능은 아직 연결되지 않았어요.</p>
                <button className="home-back" type="button" onClick={() => setStep('compose')}>← 여행 이야기 수정하기</button>
              </>
            ) : (
              <div className="home-chat-pending">
                <p>지금은 실제 AI 답변이나 추천 코스를 제공하지 않아요. 조건 선택 화면에서 여행 조건을 먼저 입력할 수 있어요.</p>
                <a className="primary home-condition-link" href="#/conditions">조건 선택 화면으로 이동 ↗</a>
                <button className="home-back" type="button" onClick={() => setStep('choose')}>← 추천 방식 다시 선택</button>
              </div>
            )}
          </>
        )}
      </section>
    </div>
  );
}
