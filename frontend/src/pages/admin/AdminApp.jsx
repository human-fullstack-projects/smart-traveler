import { useCallback, useEffect, useState } from 'react';
import './Admin.css';
import { adminGet, adminPost } from '../../api/admin';

// 관리자 페이지 (ADM-01~19) — 서버 /api/admin (M55) 을 부르는 React 화면

const MENU = [
  { path: '/admin', label: '대시보드' },
  { path: '/admin/orders', label: '주문 관리' },
  { path: '/admin/lodgings', label: '숙소 관리' },
  { path: '/admin/members', label: '회원 관리', badge: 'pendingOwners' },
  { path: '/admin/human-courses', label: '휴먼 추천 코스', badge: 'openReports' },
  { path: '/admin/ai-courses', label: 'AI 코스 추천' },
  { path: '/admin/tour-data', label: '관광지 데이터' },
];

const LABEL = {
  ACTIVE: '정상', SUSPENDED: '정지', WITHDRAWN: '탈퇴', DELETED: '삭제',
  USER: '여행자', OWNER: '업주', ADMIN: '관리자',
  PENDING: '승인대기', APPROVED: '승인', REJECTED: '반려', INACTIVE: '운영중지', CLOSED: '폐업',
  PENDING_PAYMENT: '결제대기', CONFIRMED: '예약확정', COMPLETED: '숙박완료', CANCELLED: '취소', CANCELED: '취소', REFUNDED: '환불', NO_SHOW: '노쇼', EXPIRED: '만료',
  PUBLISHED: '게시', BLINDED: '블라인드', HIDDEN: '숨김', RESOLVED: '처리완료', DISMISSED: '기각', OPEN: '신고 접수', RECEIVED: '신고 접수',
  done: '완료', failed: '생성실패', generating: '생성중', preview: '미리보기', selected: '선택됨', confirmed: '확정',
  SUCCESS: '성공', FAILED: '실패', PARTIAL: '일부 성공',
  PENSION: '펜션', HOTEL: '호텔', MOTEL: '모텔', GUESTHOUSE: '게스트하우스', HANOK: '한옥', RESORT: '리조트', CAMPING: '캠핑',
};
const tone = (s) => (['SUSPENDED', 'REJECTED', 'CANCELLED', 'CANCELED', 'BLINDED', 'failed', 'FAILED', 'DELETED', 'WITHDRAWN'].includes(s) ? 'bad'
  : ['PENDING', 'PENDING_PAYMENT', 'OPEN', 'RECEIVED', 'generating'].includes(s) ? 'warn'
  : ['INACTIVE', 'HIDDEN', 'CLOSED', 'EXPIRED'].includes(s) ? 'off' : '');
const Pill = ({ v }) => (v == null || v === '' ? '—' : <span className={`adm-pill ${tone(v)}`}>{LABEL[v] || v}</span>);
const date = (v) => (v ? String(v).replace('T', ' ').replace(/\.\d+Z?$/, '').replace(/Z$/, '').slice(0, 16) : '—');
const won = (v) => (v == null ? '—' : `${Number(v).toLocaleString('ko-KR')}원`);
const show = (v) => (v == null || v === '' ? '—' : String(v));

function useLoad(fn, deps) {
  const [state, setState] = useState({ data: null, error: '', loading: true });
  const load = useCallback(() => {
    setState((s) => ({ ...s, loading: true }));
    fn().then((data) => setState({ data, error: '', loading: false }))
      .catch((e) => setState({ data: null, error: e.message, loading: false }));
  }, deps); // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(load, [load]);
  return [state, load];
}

const PAGE_SIZES = [3, 5, 10];

function Pager({ page, pages, size, total, onPage, onSize }) {
  if (total === 0) return null;
  const nums = [];
  const from = Math.max(1, Math.min(page - 2, pages - 4));
  for (let i = from; i <= Math.min(pages, from + 4); i += 1) nums.push(i);
  return <div className="adm-pager">
    <span className="st-note">{(page - 1) * size + 1}–{Math.min(page * size, total)} / {total}건</span>
    <div className="adm-pages">
      <button type="button" disabled={page === 1} onClick={() => onPage(1)} aria-label="처음">«</button>
      <button type="button" disabled={page === 1} onClick={() => onPage(page - 1)} aria-label="이전">‹</button>
      {nums.map((n) => <button type="button" key={n} aria-current={n === page ? 'page' : undefined} onClick={() => onPage(n)}>{n}</button>)}
      <button type="button" disabled={page === pages} onClick={() => onPage(page + 1)} aria-label="다음">›</button>
      <button type="button" disabled={page === pages} onClick={() => onPage(pages)} aria-label="마지막">»</button>
    </div>
    <label className="adm-size">
      <select value={size} onChange={(e) => onSize(Number(e.target.value))}>{PAGE_SIZES.map((n) => <option key={n} value={n}>{n}개씩</option>)}</select>
    </label>
  </div>;
}

