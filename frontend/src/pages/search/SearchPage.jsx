import { useState } from 'react';
import './SearchPage.css';

function SlidersIcon() {
  return (
    <svg className="sc-icon" width="60" height="48" viewBox="0 0 60 48" fill="none" aria-hidden="true">
      <line x1="10" y1="8" x2="50" y2="8" stroke="#1c2b33" strokeWidth="2.2" strokeLinecap="round" />
      <circle cx="24" cy="8" r="4.2" fill="#fff" stroke="#1c2b33" strokeWidth="2.2" />
      <line x1="10" y1="24" x2="50" y2="24" stroke="#1c2b33" strokeWidth="2.2" strokeLinecap="round" />
      <circle cx="33" cy="24" r="4.6" fill="#0c806b" />
      <line x1="10" y1="40" x2="50" y2="40" stroke="#1c2b33" strokeWidth="2.2" strokeLinecap="round" />
      <circle cx="24" cy="40" r="4.2" fill="#fff" stroke="#1c2b33" strokeWidth="2.2" />
    </svg>
  );
}

function ChevronIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M4 6l4 4 4-4" stroke="#6b7c76" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
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

const fields = [
  { name: 'region', label: '지역', options: ['부산', '서울', '제주', '강릉', '여수'] },
  { name: 'duration', label: '여행 기간', options: ['당일', '1박 2일', '2박 3일', '3박 4일'] },
  { name: 'companion', label: '동행', options: ['혼자', '친구', '연인', '가족'] },
  { name: 'transport', label: '교통수단', options: ['대중교통', '자동차'] },
];

export default function SearchPage() {
  const [values, setValues] = useState({ region: '부산', duration: '1박 2일', companion: '친구', transport: '대중교통' });
  const [notice, setNotice] = useState('');

  function handleChange(name, value) {
    setValues((prev) => ({ ...prev, [name]: value }));
    setNotice('');
  }

  function submit(event) {
    event.preventDefault();
    setNotice('선택한 조건은 유지됩니다. 코스 추천 기능은 연결 준비 중입니다.');
  }

  return (
    <div className="sc-main">
      <div className="sc-step"><span className="sc-step-line" /><span className="sc-step-num">03</span><span className="sc-step-label">시작 방식 선택형</span><span className="sc-step-line" /></div>

      <h1 className="sc-title">취향은 당신이, 여행 코스는 <span>AI</span>가.</h1>
      <p className="sc-subtitle">새로운 코스를 찾고, 내 일정을 편하게 바꿔보세요.</p>

      <h2 className="sc-question">어떤 방식으로 여행을 시작할까요?</h2>

      <section className="sc-card" aria-labelledby="sc-card-title">
        <SlidersIcon />
        <h3 id="sc-card-title">조건 선택으로 추천받기</h3>
        <p className="sc-card-desc">필요한 조건을 골라 빠르게 추천받으세요.</p>

        <form className="sc-form" onSubmit={submit}>
          <div className="sc-fields">
            {fields.map((field) => (
              <label key={field.name} className="sc-field">
                <span className="sc-field-label">{field.label}</span>
                <span className="sc-select-wrap">
                  <select
                    value={values[field.name]}
                    onChange={(e) => handleChange(field.name, e.target.value)}
                  >
                    {field.options.map((option) => <option key={option} value={option}>{option}</option>)}
                  </select>
                  <ChevronIcon />
                </span>
              </label>
            ))}
          </div>

          <button type="submit" className="sc-cta">조건으로 추천받기 <span aria-hidden="true">→</span></button>

          {notice && <p role="status" className="sc-notice">{notice}</p>}
        </form>
      </section>

      <div className="sc-tip"><BulbIcon /><span>입력 방식은 달라도, 같은 여행 일정으로 이어져요.</span></div>
    </div>
  );
}
