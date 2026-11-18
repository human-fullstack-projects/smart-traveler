import { useEffect, useState } from 'react';
import CourseForm from '../components/CourseForm';
import CoursePlan from '../components/CoursePlan';
import PageHeading from '../components/PageHeading';
import { finalCourse, creationError } from '../data/courseState';
import '../../../css/human-course.css';

export default function HumanCourseCreate({ reservations = [], courses = [], execute, serverMode, refresh, reservationId = '', from = '' }) {
 const [selectedId,setSelectedId]=useState(String(reservationId));
 const [notice,setNotice]=useState('');
 const back=from==='reservations'?'#/reservations':'#/human-courses';
 useEffect(()=>{refresh?.();},[]);
 const selected=reservations.find(r=>String(r.id)===selectedId);
 const final=finalCourse(selected);
 const existingId=selected?.existingPostId || courses.find(c=>c.mine&&String(c.reservationId)===selectedId)?.id;
 useEffect(()=>{if(existingId) window.location.hash=`/human-courses/${existingId}/edit?from=${from==='reservations'?'reservations':'human-courses'}`;},[existingId,from]);
 const error=selected ? serverMode ? selected.eligibilityError : creationError({courses},selected) : '';
 async function submit(fields){
  if(!serverMode){const result={error:'서버에 연결한 뒤 등록해주세요.'};setNotice(result.error);return result;}
  const result=await execute?.({type:'create',reservationId:selectedId,fields});
  if(result?.error)setNotice(result.error);
  else if(result?.id)window.location.hash=`/human-courses/${result.id}`;
  return result;
 }
 const itinerary=<><p><strong>{selected?.name}</strong> · {selected?.region} · {selected?.dates}</p><CoursePlan plan={final?.plan} version={final?.version}/></>;
 return <div className="human-courses"><div className="page">
  <PageHeading breadcrumb="내 여행 추천" title="내 여행 추천하기" description="여행 일정과 숙소를 확인하고 나의 여행 후기를 공유하세요."><a className="button" href={back}>← 목록으로</a></PageHeading>
  {notice&&<p className="status" role="alert">{notice}</p>}
  <section className="panel"><h2>예약 선택</h2><label>후기를 작성할 여행<select value={selectedId} onChange={e=>{setSelectedId(e.target.value);setNotice('');}}><option value="">예약을 선택해주세요</option>{reservations.map(r=><option key={r.id} value={r.id}>{r.name} · {r.region} · {r.dates}</option>)}</select></label>{error&&<p role="alert">{error}</p>}</section>
  <section className="panel"><h2>최종 여행 일정</h2>{itinerary}</section>
  <section className="panel"><h2>후기 작성</h2><CourseForm key={selectedId} course={{title:selected?`${selected.region} 여행 추천`:''}} disabled={!selected||!!error||!final?.plan?.length} onSubmit={submit} cancelHref={back} serverMode={serverMode} previewContext={itinerary}/></section>
 </div></div>;
}