function Table({ title, rows, columns, rowKey, selected, onSelect, empty = '자료가 없습니다.', pageSize = 5 }) {
  const [size, setSize] = useState(pageSize);
  const [page, setPage] = useState(1);
  const total = rows ? rows.length : 0;
  const pages = Math.max(1, Math.ceil(total / size));
  useEffect(() => { if (page > pages) setPage(pages); }, [page, pages]);
  const shown = rows ? rows.slice((page - 1) * size, page * size) : null;
  return <div className="adm-card">
    {title && <h2 className="adm-card-title">{title} <span className="st-note">{rows ? `${rows.length}건` : ''}</span></h2>}
    <div className="adm-table-wrap">
      {!rows ? <p className="adm-empty">불러오는 중…</p> : rows.length === 0 ? <p className="adm-empty">{empty}</p> :
        <table className="adm-table"><thead><tr>{columns.map((c) => <th key={c[0]}>{c[0]}</th>)}</tr></thead>
          <tbody>{shown.map((r) => <tr key={r[rowKey]} aria-selected={selected === r[rowKey]} onClick={() => onSelect?.(r[rowKey])}>
            {columns.map((c) => <td key={c[0]}>{c[1](r)}</td>)}</tr>)}</tbody></table>}
    </div>
    <Pager page={Math.min(page, pages)} pages={pages} size={size} total={total} onPage={setPage} onSize={(n) => { setSize(n); setPage(1); }} />
  </div>;
}

function Detail({ title, children }) {
  return <div className="adm-card"><h2 className="adm-card-title">{title}</h2><div className="adm-detail">{children}</div></div>;
}
const DL = ({ items }) => <dl className="adm-dl">{items.map(([k, v]) => [<dt key={`${k}t`}>{k}</dt>, <dd key={`${k}d`}>{v}</dd>])}</dl>;
const Pick = () => <p className="adm-empty">왼쪽 목록에서 하나를 고르면 자세한 내용이 나와요.</p>;

function useAction(after) {
  const [msg, setMsg] = useState(null);
  const run = async (fn, okText) => {
    setMsg(null);
    try { await fn(); setMsg({ ok: true, text: okText }); after?.(); }
    catch (e) { setMsg({ ok: false, text: e.message }); }
  };
  return [msg, run];
}
const Msg = ({ msg }) => (msg ? <p className={`adm-msg${msg.ok ? '' : ' error'}`} role="status">{msg.text}</p> : null);

function Filters({ fields, value, onChange }) {
  return <div className="adm-filters">{fields.map(([name, label, options]) => <label key={name} className="st-field">{label}
    {options ? <select className="st-input" value={value[name] || ''} onChange={(e) => onChange({ ...value, [name]: e.target.value })}>
      <option value="">전체</option>{options.map((o) => <option key={o} value={o}>{LABEL[o] || o}</option>)}</select>
      : <input className="st-input" value={value[name] || ''} placeholder="검색어" onChange={(e) => onChange({ ...value, [name]: e.target.value })} />}
  </label>)}</div>;
}

/* ---------- 대시보드 ---------- */
function Dashboard({ stats }) {
  const s = stats || {};
  const cards = [['전체 회원', s.members, '/admin/members'], ['승인 대기 업주', s.pendingOwners, '/admin/members', true],
    ['등록 숙소', s.lodgings, '/admin/lodgings'], ['전체 주문', s.orders, '/admin/orders'],
    ['휴먼 추천 코스', s.humanCourses, '/admin/human-courses'], ['미처리 신고', s.openReports, '/admin/human-courses', true]];
  return <>
    <div className="adm-stats">{cards.map(([label, n, href, alert]) => <a key={label} href={`#${href}`} className={`adm-stat${alert && n > 0 ? ' alert' : ''}`}>
      <span>{label}</span><strong>{n ?? '…'}</strong></a>)}</div>
    <div className="st-panel"><p className="st-note" style={{ margin: 0 }}>숫자를 누르면 해당 관리 화면으로 이동해요. 승인 대기 업주·미처리 신고가 있으면 빨간 숫자로 보여요.</p></div>
  </>;
}

