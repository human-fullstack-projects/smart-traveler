import ChatStart from './components/ChatStart';
import ChatMessages from './components/ChatMessages';
import ChatComposer from './components/ChatComposer';
import TravelConditions from '../../TravelConditions.jsx';
import '../../css/AiChatPage.css';

export default function AiChatPage({
  messages,
  sessionId,
  busy,
  loading,
  loadError,
  saveStatus,
  conditions,
  conditionsConfirmed,
  startChat,
  sendMessage,
  resetChat,
  changeConditions,
  confirmConditions,
  retrySave,
  onSaveTrialCourse,
}) {
  if (loading) return <p className="cb-status" role="status">대화를 불러오는 중입니다.</p>;
  if (!sessionId) return <ChatStart busy={busy} notice={loadError} onSend={startChat} />;

  return (
    <section className="cb-page" aria-label="AI 여행 대화">
      <div className="cb-chat">
        <div className="cb-chat-head">
          <strong>여행 플래너</strong>
          <button type="button" className="cb-new" onClick={resetChat} disabled={busy}>+ 새 대화</button>
        </div>
        <ChatMessages messages={messages} busy={busy} />
        <ChatComposer busy={busy} onSend={sendMessage} placeholder="여행에 대해 더 이야기해 주세요." />
        {saveStatus === 'error' && (
          <p className="cb-error" role="alert">
            조건을 저장하지 못했습니다. <button type="button" onClick={retrySave}>다시 저장</button>
          </p>
        )}
      </div>
      <aside className="cb-side">
        <fieldset className="condition-controls" disabled={busy}>
          <TravelConditions
            conditions={conditions}
            confirmed={conditionsConfirmed}
            onChange={changeConditions}
            onConfirm={confirmConditions}
          />
        </fieldset>
        {onSaveTrialCourse && <>
          <button type="button" className="cb-generate" disabled={busy || !conditionsConfirmed || saveStatus !== 'saved'} onClick={onSaveTrialCourse}>확인한 조건을 내 코스에 보관</button>
          <p className="cb-status">비회원은 1회 보관할 수 있습니다. AI 일정 생성은 준비 중이며, 확인한 조건과 고정 일정만 임시 보관합니다. 사이트를 떠나거나 새로고침하면 삭제됩니다.</p>
        </>}
        <button type="button" className="cb-generate" disabled title="코스 생성 기능 연결 준비 중">
          이 조건으로 코스 만들기
        </button>
      </aside>
    </section>
  );
}
