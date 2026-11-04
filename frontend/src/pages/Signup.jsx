export default function Signup() {
  return (
    <section className="login-page" aria-labelledby="signup-title">
      <a className="login-back" href="#/login">
        ← 로그인으로 돌아가기
      </a>

      <div className="login-card">
        <div className="login-intro">
          <span className="login-kicker">JOIN SMART TRAVEL</span>

          <h1 id="signup-title">여행을 함께 시작해요.</h1>

          <p>회원가입에 필요한 정보를 입력해주세요.</p>
        </div>
        <form
  className="login-form"
  onSubmit={(event) => event.preventDefault()}
>
  <div className="login-field">
    <label htmlFor="signup-id">아이디</label>
    <input
      id="signup-id"
      name="loginId"
      type="text"
      autoComplete="username"
      placeholder="아이디를 입력해주세요"
      required
    />
  </div>

  <div className="login-field">
    <label htmlFor="signup-password">비밀번호</label>
    <input
      id="signup-password"
      name="password"
      type="password"
      autoComplete="new-password"
      placeholder="비밀번호를 입력해주세요"
      required
    />
  </div>

  <div className="login-field">
    <label htmlFor="signup-password-confirm">
      비밀번호 확인
    </label>
    <input
      id="signup-password-confirm"
      name="passwordConfirm"
      type="password"
      autoComplete="new-password"
      placeholder="비밀번호를 다시 입력해주세요"
      required
    />
  </div>
  <div className="login-field">
  <label htmlFor="signup-nickname">닉네임</label>
  <input
    id="signup-nickname"
    name="nickname"
    type="text"
    maxLength={50}
    placeholder="여행 후기 등에 사용할 이름"
  />
</div>

<div className="login-field">
  <label htmlFor="signup-email">이메일</label>
  <input
    id="signup-email"
    name="email"
    type="email"
    autoComplete="email"
    maxLength={255}
    placeholder="example@email.com"
  />
</div>

<button className="primary login-submit" type="submit">
  회원가입
</button>
</form>
      </div>
    </section>
  );
}