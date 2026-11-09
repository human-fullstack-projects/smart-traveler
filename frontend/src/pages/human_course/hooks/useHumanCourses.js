import { useRef, useState } from 'react';
import { transition } from '../data/courseState';
import { loadCourses, saveCourses } from '../data/courseStorage';

export default function useHumanCourses() {
  const [snapshot, setSnapshot] = useState(() => ({ ...loadCourses(() => window.localStorage), saveFailed: false }));
  const latest = useRef(snapshot.state);
  function execute(action) {
    const result = transition(latest.current, action);
    if (!result.error) {
      const notice = saveCourses(() => window.localStorage, result.state);
      latest.current = result.state;
      setSnapshot({ state: result.state, notice, saveFailed: Boolean(notice) });
    }
    return result;
  }
  function retrySave() {
    const notice = saveCourses(() => window.localStorage, latest.current);
    setSnapshot({ state: latest.current, notice, saveFailed: Boolean(notice) });
  }
  return { ...snapshot.state, storageNotice: snapshot.notice, saveFailed: snapshot.saveFailed, retrySave, execute };
}
