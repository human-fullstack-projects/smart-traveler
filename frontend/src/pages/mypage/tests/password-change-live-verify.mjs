// 실행 중인 로컬 서버의 거부 경로만 검증한다. 실제 비밀번호는 변경하지 않는다.
import assert from 'node:assert/strict';
const cookies = new Map();
async function request(path, options={}) {
  const response = await fetch('http://127.0.0.1:5173'+path,{...options,redirect:'manual',headers:{Cookie:[...cookies].map(([k,v])=>`${k}=${v}`).join('; '),...options.headers}});
  for(const cookie of response.headers.getSetCookie()){const pair=cookie.split(';')[0],index=pair.indexOf('=');cookies.set(pair.slice(0,index),pair.slice(index+1));}
  return response;
}
async function csrfHeaders(){const csrf=await(await request('/api/auth/csrf')).json();return {[csrf.headerName]:csrf.token};}
try {
  const login=await request('/api/auth/login',{method:'POST',headers:{...await csrfHeaders(),'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({loginId:'user1',password:process.env.LOCAL_TEST_PASSWORD||'123456'})});
  assert.equal(login.status,200);
  const before=await(await request('/api/mypage/profile')).json();
  const input={expectedMemberId:before.memberId,currentPassword:'INVALID_CURRENT_PASSWORD_TEST',newPassword:'ExampleNext123',passwordConfirmation:'ExampleNext123'};
  const withoutCsrf=await request('/api/mypage/profile/password',{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify(input)});
  assert.equal(withoutCsrf.status,403);
  async function put(body){return request('/api/mypage/profile/password',{method:'PUT',headers:{...await csrfHeaders(),'Content-Type':'application/json'},body:JSON.stringify(body)});}
  const wrong=await put(input);assert.equal(wrong.status,400);
  assert.match((await wrong.json()).message,/현재 비밀번호/);
  const mismatch=await put({...input,currentPassword:process.env.LOCAL_TEST_PASSWORD||'123456',passwordConfirmation:'DifferentConfirm123'});
  assert.equal(mismatch.status,400);assert.match((await mismatch.json()).message,/확인.*일치/);
  const after=await(await request('/api/mypage/profile')).json();
  assert.ok(JSON.stringify(before)===JSON.stringify(after),'Rejected password changes must preserve profile data');
  console.log('PASS: password API rejects missing CSRF, wrong current password and mismatched confirmation; profile preserved');
} finally {
  await request('/api/auth/logout',{method:'POST',headers:await csrfHeaders()});
}
