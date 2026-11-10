import { useEffect, useRef, useState } from 'react';
import { chatApi } from '../../../chatApi.js';
import { createEmptyConditions } from '../../../travelConditions.js';

const STORAGE_KEY = 'smart-travel-chat-session';

function parse(chat) {
  return {
    conditions: chat.conditions ? JSON.parse(chat.conditions.conditionsJson) : createEmptyConditions(),
    confirmed: chat.conditions?.confirmed || false,
  };
}

/** AI 코스 챗봇의 대화 state를 관리한다. #/ai-course, #/chat이 같은 상태를 공유하도록 App 최상단에서 한 번만 호출한다. */
export default function useAiChat() {
  const [messages, setMessages] = useState([]);
  const [conditions, setConditions] = useState(createEmptyConditions);
  const [conditionsConfirmed, setConditionsConfirmed] = useState(false);
  const [sessionId, setSessionId] = useState(null);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [saveStatus, setSaveStatus] = useState('saved');
  const saveQueue = useRef(Promise.resolve());
  const saveVersion = useRef(0);
  const saveFailed = useRef(false);
  const currentConditions = useRef({ conditions: createEmptyConditions(), confirmed: false });

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
    const stored = window.localStorage.getItem(STORAGE_KEY);
    if (!stored) { setLoading(false); return () => { cancelled = true; }; }
    chatApi.load(stored)
      .then((chat) => { if (!cancelled) apply(chat); })
      .catch(() => {
        // 서버 재시작 등으로 세션을 못 찾으면 시작 화면으로 보낸다.
        if (!cancelled) {
          window.localStorage.removeItem(STORAGE_KEY);
          setLoadError('이전 대화를 불러오지 못해 새 대화로 시작합니다.');
        }
      })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, []);

  async function startChat(message) {
    setBusy(true);
    try {
      // 앞선 조건 저장이 끝난 다음 새 대화를 시작해 서로 섞이지 않게 한다.
      await saveQueue.current;
      const chat = await chatApi.create(message);
      window.localStorage.setItem(STORAGE_KEY, String(chat.id));
      apply(chat);
      saveFailed.current = false;
      setSaveStatus('saved');
      window.location.hash = '/chat';
    } finally {
      setBusy(false);
    }
  }

  async function sendMessage(message) {
    if (!sessionId) throw new Error('대화 세션이 없습니다.');
    setBusy(true);
    try {
      // 앞서 수정한 조건의 저장이 끝난 다음 서버에서 메시지를 처리한다.
      await saveQueue.current;
      if (saveFailed.current) throw new Error('조건 저장이 필요합니다.');
      const chat = await chatApi.send(sessionId, message);
      apply(chat);
    } finally {
      setBusy(false);
    }
  }

  function queueConditions(next, confirmed) {
    currentConditions.current = { conditions: next, confirmed };
    setConditions(next);
    setConditionsConfirmed(confirmed);
    if (!sessionId) { setSaveStatus('error'); return; }
    const version = ++saveVersion.current;
    setSaveStatus('saving');
    // 요청 순서를 보장해 이전 입력이 나중 입력을 덮어쓰지 않도록 한다.
    saveQueue.current = saveQueue.current.catch(() => {}).then(() => chatApi.saveConditions(sessionId, next, confirmed))
      .then(() => { if (version === saveVersion.current) { saveFailed.current = false; setSaveStatus('saved'); } })
      .catch(() => { if (version === saveVersion.current) { saveFailed.current = true; setSaveStatus('error'); } });
  }

  // 새 대화: 저장된 세션 id만 지우고 시작 화면으로 돌아간다. 서버 기록은 남는다.
  function resetChat() {
    window.localStorage.removeItem(STORAGE_KEY);
    setSessionId(null);
    setMessages([]);
    setConditions(createEmptyConditions());
    setConditionsConfirmed(false);
    currentConditions.current = { conditions: createEmptyConditions(), confirmed: false };
    setSaveStatus('saved');
  }

  return {
    messages,
    conditions,
    conditionsConfirmed,
    sessionId,
    busy,
    loading,
    loadError,
    saveStatus,
    startChat,
    sendMessage,
    resetChat,
    changeConditions: (next) => queueConditions(next, false),
    confirmConditions: () => queueConditions(currentConditions.current.conditions, true),
    retrySave: () => queueConditions(currentConditions.current.conditions, currentConditions.current.confirmed),
  };
}
