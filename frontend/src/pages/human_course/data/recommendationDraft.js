const draftKey = (memberId, reservationId) => `smart-travel:recommendation-draft:${memberId}:${reservationId}`;

export function readRecommendationDraft(memberId, reservationId) {
  if (!memberId || !reservationId) return null;
  try {
    const draft = JSON.parse(window.localStorage.getItem(draftKey(memberId, reservationId)));
    return draft && ['title', 'category', 'companion', 'summary', 'body'].every(field => typeof draft[field] === 'string')
      ? draft : null;
  } catch {
    return null;
  }
}

export function saveRecommendationDraft(memberId, reservationId, fields) {
  window.localStorage.setItem(draftKey(memberId, reservationId), JSON.stringify(fields));
}

export function clearRecommendationDraft(memberId, reservationId) {
  try {
    window.localStorage.removeItem(draftKey(memberId, reservationId));
  } catch {
    // A browser storage failure must not interrupt a successful publication.
  }
}
