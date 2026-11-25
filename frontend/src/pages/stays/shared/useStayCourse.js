import { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../../../contexts/AuthContext.jsx';
import { loadKakaoMaps } from '../../../components/map/loadKakaoMaps.js';
import { coord2Address, hasValidCoordinates, resolveKakaoPlace } from '../../../components/map/resolveKakaoPlace.js';
import { myPageApi } from '../../mypage/api/myPageApi.js';
import { mapDays } from '../../mypage/data/mapDays.js';

export function readStayCourseId(params) {
  const courseId = params.get('courseId');
  const legacyId = params.get('course');
  return courseId || (/^[1-9]\d*$/.test(legacyId) ? `course-${legacyId}` : null);
}

export default function useStayCourse(courseId, { includePlaceAddresses = false } = {}) {
  const { member, authChecked, authCheckFailed, retryAuth } = useAuth();
  const memberId = member?.memberId;
  const [result, setResult] = useState(null);
  const [attempt, setAttempt] = useState(0);
  const current = result && result.courseId === courseId && result.memberId === memberId && result.attempt === attempt ? result : null;
  const course = current?.course;
  const sourceLabel = courseId?.startsWith('human-') ? '휴먼 여행' : '저장된 여행';

  useEffect(() => {
    if (!courseId || !authChecked || authCheckFailed || !memberId) return;
    let active = true;
    myPageApi.course(memberId, courseId)
      .then(course => { if (active) setResult({ courseId, memberId, attempt, course }); })
      .catch(error => {
        const message = error.status === 409 ? error.message : [403, 404].includes(error.status)
          ? `선택한 ${sourceLabel}을 찾을 수 없습니다.`
          : `${sourceLabel}을 불러오지 못했습니다.`;
        if (active) setResult({ courseId, memberId, attempt, error: message });
      });
    return () => { active = false; };
  }, [courseId, memberId, authChecked, authCheckFailed, attempt, sourceLabel]);

  const route = useCourseRoute(course, includePlaceAddresses);
  const loading = Boolean(courseId && !authCheckFailed && (!authChecked || (memberId && !current)));
  const error = !courseId || loading ? '' : current?.error || (authCheckFailed ? '로그인 상태를 확인하지 못했습니다.'
    : !memberId ? `${sourceLabel}을 연결하려면 로그인해 주세요.` : '');
  return { course, route, loading, error, sourceLabel, retry: authCheckFailed ? retryAuth : () => setAttempt(value => value + 1) };
}

function useCourseRoute(course, includePlaceAddresses) {
  const days = useMemo(() => mapDays(course?.days ?? []), [course]);
  const [resolved, setResolved] = useState(null);

  useEffect(() => {
    const missing = days.flatMap(day => day.places)
      .filter(place => !hasValidCoordinates(place) || (includePlaceAddresses && !place.address));
    if (!missing.length) return;
    let active = true;
    loadKakaoMaps().then(maps => Promise.allSettled(missing.map(async place => hasValidCoordinates(place)
      ? { ...place, address: await coord2Address(maps, place) }
      : resolveKakaoPlace(maps, place, course.region))))
      .catch(() => [])
      .then(results => {
        if (!active) return;
        const byId = new Map(missing.map((place, index) => {
          const result = results[index];
          const point = result?.status === 'fulfilled' ? result.value : place;
          return [place.id, { ...point, locationStatus: hasValidCoordinates(point) ? 'ready' : 'unresolved',
            addressStatus: point.address ? 'provided' : 'unavailable' }];
        }));
        setResolved({ days, byId });
      });
    return () => { active = false; };
  }, [course, days, includePlaceAddresses]);

  return useMemo(() => {
    const byId = resolved?.days === days ? resolved.byId : null;
    const numberedDays = days.map(day => ({
      ...day,
      places: day.places.map((place, index) => {
        const label = `${day.day}-${index + 1}`;
        return { ...(byId?.get(place.id) || place), markerLabel: label, badgeLabel: label };
      }),
    }));
    const paths = numberedDays.map(day => ({ dashed: true, points: day.places.filter(hasValidCoordinates) }));
    return { days: numberedDays, paths, places: paths.flatMap(path => path.points) };
  }, [days, resolved]);
}
