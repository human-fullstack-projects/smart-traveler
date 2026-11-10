import { useState } from 'react';

// 한글 조합 중 Enter는 전송하지 않고, Shift+Enter는 줄바꿈.
function submitOnEnter(event) {
  if (event.key !== 'Enter' || event.shiftKey || event.ctrlKey || event.altKey || event.metaKey) return;
  if (event.nativeEvent.isComposing || event.nativeEvent.keyCode === 229) return;
  event.preventDefault();
  event.currentTarget.form?.requestSubmit();
}

export default function ChatComposer({
  busy,
  onSend,
  placeholder,
  large = false,
  examples = [],
  errorText = '전송하지 못했습니다. 잠시 후 다시 시도해 주세요.',
}) {
  const [text, setText] = useState('');
  const [error, setError] = useState('');

  async function submit(event) {
    event.preventDefault();
    const value = text.trim();
    if (!value || busy) return;
    try {
      await onSend(value);
      setText('');
      setError('');
    } catch {
      // 실패하면 입력을 지우지 않는다 (서버가 메시지를 롤백함)
      setError(errorText);
    }
  }

  return (
    <form className={`cb-composer${large ? ' large' : ''}`} onSubmit={submit}>
      <label className="ac-visually-hidden" htmlFor="cb-input">여행 이야기 입력</label>
      <textarea
        id="cb-input"
        value={text}
        rows={large ? 3 : 2}
        placeholder={placeholder}
        onKeyDown={submitOnEnter}
        onChange={(event) => { setText(event.target.value); setError(''); }}
      />
      <div className="cb-composer-bottom">
        <div className="cb-examples">
          {examples.map((item) => (
            <button type="button" key={item} onClick={() => setText(item)}>{item}</button>
          ))}
        </div>
        <button type="submit" className="cb-send" disabled={busy || !text.trim()}>
          {busy ? '답변 중…' : '보내기'} <span aria-hidden="true">↗</span>
        </button>
      </div>
      {error && <p className="cb-error" role="alert">{error}</p>}
    </form>
  );
}
