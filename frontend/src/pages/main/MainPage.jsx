// 이전 메인 화면 import 보관
// import { useState } from 'react';
// import './MainPage.css';

// 팀에서 변경한 공통 CSS 위치
import '../../css/MainPage.css';
// import { useState } from 'react';



// function SparkleIcon() {
//   return (
//     <svg width="18" height="18" viewBox="0 0 20 20" fill="none" aria-hidden="true">
//       <path
//         d="M10 2v5M10 13v5M2 10h5M13 10h5M4.5 4.5l3 3M12.5 12.5l3 3M15.5 4.5l-3 3M7.5 12.5l-3 3"
//         stroke="#07836a"
//         strokeWidth="1.6"
//         strokeLinecap="round"
//       />
//     </svg>
//   );
// }

// function FilterIcon() {
//   return (
//     <svg width="16" height="16" viewBox="0 0 20 20" fill="none" aria-hidden="true">
//       <line x1="3" y1="5" x2="17" y2="5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
//       <circle cx="8" cy="5" r="2" fill="#fff" stroke="currentColor" strokeWidth="1.6" />
//       <line x1="3" y1="10" x2="17" y2="10" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
//       <circle cx="12.5" cy="10" r="2" fill="currentColor" />
//       <line x1="3" y1="15" x2="17" y2="15" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
//       <circle cx="8" cy="15" r="2" fill="#fff" stroke="currentColor" strokeWidth="1.6" />
//     </svg>
//   );
// }

// function ForkIcon() {
//   return (
//     <svg width="16" height="16" viewBox="0 0 20 20" fill="none" aria-hidden="true">
//       <path
//         d="M6 2.5v6a2 2 0 0 0 4 0v-6M8 8.5V17.5M14 2.5v6.3c0 1-.8 1.9-1.8 2L11.8 17.5"
//         stroke="currentColor"
//         strokeWidth="1.5"
//         strokeLinecap="round"
//         strokeLinejoin="round"
//       />
//     </svg>
//   );
// }

// function CompassIcon() {
//   return (
//     <svg width="16" height="16" viewBox="0 0 20 20" fill="none" aria-hidden="true">
//       <circle cx="10" cy="10" r="7.5" stroke="currentColor" strokeWidth="1.5" />
//       <path
//         d="M12.6 7.4 11 11l-3.6 1.6L9 9l3.6-1.6Z"
//         stroke="currentColor"
//         strokeWidth="1.3"
//         strokeLinejoin="round"
//       />
//     </svg>
//   );
// }

// function CalendarIcon() {
//   return (
//     <svg width="16" height="16" viewBox="0 0 20 20" fill="none" aria-hidden="true">
//       <rect
//         x="2.5"
//         y="4"
//         width="15"
//         height="13.5"
//         rx="2.4"
//         stroke="currentColor"
//         strokeWidth="1.5"
//       />
//       <path
//         d="M2.5 8h15M6.5 2.5v3M13.5 2.5v3"
//         stroke="currentColor"
//         strokeWidth="1.5"
//         strokeLinecap="round"
//       />
//     </svg>
//   );
// }

// export default function MainPage({ onStart, busy }) {
//   const [message, setMessage] = useState('');
//   const [notice, setNotice] = useState('');
//   const [helpOpen, setHelpOpen] = useState(false);

//   async function submit(event) {
//     event.preventDefault();

//     if (!message.trim()) {
//       setNotice('여행 이야기를 입력해 주세요.');
//       return;
//     }

//     try {
//       await onStart?.(message.trim());
//     } catch {
//       setNotice('대화를 시작하지 못했습니다. 서버와 OpenAI API 키를 확인해 주세요.');
//     }
//   }

//   return (
//     <>
//       <div className="mp-layout st-home">
//         <section
//           className="mp-conversation"
//           aria-labelledby="mp-greeting-title"
//         >
//           {/* 기존 플래너 헤더 보관
//           <div className="mp-panel-header">
//             <div className="mp-assistant-mark">
//               <SparkleIcon />
//             </div>
//             <div className="mp-panel-title">
//               <strong>여행 플래너</strong>
//               <span>당신만의 여행을 함께 만들어요</span>
//             </div>
//             <span className="mp-status-dot">대화 준비</span>
//           </div>
//           */}

//           {/* 기존 인사말 보관
//           <div className="mp-greeting">
//             <span className="mp-greeting-label">SMART TRAVEL</span>
//             <h2 id="mp-greeting-title">
//               안녕하세요!<br />
//               어떤 여행을 계획하고 있나요?
//             </h2>
//             <p>
//               정해진 게 없어도 괜찮아요.<br />
//               생각나는 것부터 이야기해 주세요.
//             </p>
//           </div>
//           */}

//           <div className="mp-greeting">
//             <p className="st-home-eyebrow">
//               계획은 짧게. 여행은 길게.
//             </p>