/* ---------- 공통: 탭 · 작은 목록(3개씩) ---------- */
function Tabs({ items, value, onChange }) {
  return <div className="adm-tabs" role="tablist">{items.map(([key, label, count]) =>
    <button key={key} type="button" role="tab" aria-selected={value === key} onClick={() => onChange(key)}>
      {label}{count != null && <span className="adm-tab-count">{count}</span>}</button>)}</div>;
}

function MiniList({ title, rows, columns, empty = '없음', size = 3 }) {
  const [page, setPage] = useState(1);
  const list = rows || [];
  const pages = Math.max(1, Math.ceil(list.length / size));
  const p = Math.min(page, pages);
  return <>
    <p className="adm-sub">{title} {list.length > 0 && <span className="st-note">{list.length}건</span>}
      {pages > 1 && <span className="adm-mini-pager">
        <button type="button" disabled={p === 1} onClick={() => setPage(p - 1)} aria-label="이전">‹</button>
        {p}/{pages}
        <button type="button" disabled={p === pages} onClick={() => setPage(p + 1)} aria-label="다음">›</button></span>}</p>
    {list.length === 0 ? <p className="st-note" style={{ margin: 0 }}>{empty}</p>
      : <table className="adm-table adm-mini"><thead><tr>{columns.map((c, j) => <th key={j}>{c[0]}</th>)}</tr></thead>
        <tbody>{list.slice((p - 1) * size, p * size).map((r, i) => <tr key={i}>{columns.map((c, j) => <td key={j}>{c[1](r)}</td>)}</tr>)}</tbody></table>}
  </>;
}
const histCols = [['일시', (h) => date(h.createdAt || h.created_at)], ['처리', (h) => show(h.action || h.toStatus || h.to_status)], ['사유', (h) => show(h.reason)]];

/* ---------- 주문 (ADM-02~04) ---------- */
function Orders() {
  const [f, setF] = useState({});
  const [list, reload] = useLoad(() => adminGet('/orders', f), [JSON.stringify(f)]);
  const [sel, setSel] = useState(null);
  const [detail, reloadDetail] = useLoad(() => (sel ? adminGet(`/orders/${sel}`) : Promise.resolve(null)), [sel]);
  const [reason, setReason] = useState('');
  const [msg, run] = useAction(() => { reload(); reloadDetail(); });
  const o = detail.data?.order;
  const act = (status) => run(() => adminPost(`/orders/${sel}/status`, { status, reason }), `주문 상태를 「${LABEL[status]}」(으)로 바꿨어요.`);
  return <>
    <Filters fields={[['status', '주문 상태', ['PENDING_PAYMENT', 'CONFIRMED', 'COMPLETED', 'CANCELLED']], ['type', '주문 유형', ['RESERVATION', 'CONFIRMED']], ['query', '주문번호·회원']]} value={f} onChange={setF} />
    <Msg msg={msg || (list.error && { text: list.error })} />
    <div className="adm-split">
      <Table title="주문 목록" rows={list.data} rowKey="reservationId" selected={sel} onSelect={setSel} empty="아직 주문(객실 예약)이 없어요."
        columns={[['주문번호', (r) => show(r.reservationNo)], ['체크인', (r) => show(r.checkinDate)], ['인원', (r) => show(r.guestCount)], ['금액', (r) => won(r.totalPrice)], ['상태', (r) => <Pill v={r.status} />]]} />
      <Detail title="주문 상세">{!o ? <Pick /> : <>
        <DL items={[['주문번호', show(o.reservationNo)], ['상태', <Pill v={o.status} />], ['회원', show(o.memberId)], ['예약자', `${show(o.bookerName)} · ${show(o.bookerPhone)}`],
          ['객실', show(o.roomId)], ['인원', show(o.guestCount)], ['일정', `${show(o.checkinDate)} ~ ${show(o.checkoutDate)}`], ['금액', won(o.totalPrice)], ['연결 코스', show(o.courseId)], ['요청', show(o.requestMemo)]]} />
        <MiniList title="결제" rows={detail.data.payments} empty="결제 기록 없음" columns={[['상태', (p) => <Pill v={p.status} />], ['금액', (p) => won(p.amount)], ['승인', (p) => date(p.approvedAt)]]} />
        <MiniList title="처리 이력" rows={detail.data.history || detail.data.actions} empty="처리 이력 없음" columns={histCols} />
        <div className="adm-actions">
          <input className="st-input" placeholder="처리 사유 (취소·환불 시 필수)" value={reason} onChange={(e) => setReason(e.target.value)} />
          <button className="st-btn" onClick={() => act('CONFIRMED')}>예약 확정</button>
          <button className="st-btn st-btn-line" onClick={() => act('COMPLETED')}>숙박 완료</button>
          <button className="st-btn st-btn-line" onClick={() => act('CANCELLED')}>주문 취소</button>
        </div>
      </>}</Detail>
    </div>
  </>;
}

