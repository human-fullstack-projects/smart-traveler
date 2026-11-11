import { useEffect, useMemo, useRef, useState } from 'react';
import '../../css/owner.css';
import { postJsonWithCsrf, putJsonWithCsrf, request } from '../../api/auth.js';

const tabs = [
  { id: 'dashboard', label: '대시보드' },
  { id: 'reservations', label: '예약 현황' },
  { id: 'property', label: '숙소 정보' },
  { id: 'rooms', label: '객실 · 요금' },
];

const initialReservations = [
  { id: 'ST-2601102', guest: '김민지', room: '오션 디럭스 트윈', checkIn: '2026-11-04', checkOut: '2026-11-06', people: '성인 2 · 아동 1', amount: 392000, status: '체크인 예정' },
  { id: 'ST-2601098', guest: '박도윤', room: '스탠다드 더블', checkIn: '2026-11-04', checkOut: '2026-11-05', people: '성인 2', amount: 148000, status: '체크인' },
  { id: 'ST-2601087', guest: '이서연', room: '오션 디럭스 트윈', checkIn: '2026-11-02', checkOut: '2026-11-04', people: '성인 2', amount: 368000, status: '체크아웃' },
  { id: 'ST-2601071', guest: '정우진', room: '스탠다드 더블', checkIn: '2026-11-08', checkOut: '2026-11-10', people: '성인 1', amount: 296000, status: '예약확정' },
];

const initialRooms = [
  { id: 1, name: '스탠다드 더블', basis: 2, max: 2, stock: 4, offWeekday: 148000, offWeekend: 168000, peakWeekday: 178000, peakWeekend: 198000, stopped: false },
  { id: 2, name: '오션 디럭스 트윈', basis: 2, max: 4, stock: 2, offWeekday: 196000, offWeekend: 226000, peakWeekday: 238000, peakWeekend: 268000, stopped: false },
];

function formatWon(value) {
  return `${new Intl.NumberFormat('ko-KR').format(value)}원`;
}

function OwnerHeader({ propertyName, member, onLogout, loggingOut }) {
  return (
    <header className="owner-topbar">
      <div className="owner-topbar__inner">
        {/* 기존 로고 <a className="owner-brand" href="#/">smart <span>travel</span><small>partner</small></a> */}
        <a className="owner-brand" href="#/">SMART TRAVEL</a>
        <div className="owner-topbar__right">
          <span className="owner-property-name">{propertyName}</span>
          {/* <a href="#/stays">사용자 숙소 화면</a> */}
          {/* 기존 샘플 이름 <button type="button">지훈 업주님</button> */}
          {member && (
            <span>{member.nickname || member.loginId}님</span>
          )}
          {/* <a href="#/main">고객</a> */}
          {member ? (
            <button type="button" disabled={loggingOut} onClick={onLogout}>{loggingOut ? '로그아웃 중…' : '로그아웃'}</button>
          ) : (
            <a href="#/login">로그인</a>
          )}
        </div>
      </div>
    </header>
  );
}

function StatusBadge({ children }) {
  const tone = children.includes('확정') || children === '체크인' ? 'green' : children.includes('예정') ? 'blue' : children === '체크아웃' ? 'gray' : 'amber';
  return <span className={`owner-badge owner-badge--${tone}`}>{children}</span>;
}

