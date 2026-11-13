export const RATING_LABELS = ['비선호', '조금', '보통', '선호', '매우 선호'];

// 기존 저장 형식의 선호 방향과 강도를 사용해 다섯 단계를 구분한다.
export function ratingSelection(option, rating) {
  if (!Number.isInteger(rating) || rating < 1 || rating > 5) throw new Error('1~5단계를 선택해 주세요.');
  const [polarity, weight] = [['avoid', 3], ['avoid', 1], ['like', 1], ['like', 2], ['like', 3]][rating - 1];
  return { ...option, polarity, weight };
}

export function selectionRating(selection) {
  if (!selection) return null;
  if (selection.polarity === 'avoid') return selection.weight >= 3 ? 1 : 2;
  return selection.weight >= 3 ? 5 : selection.weight === 2 ? 4 : 3;
}