/* ---------- 숙소 (ADM-05~07) ---------- */
function Lodgings() {
  const [f, setF] = useState({});
  const [list, reload] = useLoad(() => adminGet('/lodgings', f), [JSON.stringify(f)]);
  const [sel, setSel] = useState(null);
  const [detail, reloadDetail] = useLoad(() => (sel ? adminGet(`/lodgings/${sel}`) : Promise.resolve(null)), [sel]);
  const [reason, setReason] = useState('');
  const [msg, run] = useAction(() => { reload(); reloadDetail(); });
  const l = detail.data?.lodging;
  const act = (status) => run(() => adminPost(`/lodgings/${sel}/status`, { status, reason: reason || null }), `숙소 상태를 「${LABEL[status]}」(으)로 바꿨어요.`);
  return <>
    <Filters fields={[['status', '운영 상태', ['PENDING', 'ACTIVE', 'REJECTED', 'INACTIVE']], ['query', '숙소 이름']]} value={f} onChange={setF} />
    <Msg msg={msg || (list.error && { text: list.error })} />
    <div className="adm-split">
      <Table title="숙소 목록" rows={list.data} rowKey="lodgingId" selected={sel} onSelect={setSel}
        columns={[['숙소', (r) => r.name], ['유형', (r) => LABEL[r.lodgingType] || show(r.lodgingType)], ['주소', (r) => show(r.address)], ['상태', (r) => <Pill v={r.status} />]]} />
      <Detail title="숙소 상세">{!l ? <Pick /> : <>
        <DL items={[['숙소', l.name], ['상태', <Pill v={l.status} />], ['유형', LABEL[l.lodgingType] || show(l.lodgingType)], ['연락처', show(l.phone)], ['주소', show(l.address)],
          ['체크인/아웃', `${show(l.checkinTime).slice(0, 5)} / ${show(l.checkoutTime).slice(0, 5)}`], ['업주', show(l.ownerId)], ['등록', date(l.createdAt)], ['반려 사유', show(l.rejectReason)], ['예약', `${(detail.data.reservations || []).length}건`]]} />
        <MiniList title="객실" rows={detail.data.rooms} empty="객실 없음" columns={[['객실', (r) => r.name], ['인원', (r) => `${r.baseCapacity}~${r.maxCapacity}명`], ['주중', (r) => won(r.priceOffWeekday)], ['주말', (r) => won(r.priceOffWeekend)]]} />
        <div className="adm-actions">
          <input className="st-input" placeholder="반려·운영중지 사유" value={reason} onChange={(e) => setReason(e.target.value)} />
          <button className="st-btn" onClick={() => act('ACTIVE')}>승인 · 운영</button>
          <button className="st-btn st-btn-line" onClick={() => act('REJECTED')}>반려</button>
          <button className="st-btn st-btn-line" onClick={() => act('INACTIVE')}>운영 중지</button>
        </div>
      </>}</Detail>
    </div>
  </>;
}