//             <h1 id="mp-greeting-title" className="st-home-title">
//               다음 여행,<br />
//               여기서 시작해요.
//             </h1>

//             <p className="st-home-description">
//               좋아하는 것을 말해 주세요. 코스는 저희가 짤게요.
//             </p>
//           </div>

//           <form className="mp-composer" onSubmit={submit}>
//             <label className="mp-visually-hidden" htmlFor="mp-story">
//               여행 이야기
//             </label>

//             <textarea
//               id="mp-story"
//               value={message}
//               onChange={(event) => {
//                 setMessage(event.target.value);
//                 setNotice('');
//               }}
//               placeholder="어떤 여행을 떠나고 싶나요?"
//             />

//             <div className="mp-composer-bottom">
//               <span>원하는 분위기나 장소도 함께 적어보세요</span>

//               <button
//                 type="submit"
//                 className="mp-primary"
//                 disabled={busy}
//               >
//                 {busy ? (
//                   '대화를 시작하는 중…'
//                 ) : (
//                   <>
//                     대화 시작하기
//                     <span aria-hidden="true">↗</span>
//                   </>
//                 )}
//               </button>
//             </div>

//             {notice && (
//               <p role="status" className="mp-notice">
//                 {notice}
//               </p>
//             )}
//           </form>

//           <div className="mp-suggestions">
//             <a className="mp-chip mp-chip-wide" href="#/search">
//               <FilterIcon />
//               조건 검색
//             </a>
//           </div>

//           <div className="mp-suggestions mp-suggestions-row">
//             <button
//               type="button"
//               className="mp-chip"
//               onClick={() =>
//                 setNotice('휴먼 코스 추천 1 페이지는 연결 준비 중입니다.')
//               }
//             >
//               <ForkIcon />
//               휴먼 코스 추천 1
//             </button>

//             <button
//               type="button"
//               className="mp-chip"
//               onClick={() =>
//                 setNotice('휴먼 코스 추천 2 페이지는 연결 준비 중입니다.')
//               }
//             >
//               <CompassIcon />
//               휴먼 코스 추천 2
//             </button>

//             <button
//               type="button"
//               className="mp-chip"
//               onClick={() =>
//                 setNotice('휴먼 코스 추천 3 페이지는 연결 준비 중입니다.')
//               }
//             >
//               <CalendarIcon />
//               휴먼 코스 추천 3
//             </button>
//           </div>

//           <button
//             type="button"
//             className="mp-help-toggle"
//             aria-expanded={helpOpen}
//             onClick={() => setHelpOpen(!helpOpen)}
//           >
//             <span
//               className={`mp-help-arrow${helpOpen ? ' open' : ''}`}
//               aria-hidden="true"
//             >
//               ▶
//             </span>
//             대화 응답에 문제가 있나요?
//           </button>

//           {helpOpen && (
//             <p className="mp-help-body">
//               잠시 후 다시 시도하거나, 더 구체적인 문장으로 이야기해 주세요.
//               문제가 계속되면 고객센터로 문의해 주세요.
//             </p>
//           )}
//         </section>
//       </div>

//       <p className="mp-sample-note">
//         지도형 ver9 화면 샘플 · 대화·추출·재고·결제·환불은 샘플 동작이며
//         실제 서비스와 연결되지 않습니다.
//       </p>
//     </>
//   );
// }

import { useRef, useState } from 'react';
import './HomeLanding.css';

// 화면 구성용 더미 데이터입니다.
const sampleCourses = [
  {
    id: 'autumn-gyeongju',
    region: '경주',
    season: '늦가을',
    title: '가을 끝자락, 경주 산책',
    route: '대릉원 → 황리단길 → 동궁과 월지',
    image: '/images/course-gyeongju.jpg',
    background: 'autumn',
  },
  {
    id: 'winter-gangneung',
    region: '강릉',
    season: '겨울',
    title: '겨울 바다와 따뜻한 커피',
    route: '안목해변 → 강릉중앙시장 → 경포해변',
    image: '/images/course-gangneung.jpg',
    background: 'winter',
  },
  {
    id: 'winter-jeonju',
    region: '전주',
    season: '가을·겨울',
    title: '한옥 골목에서 보내는 하루',
    route: '전주한옥마을 → 경기전 → 남부시장',
    image: '/images/course-jeonju.jpg',
    background: 'hanok',
  },
];

const quickPrompts = [
  {
    label: '주말 여행',
    text: '이번 주말에 1박 2일로 여행하고 싶어요.',
  },
  {
    label: '차 없이 여행',
    text: '자가용 없이 대중교통과 도보로 여행하고 싶어요.',
  },
  {
    label: '아무 데나 추천',
    text: '아직 목적지를 정하지 못했어요. 여행지부터 추천해 주세요.',
  },
];

