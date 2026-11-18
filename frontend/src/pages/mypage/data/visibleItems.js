// 홈 집계와 목록 화면에서 같은 자료만 표시한다.
export const isSavedCourse = course => ['GUEST', 'DRAFT'].includes(course.state);
export const isVisibleBooking = booking => !booking.courseId?.startsWith('draft-');

export function bookingCategory(booking, today) {
  if (booking.status === 'CANCELLED') return 'cancelled';
  return booking.endDate && booking.endDate < today ? 'past' : 'upcoming';
}

export function isPastRecommendationBooking(booking, today) {
  return isVisibleBooking(booking) && bookingCategory(booking, today) === 'past'
    && ['CONFIRMED', 'COMPLETED'].includes(booking.status);
}

export function recommendationTrips(bookings, posts, today) {
  const pastBookings = bookings.filter(booking => isPastRecommendationBooking(booking, today));
  const bookingsById = new Map(pastBookings.map(booking => [String(booking.id), booking]));
  const published = posts.filter(post => bookingsById.has(String(post.reservationId)));
  const publishedIds = new Set(published.map(post => String(post.reservationId)));
  const pending = pastBookings.filter(booking => !publishedIds.has(String(booking.id)));
  return { bookingsById, published, pending };
}
