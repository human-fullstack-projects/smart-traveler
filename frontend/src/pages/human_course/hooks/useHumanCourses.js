import { useEffect, useRef, useState } from 'react';
import { transition } from '../data/courseState';
import { loadCourses, saveCourses } from '../data/courseStorage';
import * as api from '../api/humanCourses';

export default function useHumanCourses(member) {
  const [snapshot, setSnapshot] = useState(() => ({ ...loadCourses(() => window.localStorage),
    saveFailed: false, serverMode: false, serverError: '', loading: true }));
  const demo = useRef(snapshot.state);
  const requestNumber = useRef(0);
  const memberId = member?.memberId;

  async function refresh() {
    const number = ++requestNumber.current;
    setSnapshot((previous) => ({ ...previous, loading: true }));
    try {
      const [courses, reservations] = await Promise.all([
        api.listCourses(), memberId ? api.listEligibleReservations() : Promise.resolve([]),
      ]);
      if (number !== requestNumber.current) return;
      setSnapshot((previous) => ({ ...previous, state: { courses, reservations },
        serverMode: true, serverError: '', loading: false, notice: '', saveFailed: false }));
    } catch (error) {
      if (number !== requestNumber.current) return;
      setSnapshot((previous) => ({ ...previous, state: previous.serverMode ? previous.state : demo.current,
        serverError: error.message, loading: false }));
    }
  }

  useEffect(() => { refresh(); }, [memberId]);

  async function execute(action) {
    if (!snapshot.serverMode) {
      const result = transition(demo.current, action);
      if (!result.error) {
        const notice = saveCourses(() => window.localStorage, result.state);
        demo.current = result.state;
        setSnapshot((previous) => ({ ...previous, state: result.state, notice, saveFailed: Boolean(notice) }));
      }
      return result;
    }
    try {
      let result;
      if (action.type === 'create') { result = await api.createCourse(action.reservationId, action.fields); await refresh(); return result; }
      if (action.type === 'edit') { result = await api.editCourse(action.id, action.fields); await refresh(); return result; }
      if (action.type === 'delete') { await api.deleteCourse(action.id); await refresh(); return { id: action.id }; }
      if (action.type === 'like') result = await api.likeCourse(action.id);
      else if (action.type === 'comment') result = await api.commentCourse(action.id, action.text);
      else if (action.type === 'delete-comment') result = await api.deleteCourseComment(action.id, action.commentId);
      else if (action.type === 'report') result = await api.reportCourse(action.id, action.reason, action.details);
      else return { error: '지원하지 않는 작업입니다.' };
      setSnapshot((previous) => ({ ...previous, state: { ...previous.state,
        courses: previous.state.courses.map((course) => String(course.id) === String(result.id) ? result : course) } }));
      return { id: result.id };
    } catch (error) { return { error: error.message }; }
  }

  async function loadCourse(id) {
    try {
      const course = await api.getCourse(id);
      setSnapshot((previous) => ({ ...previous, state: { ...previous.state,
        courses: [...previous.state.courses.filter((item) => String(item.id) !== String(id)), course] } }));
      return { course };
    } catch (error) { return { error: error.message }; }
  }

  function retrySave() {
    const notice = saveCourses(() => window.localStorage, demo.current);
    setSnapshot((previous) => ({ ...previous, notice, saveFailed: Boolean(notice) }));
  }

  return { ...snapshot.state, storageNotice: snapshot.notice, saveFailed: snapshot.saveFailed,
    serverMode: snapshot.serverMode, serverError: snapshot.serverError, loading: snapshot.loading,
    retrySave, refresh, loadCourse, execute };
}
