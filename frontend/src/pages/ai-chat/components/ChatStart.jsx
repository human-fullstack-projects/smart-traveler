import ChatComposer from './ChatComposer';

const examples = ['친구와 부산 1박 2일', '아이 둘과 강릉 가족여행', '서울 당일치기 카페 투어'];

export default function ChatStart({ busy, notice, onSend }) {
  return (
    <section className="cb-start" aria-labelledby="cb-start-title">
      <h1 id="cb-start-title"><span aria-hidden="true">✳</span> 안녕하세요!</h1>
      <ChatComposer
        large
        busy={busy}
        onSend={onSend}
        placeholder="어떤 여행을 계획하고 있나요?"
        examples={examples}
        errorText="대화를 시작하지 못했습니다. 서버 연결을 확인하고 다시 시도해 주세요."
      />
      {notice && <p className="cb-status" role="status">{notice}</p>}
    </section>
  );
}
