import { useEffect, useState } from 'react';
import RecommendationEditor from '../components/RecommendationEditor';
import PageHeading from '../components/PageHeading';
import { finalCourse, creationError } from '../data/courseState';
import { clearRecommendationDraft, readRecommendationDraft, saveRecommendationDraft } from '../data/recommendationDraft';
import { isPastRecommendationBooking } from '../../mypage/data/visibleItems';
import useBookingDate from '../../mypage/hooks/useBookingDate';
import '../../../css/human-course.css';

export default function HumanCourseCreate({ reservations = [], courses = [], execute, serverMode, refresh, reservationId = '', from = '', member }) {
 const [selectedId,setSelectedId]=useState(String(reservationId));
 const [notice,setNotice]=useState('');
 const [draftNotice,setDraftNotice]=useState('');
 const source=['reservations','my-posts'].includes(from)?from:'human-courses';
 const back=`#/${source}`;
 useEffect(()=>{refresh?.();},[]);
 const today=useBookingDate();
 const pastTrips=reservations.filter(reservation=>{
  const endDate=reservation.checkoutAt?.slice(0,10) || reservation.dates?.split(' ~ ')[1];
  return isPastRecommendationBooking({...reservation,endDate},today);
 });
 const selected=pastTrips.find(r=>String(r.id)===selectedId);
 const final=finalCourse(selected);
 const existingId=selected && (selected.existingPostId || courses.find(c=>c.mine&&String(c.reservationId)===selectedId)?.id);
 useEffect(()=>{if(existingId) window.location.hash=`/human-courses/${existingId}/edit?from=${source}`;},[existingId,source]);
 const error=selected ? serverMode ? selected.eligibilityError : creationError({courses},selected) : '';
 const draft=readRecommendationDraft(member?.memberId,selectedId);
 function saveDraft(fields) {
  try {
   saveRecommendationDraft(member.memberId,selectedId,fields);
   setDraftNotice('글을 이 브라우저에 임시 저장했습니다. 첨부 사진은 다시 선택해 주세요.');
  } catch {
   setDraftNotice('글을 임시 저장하지 못했습니다. 브라우저의 저장 설정을 확인해 주세요.');
  }
 }
 async function submit(fields){
  if(!serverMode){const result={error:'서버에 연결한 뒤 등록해주세요.'};setNotice(result.error);return result;}
  const result=await execute?.({type:'create',reservationId:selectedId,fields});
  if(result?.error)setNotice(result.error);
  else if(result?.id){
   if(member?.memberId)clearRecommendationDraft(member.memberId,selectedId);
   window.location.hash=source==='my-posts'?`/my-posts/${result.id}`:`/human-courses/${result.id}`;
  }
  return result;
 }
 return <div className="human-courses human-course-create human-course-editor"><div className="page">
  <PageHeading kicker="My Trips Post" title="내 여행 추천하기" description="여행 일정과 숙소를 확인하고 나의 여행 후기를 공유하세요."><a className="button" href={back}>← 목록으로</a></PageHeading>
  {notice&&<p className="status" role="alert">{notice}</p>}
  <RecommendationEditor trip={selected} plan={final?.plan}
   tripSelection={<section className="panel recommendation-trip-select"><h2>지난 여행 선택</h2>
    <label>후기를 작성할 여행<select value={selected ? selectedId : ''} onChange={e=>{setSelectedId(e.target.value);setNotice('');setDraftNotice('');}}>
     <option value="">지난 여행을 선택해주세요</option>
     {pastTrips.map(trip => <option key={trip.id} value={trip.id}>{trip.region} · {trip.name} · {trip.dates}</option>)}
    </select></label>
    {error&&<p role="alert">{error}</p>}
   </section>}
   formKey={`${member?.memberId ?? 'guest'}-${selected?.id ?? 'unselected'}`}
   formProps={{ course: draft ?? { title: selected ? `${selected.region} 여행 추천` : '' },
    disabled: !selected || !!error || !final?.plan?.length, onSubmit: submit,
    onSaveDraft: serverMode && member?.memberId && selected ? saveDraft : undefined, cancelHref: back, serverMode }}
   reviewNotice={draftNotice && <p className="status" role="status">{draftNotice}</p>} />
 </div></div>;
}
