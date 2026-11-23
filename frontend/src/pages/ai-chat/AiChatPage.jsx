import { useState } from 'react';
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
  savedStayHash,
  guestLimitReached,
  dismissGuestLimit,
  saveCourse,
  skipCourse,
  goToStayPage,
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
  const [startDateInput, setStartDateInput] = useState('');
  const [courseSelection, setCourseSelection] = useState(null);
  if (loading) return <p className="cb-status" role="status">대화를 불러오는 중입니다.</p>;
  if (!sessionId) return (
    <>
      <ChatStart busy={busy} notice={loadError} onSend={startChat} onSelectRegion={selectRegion} />
      {guestLimitReached && (
        <div className="cb-modal-backdrop" role="dialog" aria-modal="true">
          <div className="cb-modal">
            <p>비회원은 대화를 1회만 이용할 수 있습니다.</p>
            <p>로그인 하시고 더 많은 서비스를 누리세요.</p>
            <div className="cb-save-choice">
              <a className="cb-confirm-yes" href="#/login">로그인</a>
              <button type="button" className="cb-confirm-no" onClick={dismissGuestLimit}>닫기</button>
            </div>
          </div>
        </div>
      )}
    </>
  );

  const recommendationMessage = messages.findLast((message) => message.recommendation);
  const lastRecommendation = recommendationMessage?.recommendation;
  const candidateIds = lastRecommendation?.candidateCourseIds ?? [];
  const multipleCourses = candidateIds.length > 1;
  const selectedCourseId = courseSelection?.sessionId === sessionId && courseSelection?.messageId === recommendationMessage?.id
    ? courseSelection.courseId : null;
  const saveSelectedCourse = () => saveCourse(startDateInput || null, selectedCourseId);
  const needsStartDate = courseSaveStatus === 'ask' && !lastRecommendation?.conditions?.startDate;
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
            {multipleCourses ? (
              <div className="cb-save-choice" role="group" aria-label="저장할 코스 선택">
                {candidateIds.map((courseId, index) => (
                  <label key={courseId}>
                    <input type="checkbox" checked={selectedCourseId === courseId}
                      onChange={(event) => setCourseSelection({
                        sessionId, messageId: recommendationMessage.id, courseId: event.target.checked ? courseId : null,
                      })} />
                    코스{index + 1}
                  </label>
                ))}
              </div>
            ) : <span>추천 코스를 저장할까요?</span>}
            {needsStartDate && (
              <label>출발일 <input type="date" value={startDateInput} onChange={(event) => setStartDateInput(event.target.value)} /></label>
            )}
            <button type="button" className="cb-confirm-yes"
              disabled={(multipleCourses && !selectedCourseId) || (needsStartDate && !startDateInput)}
              onClick={saveSelectedCourse}>저장하기</button>
            <button type="button" className="cb-confirm-no" onClick={skipCourse}>안 할게요</button>
          </div>
        )}
        {courseSaveStatus === 'saving' && <p className="cb-status" role="status">추천 코스를 저장하고 있습니다.</p>}
        {courseSaveStatus === 'guest-saved' && <p className="cb-status" role="status">1위 추천 코스를 보관했습니다. <a href="#/login">로그인</a>하거나 <a href="#/signup">회원가입</a> 후 로그인하면 저장된 여행으로 옮겨집니다.</p>}
        {courseSaveStatus === 'saved' && (
          <div className="cb-save-choice" role="group" aria-label="저장 완료">
            <p className="cb-status" role="status">1위 추천 코스를 저장했습니다. <a href="#/my-courses">저장된 여행 보기</a></p>
            {savedStayHash && (
              <>
                <span>관련 숙소 예약 페이지로 안내할게요.</span>
                <button type="button" className="cb-confirm-yes" onClick={goToStayPage}>숙소 예약 페이지로 이동</button>
              </>
            )}
          </div>
        )}
        {courseSaveStatus === 'declined' && <p className="cb-status" role="status">저장하지 않았어요. 다른 조건을 말씀하시면 새 코스를 추천해 드릴게요.</p>}
        {courseSaveStatus === 'duplicate' && <p className="cb-error" role="alert">이미 저장한 코스예요. <a href="#/my-courses">저장된 여행 보기</a></p>}
        {courseSaveStatus === 'error' && <p className="cb-error" role="alert">코스를 저장하지 못했습니다. <button type="button" onClick={saveSelectedCourse}>다시 저장</button></p>}
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
