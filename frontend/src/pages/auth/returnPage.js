const isAuthPage = route => /^\/(login|signup)(?:\/|\?|$)/.test(route);

// 로그인 ↔ 회원가입 이동 중에는 인증 화면 진입 전의 페이지를 유지한다.
export function createReturnPage(initialRoute = '/') {
  let previous = '/';
  const remember = route => {
    if (typeof route === 'string' && route.startsWith('/') && !route.startsWith('//') && !isAuthPage(route)) previous = route;
  };
  remember(initialRoute);
  return { remember, get: () => previous };
}
