import CourseForm from './CourseForm';
import CourseMap from './CourseMap';
import CoursePlan from './CoursePlan';

export default function RecommendationEditor({ trip, plan, tripSelection, formKey, formProps, reviewNotice }) {
  const map = plan?.length > 0
    ? <CourseMap key={formKey} region={trip?.region} plan={plan} preview allDaysLabel="전체일정" />
    : null;
  const itinerary = <>
    <h3 className="itinerary-title">여행일정</h3>
    {trip && <p>{[trip.region, trip.dates].filter(Boolean).join(' · ')}</p>}
    <CoursePlan key={formKey} plan={plan} showPhotos />
  </>;

  return <div className="recommendation-layout">
    {tripSelection}
    <div className="recommendation-itinerary">
      <section className="panel recommendation-map"><h2>여행 지도</h2>
        {map || <p>{trip ? '표시할 여행지가 없습니다.' : '지난 여행을 선택하면 여행지 위치가 표시됩니다.'}</p>}
      </section>
      <section className="panel">{itinerary}</section>
    </div>
    <section className="panel recommendation-review">
      <h2>{formProps.mode === 'edit' && !formProps.republishing ? '후기 수정' : '후기 작성'}</h2>
      <CourseForm key={formKey} {...formProps} previewContext={<>
        {map && <div className="preview-itinerary-map">{map}</div>}
        {itinerary}
      </>} />
      {reviewNotice}
    </section>
  </div>;
}
