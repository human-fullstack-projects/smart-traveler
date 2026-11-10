import CourseForm from '../components/CourseForm';
import '../../../css/human-course.css';
import CoursePlan from '../components/CoursePlan';
import PageHeading from '../components/PageHeading';
import { previewCourses, previewReservations } from '../data/previewData';
import { useState } from 'react';
import { creationError, finalCourse } from '../data/courseState';

const names = { COMPLETED: '숙박 완료', CANCELED: '취소된 예약', UPCOMING: '숙박 예정', CONFIRMED: '예약 확정 / 숙박완료 전환 전' };

export default function HumanCourseCreate({ reservations = previewReservations, courses = previewCourses, execute, serverMode }) {
  const [selectedId, setSelectedId] = useState('');
  const [loadedId, setLoadedId] = useState('');
  const [notice, setNotice] = useState('');
  const selected = reservations.find((reservation) => reservation.id === loadedId);
  const final = finalCourse(selected);
  const eligibility = (reservation) => serverMode ? (reservation?.eligibilityError || (!reservation ? '예약을 먼저 선택해주세요.' : '')) : creationError({ courses }, reservation);
  async function submit(fields) {
    const result = await execute?.({ type: 'create', reservationId: loadedId, fields });
    if (result?.error) setNotice(result.error);
    else if (result?.id) window.location.hash = `/human-courses/${result.id}`;
  }
  return <div className="human-courses"><div className="page">
    <PageHeading breadcrumb="코스 작성" title="여행 코스 공유하기" description="숙박 완료 예약에 연결된 확정 일정을 불러와서 후기를 등록합니다."><a className="button" href="#/human-courses">← 목록으로</a></PageHeading>
    <div className="step-row" aria-label="작성 단계"><div className="step"><strong>1</strong> 예약 선택</div><span className="step-arrow">→</span><div className="step"><strong>2</strong> 최종 일정 확인</div><span className="step-arrow">→</span><div className="step"><strong>3</strong> 후기 작성</div></div>
    <div className="layout wide-left"><div>
      <section className="panel"><div className="panel-heading"><h2>1. 숙박 완료 예약 선택</h2><span className="helper">{serverMode ? '내 예약' : '가상 예약 데이터'}</span></div><p className="form-help">체크아웃 시각이 지나 숙박 완료로 전환된 예약만 작성할 수 있습니다. 예약 1건당 1회 작성하며 이후에는 기존 글을 수정합니다.</p>
        {!serverMode && <details className="mock-tools"><summary>작성 자격 배치 시연</summary><p>R106은 체크아웃 후 배치 전, R107은 기준 시각에 체크아웃 전입니다.</p><button onClick={async () => { const result = await execute?.({ type: 'batch' }); setNotice(`${result?.count || 0}건을 숙박 완료로 전환했습니다.`); setLoadedId(''); }}>12:00 숙박완료 배치 실행 (샘플)</button></details>}
        <div className="reservation-list" role="group" aria-label="내 예약 목록">{reservations.map((reservation) => {
          const message = eligibility(reservation);
          const existingId = reservation.existingPostId || courses.find((course) => course.mine && course.reservationId === reservation.id)?.id;
          return <div className={`reservation-option${message ? ' is-disabled' : ''}`} key={reservation.id}><div className="reservation-top"><label className="reservation-label"><input type="radio" name="reservation" value={reservation.id} checked={selectedId === reservation.id} onChange={() => { setSelectedId(reservation.id); setLoadedId(''); setNotice(''); }} disabled={Boolean(message)} /><span>{reservation.id} | {reservation.name} | {reservation.dates}</span></label><span className={`badge${message ? ' badge-disabled' : ''}`}>{names[reservation.status] || reservation.status}</span></div><p className="reservation-reason">{message || '작성 가능'}</p><p className="helper">체크아웃 시각: {reservation.checkoutAt}</p>{existingId && <a className="button" href={`#/human-courses/${existingId}/edit`}>이미 작성한 코스 수정하기 →</a>}</div>;
        })}{!reservations.length && <p className="empty-state">{serverMode ? '작성 가능한 내 예약이 없습니다. 로그인 상태와 예약 내역을 확인해주세요.' : '예약 내역이 없습니다.'}</p>}</div>
        <p><button className="primary" onClick={() => { const reservation = reservations.find((r) => r.id === selectedId); const error = eligibility(reservation); if (error) { setNotice(error); return; } setLoadedId(selectedId); setNotice(''); }}>선택한 예약의 최종 코스 불러오기</button></p>
        {notice && <p className="status" role="status">{notice}</p>}
      </section>
      <section className="panel"><div className="panel-heading"><h2>2. 최종 여행 코스 확인</h2><span className="badge">읽기 전용</span></div><p className="form-help">예약을 선택하고 최종 코스를 불러오세요. 첨부할 일정은 이 화면에서 수정할 수 없습니다.</p><CoursePlan plan={final?.plan} version={final?.version} /></section>
      <section className="panel"><h2>3. 사진 / 후기 작성</h2><CourseForm key={loadedId} disabled={!selected} onSubmit={submit} cancelHref="#/human-courses" serverMode={serverMode} /></section>
    </div><aside className="panel sidebar-hint"><h3>이 화면의 처리 흐름</h3><ol><li>숙박이 완료된 예약 선택</li><li>예약에 연결된 최종 코스 불러오기</li><li>코스 내용은 고정된 상태로 후기 입력</li><li>등록하면 상세 페이지로 이동</li></ol>{!serverMode && <p className="note">R101은 작성 가능, R102는 기존 게시글 수정 대상입니다.</p>}</aside></div>
  </div></div>;
}