// function Dashboard({ onNavigate }) {
//   return (
//     <div className="owner-dashboard">
//       <section className="owner-welcome"><div><span>2026년 11월 4일 수요일</span><h1>오늘의 업장 현황을 확인하세요.</h1><p>예약과 객실 판매 상태는 서버 연결 전 샘플 데이터로 표시됩니다.</p></div><button type="button" onClick={() => onNavigate('reservations')}>오늘 예약 보기</button></section>
//       <section className="owner-metrics" aria-label="오늘의 현황">
//         <article><span className="owner-metric-icon is-green">IN</span><div><small>오늘 체크인</small><strong>2</strong><p>오후 3시부터</p></div></article>
//         <article><span className="owner-metric-icon is-blue">OUT</span><div><small>오늘 체크아웃</small><strong>1</strong><p>오전 11시까지</p></div></article>
//         <article><span className="owner-metric-icon is-amber">NEW</span><div><small>신규 예약</small><strong>6</strong><p>지난 24시간</p></div></article>
//         <article><span className="owner-metric-icon is-purple">ROOM</span><div><small>오늘 판매 가능</small><strong>4</strong><p>전체 6실 중</p></div></article>
//       </section>
//       <div className="owner-dashboard-grid">
//         <section className="owner-panel owner-today"><div className="owner-panel__head"><div><span>TODAY</span><h2>오늘 도착·출발</h2></div><button type="button" onClick={() => onNavigate('reservations')}>전체 보기</button></div>{initialReservations.slice(0, 3).map((item) => <article key={item.id}><div className="owner-date-box"><strong>{item.checkIn.slice(8)}</strong><span>11월</span></div><div><strong>{item.guest}</strong><span>{item.room} · {item.people}</span><small>{item.id}</small></div><StatusBadge>{item.status}</StatusBadge></article>)}</section>
//         <section className="owner-panel owner-property-status"><div className="owner-panel__head"><div><span>PROPERTY STATUS</span><h2>숙소 운영 상태</h2></div></div><div className="owner-approval-card"><span className="owner-approval-card__icon">✓</span><div><strong>숙소 승인 완료</strong><p>사용자 검색과 예약에 정상 노출 중입니다.</p></div><StatusBadge>운영 중</StatusBadge></div><dl><div><dt>업주 가입 심사</dt><dd>승인 완료</dd></div><div><dt>숙소 심사</dt><dd>승인 완료 · 2026.10.31</dd></div><div><dt>등록 객실</dt><dd>2개 유형 · 총 6실</dd></div><div><dt>판매 중지</dt><dd>없음</dd></div></dl><button type="button" className="owner-line-button" onClick={() => onNavigate('property')}>숙소 정보 관리</button></section>
//       </div>
//       <section className="owner-panel owner-quick"><div className="owner-panel__head"><div><span>QUICK ACTION</span><h2>빠른 관리</h2></div></div><div><button type="button" onClick={() => onNavigate('property')}><b>＋</b><strong>숙소 정보 수정</strong><span>사진·편의시설·운영 정보</span></button><button type="button" onClick={() => onNavigate('rooms')}><b>₩</b><strong>객실 요금 변경</strong><span>시즌·주중·주말 요금</span></button><button type="button" onClick={() => onNavigate('rooms')}><b>×</b><strong>판매 중지 설정</strong><span>날짜별 객실 판매 제어</span></button></div></section>
//     </div>
//   );
// }

const lodgingStatusLabels = {
  PENDING: '승인 대기',
  ACTIVE: '운영 중',
  APPROVED: '승인',
  REJECTED: '반려',
  INACTIVE: '운영 중지',
  SUSPENDED: '운영 중지',
  CLOSED: '폐업',
};

function OwnerLodgingStatus({ lodgings, error }) {
  if (error) return <p role="alert">{error}</p>;
  if (lodgings === null) return <p role="status">숙소 정보를 불러오는 중이에요.</p>;
  if (lodgings.length === 0) return <p>등록된 숙소가 없어요.</p>;

  return (
    <ul>
      {lodgings.map(({ lodgingId, name, status }) => (
        <li key={lodgingId}>
          {name} · {lodgingStatusLabels[status] ?? status}
        </li>
      ))}
    </ul>
  );
}

function Dashboard({ onNavigate, lodgings, lodgingError }) {
  const metrics = [
    { label: '오늘 체크인', value: 2 },
    { label: '오늘 체크아웃', value: 1 },
    { label: '신규 예약', value: 6 },
  ];

  return (
    <div className="owner-overview">
      <header className="owner-overview-heading">
        <div>
          <h1>업장 현황</h1>
          <p>2026년 11월 4일 기준 예시 데이터</p>
        </div>
        <button
          type="button"
          className="owner-overview-action"
          onClick={() => onNavigate('reservations')}
        >
          예약 현황 보기 →
        </button>
      </header>

      <section className="owner-overview-metrics" aria-label="예약 요약">
        {metrics.map(({ label, value }) => (
          <article key={label} className="owner-overview-card">
            <h2>{label}</h2>
            <p className="owner-overview-number">
              {value}<span>건</span>
            </p>
          </article>
        ))}
      </section>

      <section className="owner-overview-card owner-overview-status">
        <div>
          <h2>숙소 운영 상태</h2>
              {/*
              <p>숙소 승인 완료 · 운영 중</p>
              <p>등록 객실 2개 유형 · 총 6실</p>
              */}
          {/* <OwnerLodgingStatus /> */}
          <OwnerLodgingStatus lodgings={lodgings} error={lodgingError} />
        </div>
        <button
          type="button"
          className="owner-overview-action owner-overview-action--line"
          onClick={() => onNavigate('property')}
        >
          숙소 정보 관리 →
        </button>
      </section>
    </div>
  );
}