export default function MainPage({ onStart, busy = false }) {
  const [message, setMessage] = useState('');
  const [notice, setNotice] = useState('');
  const [selectedCourse, setSelectedCourse] = useState(null);

  const inputRef = useRef(null);
  const inFlight = useRef(false);

  function fillMessage(text) {
    setMessage(text);
    setNotice('');
    inputRef.current?.focus();
  }

  async function submit(event) {
    event.preventDefault();

    if (busy || inFlight.current) return;

    if (!message.trim()) {
      setNotice('여행 이야기를 입력해 주세요.');
      inputRef.current?.focus();
      return;
    }

    if (!onStart) {
      setNotice('대화 연결을 준비 중이에요. 잠시 후 다시 이용해 주세요.');
      return;
    }

    setNotice('');
    inFlight.current = true;

    try {
      await onStart(message.trim());
    } catch {
      setNotice('대화를 시작하지 못했어요. 잠시 후 다시 시도해 주세요.');
    } finally {
      inFlight.current = false;
    }
  }

  return (
    <div className="home-landing">
      <header className="landing-hero">
        <p className="landing-eyebrow">계획은 짧게. 여행은 길게.</p>
        <h1>
          다음 여행,<br />
          여기서 시작해요.
        </h1>
        <p className="landing-description">
          좋아하는 것을 말해 주세요. 코스는 저희가 짤게요.
        </p>
      </header>

      <form className="landing-composer" onSubmit={submit}>
        <label className="landing-sr-only" htmlFor="landing-story">
          원하는 여행 이야기
        </label>

        <textarea
          ref={inputRef}
          id="landing-story"
          value={message}
          disabled={busy}
          placeholder="친구들과 부산에서 주말 여행, 맛집이랑 바다 풍경이 좋아요..."
          aria-invalid={Boolean(notice)}
          aria-describedby={notice ? 'landing-error' : undefined}
          onChange={(event) => {
            setMessage(event.target.value);
            setNotice('');
          }}
        />

        <div className="landing-composer-bottom">
          <a className="landing-condition-link" href="#/search">
            + 조건 선택
          </a>
          <button
            className="landing-start"
            type="submit"
            disabled={busy}
          >
            {busy ? '시작하는 중…' : '시작하기'}
            <span aria-hidden="true">→</span>
          </button>
        </div>

        {notice && (
          <p id="landing-error" className="landing-error" role="alert">
            {notice}
          </p>
        )}
      </form>

      <div className="landing-shortcuts">
        <p>대화로 시작하고, 원할 때 조건 선택으로 바꿀 수 있어요.</p>
        <div className="landing-quick-actions" aria-label="여행 이야기 예시">
          {quickPrompts.map((prompt, index) => (
            <button
              key={prompt.label}
              type="button"
              className={index === 1 ? 'is-featured' : ''}
              disabled={busy}
              onClick={() => fillMessage(prompt.text)}
            >
              {prompt.label}
            </button>
          ))}
        </div>
        <a className="landing-toss-demo" href="#/payments/demo">토스 1,000원 테스트 결제 →</a>
      </div>

      <section className="landing-recommendations" aria-labelledby="courses-title">
        <div className="landing-section-heading">
          <h2 id="courses-title">이런 여행은 어때요?</h2>
          <a href="#/human-courses">전체 보기 →</a>
        </div>

        <div className="landing-course-grid">
          {sampleCourses.map((course) => (
            <button
              key={course.id}
              type="button"
              className="landing-course"
              onClick={() => setSelectedCourse(course)}
            >
              <div className={`landing-course-image ${course.background}`}>
                <span className="landing-region">{course.region}</span>
                <img
                  src={course.image}
                  alt=""
                  loading="lazy"
                  onError={(event) => {
                    event.currentTarget.style.display = 'none';
                  }}
                />
                <span className="landing-example">예시 코스</span>
              </div>

              <div className="landing-course-body">
                <span className="landing-course-tag">
                  {course.region} · {course.season} · 당일치기
                </span>
                <h3>{course.title}</h3>
                <p>{course.route}</p>
              </div>
            </button>
          ))}
        </div>
      </section>

      {selectedCourse && (
        <div
          className="landing-modal-backdrop"
          onClick={() => setSelectedCourse(null)}
        >
          <section
            className="landing-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="sample-course-title"
            onClick={(event) => event.stopPropagation()}
            onKeyDown={(event) => {
              if (event.key === 'Escape') setSelectedCourse(null);
            }}
          >
            <button
              autoFocus
              type="button"
              className="landing-modal-close"
              onClick={() => setSelectedCourse(null)}
            >
              닫기 ×
            </button>
            <p className="landing-course-tag">화면 구성용 예시 코스</p>
            <h2 id="sample-course-title">{selectedCourse.title}</h2>
            <p>{selectedCourse.route}</p>
            <p>실제 이용자 후기나 AI가 생성한 결과는 아닙니다.</p>
          </section>
        </div>
      )}
    </div>
  );
}