/* ---------- 회원 (ADM-08~11) ---------- */
function Members({ onChanged }) {
  const [tab, setTab] = useState('all');
  const [f, setF] = useState({});
  const [list, reload] = useLoad(() => adminGet('/members', f), [JSON.stringify(f)]);
  const [pending, reloadPending] = useLoad(() => adminGet('/owners/pending'), []);
  const [sel, setSel] = useState(null);
  const [detail, reloadDetail] = useLoad(() => (sel ? adminGet(`/members/${sel}`) : Promise.resolve(null)), [sel]);
  const [reason, setReason] = useState('');
  const [msg, run] = useAction(() => { reload(); reloadPending(); reloadDetail(); onChanged?.(); });
  const m = detail.data?.member; const op = detail.data?.ownerProfile;
  const review = (decision) => run(() => adminPost(`/owners/${op.ownerId}/review`, { decision, comment: reason || null }), decision === 'APPROVED' ? '업주 가입을 승인했어요.' : '업주 가입을 반려했어요.');
  const status = (s) => run(() => adminPost(`/members/${sel}/status`, { status: s, reasonCode: 'OTHER', reason: reason || null }), `회원 상태를 「${LABEL[s]}」(으)로 바꿨어요.`);
  return <>
    <div className="adm-toolbar">
      <Tabs value={tab} onChange={setTab} items={[['all', '전체 회원', list.data?.length], ['pending', '승인 대기 업주', pending.data?.length]]} />
      {tab === 'all' && <Filters fields={[['role', '회원 유형', ['USER', 'OWNER', 'ADMIN']], ['status', '계정 상태', ['ACTIVE', 'SUSPENDED', 'WITHDRAWN']], ['query', '아이디·닉네임']]} value={f} onChange={setF} />}
    </div>
    <Msg msg={msg || (list.error && { text: list.error })} />
    <div className="adm-split">
      {tab === 'all'
        ? <Table title="회원 목록" rows={list.data} rowKey="memberId" selected={sel} onSelect={setSel}
          columns={[['아이디', (r) => r.loginId], ['닉네임', (r) => show(r.nickname)], ['유형', (r) => <Pill v={r.roleCode} />], ['가입일', (r) => date(r.createdAt).slice(0, 10)], ['상태', (r) => <Pill v={r.status} />]]} />
        : <Table title="승인 대기 업주" rows={pending.data} rowKey="memberId" selected={sel} onSelect={setSel} empty="승인을 기다리는 업주가 없어요."
          columns={[['상호', (r) => r.businessName], ['대표', (r) => r.representativeName], ['사업자번호', (r) => r.businessNumber], ['신청일', (r) => date(r.createdAt).slice(0, 10)], ['심사', (r) => <Pill v={r.reviewStatusCode} />]]} />}
      <Detail title="회원 상세">{!m ? <Pick /> : <>
        <DL items={[['아이디', m.loginId], ['닉네임', show(m.nickname)], ['유형', <Pill v={m.roleCode} />], ['상태', <Pill v={m.status} />], ['이메일', show(m.email)], ['가입일', date(m.createdAt)]]} />
        {op && <><p className="adm-sub">업주 정보</p>
          <DL items={[['상호', op.businessName], ['심사', <Pill v={op.reviewStatusCode} />], ['사업자번호', op.businessNumber], ['대표', op.representativeName], ['연락처', show(op.businessPhone)], ['숙소', `${(detail.data.ownerLodgings || []).length}곳`], ['주소', show(op.businessAddress)]]} /></>}
        {(detail.data.suspensions || []).length > 0 && <MiniList title="정지 이력" rows={detail.data.suspensions}
          columns={[['시작', (s) => date(s.startedAt || s.createdAt)], ['사유', (s) => show(s.reasonCode)], ['내용', (s) => show(s.reasonDetail || s.reason)]]} />}
        <div className="adm-actions">
          <input className="st-input" placeholder="사유 (정지·반려 시 입력)" value={reason} onChange={(e) => setReason(e.target.value)} />
          {op && op.reviewStatusCode === 'PENDING' && <><button className="st-btn" onClick={() => review('APPROVED')}>업주 승인</button>
            <button className="st-btn st-btn-line" onClick={() => review('REJECTED')}>업주 반려</button></>}
          {m.roleCode !== 'ADMIN' && (m.status === 'SUSPENDED'
            ? <button className="st-btn st-btn-line" onClick={() => status('ACTIVE')}>정지 해제</button>
            : <button className="st-btn st-btn-line" onClick={() => status('SUSPENDED')}>회원 정지</button>)}
        </div>
      </>}</Detail>
    </div>
  </>;
}

