import Card from './Card';

export default function AccessNotice({ kind = 'denied', onRetry }) {
  const checking = kind === 'checking';
  const unavailable = kind === 'unavailable';
  const login = kind === 'login';
  return <section className="mypage" aria-live="polite"><Card>
    <h1>{checking ? '로그인 상태를 확인하고 있습니다.' : unavailable ? '로그인 상태를 확인할 수 없습니다.' : login ? '로그인이 필요합니다.' : '접근이 제한되었습니다.'}</h1>
    {!checking && <>
      <p>{unavailable ? '서버 연결을 확인한 뒤 다시 시도해 주세요. 회원 자료는 표시하지 않습니다.' : login ? '마이페이지의 모든 기능은 로그인 후 이용할 수 있습니다.' : '본인 소유가 아니거나 존재하지 않는 자료입니다.'}</p>
      <div className="actions"><a className="primary" href="#/login">로그인</a><a className="secondary" href={login ? '#/signup' : '#/mypage'}>{login ? '회원가입' : '마이페이지로'}</a>{onRetry && <button type="button" onClick={onRetry}>다시 확인</button>}</div>
    </>}
  </Card></section>;
}
