import { useState } from 'react';
import './MainPage.css';

function SparkleIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <path d="M10 2v5M10 13v5M2 10h5M13 10h5M4.5 4.5l3 3M12.5 12.5l3 3M15.5 4.5l-3 3M7.5 12.5l-3 3" stroke="#07836a" strokeWidth="1.6" strokeLinecap="round" />
    </svg>
  );
}

function FilterIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <line x1="3" y1="5" x2="17" y2="5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      <circle cx="8" cy="5" r="2" fill="#fff" stroke="currentColor" strokeWidth="1.6" />
      <line x1="3" y1="10" x2="17" y2="10" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      <circle cx="12.5" cy="10" r="2" fill="currentColor" />
      <line x1="3" y1="15" x2="17" y2="15" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      <circle cx="8" cy="15" r="2" fill="#fff" stroke="currentColor" strokeWidth="1.6" />
    </svg>
  );
}

function ForkIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <path d="M6 2.5v6a2 2 0 0 0 4 0v-6M8 8.5V17.5M14 2.5v6.3c0 1-.8 1.9-1.8 2L11.8 17.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function CompassIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <circle cx="10" cy="10" r="7.5" stroke="currentColor" strokeWidth="1.5" />
      <path d="M12.6 7.4 11 11l-3.6 1.6L9 9l3.6-1.6Z" stroke="currentColor" strokeWidth="1.3" strokeLinejoin="round" />
    </svg>
  );
}

function CalendarIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <rect x="2.5" y="4" width="15" height="13.5" rx="2.4" stroke="currentColor" strokeWidth="1.5" />
      <path d="M2.5 8h15M6.5 2.5v3M13.5 2.5v3" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  );
}

export default function MainPage() {
  const [message, setMessage] = useState('');
  const [notice, setNotice] = useState('');
  const [helpOpen, setHelpOpen] = useState(false);

  function submit(event) {
    event.preventDefault();
    if (!message.trim()) { setNotice('여행 이야기를 입력해 주세요.'); return; }
    setNotice('입력한 여행 이야기는 유지됩니다. 대화 추천 기능은 연결 준비 중입니다.');
  }

  return (
    <>
      <div className="mp-layout">
        <section className="mp-conversation" aria-labelledby="mp-greeting-title">
          <div className="mp-panel-header">
            <div className="mp-assistant-mark"><SparkleIcon /></div>
            <div className="mp-panel-title"><strong>여행 플래너</strong><span>당신만의 여행을 함께 만들어요</span></div>
            <span className="mp-status-dot">대화 준비</span>
          </div>

          <div className="mp-greeting">
            <span className="mp-greeting-label">SMART TRAVEL</span>
            <h2 id="mp-greeting-title">안녕하세요!<br />어떤 여행을 계획하고 있나요?</h2>
            <p>정해진 게 없어도 괜찮아요.<br />생각나는 것부터 이야기해 주세요.</p>
          </div>

          <form className="mp-composer" onSubmit={submit}>
            <label className="mp-visually-hidden" htmlFor="mp-story">여행 이야기</label>
            <textarea
              id="mp-story"
              value={message}
              onChange={(e) => { setMessage(e.target.value); setNotice(''); }}
              placeholder="어떤 여행을 떠나고 싶나요?"
            />
            <div className="mp-composer-bottom">
              <span>원하는 분위기나 장소도 함께 적어보세요</span>
              <button type="submit" className="mp-primary">대화 시작하기 <span aria-hidden="true">↗</span></button>
            </div>
            {notice && <p role="status" className="mp-notice">{notice}</p>}
          </form>

          <div className="mp-suggestions">
            <a className="mp-chip mp-chip-wide" href="#/search">
              <FilterIcon /> 조건 검색
            </a>
          </div>
          <div className="mp-suggestions mp-suggestions-row">
            <button type="button" className="mp-chip" onClick={() => setNotice('휴먼 코스 추천 1 페이지는 연결 준비 중입니다.')}>
              <ForkIcon /> 휴먼 코스 추천 1
            </button>
            <button type="button" className="mp-chip" onClick={() => setNotice('휴먼 코스 추천 2 페이지는 연결 준비 중입니다.')}>
              <CompassIcon /> 휴먼 코스 추천 2
            </button>
            <button type="button" className="mp-chip" onClick={() => setNotice('휴먼 코스 추천 3 페이지는 연결 준비 중입니다.')}>
              <CalendarIcon /> 휴먼 코스 추천 3
            </button>
          </div>

          <button type="button" className="mp-help-toggle" aria-expanded={helpOpen} onClick={() => setHelpOpen(!helpOpen)}>
            <span className={`mp-help-arrow${helpOpen ? ' open' : ''}`} aria-hidden="true">▶</span> 대화 응답에 문제가 있나요?
          </button>
          {helpOpen && <p className="mp-help-body">잠시 후 다시 시도하거나, 더 구체적인 문장으로 이야기해 주세요. 문제가 계속되면 고객센터로 문의해 주세요.</p>}
        </section>
      </div>

      <p className="mp-sample-note">지도형 ver9 화면 샘플 · 대화·추출·재고·결제·환불은 샘플 동작이며 실제 서비스와 연결되지 않습니다.</p>
    </>
  );
}