// function Reservations() {
//   const [status, setStatus] = useState('전체');
//   const [query, setQuery] = useState('');
//   const [selected, setSelected] = useState(initialReservations[0]);
//   const visible = initialReservations.filter((item) => (status === '전체' || item.status === status) && `${item.guest}${item.id}${item.room}`.toLowerCase().includes(query.toLowerCase()));
//   return (
//     <div className="owner-view"><div className="owner-view__title"><div><span>RESERVATIONS</span><h1>예약 현황</h1><p>본인 업장의 날짜별·객실별 예약만 조회합니다.</p></div><button type="button">예약 목록 내려받기</button></div><section className="owner-toolbar"><div className="owner-segment">{['전체', '체크인 예정', '체크인', '체크아웃', '예약확정'].map((value) => <button type="button" key={value} className={status === value ? 'is-active' : ''} onClick={() => setStatus(value)}>{value}</button>)}</div><label><span className="owner-sr-only">예약 검색</span><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="예약번호, 예약자, 객실 검색" /></label></section><div className="owner-reservation-layout"><section className="owner-panel owner-table-wrap"><table><thead><tr><th>예약번호</th><th>예약자</th><th>객실</th><th>숙박 일정</th><th>결제 금액</th><th>상태</th></tr></thead><tbody>{visible.map((item) => <tr key={item.id} className={selected.id === item.id ? 'is-selected' : ''} onClick={() => setSelected(item)}><td><strong>{item.id}</strong></td><td>{item.guest}<small>{item.people}</small></td><td>{item.room}</td><td>{item.checkIn}<small>~ {item.checkOut}</small></td><td>{formatWon(item.amount)}</td><td><StatusBadge>{item.status}</StatusBadge></td></tr>)}</tbody></table>{!visible.length && <p className="owner-table-empty">조건에 맞는 예약이 없습니다.</p>}</section><aside className="owner-panel owner-reservation-detail"><div className="owner-panel__head"><div><span>BOOKING DETAIL</span><h2>{selected.id}</h2></div><StatusBadge>{selected.status}</StatusBadge></div><dl><div><dt>예약자</dt><dd>{selected.guest}</dd></div><div><dt>투숙 인원</dt><dd>{selected.people}</dd></div><div><dt>객실</dt><dd>{selected.room}</dd></div><div><dt>체크인</dt><dd>{selected.checkIn} · 15:00</dd></div><div><dt>체크아웃</dt><dd>{selected.checkOut} · 11:00</dd></div><div><dt>결제 금액</dt><dd>{formatWon(selected.amount)}</dd></div></dl><p>예약 상태와 PG 결제 상태는 별도로 관리하며, 화면 조작만으로 결제 성공 상태를 만들지 않습니다.</p><button type="button" className="owner-line-button">예약 상세 열기</button></aside></div></div>
//   );
// }

