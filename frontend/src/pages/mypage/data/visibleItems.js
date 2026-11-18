// 홈 집계와 목록 화면에서 같은 자료만 표시한다.
export const isSavedCourse = course => ['GUEST', 'DRAFT'].includes(course.state);
export const isVisibleBooking = booking => !booking.courseId?.startsWith('draft-');
