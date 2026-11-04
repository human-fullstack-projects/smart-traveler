import { useState } from 'react';
import Button from './Button';
import TripTimeline from './TripTimeline';

export default function CourseEditor({ booking, dispatch }) {
  const [request, setRequest] = useState('');
  const [proposal, setProposal] = useState(null);
  const [notice, setNotice] = useState('');
  const current = booking.versions.at(-1);
  function propose(event) {
    event.preventDefault();
    if (current && !request.trim()) { setNotice('변경할 내용을 입력해주세요.'); return; }
    setProposal({ v: (current?.v || 0) + 1, name: current ? '실내 중심 여행' : '부산 여행 코스', note: request.trim() || '예약 후 최초 코스 생성', time: new Date().toLocaleString('ko-KR'), places: current ? ['부산시립미술관', '실내 전시관', '전포 카페거리'] : ['광안리 해변', '부산시립미술관', '전포 카페거리'] });
    setNotice('샘플 변경안입니다. 승인해야 내 코스에 반영됩니다.');
  }
  return <details><summary>{current ? 'AI 코스 추천 · 변경 요청' : '예약 후 코스 생성'}</summary>
    <form onSubmit={propose}><label>어떤 일정을 바꿀까요?<textarea value={request} onChange={(event) => { setRequest(event.target.value); setProposal(null); }} placeholder="예: 비가 와서 실내 장소로 변경하고 싶어요" /></label>
      <label className="checkline"><input type="checkbox" /> 현재 위치 사용 동의</label><p className="note">실제 위치·날씨를 조회하지 않습니다. 입력한 요청은 변경 이력에 기록하고, 실내 장소 중심의 고정 샘플을 보여줍니다.</p>
      <div className="actions"><Button type="submit">{current ? '변경안 만들기' : '샘플 코스 생성'}</Button><Button variant="secondary" onClick={() => { setProposal(null); setNotice('샘플 응답 실패입니다. 다시 생성해주세요.'); }}>응답 실패 상황</Button></div>
    </form>
    {notice && <p role="status">{notice}</p>}
    {proposal && <div><h3>변경안 · 버전 {proposal.v}</h3><p>{proposal.note}</p><TripTimeline course={proposal} nights={booking.nights} /><div className="actions"><Button onClick={() => { dispatch?.({ type: 'course', id: booking.id, course: proposal }); setProposal(null); setRequest(''); setNotice('코스를 저장했습니다.'); }}>변경안 승인 · 저장</Button><Button variant="secondary" onClick={() => { setProposal(null); setNotice('변경안을 폐기했습니다. 기존 코스는 유지됩니다.'); }}>변경안 폐기</Button></div></div>}
  </details>;
}
