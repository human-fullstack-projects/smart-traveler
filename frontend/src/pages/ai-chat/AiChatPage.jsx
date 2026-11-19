import ChatStart from './components/ChatStart';
import ChatMessages from './components/ChatMessages';
import ChatComposer from './components/ChatComposer';
import CourseRegionPicker from './components/CourseRegionPicker';
import TravelConditions from '../../TravelConditions.jsx';
import { CHAT_INTRO } from './chatIntro';
import '../../css/AiChatPage.css';

export default function AiChatPage({
  messages,
  sessionId,
  busy,
  loading,
  loadError,
  saveStatus,
  courseSaveStatus,
  retryCourseSave,
  conditions,
  conditionsConfirmed,
  startChat,
  selectRegion,
  sendMessage,
  resetChat,
  changeConditions,
  confirmConditions,
  retrySave,
}) {
  if (loading) return <p className="cb-status" role="status">대화를 불러오는 중입니다.</p>;
  if (!sessionId) return <ChatStart busy={busy} notice={loadError} onSend={startChat} onSelectRegion={selectRegion} />;

  const lastMessage = messages.at(-1);
  const awaitingConfirmation = !busy && lastMessage?.role === 'ASSISTANT' && lastMessage.content.includes('조건을 확인해 주세요');
  // 세션 생성 시 저장되는 첫 INTRO 인사말은 시작 화면에서 이미 보여줬으므로 대화창에서는 생략한다.
  const displayMessages = messages[0]?.role === 'ASSISTANT' && messages[0].content === CHAT_INTRO
    ? messages.slice(1)
    : messages;

  return (
    <section className="cb-page" aria-label="AI 여행 대화">
      <div className="cb-chat">
        <div className="cb-chat-head">
          <strong>여행 플래너</strong>
          <button type="button" className="cb-new" onClick={resetChat} disabled={busy}>+ 새 대화</button>
        </div>
        <ChatMessages messages={displayMessages} busy={busy} />
        {awaitingConfirmation && (
          <div className="cb-confirm-buttons">
            <button type="button" className="cb-confirm-yes" onClick={() => sendMessage('맞다')}>맞다</button>
            <button type="button" className="cb-confirm-no" onClick={() => sendMessage('아니다')}>아니다</button>
          </div>
        )}
        {courseSaveStatus === 'saving' && <p className="cb-status" role="status">1위 추천 코스를 보관하고 있습니다.</p>}
        {courseSaveStatus === 'guest-saved' && <p className="cb-status" role="status">1위 추천 코스를 보관했습니다. <a href="#/login">로그인</a>하거나 <a href="#/signup">회원가입</a> 후 로그인하면 내 코스로 저장됩니다.</p>}
        {courseSaveStatus === 'saved' && <p className="cb-status" role="status">1위 추천 코스를 저장했습니다. <a href="#/my-courses">내 코스 보기</a></p>}
        {courseSaveStatus === 'error' && <p className="cb-error" role="alert">코스를 저장하지 못했습니다. <button type="button" onClick={retryCourseSave}>다시 저장</button></p>}
        <ChatComposer busy={busy} onSend={sendMessage} placeholder="여행에 대해 더 이야기해 주세요." />
        {saveStatus === 'error' && (
          <p className="cb-error" role="alert">
            조건을 저장하지 못했습니다. <button type="button" onClick={retrySave}>다시 저장</button>
          </p>
        )}
      </div>
      <aside className="cb-side">
        <CourseRegionPicker busy={busy || saveStatus === 'saving'} onSelect={selectRegion} />
        <fieldset className="condition-controls" disabled={busy}>
          <TravelConditions
            conditions={conditions}
            confirmed={conditionsConfirmed}
            onChange={changeConditions}
            onConfirm={confirmConditions}
          />
        </fieldset>
        <p className="cb-status">조건을 확인한 뒤 채팅에 ‘추천해줘’라고 말해 주세요.</p>
      </aside>
    </section>
  );
}
