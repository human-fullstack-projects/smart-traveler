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
//         stroke="#0B3DC9"
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

import { useEffect, useRef, useState } from 'react';
import './HomeLanding.css';
import HomeReviewCards from './HomeReviewCards';
import BrowserPushTest from './BrowserPushTest';
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

// 메인 첫 화면 배경 사진 (public/images/hero/출처.txt)
const HERO_PHOTOS = [
  { src: '/images/hero/busan.jpg', place: '부산 · 해운대해수욕장', credit: '사진: 부산광역시 (공공누리 제1유형)' },
  { src: '/images/hero/jeju.jpg', place: '제주 · 우도', credit: '사진: 제주영상문화산업진흥원 (공공누리 제1유형)' },
  { src: '/images/hero/gyeongju.jpg', place: '경주 · 불국사', credit: '사진: 문화재청 (공공누리 제1유형)' },
  { src: '/images/hero/seoul.jpg', place: '서울 · 경복궁 경회루', credit: '사진: 국가유산청 궁능유적본부 (공공누리 제1유형)' },
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

export default function MainPage({ onStart, busy = false, reviewAccess, reviewKey }) {
  const [message, setMessage] = useState('');
  const [notice, setNotice] = useState('');
  const [selectedCourse, setSelectedCourse] = useState(null);

  const [photoIndex, setPhotoIndex] = useState(0);
  useEffect(() => {
    const timer = setInterval(() => setPhotoIndex((index) => (index + 1) % HERO_PHOTOS.length), 6000);
    return () => clearInterval(timer);
  }, []);

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

    const text = message.trim();
    if (!text) { window.location.hash = '/chat'; return; } // 빈 입력은 챗봇 시작 화면으로

    if (!onStart) {
      setNotice('대화 연결을 준비 중이에요. 잠시 후 다시 이용해 주세요.');
      return;
    }

    setNotice('');
    inFlight.current = true;

    try {
      await onStart(text);
    } catch {
      setNotice('대화를 시작하지 못했어요. 잠시 후 다시 시도해 주세요.');
    } finally {
      inFlight.current = false;
    }
  }

  return (
    <div className="home-landing home-hero">
      {/* 배경: 여행지 사진이 6초마다 바뀜 + 왼쪽·아래 파스텔 막 */}
      <div className="hero-bg" aria-hidden="true">
        {HERO_PHOTOS.map((photo, index) => (
          <div key={photo.src} className={`hero-slide${index === photoIndex ? ' is-on' : ''}`} style={{ backgroundImage: `url("${photo.src}")` }} />
        ))}
        <div className="hero-veil" />
      </div>

      <header className="landing-hero">
        <p className="landing-eyebrow">계획은 짧게, 여행은 길게.</p>
        <h1>
          <span className="hero-line1">다음 <span className="hero-accent">여행,</span><img className="hero-hotel" src="/images/hero/hotel_3d.png" alt="" /></span>
          <br />
          여기서 시작해요.
        </h1>
        <p className="landing-description">
          좋아하는 것을 말해 주세요. 코스는 저희가 짤게요.
        </p>

        <form className="landing-composer" onSubmit={submit}>
          <label className="landing-sr-only" htmlFor="landing-story">
            원하는 여행 이야기
          </label>
          <textarea
            ref={inputRef}
            id="landing-story"
            rows={1}
            value={message}
            disabled={busy}
            placeholder="친구들과 부산에서 주말 여행, 맛집이랑 바다 풍경이 좋아요..."
            aria-invalid={Boolean(notice)}
            aria-describedby={notice ? 'landing-error' : undefined}
            onChange={(event) => {
              setMessage(event.target.value);
              setNotice('');
            }}
            onKeyDown={(event) => {
              if (event.key === 'Enter' && !event.shiftKey && !event.nativeEvent.isComposing) {
                event.preventDefault();
                event.currentTarget.form?.requestSubmit();
              }
            }}
          />
          <button className="landing-start" type="submit" disabled={busy}>
            {busy ? '시작하는 중…' : '시작하기'}
            <span aria-hidden="true">→</span>
          </button>
        </form>
        {notice && (
          <p id="landing-error" className="landing-error" role="alert">
            {notice}
          </p>
        )}

        <div className="landing-quick-actions" aria-label="여행 이야기 예시">
          {quickPrompts.map((prompt) => (
            <button key={prompt.label} type="button" disabled={busy} onClick={() => fillMessage(prompt.text)}>
              {prompt.label}
            </button>
          ))}
        </div>
      </header>

      <p className="hero-credit">
        <span className="hero-place"><img src="/images/hero/round_pushpin_3d.png" alt="" />{HERO_PHOTOS[photoIndex].place}</span>
        <small>{HERO_PHOTOS[photoIndex].credit}</small>
      </p>

      {/* 휴먼 추천 코스 3개: 사진 안쪽 아래 */}
      <div className="hero-courses">
        <HomeReviewCards key={reviewKey} access={reviewAccess} />
      </div>

      <div className="hero-dots" aria-hidden="true">
        {HERO_PHOTOS.map((photo, index) => <i key={photo.src} className={index === photoIndex ? 'is-on' : ''} />)}
      </div>

      {/* 개발용 시험 도구(토스 결제·브라우저 알림)는 한 화면을 가리지 않게 오른쪽 아래 버튼 안으로 */}
      <details className="hero-devtools">
        <summary>시험 도구</summary>
        <a className="landing-toss-demo" href="#/payments/demo">토스 1,000원 테스트 결제 →</a>
        <BrowserPushTest />
      </details>

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
