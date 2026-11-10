import { useEffect, useRef } from 'react';

export default function ChatMessages({ messages, busy }) {
  const end = useRef(null);
  // 새 메시지가 오면 맨 아래로 스크롤
  useEffect(() => { end.current?.scrollIntoView({ block: 'end' }); }, [messages.length, busy]);

  return (
    <div className="cb-messages" aria-live="polite">
      {messages.length === 0 && !busy && (
        <p className="cb-status">어떤 여행을 계획하고 있는지 이야기해 주세요.</p>
      )}
      {messages.map((item) => (
        <div key={item.id} className={item.role === 'USER' ? 'cb-msg user' : 'cb-msg bot'}>
          <span className="ac-visually-hidden">{item.role === 'USER' ? '나: ' : '플래너: '}</span>
          {item.content}
        </div>
      ))}
      {busy && <div className="cb-msg bot cb-typing" role="status">답변을 작성하고 있어요…</div>}
      <div ref={end} />
    </div>
  );
}