/* ---------- 휴먼 추천 코스 (ADM-12~14, 19) ---------- */
function HumanCourses({ onChanged }) {
  const [tab, setTab] = useState('courses');
  const [f, setF] = useState({});
  const [list, reload] = useLoad(() => adminGet('/human-courses', f), [JSON.stringify(f)]);
  const [reports, reloadReports] = useLoad(() => adminGet('/human-reports'), []);
  const [sel, setSel] = useState(null);
  const [detail, reloadDetail] = useLoad(() => (sel ? adminGet(`/human-courses/${sel}`) : Promise.resolve(null)), [sel]);
  const [reason, setReason] = useState('');
  const [msg, run] = useAction(() => { reload(); reloadReports(); reloadDetail(); onChanged?.(); });
  const c = detail.data?.course || detail.data?.post;
  const featured = detail.data?.featured;
  return <>
    <div className="adm-toolbar">
      <Tabs value={tab} onChange={setTab} items={[['courses', '휴먼 추천 코스', list.data?.length], ['reports', '신고', reports.data?.length]]} />
      {tab === 'courses' && <Filters fields={[['status', '게시 상태', ['PUBLISHED', 'BLINDED', 'DELETED']], ['query', '제목']]} value={f} onChange={setF} />}
    </div>
    <Msg msg={msg || (list.error && { text: list.error })} />
    <div className="adm-split">
      {tab === 'courses'
        ? <Table title="휴먼 추천 코스" rows={list.data} rowKey="id" selected={sel} onSelect={setSel} empty="아직 등록된 휴먼 추천 코스가 없어요."
          columns={[['제목', (r) => r.title], ['작성자', (r) => show(r.memberId)], ['작성일', (r) => date(r.createdAt).slice(0, 10)], ['노출', (r) => (r.featured ? '추천' : '—')], ['상태', (r) => <Pill v={r.status} />]]} />
        : <Table title="신고" rows={reports.data} rowKey="id" onSelect={(id) => { const r = reports.data.find((x) => x.id === id); if (r) setSel(r.courseId); }} empty="접수된 신고가 없어요."
          columns={[['코스', (r) => show(r.courseId)], ['사유', (r) => show(r.reason)], ['신고일', (r) => date(r.createdAt).slice(0, 10)], ['상태', (r) => <Pill v={r.status} />]]} />}
      <Detail title="코스 상세">{!c ? <Pick /> : <>
        <DL items={[['제목', c.title], ['상태', <Pill v={c.status} />], ['작성자', show(c.memberId)], ['연결 코스', show(c.courseId)], ['작성일', date(c.createdAt)], ['메인 노출', featured ? '추천 중' : '아님']]} />
        <p className="adm-sub">내용</p><p className="adm-clamp">{show(c.content)}</p>
        <MiniList title="신고" rows={detail.data.reports} empty="신고 없음"
          columns={[['사유', (r) => show(r.reason)], ['상태', (r) => <Pill v={r.status} />], ['', (r) => <button className="adm-link" onClick={() => run(() => adminPost(`/human-reports/${r.id}/resolve`, { status: 'RESOLVED', reason }), '신고를 처리했어요.')}>처리 완료</button>]]} />
        <MiniList title="처리 이력" rows={detail.data.history || detail.data.actions} empty="처리 이력 없음" columns={histCols} />
        <div className="adm-actions">
          <input className="st-input" placeholder="처리 사유" value={reason} onChange={(e) => setReason(e.target.value)} />
          {c.status === 'BLINDED' ? <button className="st-btn" onClick={() => run(() => adminPost(`/human-courses/${sel}/status`, { status: 'PUBLISHED', reason }), '블라인드를 해제했어요.')}>블라인드 해제</button>
            : <button className="st-btn st-btn-line" onClick={() => run(() => adminPost(`/human-courses/${sel}/status`, { status: 'BLINDED', reason }), '블라인드 처리했어요.')}>블라인드</button>}
          <button className="st-btn st-btn-line" onClick={() => run(() => adminPost(`/human-courses/${sel}/feature`, { featured: !featured }), featured ? '추천 노출을 해제했어요.' : '메인 추천으로 노출해요.')}>{featured ? '추천 해제' : '메인 추천 지정'}</button>
        </div>
      </>}</Detail>
    </div>
  </>;
}

