const KEY = 'smart-travel-guest-ai-used';

export function hasUsedGuestAi(storage) {
  try { return (storage ?? window.localStorage).getItem(KEY) === '1'; }
  catch { return false; } // 저장소가 차단된 환경에서도 서버의 제한은 적용된다.
}

export function markGuestAiUsed(storage) {
  try { (storage ?? window.localStorage).setItem(KEY, '1'); } catch { /* 서버 세션에서도 사용 여부를 검사한다. */ }
}

export function canStartAi({ member, authChecked, authCheckFailed, used }) {
  return authChecked && !authCheckFailed && (Boolean(member) || !used);
}
