export default function CoursePlan({ plan = [], version }) {
  return <div className="course-plan">
    {version != null && <p className="helper">공유 시 첨부한 최종 코스 v{version} · 읽기 전용</p>}
    {plan.length ? plan.map((day) => <section className="plan-day" key={day.day}><h4>{day.day}일차 일정</h4><ol className="plan-stops">{day.stops.map((stop, index) => <li key={`${day.day}-${index}`}>{stop}</li>)}</ol></section>) : <p>불러올 최종 일정이 없습니다.</p>}
  </div>;
}