/* ---------- AI 코스 (ADM-16~18) ---------- */
function AiCourses() {
  const [tab, setTab] = useState('courses');
  const [f, setF] = useState({});
  const [list] = useLoad(() => adminGet('/ai-courses', f), [JSON.stringify(f)]);
  const [errors] = useLoad(() => adminGet('/ai-courses/errors'), []);
  const [criteria, reloadCriteria] = useLoad(() => adminGet('/ai-courses/criteria'), []);
  const [sel, setSel] = useState(null);
  const [detail] = useLoad(() => (sel ? adminGet(`/ai-courses/${sel}`) : Promise.resolve(null)), [sel]);
  const [edit, setEdit] = useState({});
  const [msg, run] = useAction(reloadCriteria);
  const c = detail.data?.course || detail.data;
  return <>
    <div className="adm-toolbar">
      <Tabs value={tab} onChange={setTab} items={[['courses', 'AI 추천 코스', list.data?.length], ['errors', '생성 오류', errors.data?.length], ['criteria', '추천 기준', criteria.data?.length]]} />
      {tab === 'courses' && <Filters fields={[['status', '생성 상태', ['generating', 'preview', 'done', 'failed']], ['query', '제목·지역']]} value={f} onChange={setF} />}
    </div>
    <Msg msg={msg || (list.error && { text: list.error })} />
    {tab === 'criteria'
      ? <Table title="AI 추천 기준 (코스 생성에 쓰는 기준)" rows={criteria.data} rowKey="id"
        columns={[['기준', (r) => r.name], ['값', (r) => <input className="st-input adm-inline-input" value={edit[r.id] ?? r.value ?? ''} onClick={(e) => e.stopPropagation()} onChange={(e) => setEdit({ ...edit, [r.id]: e.target.value })} />],
          ['수정일', (r) => date(r.updatedAt)],
          ['', (r) => <button className="st-btn adm-inline-btn" onClick={(e) => { e.stopPropagation(); run(() => adminPost(`/ai-courses/criteria/${r.id}`, { value: edit[r.id] ?? r.value }), `「${r.name}」 기준을 저장했어요.`); }}>저장</button>]]} />
      : <div className="adm-split">
        {tab === 'courses'
          ? <Table title="AI 추천 코스" rows={list.data} rowKey="id" selected={sel} onSelect={setSel}
            columns={[['제목', (r) => show(r.title)], ['지역', (r) => show(r.destination)], ['일정', (r) => `${show(r.startDate).slice(5)} ~ ${show(r.endDate).slice(5)}`], ['상태', (r) => <Pill v={r.status} />]]} />
          : <Table title="생성 오류" rows={errors.data} rowKey="id" selected={sel} onSelect={setSel} empty="생성 오류가 없어요."
            columns={[['코스', (r) => show(r.title)], ['오류', (r) => show(r.errorMessage)], ['일시', (r) => date(r.updatedAt || r.createdAt)]]} />}
        <Detail title="코스 상세">{!c || !c.id ? <Pick /> : <DL items={[['제목', show(c.title)], ['상태', <Pill v={c.status} />], ['지역', show(c.destination)], ['인원', `성인 ${c.adults ?? 0} · 아이 ${c.children ?? 0}`],
          ['기간', `${show(c.startDate)} ~ ${show(c.endDate)}`], ['회원', show(c.memberNo)], ['모델', show(c.model)], ['생성일', date(c.createdAt)], ['요약', show(c.summary)], ['연결 예약', `${(detail.data?.reservations || []).length}건`], ['오류', show(c.errorMessage)]]} />}</Detail>
      </div>}
  </>;
}

/* ---------- 관광지 데이터 (EXT-01·07) ---------- */
function TourData() {
  const [tab, setTab] = useState('hubs');
  const [data, reload] = useLoad(() => adminGet('/tour-data'), []);
  const [busy, setBusy] = useState(false);
  const [msg, run] = useAction(reload);
  const d = data.data || {}; const n = d.counts || {};
  const sync = async () => { setBusy(true); await run(() => adminPost('/tour-data/sync', { region: '47130' }), '경주 관광지 데이터를 다시 받았어요.'); setBusy(false); };
  return <>
    <Msg msg={msg || (data.error && { text: data.error })} />
    <div className="adm-stats adm-stats-row">
      {[['관광 장소', n.places], ['좌표 있음', n.withCoords], ['사진 있음', n.withImage], ['중심 관광지', n.hubRanks], ['연관 관광지', n.related]].map(([k, v]) =>
        <div key={k} className="adm-stat"><span>{k}</span><strong>{v ?? '…'}</strong></div>)}
      <div className="adm-stat"><span>{(d.regions || [])[0]?.full_name || '경상북도 경주시'}</span>
        <button className="st-btn adm-inline-btn" style={{ marginTop: 8 }} disabled={busy} onClick={sync}>{busy ? '받는 중…' : '다시 받기'}</button></div>
    </div>
    <div className="adm-toolbar"><Tabs value={tab} onChange={setTab} items={[['hubs', '중심 관광지 순위', d.hubs?.length], ['logs', '수집 기록', d.logs?.length]]} /></div>
    {tab === 'hubs'
      ? <Table title="중심 관광지 순위 (기초지자체 중심 관광지)" rows={d.hubs} rowKey="hub_rank"
        columns={[['순위', (r) => r.hub_rank], ['관광지', (r) => r.hub_name], ['분류', (r) => show(r.category_middle)], ['주소', (r) => show(r.address)], ['기준', (r) => show(r.base_ym)]]} />
      : <Table title="수집 기록" rows={d.logs} rowKey="sync_log_id"
        columns={[['자료', (r) => show(r.data_type)], ['제공처', (r) => show(r.provider)], ['결과', (r) => <Pill v={r.status} />], ['건수', (r) => `${r.success_count ?? 0}/${r.total_count ?? 0}`], ['시각', (r) => date(r.finished_at || r.started_at)], ['메모', (r) => show(r.error_message)]]} />}
  </>;
}

