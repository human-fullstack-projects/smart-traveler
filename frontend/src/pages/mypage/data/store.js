// App이 살아 있는 동안만 보관한다. 기존 공용 localStorage 샘플은 가져오지 않는다.
export function emptyData() { return { courses: [], bookings: [], posts: [] }; }

export function makeDraft(input, clientId = globalThis.crypto.randomUUID()) {
  const title = String(input.title || '').trim();
  const region = String(input.region || '').trim();
  const start = new Date(`${input.startDate}T00:00:00Z`);
  const end = new Date(`${input.endDate}T00:00:00Z`);
  const length = (end - start) / 86400000 + 1;
  if (!/^\d{4}-\d{2}-\d{2}$/.test(input.startDate) || !/^\d{4}-\d{2}-\d{2}$/.test(input.endDate)
      || !title || title.length > 200 || !region || region.length > 100
      || !Number.isInteger(length) || length < 1 || length > 31
      || start.toISOString().slice(0, 10) !== input.startDate || end.toISOString().slice(0, 10) !== input.endDate) {
    throw new Error('코스명·지역과 1~31일의 올바른 여행 기간을 입력해 주세요.');
  }
  const places = String(input.places || '').split('\n').map(s => s.trim()).filter(Boolean);
  if (places.length > 30 || places.some(p => p.length > 200)) throw new Error('장소는 30개 이하, 이름은 200자 이하로 입력해 주세요.');
  return { clientId, title, region, startDate: input.startDate, endDate: input.endDate,
    days: Array.from({ length }, (_, i) => ({ date: new Date(+start + i * 86400000).toISOString().slice(0, 10), places: i === 0 ? places : [] })) };
}

export function routeAccess(path, { member, authChecked, authCheckFailed }) {
  if (!authChecked) return 'checking';
  if (authCheckFailed) return 'unavailable';
  if (path === '/mypage' || path === '/my-courses' || path.startsWith('/my-courses/')) return 'allowed';
  return member ? 'allowed' : 'login';
}

// 비동기 응답이 늦게 와도 이전 회원의 자료를 다음 회원에게 표시하지 않는다.
export function createMyPageSession(api) {
  let snapshot = { identity: 'checking', status: 'checking', error: '', ...emptyData(), pendingCount: 0 };
  let viewer = null;
  let epoch = 0;
  let drafts = [];
  const listeners = new Set();
  const publish = value => { snapshot = { ...snapshot, ...value }; listeners.forEach(fn => fn()); };
  const pending = id => drafts.filter(d => d.owner === id);

  async function refresh() {
    const version = ++epoch;
    const memberId = viewer;
    if (!memberId) return;
    publish({ status: 'loading', error: '', pendingCount: pending(memberId).length });
    try {
      const batch = pending(memberId);
      for (let i = 0; i < batch.length; i += 20) {
        if (version !== epoch) return;
        const part = batch.slice(i, i + 20);
        const result = await api.importCourses(memberId, part.map(d => d.course));
        const accepted = new Set(result.importedClientIds);
        if (part.some(d => !accepted.has(d.course.clientId))) throw new Error('일부 코스의 연결을 확인하지 못했습니다. 다시 시도해 주세요.');
        drafts = drafts.filter(d => !(d.owner === memberId && accepted.has(d.course.clientId)));
      }
      if (version !== epoch) return;
      const result = await api.load(memberId);
      if (version !== epoch) return;
      if (result.memberId !== memberId) throw new Error('회원 정보가 달라 다시 로그인해야 합니다.');
      publish({ courses: result.courses, bookings: result.bookings, posts: result.posts,
        identity: memberId, status: 'ready', error: '', pendingCount: pending(memberId).length });
      return true;
    } catch (error) {
      if (version !== epoch) return;
      publish({ status: error.status === 401 ? 'login' : error.status === 403 ? 'denied' : 'error',
        ...emptyData(), error: error.message, pendingCount: pending(memberId).length });
      return false;
    }
  }

  return {
    subscribe(fn) { listeners.add(fn); return () => listeners.delete(fn); },
    getSnapshot() { return snapshot; },
    setIdentity(member, checked, failed) {
      ++epoch;
      viewer = checked && !failed ? member?.memberId || null : null;
      const identity = !checked ? 'checking' : failed ? 'unavailable' : viewer || 'guest';
      publish({ identity, status: !checked ? 'checking' : failed ? 'unavailable' : 'ready',
        ...emptyData(), error: '', pendingCount: 0 });
      if (viewer) {
        // 한 번 연결을 시도한 임시 코스는 다른 계정에 자동으로 넘기지 않는다.
        drafts = drafts.map(d => d.owner === null ? { ...d, owner: viewer } : d);
        return refresh();
      }
      if (identity === 'guest') publish({ courses: pending(null).map(d => ({ ...d.course, id: d.course.clientId, state: 'GUEST' })) });
    },
    async addCourse(input) {
      if (!['ready', 'error'].includes(snapshot.status)) throw new Error('연결 상태를 확인한 뒤 다시 시도해 주세요.');
      if (drafts.length >= 100) throw new Error('임시 코스는 한 번에 100개까지 보관할 수 있습니다.');
      const course = makeDraft(input);
      drafts.push({ owner: viewer, course });
      if (viewer) {
        const saved = await refresh();
        if (saved === false) throw new Error('회원 연결에 실패했습니다. 임시 코스는 현재 페이지에 보관 중이므로 다시 시도해 주세요.');
      }
      else publish({ courses: pending(null).map(d => ({ ...d.course, id: d.course.clientId, state: 'GUEST' })) });
    },
    refresh,
    clear() { ++epoch; viewer = null; drafts = []; publish({ identity: 'guest', status: 'ready', error: '', ...emptyData(), pendingCount: 0 }); },
  };
}
