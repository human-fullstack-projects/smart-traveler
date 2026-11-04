import { useState } from 'react';
import './AiCoursePage.css';

function ChatIcon() {
  return (
    <span className="ac-icon" aria-hidden="true">
      <svg width="52" height="40" viewBox="0 0 52 40" fill="none">
        <path d="M4 4h32a4 4 0 0 1 4 4v16a4 4 0 0 1-4 4H16l-9 8v-8H4a4 4 0 0 1-4-4V8a4 4 0 0 1 4-4Z" stroke="#1c2b33" strokeWidth="2.4" strokeLinejoin="round" />
      </svg>
      <svg className="ac-icon-bubble" width="30" height="24" viewBox="0 0 30 24" fill="none">
        <path d="M4 2h22a4 4 0 0 1 4 4v6a4 4 0 0 1-4 4H13l-6 5v-5H4a4 4 0 0 1-4-4V6a4 4 0 0 1 4-4Z" fill="#bfe7da" />
        <circle cx="10" cy="9" r="1.4" fill="#0c806b" />
        <circle cx="15" cy="9" r="1.4" fill="#0c806b" />
        <circle cx="20" cy="9" r="1.4" fill="#0c806b" />
      </svg>
    </span>
  );
}

function SendIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <path d="M2 10 18 2 13 18l-3.5-6.5L2 10Z" stroke="#0c806b" strokeWidth="1.6" strokeLinejoin="round" strokeLinecap="round" />
    </svg>
  );
}

function BulbIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <circle cx="10" cy="10" r="8" stroke="#0c806b" strokeWidth="1.4" strokeDasharray="2 3" />
      <path d="M10 6v3M10 2v1M10 17v1M14.5 10l1-.3M4.5 10.3l1 .3" stroke="#0c806b" strokeWidth="1.4" strokeLinecap="round" />
    </svg>
  );
}

export default function AiCoursePage() {
  const [message, setMessage] = useState('');
  const [notice, setNotice] = useState('');

  function startConversation() {
    if (!message.trim()) {
      setNotice('여행 이야기를 입력해 주세요.');
      return;
    }
    setNotice('입력한 여행 이야기는 유지됩니다. 대화 추천 기능은 연결 준비 중입니다.');
  }

  return (
    <div className="ac-main">
      <div className="ac-step"><span className="ac-step-line" /><span className="ac-step-num">03</span><span className="ac-step-label">시작 방식 선택형</span><span className="ac-step-line" /></div>

      <h1 className="ac-title">취향은 당신이, 여행 코스는 <span>AI</span>가.</h1>
      <p className="ac-subtitle">새로운 코스를 찾고, 내 일정을 편하게 바꿔보세요.</p>

      <h2 className="ac-question">어떤 방식으로 여행을 시작할까요?</h2>

      <section className="ac-card" aria-labelledby="ac-card-title">
        <ChatIcon />
        <h3 id="ac-card-title">대화로 추천받기</h3>
        <p className="ac-card-desc">원하는 여행을 편하게 이야기해 주세요.</p>

        <form className="ac-form" onSubmit={(e) => { e.preventDefault(); startConversation(); }}>
          <label className="ac-visually-hidden" htmlFor="ac-message">여행 이야기</label>
          <div className="ac-input-row">
            <input
              id="ac-message"
              type="text"
              value={message}
              onChange={(e) => { setMessage(e.target.value); setNotice(''); }}
              placeholder="친구와 부산 1박 2일 여행을 가고 싶어요"
            />
            <button type="submit" className="ac-send" aria-label="전송"><SendIcon /></button>
          </div>

          <button type="submit" className="ac-cta">대화 시작하기 <span aria-hidden="true">→</span></button>

          {notice && <p role="status" className="ac-notice">{notice}</p>}
        </form>
      </section>

      <div className="ac-tip"><BulbIcon /><span>입력 방식은 달라도, 같은 여행 일정으로 이어져요.</span></div>
    </div>
  );
}