const TITLES = {
  '/admin': ['대시보드', '오늘 처리할 일을 한눈에 봐요.'],
  '/admin/orders': ['주문 관리', '예약주문(결제대기)과 확정주문을 조회하고 상태를 처리해요.'],
  '/admin/lodgings': ['숙소 관리', '업주가 등록한 숙소를 승인하고 운영 상태를 관리해요.'],
  '/admin/members': ['회원 관리', '여행자·업주·관리자를 조회하고 업주 가입 승인, 회원 정지를 처리해요.'],
  '/admin/human-courses': ['휴먼 추천 코스', '신고된 코스를 블라인드하고 메인 추천 코스를 지정해요.'],
  '/admin/ai-courses': ['AI 코스 추천', '생성된 AI 코스와 생성 오류를 보고 추천 기준을 관리해요.'],
  '/admin/tour-data': ['관광지 데이터', '한국관광공사 관광지·중심 관광지·연관 관광지 수집 현황이에요.'],
};

export default function AdminApp({ route, member, onLogout }) {
  const [stats, setStats] = useState(null);
  const loadStats = useCallback(() => { adminGet('').then(setStats).catch(() => {}); }, []);
  useEffect(loadStats, [loadStats, route]);
  const path = TITLES[route] ? route : '/admin';
  const [title, sub] = TITLES[path];
  const page = { '/admin': <Dashboard stats={stats} />, '/admin/orders': <Orders />, '/admin/lodgings': <Lodgings />,
    '/admin/members': <Members onChanged={loadStats} />, '/admin/human-courses': <HumanCourses onChanged={loadStats} />,
    '/admin/ai-courses': <AiCourses />, '/admin/tour-data': <TourData /> }[path];
  return <div className="st-page adm">
    <aside className="adm-side" aria-label="관리자 메뉴">
      <a className="adm-logo" href="#/admin">smart travel<small>ADMIN</small></a>
      {MENU.map((m) => <a key={m.path} href={`#${m.path}`} aria-current={path === m.path ? 'page' : undefined}>{m.label}
        {m.badge && stats?.[m.badge] > 0 && <span className="adm-badge">{stats[m.badge]}</span>}</a>)}
      <div className="adm-side-foot">{member?.nickname || member?.loginId} 님 (관리자)
        <a href="#/" style={{ padding: '8px 0 0', fontSize: 13 }}>사이트로 가기</a>
        <button className="st-btn st-btn-line" onClick={onLogout}>로그아웃</button></div>
    </aside>
    <main className="adm-main">
      <div className="adm-head"><div><h1>{title}</h1><p>{sub}</p></div></div>
      {page}
    </main>
  </div>;
}

export function AdminGuard({ member, checking }) {
  return <div className="st-page adm-guard"><div className="st-panel" style={{ textAlign: 'center', maxWidth: 420 }}>
    <h1 className="st-h2" style={{ fontSize: 30 }}>{checking ? '확인 중…' : '관리자만 볼 수 있어요'}</h1>
    {!checking && <><p className="st-note">{member ? '관리자 계정으로 다시 로그인해주세요.' : '관리자 계정으로 로그인해주세요.'}</p>
      <a className="st-btn" href="#/login">로그인</a></>}
  </div></div>;
}
