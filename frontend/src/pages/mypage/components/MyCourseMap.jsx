import { useEffect, useMemo, useState } from 'react';
import ItineraryMapPanel from '../../../components/map/ItineraryMapPanel';
import { myPageApi } from '../api/myPageApi';
import { mapDays } from '../data/mapDays';
export { mapDays } from '../data/mapDays';

function CourseMapContent({ course, courseId, memberId, snapshot, hideMapControls = false, focusPlace, showPlaceDetails = false }) {
  const [loaded, setLoaded] = useState(null);
  const [status, setStatus] = useState('loading');
  const [attempt, setAttempt] = useState(0);
  const saved = useMemo(() => {
    if (!snapshot) return null;
    try {
      const value = typeof snapshot === 'string' ? JSON.parse(snapshot) : snapshot;
      return { region: value.region, days: value.plan || value.days || [] };
    } catch { return null; }
  }, [snapshot]);
  const provided = saved?.days?.length ? saved : course?.days?.length ? course : null;
  useEffect(() => {
    let cancelled = false;
    if ((provided && !saved) || !courseId || !memberId) return;
    setStatus('loading'); setLoaded(null);
    myPageApi.course(memberId, courseId).then(value => {
      if (!cancelled) { setLoaded(value); setStatus('ready'); }
    }).catch(() => { if (!cancelled) setStatus('error'); });
    return () => { cancelled = true; };
  }, [provided, saved, courseId, memberId, attempt]);
  const value = provided || loaded;
  const days = useMemo(() => mapDays(value?.days, loaded?.days), [value?.days, loaded?.days]);
  if (!value && courseId && memberId) return status === 'error'
    ? <p role="alert">일정 지도를 불러오지 못했습니다. <button type="button" className="text-button" onClick={() => setAttempt(n => n + 1)}>다시 시도</button></p>
    : <p role="status">지도에 표시할 일정을 불러오는 중입니다.</p>;
  if (!days.some(day => day.places.length)) return <p className="muted">지도에 표시할 방문 장소가 없습니다.</p>;
  return <ItineraryMapPanel days={days} searchRegion={value?.region || course?.region || ''} height="var(--mypage-map-height, 360px)" showIntro={!hideMapControls} showCategories={!hideMapControls} focusPlace={focusPlace} loadPlaceDetails={showPlaceDetails ? myPageApi.placeDetails : undefined}
    sourceNote="카카오 지도 · 번호는 일차별 방문 순서입니다. 점선은 방문지 사이의 직선이며 실제 도로 경로가 아닙니다." />;
}

export default function MyCourseMap({ expanded = false, title = '지도로 보는 여행 일정', ...props }) {
  const [open, setOpen] = useState(expanded);
  return <section className="mypage-course-map">
    {expanded ? <h2>{title}</h2> : <button className="secondary" type="button" aria-expanded={open} onClick={() => setOpen(value => !value)}>{open ? '일정 지도 접기' : '일정 지도 보기'}</button>}
    {(expanded || open) && <CourseMapContent {...props} />}
  </section>;
}