function Reservations() {
  const [status, setStatus] = useState('전체');
  const [query, setQuery] = useState('');
  const [selectedId, setSelectedId] = useState(null);

  const statuses = ['전체', '체크인 예정', '체크인', '체크아웃', '예약확정'];
  const keyword = query.trim().toLowerCase();
  const visible = initialReservations.filter((item) => {
    const matchesStatus = status === '전체' || item.status === status;
    const matchesQuery = `${item.id} ${item.guest} ${item.room}`
      .toLowerCase()
      .includes(keyword);
    return matchesStatus && matchesQuery;
  });
  const selected = visible.find((item) => item.id === selectedId) ?? visible[0];

  return (
    <div className="owner-view owner-reservations">
      <div className="owner-view__title">
        <div>
          <h1>예약 현황</h1>
          <p>예약을 선택하면 투숙 정보와 결제 금액을 확인할 수 있어요.</p>
        </div>
      </div>

      <section className="owner-panel owner-booking-filters" aria-label="예약 검색과 필터">
        <label className="owner-booking-search">
          <span>예약 검색</span>
          <input
            type="search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="예약번호, 예약자, 객실명"
          />
        </label>
        <div className="owner-booking-statuses" role="group" aria-label="예약 상태">
          {statuses.map((value) => (
            <button
              key={value}
              type="button"
              aria-pressed={status === value}
              onClick={() => setStatus(value)}
            >
              {value}
            </button>
          ))}
        </div>
      </section>

      <div className="owner-reservation-layout">
        <section className="owner-panel owner-table-wrap" aria-label="예약 목록">
          {visible.length ? (
            <table>
              <thead>
                <tr>
                  <th scope="col">예약자</th>
                  <th scope="col">객실</th>
                  <th scope="col">숙박 일정</th>
                  <th scope="col">상태</th>
                </tr>
              </thead>
              <tbody>
                {visible.map((item) => (
                  <tr
                    key={item.id}
                    className={selected?.id === item.id ? 'is-selected' : ''}
                    onClick={() => setSelectedId(item.id)}
                  >
                    <td>
                      <button
                        type="button"
                        className="owner-booking-select"
                        aria-pressed={selected?.id === item.id}
                        onClick={() => setSelectedId(item.id)}
                      >
                        {item.guest}
                        <small>{item.id}</small>
                      </button>
                    </td>
                    <td>{item.room}</td>
                    <td>{item.checkIn}<small>~ {item.checkOut}</small></td>
                    <td><StatusBadge>{item.status}</StatusBadge></td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <p className="owner-booking-empty" role="status">
              조건에 맞는 예약이 없어요.
            </p>
          )}
        </section>

        {selected && (
          <aside className="owner-panel owner-reservation-detail" aria-label="예약 상세">
            <div className="owner-panel__head">
              <div>
                <h2>예약 상세</h2>
                <p className="owner-booking-id">{selected.id}</p>
              </div>
              <StatusBadge>{selected.status}</StatusBadge>
            </div>
            <dl>
              <div><dt>예약자</dt><dd>{selected.guest}</dd></div>
              <div><dt>투숙 인원</dt><dd>{selected.people}</dd></div>
              <div><dt>객실</dt><dd>{selected.room}</dd></div>
              <div><dt>체크인</dt><dd>{selected.checkIn} · 15:00</dd></div>
              <div><dt>체크아웃</dt><dd>{selected.checkOut} · 11:00</dd></div>
              <div><dt>결제 금액</dt><dd>{formatWon(selected.amount)}</dd></div>
            </dl>
          </aside>
        )}
      </div>
    </div>
  );
}

// function PropertyForm({ property, setProperty, setToast }) {
//   const facilities = ['주차', '조식', '와이파이', '짐 보관', '수영장', '피트니스', '세탁실', '반려동물 동반'];
//   function submit(event) { event.preventDefault(); setToast('숙소 정보가 임시 저장되었습니다. 주요 정보 변경 시 관리자 재승인 정책을 확인해야 합니다.'); }
//   return (
//     <div className="owner-view"><div className="owner-view__title"><div><span>PROPERTY</span><h1>숙소 정보 관리</h1><p>검색·예약 상세에 노출되는 업장 정보를 관리합니다.</p></div><StatusBadge>운영 중</StatusBadge></div><form className="owner-property-form" onSubmit={submit}><section className="owner-panel"><div className="owner-panel__head"><div><span>BASIC INFORMATION</span><h2>기본 정보</h2></div><span className="owner-required">* 필수 입력</span></div><div className="owner-form-grid"><label>숙소명 *<input value={property.name} onChange={(event) => setProperty({ ...property, name: event.target.value })} required /></label><label>숙소 유형 *<select value={property.type} onChange={(event) => setProperty({ ...property, type: event.target.value })}><option>호텔</option><option>리조트</option><option>펜션</option><option>게스트하우스</option></select></label><label>우편번호<input value={property.zip} onChange={(event) => setProperty({ ...property, zip: event.target.value })} /></label><label className="owner-address">주소 *<div><input value={property.address} onChange={(event) => setProperty({ ...property, address: event.target.value })} required /><button type="button">주소 검색</button></div></label><label className="owner-wide">상세 주소<input value={property.addressDetail} onChange={(event) => setProperty({ ...property, addressDetail: event.target.value })} /></label><label>전화번호 *<input value={property.phone} onChange={(event) => setProperty({ ...property, phone: event.target.value })} required /></label><label>홈페이지<input value={property.website} onChange={(event) => setProperty({ ...property, website: event.target.value })} /></label><label className="owner-wide">숙소 소개 *<textarea rows="5" value={property.description} onChange={(event) => setProperty({ ...property, description: event.target.value })} required /></label></div></section><section className="owner-panel"><div className="owner-panel__head"><div><span>OPERATION</span><h2>운영 정보</h2></div></div><div className="owner-form-grid"><label>체크인 시간 *<input type="time" value={property.checkIn} onChange={(event) => setProperty({ ...property, checkIn: event.target.value })} /></label><label>체크아웃 시간 *<input type="time" value={property.checkOut} onChange={(event) => setProperty({ ...property, checkOut: event.target.value })} /></label><label className="owner-wide">주차 안내<textarea rows="3" value={property.parking} onChange={(event) => setProperty({ ...property, parking: event.target.value })} /></label><fieldset className="owner-wide"><legend>편의시설</legend><div className="owner-facility-grid">{facilities.map((item) => <label key={item}><input type="checkbox" checked={property.facilities.includes(item)} onChange={() => setProperty({ ...property, facilities: property.facilities.includes(item) ? property.facilities.filter((value) => value !== item) : [...property.facilities, item] })} />{item}</label>)}</div></fieldset><label className="owner-wide">취소·환불 규정 *<textarea rows="5" value={property.cancellation} onChange={(event) => setProperty({ ...property, cancellation: event.target.value })} required /></label></div></section><section className="owner-panel"><div className="owner-panel__head"><div><span>PHOTOS &amp; LOCATION</span><h2>사진과 지도 위치</h2></div></div><div className="owner-photo-grid"><button type="button" className="owner-photo-add"><strong>＋</strong><span>사진 추가</span><small>JPG, PNG · 최대 10장</small></button>{['대표 이미지', '로비', '외관'].map((label) => <div key={label} className="owner-photo-sample"><span>{label}</span></div>)}</div><div className="owner-location-preview"><i /><strong>부산 해운대구 달맞이길 42</strong><p>주소 지오코딩 실패 시 지도에서 위치를 직접 지정할 수 있습니다.</p><button type="button">지도 위치 조정</button></div></section><div className="owner-form-actions"><button type="button" className="owner-secondary">변경 취소</button><button type="submit" className="owner-primary">숙소 정보 저장</button></div></form></div>
//   );
// }

function LodgingDetail({ lodgingId, onBack, onSaved }) {
  const [detail, setDetail] = useState(null);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    request('/api/owner/lodgings/' + lodgingId, { signal: controller.signal })
      .then((data) => {
        if (!controller.signal.aborted) setDetail(data);
      })
      .catch((failure) => {
        if (!controller.signal.aborted) {
          setError(failure.status === 404 ? '숙소를 찾을 수 없어요.' : failure.message);
        }
      });
    return () => controller.abort();
  }, [lodgingId]);

  if (editing && detail) {
    return (
      <LodgingForm
        initialDetail={detail}
        onCancel={() => setEditing(false)}
        onSaved={onSaved}
      />
    );
  }

  return (
    <div className="owner-view">
      <header className="owner-overview-heading">
        <div><h1>{detail?.name ?? '숙소 상세'}</h1></div>
        {detail?.status === 'PENDING' && (
          <button
            type="button"
            className="owner-overview-action"
            onClick={() => setEditing(true)}
          >
            숙소 정보 수정
          </button>
        )}
        <button type="button" className="owner-overview-action owner-overview-action--line" onClick={onBack}>
          목록으로
        </button>
      </header>
      <section className="owner-panel owner-lodging-detail" aria-label="숙소 상세 정보">
        {error && <p role="alert">{error}</p>}
        {!error && !detail && <p role="status">숙소 정보를 불러오는 중이에요.</p>}
        {detail && (
          <>
            <dl className="owner-form-grid">
              <div><dt>숙소 유형</dt><dd>{detail.lodgingType}</dd></div>
              <div><dt>숙소 상태</dt><dd>{lodgingStatusLabels[detail.status] ?? detail.status}</dd></div>
              <div className="owner-wide"><dt>주소</dt><dd>{detail.address}</dd></div>
              <div><dt>위도</dt><dd>{detail.latitude}</dd></div>
              <div><dt>경도</dt><dd>{detail.longitude}</dd></div>
              <div><dt>체크인</dt><dd>{detail.checkinTime?.slice(0, 5)}</dd></div>
              <div><dt>체크아웃</dt><dd>{detail.checkoutTime?.slice(0, 5)}</dd></div>
            </dl>
            {detail.status === 'ACTIVE' && <p>운영 중인 숙소는 현재 상세 정보만 확인할 수 있어요.</p>}
            {detail.status === 'REJECTED' && <p>반려 사유 · {detail.rejectReason || '등록된 사유가 없어요.'}</p>}
          </>
        )}
      </section>
    </div>
  );
}

function LodgingManagement({ lodgings, error, onCreated, onUpdated }) {
  const [creating, setCreating] = useState(false);
  const [selectedLodgingId, setSelectedLodgingId] = useState(null);

  if (selectedLodgingId !== null) {
    return (
      <LodgingDetail
        key={selectedLodgingId}
        lodgingId={selectedLodgingId}
        onBack={() => setSelectedLodgingId(null)}
        onSaved={() => {
          setSelectedLodgingId(null);
          onUpdated();
        }}
      />
    );
  }

  if (creating) {
    return (
      <LodgingForm
        onCancel={() => setCreating(false)}
        onSaved={() => {
          setCreating(false);
          onCreated();
        }}
      />
    );
  }

  return (
    <div className="owner-view">
      <header className="owner-overview-heading">
        <div>
          <h1>숙소 정보 관리</h1>
          <p>내가 등록한 숙소와 심사 상태를 확인할 수 있어요.</p>
        </div>
        <button
          type="button"
          className="owner-overview-action"
          onClick={() => setCreating(true)}
        >
          + 새 숙소 등록
        </button>
      </header>

      <section className="owner-panel" aria-labelledby="owner-lodging-list-title">
        <div className="owner-panel__head">
          <h2 id="owner-lodging-list-title">내 숙소 목록</h2>
          {lodgings && !error && <span>{lodgings.length}건</span>}
        </div>
        {error || !lodgings?.length ? (
          <OwnerLodgingStatus lodgings={lodgings} error={error} />
        ) : (
          <ul>
            {lodgings.map(({ lodgingId, name, status }) => (
              <li key={lodgingId}>
                <button
                  type="button"
                  className="owner-overview-action owner-overview-action--line"
                  onClick={() => setSelectedLodgingId(lodgingId)}
                >
                  {name} 상세 보기
                </button>
                {' · '}{lodgingStatusLabels[status] ?? status}
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

function LodgingForm({ initialDetail, onSaved, onCancel }) {
  const editing = Boolean(initialDetail);
  const lodgingTypes = ['호텔', '리조트', '펜션', '게스트하우스', '한옥'];
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const inFlight = useRef(false);

  async function submit(event) {
    event.preventDefault();
    if (inFlight.current) return;

    const form = event.currentTarget;
    const fields = new FormData(form);
    const name = fields.get('name').trim();
    const address = fields.get('address').trim();

    if (!name || !address) {
      setError('숙소명과 주소를 입력해 주세요.');
      return;
    }

    inFlight.current = true;
    setBusy(true);
    setError('');

    try {
      const data = {
        name,
        address,
        lodgingType: fields.get('lodgingType'),
        latitude: Number(fields.get('latitude')),
        longitude: Number(fields.get('longitude')),
        checkinTime: fields.get('checkinTime'),
        checkoutTime: fields.get('checkoutTime'),
      };

      if (editing) {
        await putJsonWithCsrf(`/api/owner/lodgings/${initialDetail.lodgingId}`, data);
      } else {
        await postJsonWithCsrf('/api/owner/lodgings', data);
        form.reset();
      }

      onSaved();
    } catch (error) {
      setError(error.status === 400
        ? '입력값을 확인해 주세요. 좌표는 소수점 7자리까지 입력할 수 있어요.'
        : error.message);
    } finally {
      inFlight.current = false;
      setBusy(false);
    }
  }

  return (
    <div className="owner-view">
      <div className="owner-view__title">
        <div>
          <span>PROPERTY</span>
          <h1>{editing ? '숙소 정보 수정' : '숙소 등록'}</h1>
          <p>{editing ? '변경할 정보를 확인하고 저장해 주세요.' : '기본 정보를 제출하면 관리자 승인 대기 상태로 저장돼요.'}</p>
        </div>
      </div>

      <form className="owner-property-form" onSubmit={submit} aria-busy={busy}>
        <section className="owner-panel">
          <div className="owner-panel__head">
            <h2>기본 정보와 위치</h2>
            <span className="owner-required">* 필수 입력</span>
          </div>
          <div className="owner-form-grid">
            <label>숙소명 *
              <input name="name" maxLength={200} defaultValue={initialDetail?.name} required />
            </label>
            <label>숙소 유형 *
              <select name="lodgingType" defaultValue={initialDetail?.lodgingType ?? ''} required>
                <option value="">선택해 주세요</option>
                {initialDetail?.lodgingType && !lodgingTypes.includes(initialDetail.lodgingType) && (
                  <option>{initialDetail.lodgingType}</option>
                )}
                {lodgingTypes.map((type) => <option key={type}>{type}</option>)}
              </select>
            </label>
            <label className="owner-wide">주소 *
              <input name="address" maxLength={300} defaultValue={initialDetail?.address} required />
            </label>
            <label>위도 *
              <input name="latitude" type="number" min="-90" max="90" step="0.0000001"
                defaultValue={initialDetail?.latitude} required />
            </label>
            <label>경도 *
              <input name="longitude" type="number" min="-180" max="180" step="0.0000001"
                defaultValue={initialDetail?.longitude} required />
            </label>
            <label>체크인 시간 *
              <input name="checkinTime" type="time" step="1" defaultValue={initialDetail?.checkinTime} required />
            </label>
            <label>체크아웃 시간 *
              <input name="checkoutTime" type="time" step="1" defaultValue={initialDetail?.checkoutTime} required />
            </label>
          </div>
          {error && <p role="alert">{error}</p>}
        </section>
        <div className="owner-form-actions">
          <button type="button" className="owner-secondary" onClick={onCancel} disabled={busy}>
            {editing ? '수정 취소' : '목록으로'}
          </button>
          <button type="submit" className="owner-primary" disabled={busy}>
            {busy ? '저장 중…' : editing ? '변경사항 저장' : '숙소 등록 신청'}
          </button>
        </div>
      </form>
    </div>
  );
}

function Rooms({ rooms, setRooms, setToast }) {
  const [stopDate, setStopDate] = useState('2026-11-12');
  const [selectedRoom, setSelectedRoom] = useState(rooms[0].id);
  const rateLabels = {
  offWeekday: '비수기 주중',
  offWeekend: '비수기 주말',
  peakWeekday: '성수기 주중',
  peakWeekend: '성수기 주말',
  };
  function updateRoom(id, key, value) { setRooms((current) => current.map((room) => room.id === id ? { ...room, [key]: value } : room)); }
  return (
    <div className="owner-view owner-rooms-view"><div className="owner-view__title"><div><span>ROOM &amp; RATE</span><h1>객실 · 요금 관리</h1><p>객실 기본 정보, 시즌 요금, 날짜별 재고와 판매 중지를 관리합니다.</p></div><button type="button" onClick={() => setToast('새 객실 등록 폼이 열렸습니다. 샘플에서는 기존 목록에만 표시됩니다.')}>＋ 객실 추가</button></div><section className="owner-panel owner-room-list"><div className="owner-panel__head"><div><span>ROOM TYPES</span><h2>등록 객실</h2></div><span>예약이 있는 객실은 삭제 대신 판매 중지</span></div>{rooms.map((room) => <article key={room.id} className={selectedRoom === room.id ? 'is-selected' : ''} onClick={() => setSelectedRoom(room.id)}><div className="owner-room-thumb"><span>ROOM {room.id}</span></div><div><h3>{room.name}</h3><p>기준 {room.basis}명 · 최대 {room.max}명 · 판매 수량 {room.stock}실</p><span>비수기 주중 {formatWon(room.offWeekday)}부터</span></div><StatusBadge>{room.stopped ? '판매 중지' : '판매 중'}</StatusBadge><button type="button">수정</button></article>)}</section><section className="owner-panel"><div className="owner-panel__head"><div><span>SEASON RATE</span><h2>기본 요금표</h2></div><button type="button" onClick={() => setToast('요금표를 저장했습니다. 이미 생성된 주문의 확정 요금은 변경되지 않습니다.')}>요금 저장</button></div><div className="owner-rate-table"><div className="owner-rate-row is-head"><span>객실</span><span>비수기 주중</span><span>비수기 주말</span><span>성수기 주중</span><span>성수기 주말</span></div>{rooms.map((room) => <div className="owner-rate-row" key={room.id}><strong>{room.name}</strong>{['offWeekday','offWeekend','peakWeekday','peakWeekend'].map((key) => <label key={key}><span className="owner-sr-only owner-rate-label">
  <span className="owner-sr-only">{room.name} </span>
  {rateLabels[key]}
</span><input type="number" step="1000" value={room[key]} onChange={(event) => updateRoom(room.id, key, Number(event.target.value))} /><i>원</i></label>)}</div>)}</div><p className="owner-rate-note">공휴일은 주말 요금을 적용합니다. 날짜별 직접 설정 요금은 자동 재계산으로 덮어쓰지 않습니다.</p></section><div className="owner-room-grid"><section className="owner-panel"><div className="owner-panel__head"><div><span>PEAK SEASON</span><h2>성수기 기간</h2></div></div><div className="owner-season-row"><label>시작일<input type="date" defaultValue="2026-12-20" /></label><span>~</span><label>종료일<input type="date" defaultValue="2027-01-03" /></label><button type="button">적용</button></div><p>성수기 기간 변경은 신규 판매 요금에만 반영합니다.</p></section><section className="owner-panel"><div className="owner-panel__head"><div><span>STOP SALE</span><h2>날짜별 판매 중지</h2></div></div><label className="owner-stop-date">날짜<input type="date" value={stopDate} onChange={(event) => setStopDate(event.target.value)} /></label><div className="owner-stop-list">{rooms.map((room) => <label key={room.id}><span><strong>{room.name}</strong><small>{stopDate} 판매 상태</small></span><input type="checkbox" checked={room.stopped} onChange={(event) => updateRoom(room.id, 'stopped', event.target.checked)} /><i /></label>)}</div><p>예약이 확정된 날짜는 판매 중지할 수 없도록 서버에서 다시 검증합니다.</p></section></div></div>
  );
}

export default function OwnerPage({ member, onLogout, loggingOut }) {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [toast, setToast] = useState('');
  const [rooms, setRooms] = useState(initialRooms);
  // const [property, setProperty] = useState({ name: '해운대 웨이브 호텔', type: '호텔', zip: '48099', address: '부산 해운대구 달맞이길 42', addressDetail: '스마트트래블 빌딩', phone: '051-000-1200', website: 'https://wave-hotel.example.com', description: '해운대 바다와 달맞이길을 함께 즐길 수 있는 도심형 호텔입니다.', checkIn: '15:00', checkOut: '11:00', parking: '투숙객은 객실당 1대 무료로 주차할 수 있습니다.', facilities: ['주차', '조식', '와이파이', '짐 보관'], cancellation: '체크인 5일 전까지 무료 취소, 이후 날짜별 환불 비율을 적용합니다.' });
  const [lodgings, setLodgings] = useState(null);
  const [lodgingError, setLodgingError] = useState('');
  const [lodgingRevision, setLodgingRevision] = useState(0);

  useEffect(() => {
    let cancelled = false;

    setLodgingError('');
    request('/api/owner/lodgings')
      .then((data) => {
        if (!cancelled) setLodgings(data);
      })
      .catch(() => {
        if (!cancelled) setLodgingError('숙소 정보를 불러오지 못했어요. 새로고침 후 다시 시도해 주세요.');
      });

    return () => {
      cancelled = true;
    };
  }, [lodgingRevision]);

  const view = useMemo(() => {
    if (activeTab === 'reservations') return <Reservations />;
    // if (activeTab === 'property') return <PropertyForm property={property} setProperty={setProperty} setToast={setToast} />;
    if (activeTab === 'property') {
      return (
        <LodgingManagement
          lodgings={lodgings}
          error={lodgingError}
          onUpdated={() => {
            setLodgingRevision((revision) => revision + 1);
            setToast('숙소 정보를 저장했어요.');
          }}
          onCreated={() => {
            setLodgingRevision((revision) => revision + 1);
            setToast('숙소 등록 신청이 접수됐어요. 관리자 승인 대기 상태예요.');
          }}
        />
      );
    }
    if (activeTab === 'rooms') return <Rooms rooms={rooms} setRooms={setRooms} setToast={setToast} />;
    // return <Dashboard onNavigate={setActiveTab} />;
    return <Dashboard onNavigate={setActiveTab} lodgings={lodgings} lodgingError={lodgingError} />;
  }, [activeTab, rooms, lodgings, lodgingError]);
  return (
    <div className="owner-page">
      {/* <OwnerHeader propertyName={property.name} member={member} onLogout={onLogout} loggingOut={loggingOut} /> */}
      <OwnerHeader
        propertyName={lodgings?.length === 1 ? lodgings[0].name : '업주센터'}
        member={member}
        onLogout={onLogout}
        loggingOut={loggingOut}
      />
      <div className="owner-shell">
        <aside className="owner-sidebar"><div className="owner-sidebar__status"><span>PARTNER STATUS</span><strong>업주 승인 완료</strong><small>본인 업장만 조회·수정 가능</small></div><nav aria-label="업주센터 메뉴">{tabs.map((tab) => <button type="button" key={tab.id} className={activeTab === tab.id ? 'is-active' : ''} onClick={() => { setActiveTab(tab.id); setToast(''); }}><i aria-hidden="true">{tab.id === 'dashboard' ? '▦' : tab.id === 'reservations' ? '▤' : tab.id === 'property' ? '⌂' : '₩'}</i>{tab.label}</button>)}</nav><div className="owner-sidebar__help"><strong>도움이 필요하신가요?</strong><span>파트너 운영 가이드를 확인하세요.</span><button type="button">운영 가이드</button></div></aside>
        <main className="owner-main">{toast && <div className="owner-toast" role="status">{toast}<button type="button" aria-label="알림 닫기" onClick={() => setToast('')}>×</button></div>}{view}<p className="owner-demo-caption">숙소 등록·조회는 서버에 연결되어 있어요. 예약·객실·요금은 예시 데이터예요.</p></main>
      </div>
    </div>
  );
}
