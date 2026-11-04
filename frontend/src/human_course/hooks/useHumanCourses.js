import { useRef, useState } from 'react';
import { transition } from '../data/courseState';
import { loadCourses, saveCourses } from '../data/courseStorage';

export default function useHumanCourses() {
  const [snapshot, setSnapshot] = useState(() => loadCourses(() => window.localStorage));
  const latest = useRef(snapshot.state);
  function execute(action) {
    const result = transition(latest.current, action);
    if (!result.error) {
      const notice = saveCourses(() => window.localStorage, result.state);
      latest.current = result.state;
      setSnapshot({ state: result.state, notice });
    }
    return result;
  }
  return { ...snapshot.state, storageNotice: snapshot.notice, execute };
}
