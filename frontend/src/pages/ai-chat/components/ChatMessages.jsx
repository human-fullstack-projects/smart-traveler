import { useEffect, useRef } from 'react';

// "**굵게**"는 <strong>, "[글자](#/주소)"는 사이트 안 링크로 바꾸고 줄바꿈은 그대로 둔다.
// 링크는 #/로 시작하는 사이트 안 주소만 허용한다 (휴먼 추천 코스 안내).
function renderContent(content) {
  return content.split(/(\*\*[^*]+\*\*|\[[^\]\n]+\]\(#\/[^)\s]*\))/g).map((part, index) => {
    if (part.startsWith('**') && part.endsWith('**')) {
      return <strong key={index}>{part.slice(2, -2)}</strong>;
    }
    const link = part.match(/^\[([^\]\n]+)\]\((#\/[^)\s]*)\)$/);
    if (link) return <a key={index} href={link[2]}>{link[1]}</a>;
    return part;
  });
}

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
          {renderContent(item.content)}
        </div>
      ))}
      {busy && <div className="cb-msg bot cb-typing" role="status">답변을 작성하고 있어요…</div>}
      <div ref={end} />
    </div>
  );
}
