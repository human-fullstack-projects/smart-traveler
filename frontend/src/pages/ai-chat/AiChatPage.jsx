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
  saveCourse,
  skipCourse,
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
        {/* 추천 코스 저장 여부 (피드백: 저장하기 Y/N) */}
        {!busy && courseSaveStatus === 'ask' && (
          <div className="cb-save-choice" role="group" aria-label="추천 코스 저장">
            <span>1위 추천 코스를 저장할까요?</span>
            <button type="button" className="cb-confirm-yes" onClick={saveCourse}>저장하기</button>
            <button type="button" className="cb-confirm-no" onClick={skipCourse}>안 할게요</button>
          </div>
        )}
        {courseSaveStatus === 'saving' && <p className="cb-status" role="status">추천 코스를 저장하고 있습니다.</p>}
        {courseSaveStatus === 'guest-saved' && <p className="cb-status" role="status">1위 추천 코스를 보관했습니다. <a href="#/login">로그인</a>하거나 <a href="#/signup">회원가입</a> 후 로그인하면 저장된 여행으로 옮겨집니다.</p>}
        {courseSaveStatus === 'saved' && <p className="cb-status" role="status">1위 추천 코스를 저장했습니다. <a href="#/my-courses">저장된 여행 보기</a></p>}
        {courseSaveStatus === 'declined' && <p className="cb-status" role="status">저장하지 않았어요. 다른 조건을 말씀하시면 새 코스를 추천해 드릴게요.</p>}
        {courseSaveStatus === 'duplicate' && <p className="cb-error" role="alert">이미 저장한 코스예요. <a href="#/my-courses">저장된 여행 보기</a></p>}
        {courseSaveStatus === 'error' && <p className="cb-error" role="alert">코스를 저장하지 못했습니다. <button type="button" onClick={saveCourse}>다시 저장</button></p>}
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
        <p className="cb-status">지역이나 함께 갈 사람처럼 조건을 하나만 말해도 바로 추천해 드려요.</p>
      </aside>
    </section>
  );
}
