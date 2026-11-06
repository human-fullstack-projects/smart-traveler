import { useEffect, useState } from 'react';
import Card from '../components/Card';
import PageHero from '../components/PageHero';
import AccessNotice from '../components/AccessNotice';
import { myPageApi } from '../api/myPageApi';
import '../../../css/mypage.css';

export default function MyCourses({ courses = [], isGuest, member, addCourse, courseId }) {
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const [detail, setDetail] = useState(null);
  const [detailStatus, setDetailStatus] = useState('loading');
  useEffect(() => {
    let cancelled = false;
    if (!courseId) return;
    setDetailStatus('loading'); setDetail(null);
    if (isGuest) {
      const course = courses.find(c => c.id === courseId);
      setDetail(course || null); setDetailStatus(course ? 'ready' : 'denied');
    } else {
      myPageApi.course(member.memberId, courseId).then(course => {
        if (!cancelled) { setDetail(course); setDetailStatus('ready'); }
      }).catch(error => { if (!cancelled) setDetailStatus(error.status === 401 ? 'login' : error.status === 403 || error.status === 404 ? 'denied' : 'unavailable'); });
    }
    return () => { cancelled = true; };
  }, [courseId, member?.memberId, isGuest, courses]);

  async function submit(event) {
    event.preventDefault();
    const form = event.currentTarget;
    setBusy(true); setNotice('');
    try { await addCourse(Object.fromEntries(new FormData(form))); form.reset(); setNotice(isGuest ? '임시 코스를 보관했습니다. 로그인하면 회원 코스로 연결합니다.' : '코스 저장 요청을 처리했습니다. 저장 상태를 확인해 주세요.'); }
    catch (error) { setNotice(error.message); }
    finally { setBusy(false); }
  }

  if (courseId && detailStatus !== 'ready') return <AccessNotice kind={detailStatus === 'loading' ? 'checking' : detailStatus} />;
  const list = courseId ? [detail] : courses;
  return <div className="mypage">
    <PageHero eyebrow="MY COURSES" title="내 코스">{isGuest ? '사이트 내 이동 동안 임시로 보관합니다. 새로고침하거나 사이트를 떠나면 삭제됩니다.' : '본인에게 연결된 코스만 표시합니다.'}</PageHero>
    {courseId && <p><a href="#/my-courses">← 내 코스 목록</a></p>}
    {!list.length && <p className="empty">저장된 코스가 없습니다.</p>}
    <div className="stack">{list.map(course => <Card key={course.id}>
      <span className="pill">{course.state === 'GUEST' ? '비회원 임시 코스' : course.state === 'DRAFT' ? '회원 임시 코스' : '저장된 여행 코스'}</span>
      <h2>{courseId ? course.title : <a href={`#/my-courses/${encodeURIComponent(course.id)}`}>{course.title}</a>}</h2>
      <p>{course.region} · {course.startDate} ~ {course.endDate}</p>
      {courseId && course.days.map((day, index) => <section key={day.date}><h3>{index + 1}일차 · {day.date}</h3>{day.places.length ? <ol>{day.places.map((p, i) => <li key={`${i}-${p}`}>{p}</li>)}</ol> : <p>등록된 장소가 없습니다.</p>}</section>)}
    </Card>)}</div>
    {!courseId && <Card style={{ marginTop: 24 }}>
      <h2>임시 코스 보관</h2><p className="note">직접 입력한 초안입니다. AI 생성이나 예약 확정은 처리하지 않습니다. 장소는 첫날에 보관합니다.</p>
      <form onSubmit={submit}><fieldset disabled={busy}>
        <label>코스명<input name="title" maxLength={200} required /></label>
        <label>지역<input name="region" maxLength={100} required /></label>
        <div className="grid two"><label>시작일<input name="startDate" type="date" required /></label><label>종료일<input name="endDate" type="date" required /></label></div>
        <label>방문 장소 (한 줄에 하나)<textarea name="places" rows={3} maxLength={6000} /></label>
        <button className="primary" type="submit">{busy ? '보관 중…' : '임시 코스 보관'}</button>
      </fieldset></form>
      {notice && <p role="status">{notice}</p>}
    </Card>}
  </div>;
}