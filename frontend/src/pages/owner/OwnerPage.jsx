import { useEffect, useMemo, useRef, useState } from 'react';
import '../../css/owner.css';
import { postJsonWithCsrf, putJsonWithCsrf, request } from '../../api/auth.js';

const tabs = [
  { id: 'dashboard', label: '대시보드' },
  { id: 'reservations', label: '예약 현황' },
  { id: 'property', label: '숙소 정보' },
  { id: 'rooms', label: '객실 · 요금' },
];

// 예약과 결제가 한 번에 이뤄지므로 결제가 끝난 예약(CONFIRMED)은 「예약확정」이 아니라 「예약」으로 보여 준다 (피드백)
const reservationLabels = {
  CONFIRMED: '예약', PENDING_PAYMENT: '결제 대기', COMPLETED: '숙박 완료', CANCELLED: '취소', CANCELED: '취소',
  REFUNDING: '환불 중', REFUNDED: '환불', REFUND_FAILED: '환불 실패', NO_SHOW: '노쇼',
};

function formatWon(value) {
  return `${new Intl.NumberFormat('ko-KR').format(value)}원`;
}

function OwnerHeader({ propertyName, member, onLogout, loggingOut }) {
  return (
    <header className="owner-topbar">
      <div className="owner-topbar__inner">
        {/* 기존 로고 <a className="owner-brand" href="#/">smart <span>travel</span><small>partner</small></a> */}
        <a className="owner-brand brand-mark" href="#/" aria-label="AI TRIP 홈"><img className="brand-logo" src="/images/brand/ait_pin.svg" alt="" /><span className="brand-word"><b>AI</b> TRIP</span></a>
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
  const tone = ['예약', '체크인', '판매 중'].includes(children) ? 'green' : children.includes('예정') || children === '숙박 완료' ? 'blue' : ['체크아웃', '취소', '객실 없음'].includes(children) ? 'gray' : 'amber';
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

function Reservations() {
  const [status, setStatus] = useState('전체');
  const [query, setQuery] = useState('');
  const [selectedId, setSelectedId] = useState(null);
  const [rows, setRows] = useState(null);
  const [loadError, setLoadError] = useState('');

  useEffect(() => {
    let cancelled = false;
    request('/api/owner/reservations')
      .then((data) => { if (!cancelled) setRows(data.map((item) => ({
        id: item.reservationNo,
        guest: item.bookerName,
        room: `${item.lodgingName} · ${item.roomName}`,
        checkIn: item.checkIn,
        checkOut: item.checkOut,
        people: `${item.guestCount}명 · 객실 ${item.roomQuantity}개`,
        amount: item.totalPrice,
        phone: item.bookerPhone,
        memo: item.requestMemo,
        status: reservationLabels[item.status] || item.status,
      }))); })
      .catch(() => { if (!cancelled) setLoadError('예약을 불러오지 못했어요. 새로고침 후 다시 시도해 주세요.'); });
    return () => { cancelled = true; };
  }, []);

  const statuses = ['전체', '예약', '결제 대기', '숙박 완료', '취소'];
  const keyword = query.trim().toLowerCase();
  const visible = (rows || []).filter((item) => {
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
          {loadError && <p className="owner-booking-empty" role="alert">{loadError}</p>}
          {!rows && !loadError && <p className="owner-booking-empty" role="status">예약을 불러오는 중이에요.</p>}
          {rows && visible.length ? (
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
          ) : rows && (
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
              <div><dt>체크인</dt><dd>{selected.checkIn}</dd></div>
              <div><dt>체크아웃</dt><dd>{selected.checkOut}</dd></div>
              <div><dt>결제 금액</dt><dd>{formatWon(selected.amount)}</dd></div>
              <div><dt>연락처</dt><dd>{selected.phone}</dd></div>
              {selected.memo && <div><dt>요청사항</dt><dd>{selected.memo}</dd></div>}
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

const emptyRoom = () => ({ name: '', baseCapacity: 2, maxCapacity: 2, priceWeekday: 100000, priceWeekend: 120000, quantity: 1 });

function LodgingForm({ initialDetail, onSaved, onCancel }) {
  const editing = Boolean(initialDetail);
  // 피드백: 숙소 등록 때 객실 정보도 함께 넣는다 (등록할 때만 · 수정은 객실·요금 메뉴에서)
  const [roomRows, setRoomRows] = useState(() => [emptyRoom()]);
  const changeRoom = (index, key, value) => setRoomRows((current) => current.map((room, at) => at === index ? { ...room, [key]: value } : room));
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
      if (!editing) {
        const filled = roomRows.filter((room) => room.name.trim());
        if (!filled.length) throw Object.assign(new Error('객실을 1개 이상 입력해 주세요.'), { status: 0 });
        if (filled.some((room) => Number(room.maxCapacity) < Number(room.baseCapacity))) throw Object.assign(new Error('최대 인원은 기준 인원보다 적을 수 없어요.'), { status: 0 });
        data.rooms = filled.map((room) => ({ ...room, name: room.name.trim(), baseCapacity: Number(room.baseCapacity), maxCapacity: Number(room.maxCapacity), priceWeekday: Number(room.priceWeekday), priceWeekend: Number(room.priceWeekend), quantity: Number(room.quantity) }));
      }

      if (editing) {
        await putJsonWithCsrf(`/api/owner/lodgings/${initialDetail.lodgingId}`, data);
      } else {
        await postJsonWithCsrf('/api/owner/lodgings', data);
        form.reset();
        setRoomRows([emptyRoom()]);
      }

      onSaved();
    } catch (error) {
      setError(error.status === 400
        ? '입력값을 확인해 주세요. 좌표는 소수점 7자리까지, 객실 요금은 1,000원 이상이에요.'
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
          {editing && error && <p role="alert">{error}</p>}
        </section>
        {!editing && (
          <section className="owner-panel owner-room-inputs" aria-label="객실 정보">
            <div className="owner-panel__head">
              <h2>객실 정보</h2>
              <span className="owner-required">객실 1개 이상 · 판매 수량은 오늘부터 90일 동안 매일 적용</span>
            </div>
            {roomRows.map((room, index) => (
              <div className="owner-room-input" key={index}>
                <label>객실 이름 *<input value={room.name} maxLength={100} placeholder="예: 스탠다드 더블" onChange={(event) => changeRoom(index, 'name', event.target.value)} /></label>
                <label>기준 인원<input type="number" min="1" max="30" value={room.baseCapacity} onChange={(event) => changeRoom(index, 'baseCapacity', event.target.value)} /></label>
                <label>최대 인원<input type="number" min="1" max="30" value={room.maxCapacity} onChange={(event) => changeRoom(index, 'maxCapacity', event.target.value)} /></label>
                <label>주중 요금(원)<input type="number" min="1000" step="1000" value={room.priceWeekday} onChange={(event) => changeRoom(index, 'priceWeekday', event.target.value)} /></label>
                <label>주말 요금(원)<input type="number" min="1000" step="1000" value={room.priceWeekend} onChange={(event) => changeRoom(index, 'priceWeekend', event.target.value)} /></label>
                <label>하루 판매 수량<input type="number" min="0" max="500" value={room.quantity} onChange={(event) => changeRoom(index, 'quantity', event.target.value)} /></label>
                {roomRows.length > 1 && <button type="button" className="owner-secondary" onClick={() => setRoomRows((current) => current.filter((_, at) => at !== index))}>삭제</button>}
              </div>
            ))}
            <button type="button" className="owner-secondary" onClick={() => setRoomRows((current) => [...current, emptyRoom()])}>＋ 객실 추가</button>
            {error && <p role="alert">{error}</p>}
          </section>
        )}
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

const today = () => new Date(Date.now() + 9 * 3600 * 1000).toISOString().slice(0, 10);
const addDays = (day, count) => new Date(Date.parse(day) + count * 86400000).toISOString().slice(0, 10);

// 객실 · 요금: 내 숙소들의 실제 객실 (숙소 이름 표시) · 날짜별 판매 수량 정하기 · 객실 추가 (피드백)
function Rooms({ lodgings, setToast }) {
  const [rooms, setRooms] = useState(null);
  const [loadError, setLoadError] = useState('');
  const [revision, setRevision] = useState(0);
  const [selectedRoom, setSelectedRoom] = useState(null);
  const [days, setDays] = useState([]);
  const [range, setRange] = useState(() => ({ from: today(), to: addDays(today(), 6), quantity: 0 }));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [adding, setAdding] = useState(null);

  useEffect(() => {
    let cancelled = false;
    request('/api/owner/rooms')
      .then((data) => { if (!cancelled) { setRooms(data); setSelectedRoom((current) => current ?? data[0]?.roomId ?? null); } })
      .catch(() => { if (!cancelled) setLoadError('객실을 불러오지 못했어요. 새로고침 후 다시 시도해 주세요.'); });
    return () => { cancelled = true; };
  }, [revision]);

  useEffect(() => {
    if (!selectedRoom) { setDays([]); return undefined; }
    let cancelled = false;
    request(`/api/owner/rooms/${selectedRoom}/inventory?days=14`)
      .then((data) => { if (!cancelled) setDays(data); })
      .catch(() => { if (!cancelled) setDays([]); });
    return () => { cancelled = true; };
  }, [selectedRoom, revision]);

  const room = rooms?.find((item) => item.roomId === selectedRoom);

  async function saveQuantity(event) {
    event.preventDefault();
    setBusy(true); setError('');
    try {
      await putJsonWithCsrf(`/api/owner/rooms/${selectedRoom}/inventory`, { from: range.from, to: range.to, quantity: Number(range.quantity) });
      setToast(`${room.lodgingName} · ${room.name}: ${range.from} ~ ${range.to} 판매 수량을 ${range.quantity}개로 정했어요.${Number(range.quantity) === 0 ? ' 이 기간은 여행자에게 「객실 없음」으로 보여요.' : ''}`);
      setRevision((value) => value + 1);
    } catch (failure) {
      setError(failure.status === 409 ? (failure.message || '이미 예약된 객실보다 적게 정할 수 없어요.') : failure.message);
    } finally { setBusy(false); }
  }

  async function addRoom(event) {
    event.preventDefault();
    setBusy(true); setError('');
    try {
      const { lodgingId, ...input } = adding;
      const created = await postJsonWithCsrf(`/api/owner/lodgings/${lodgingId}/rooms`, {
        ...input, name: input.name.trim(), baseCapacity: Number(input.baseCapacity), maxCapacity: Number(input.maxCapacity),
        priceWeekday: Number(input.priceWeekday), priceWeekend: Number(input.priceWeekend), quantity: Number(input.quantity),
      });
      setToast(`${created.lodgingName}에 「${created.name}」 객실을 추가했어요.`);
      setAdding(null); setSelectedRoom(created.roomId); setRevision((value) => value + 1);
    } catch (failure) {
      setError(failure.status === 400 ? '객실 입력값을 확인해 주세요. 요금은 1,000원 이상, 최대 인원은 기준 인원 이상이에요.' : failure.message);
    } finally { setBusy(false); }
  }

  return (
    <div className="owner-view owner-rooms-view">
      <div className="owner-view__title">
        <div><span>ROOM &amp; RATE</span><h1>객실 · 요금 관리</h1><p>객실마다 어느 숙소의 객실인지 함께 보여요. 날짜별 판매 수량을 0으로 정하면 여행자 화면에 「객실 없음」으로 보이고 예약이 막혀요.</p></div>
        {lodgings?.length > 0 && !adding && <button type="button" onClick={() => setAdding({ lodgingId: lodgings[0].lodgingId, ...emptyRoom() })}>＋ 객실 추가</button>}
      </div>
      {adding && (
        <form className="owner-panel owner-room-inputs" onSubmit={addRoom} aria-label="객실 추가">
          <div className="owner-panel__head"><h2>객실 추가</h2></div>
          <div className="owner-room-input">
            <label>숙소 *<select value={adding.lodgingId} onChange={(event) => setAdding({ ...adding, lodgingId: Number(event.target.value) })}>{lodgings.map((lodging) => <option key={lodging.lodgingId} value={lodging.lodgingId}>{lodging.name}</option>)}</select></label>
            <label>객실 이름 *<input required maxLength={100} value={adding.name} onChange={(event) => setAdding({ ...adding, name: event.target.value })} /></label>
            <label>기준 인원<input type="number" min="1" max="30" value={adding.baseCapacity} onChange={(event) => setAdding({ ...adding, baseCapacity: event.target.value })} /></label>
            <label>최대 인원<input type="number" min="1" max="30" value={adding.maxCapacity} onChange={(event) => setAdding({ ...adding, maxCapacity: event.target.value })} /></label>
            <label>주중 요금(원)<input type="number" min="1000" step="1000" value={adding.priceWeekday} onChange={(event) => setAdding({ ...adding, priceWeekday: event.target.value })} /></label>
            <label>주말 요금(원)<input type="number" min="1000" step="1000" value={adding.priceWeekend} onChange={(event) => setAdding({ ...adding, priceWeekend: event.target.value })} /></label>
            <label>하루 판매 수량<input type="number" min="0" max="500" value={adding.quantity} onChange={(event) => setAdding({ ...adding, quantity: event.target.value })} /></label>
          </div>
          <div className="owner-form-actions"><button type="button" className="owner-secondary" onClick={() => setAdding(null)} disabled={busy}>취소</button><button type="submit" className="owner-primary" disabled={busy}>{busy ? '저장 중…' : '객실 추가'}</button></div>
        </form>
      )}
      {loadError && <p role="alert">{loadError}</p>}
      {!rooms && !loadError && <p role="status">객실을 불러오는 중이에요.</p>}
      {rooms && !rooms.length && <p className="owner-panel">등록된 객실이 없어요. 숙소를 등록하거나 「＋ 객실 추가」로 객실을 넣어 주세요.</p>}
      {rooms?.length > 0 && (
        <section className="owner-panel owner-room-list" aria-label="등록 객실">
          <div className="owner-panel__head"><div><span>ROOM TYPES</span><h2>등록 객실</h2></div></div>
          {rooms.map((item) => (
            <article key={item.roomId} className={selectedRoom === item.roomId ? 'is-selected' : ''} onClick={() => setSelectedRoom(item.roomId)}>
              <div>
                <span className="owner-room-lodging">{item.lodgingName}</span>
                <h3>{item.name}</h3>
                <p>기준 {item.baseCapacity}명 · 최대 {item.maxCapacity}명 · 오늘 남은 객실 {item.todayRemaining ?? 0} / {item.todayTotal ?? 0}</p>
                <span>주중 {formatWon(item.priceWeekday)} · 주말 {formatWon(item.priceWeekend)}</span>
              </div>
              <StatusBadge>{item.todayRemaining > 0 ? '판매 중' : '객실 없음'}</StatusBadge>
            </article>
          ))}
        </section>
      )}
      {room && (
        <section className="owner-panel" aria-label="날짜별 판매 수량">
          <div className="owner-panel__head"><div><span>INVENTORY</span><h2>{room.lodgingName} · {room.name} 날짜별 판매 수량</h2></div></div>
          <form className="owner-season-row" onSubmit={saveQuantity}>
            <label>시작일<input type="date" value={range.from} onChange={(event) => setRange({ ...range, from: event.target.value })} /></label>
            <span>~</span>
            <label>종료일<input type="date" value={range.to} min={range.from} onChange={(event) => setRange({ ...range, to: event.target.value })} /></label>
            <label>판매 수량<input type="number" min="0" max="500" value={range.quantity} onChange={(event) => setRange({ ...range, quantity: event.target.value })} /></label>
            <button type="submit" disabled={busy}>{busy ? '저장 중…' : '적용'}</button>
          </form>
          {error && <p role="alert">{error}</p>}
          <div className="owner-inventory-table">
            <table>
              <thead><tr><th scope="col">날짜</th><th scope="col">판매 수량</th><th scope="col">예약</th><th scope="col">남은 객실</th><th scope="col">요금</th></tr></thead>
              <tbody>{days.map((day) => <tr key={day.date}><td>{day.date}</td><td>{day.total}</td><td>{day.used}</td><td>{day.remaining > 0 ? day.remaining : <strong>객실 없음</strong>}</td><td>{formatWon(day.price)}</td></tr>)}</tbody>
            </table>
          </div>
        </section>
      )}
    </div>
  );
}

export default function OwnerPage({ member, onLogout, loggingOut }) {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [toast, setToast] = useState('');
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
    if (activeTab === 'rooms') return <Rooms lodgings={lodgings} setToast={setToast} />;
    // return <Dashboard onNavigate={setActiveTab} />;
    return <Dashboard onNavigate={setActiveTab} lodgings={lodgings} lodgingError={lodgingError} />;
  }, [activeTab, lodgings, lodgingError]);
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
        <main className="owner-main">{toast && <div className="owner-toast" role="status">{toast}<button type="button" aria-label="알림 닫기" onClick={() => setToast('')}>×</button></div>}{view}<p className="owner-demo-caption">숙소·예약 현황·객실·판매 수량은 서버에 연결되어 있어요. 대시보드 숫자는 아직 예시 데이터예요.</p></main>
      </div>
    </div>
  );
}
