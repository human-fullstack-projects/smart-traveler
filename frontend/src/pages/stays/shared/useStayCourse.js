import { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../../../contexts/AuthContext.jsx';
import { loadKakaoMaps } from '../../../components/map/loadKakaoMaps.js';
import { hasValidCoordinates, resolveKakaoPlace } from '../../../components/map/resolveKakaoPlace.js';
import { myPageApi } from '../../mypage/api/myPageApi.js';
import { mapDays } from '../../mypage/data/mapDays.js';

export function readStayCourseId(params) {
  const courseId = params.get('courseId');
  const legacyId = params.get('course');
  return courseId || (/^[1-9]\d*$/.test(legacyId) ? `course-${legacyId}` : null);
}

export default function useStayCourse(courseId) {
  const { member, authChecked, authCheckFailed, retryAuth } = useAuth();
  const memberId = member?.memberId;
  const [result, setResult] = useState(null);
  const [attempt, setAttempt] = useState(0);
  const current = result && result.courseId === courseId && result.memberId === memberId && result.attempt === attempt ? result : null;
  const course = current?.course;

  useEffect(() => {
    if (!courseId || !authChecked || authCheckFailed || !memberId) return;
    let active = true;
    myPageApi.course(memberId, courseId)
      .then(course => { if (active) setResult({ courseId, memberId, attempt, course }); })
      .catch(error => {
        const message = [403, 404].includes(error.status)
          ? '선택한 저장된 여행을 찾을 수 없습니다.'
          : '저장된 여행을 불러오지 못했습니다.';
        if (active) setResult({ courseId, memberId, attempt, error: message });
      });
    return () => { active = false; };
  }, [courseId, memberId, authChecked, authCheckFailed, attempt]);

  const route = useCourseRoute(course);
  const loading = Boolean(courseId && !authCheckFailed && (!authChecked || (memberId && !current)));
  const error = !courseId || loading ? '' : current?.error || (authCheckFailed ? '로그인 상태를 확인하지 못했습니다.'
    : !memberId ? '저장된 여행을 연결하려면 로그인해 주세요.' : '');
  return { course, route, loading, error, retry: authCheckFailed ? retryAuth : () => setAttempt(value => value + 1) };
}

function useCourseRoute(course) {
  const days = useMemo(() => mapDays(course?.days ?? []), [course]);
  const [resolved, setResolved] = useState(null);

  useEffect(() => {
    const missing = days.flatMap(day => day.places).filter(place => !hasValidCoordinates(place));
    if (!missing.length) return;
    let active = true;
    loadKakaoMaps().then(maps => Promise.allSettled(missing.map(place => resolveKakaoPlace(maps, place, course.region))))
      .then(results => {
        if (!active) return;
        const points = results.filter(result => result.status === 'fulfilled' && hasValidCoordinates(result.value))
          .map(result => [result.value.id, result.value]);
        setResolved({ days, byId: new Map(points) });
      }).catch(() => {});
    return () => { active = false; };
  }, [course, days]);

  return useMemo(() => {
    const byId = resolved?.days === days ? resolved.byId : null;
    const paths = days.map(day => ({
      dashed: true,
      points: day.places.map((place, index) => ({ ...(byId?.get(place.id) || place), markerLabel: `${day.day}-${index + 1}` }))
        .filter(hasValidCoordinates),
    }));
    return { paths, places: paths.flatMap(path => path.points) };
  }, [days, resolved]);
}
