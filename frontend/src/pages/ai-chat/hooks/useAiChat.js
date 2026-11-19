import { useEffect, useRef, useState } from 'react';
import { chatApi } from '../../../chatApi.js';
import { createEmptyConditions } from '../../../travelConditions.js';

const STORAGE_KEY = 'smart-travel-chat-session';
// 챗봇 화면 주소. 이 주소에서 다른 주소로 가면 대화를 저장한다.
const CHAT_PATHS = ['/chat', '/ai-course'];
const chatPath = (hash) => CHAT_PATHS.includes((hash.replace(/^#/, '').split('?')[0].replace(/\/$/, '')) || '/');

function parse(chat) {
  return {
    conditions: chat.conditions ? JSON.parse(chat.conditions.conditionsJson) : createEmptyConditions(),
    confirmed: chat.conditions?.confirmed || false,
  };
}

/** AI 코스 챗봇의 대화 state를 관리한다. #/ai-course, #/chat이 같은 상태를 공유하도록 App 최상단에서 한 번만 호출한다. */
export default function useAiChat({ ownerKey = 'guest', ready = true, authRevision = 0 } = {}) {
  const scope = `${ready ? ownerKey : 'checking'}:${authRevision}`;
  const activeScope = useRef(scope);
  const generation = useRef(0);
  if (activeScope.current !== scope) {
    activeScope.current = scope;
    generation.current++;
  }
  const [loadedScope, setLoadedScope] = useState(null);
  const [messages, setMessages] = useState([]);
  const [conditions, setConditions] = useState(createEmptyConditions);
  const [conditionsConfirmed, setConditionsConfirmed] = useState(false);
  const [sessionId, setSessionId] = useState(null);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [saveStatus, setSaveStatus] = useState('saved');
  const [courseSaveStatus, setCourseSaveStatus] = useState('');
  const [courseSaveRetry, setCourseSaveRetry] = useState(0);
  const savingCourses = useRef(new Set());
  const saveQueue = useRef(Promise.resolve());
  const saveVersion = useRef(0);
  const saveFailed = useRef(false);
  const currentConditions = useRef({ conditions: createEmptyConditions(), confirmed: false });
  const sessionRef = useRef(null);
  sessionRef.current = loadedScope === scope && ready ? sessionId : null;

  // 대화 저장 시점: 다른 페이지로 갈 때 · 창/탭을 닫을 때 (새 대화는 resetChat에서).
  // 서버가 메시지를 모아 두었다가 이때 DB에 한 번에 쓴다. 인터넷이 끊기면 저장되지 않을 수 있다.
  useEffect(() => {
    let wasChat = chatPath(window.location.hash);
    const onRoute = () => {
      const isChat = chatPath(window.location.hash);
      if (wasChat && !isChat && sessionRef.current) chatApi.flush(sessionRef.current).catch(() => {});
      wasChat = isChat;
    };
    const onExit = () => { if (sessionRef.current) chatApi.flushOnExit(sessionRef.current); };
    window.addEventListener('hashchange', onRoute);
    window.addEventListener('pagehide', onExit);
    return () => { window.removeEventListener('hashchange', onRoute); window.removeEventListener('pagehide', onExit); };
  }, []);

  function apply(chat) {
    const next = parse(chat);
    setSessionId(chat.id);
    setMessages(chat.messages);
    setConditions(next.conditions);
    setConditionsConfirmed(next.confirmed);
    currentConditions.current = next;
  }

  useEffect(() => {
    let cancelled = false;
    const version = generation.current;
    const current = () => !cancelled && version === generation.current;
    setMessages([]);
    setConditions(createEmptyConditions());
    setConditionsConfirmed(false);
    setSessionId(null);
    setBusy(false);
    setLoading(true);
    setLoadError('');
    setSaveStatus('saved');
    setCourseSaveStatus('');
    saveVersion.current++;
    saveFailed.current = false;
    currentConditions.current = { conditions: createEmptyConditions(), confirmed: false };
    setLoadedScope(scope);
    if (!ready) return () => { cancelled = true; };
    const storedOwner = window.localStorage.getItem(`${STORAGE_KEY}-owner`);
    if (storedOwner !== ownerKey) window.localStorage.removeItem(STORAGE_KEY);
    window.localStorage.setItem(`${STORAGE_KEY}-owner`, ownerKey);
    const stored = window.localStorage.getItem(STORAGE_KEY);
    if (!stored) { setLoading(false); return () => { cancelled = true; }; }
    chatApi.load(stored)
      .then((chat) => { if (current()) apply(chat); })
      .catch(() => {
        // 서버 재시작 등으로 세션을 못 찾으면 시작 화면으로 보낸다.
        if (current()) {
          window.localStorage.removeItem(STORAGE_KEY);
          setLoadError('이전 대화를 불러오지 못해 새 대화로 시작합니다.');
        }
      })
      .finally(() => { if (current()) setLoading(false); });
    return () => { cancelled = true; };
  }, [scope, ownerKey, ready]);

  // Effects run after the recommended course has been rendered in the conversation.
  useEffect(() => {
    if (!ready || loadedScope !== scope || !sessionId) return;
    const isGuest = ownerKey === 'guest';
    if (isGuest) {
      const pending = messages.filter(message => message.recommendation && !message.recommendation.guestStored).at(-1);
      if (!pending || savingCourses.current.has(pending.id)) return;
      const version = generation.current;
      savingCourses.current.add(pending.id);
      setCourseSaveStatus('saving');
      chatApi.storeGuestCourse(sessionId, pending.id).then(() => {
        if (version !== generation.current) return;
        setMessages(previous => previous.map(message => message.id === pending.id
          ? { ...message, recommendation: { ...message.recommendation, guestStored: true } } : message));
        setCourseSaveStatus('guest-saved');
        window.dispatchEvent(new Event('smart-travel-guest-course-updated'));
      }).catch(() => { if (version === generation.current) setCourseSaveStatus('error'); })
        .finally(() => savingCourses.current.delete(pending.id));
      return;
    }
    // 회원 추천은 서버가 메시지를 만드는 즉시 "내 코스"로 저장하므로, 마지막 추천에 이미 savedCourseId가 와 있다.
    const last = messages.filter(message => message.recommendation).at(-1);
    if (!last) return;
    if (last.recommendation.savedCourseId) {
      setCourseSaveStatus('saved');
      window.dispatchEvent(new Event('smart-travel-courses-updated'));
      return;
    }
    // 서버가 즉시 저장하지 못한 경우(예: 이전 버전 세션)에 대한 대비책.
    if (savingCourses.current.has(last.id)) return;
    const version = generation.current;
    savingCourses.current.add(last.id);
    setCourseSaveStatus('saving');
    chatApi.saveRecommendedCourse(sessionId, last.id).then(result => {
      if (version !== generation.current) return;
      setMessages(previous => previous.map(message => message.id === last.id
        ? { ...message, recommendation: { ...message.recommendation, savedCourseId: result.courseId } } : message));
      setCourseSaveStatus('saved');
      window.dispatchEvent(new Event('smart-travel-courses-updated'));
    }).catch(() => { if (version === generation.current) setCourseSaveStatus('error'); })
      .finally(() => savingCourses.current.delete(last.id));
  }, [messages, sessionId, scope, ownerKey, ready, loadedScope, courseSaveRetry]);

  async function startChat(message, regionId = null) {
    if (!ready || loadedScope !== scope) return;
    const version = generation.current;
    setBusy(true);
    try {
      // 앞선 조건 저장이 끝난 다음 새 대화를 시작해 서로 섞이지 않게 한다.
      await saveQueue.current;
      if (version !== generation.current) return;
      const chat = await chatApi.create(message, regionId);
      if (version !== generation.current) return;
      window.localStorage.setItem(STORAGE_KEY, String(chat.id));
      apply(chat);
      saveFailed.current = false;
      setSaveStatus('saved');
      window.location.hash = '/chat';
    } finally {
      if (version === generation.current) setBusy(false);
    }
  }

  async function sendMessage(message, regionId = null) {
    if (!ready || loadedScope !== scope) return;
    const version = generation.current;
    if (!sessionId) throw new Error('대화 세션이 없습니다.');
    setBusy(true);
    try {
      // 앞서 수정한 조건의 저장이 끝난 다음 서버에서 메시지를 처리한다.
      await saveQueue.current;
      if (version !== generation.current) return;
      if (saveFailed.current) throw new Error('조건 저장이 필요합니다.');
      const chat = await chatApi.send(sessionId, message, regionId);
      if (version !== generation.current) return;
      apply(chat);
    } finally {
      if (version === generation.current) setBusy(false);
    }
  }

  function queueConditions(next, confirmed) {
    if (!ready || loadedScope !== scope) return;
    const contextVersion = generation.current;
    currentConditions.current = { conditions: next, confirmed };
    setConditions(next);
    setConditionsConfirmed(confirmed);
    if (!sessionId) { setSaveStatus('error'); return; }
    const version = ++saveVersion.current;
    setSaveStatus('saving');
    // 요청 순서를 보장해 이전 입력이 나중 입력을 덮어쓰지 않도록 한다.
    saveQueue.current = saveQueue.current.catch(() => {}).then(() => {
      if (contextVersion === generation.current) return chatApi.saveConditions(sessionId, next, confirmed);
    })
      .then(() => { if (version === saveVersion.current) { saveFailed.current = false; setSaveStatus('saved'); } })
      .catch(() => { if (version === saveVersion.current) { saveFailed.current = true; setSaveStatus('error'); } });
  }

  // 새 대화: 저장된 세션 id만 지우고 시작 화면으로 돌아간다. 서버 기록은 남는다.
  function resetChat() {
    // 대화 종료: 지금 대화를 DB에 저장한 뒤 새 대화로.
    if (sessionRef.current) chatApi.flush(sessionRef.current).catch(() => {});
    generation.current++;
    saveVersion.current++;
    saveFailed.current = false;
    window.localStorage.removeItem(STORAGE_KEY);
    setSessionId(null);
    setMessages([]);
    setConditions(createEmptyConditions());
    setConditionsConfirmed(false);
    currentConditions.current = { conditions: createEmptyConditions(), confirmed: false };
    setSaveStatus('saved');
    setCourseSaveStatus('');
  }

  return {
    messages: loadedScope === scope && ready ? messages : [],
    conditions: loadedScope === scope && ready ? conditions : createEmptyConditions(),
    conditionsConfirmed: loadedScope === scope && ready && conditionsConfirmed,
    sessionId: loadedScope === scope && ready ? sessionId : null,
    busy: loadedScope === scope && ready && busy,
    loading: loadedScope !== scope || !ready || loading,
    loadError,
    saveStatus,
    courseSaveStatus: loadedScope === scope ? courseSaveStatus : '',
    retryCourseSave: () => setCourseSaveRetry(value => value + 1),
    startChat,
    selectRegion: (region) => {
      const message = `${region.fullName} 지역으로 여행할게요.`;
      return sessionId ? sendMessage(message, region.regionId) : startChat(message, region.regionId);
    },
    sendMessage,
    resetChat,
    changeConditions: (next) => queueConditions(next, false),
    confirmConditions: () => queueConditions(currentConditions.current.conditions, true),
    retrySave: () => queueConditions(currentConditions.current.conditions, currentConditions.current.confirmed),
  };
}
