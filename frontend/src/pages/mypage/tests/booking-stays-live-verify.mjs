import assert from 'node:assert/strict';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { createServer } from 'vite';
const cookies = new Map();
async function request(path, options = {}) {
  const response = await fetch('http://127.0.0.1:5173' + path, { ...options, redirect: 'manual',
    headers: { Cookie: [...cookies].map(([key,value]) => `${key}=${value}`).join('; '), ...options.headers } });
  for (const cookie of response.headers.getSetCookie()) {
    const pair = cookie.split(';')[0], index = pair.indexOf('=');
    cookies.set(pair.slice(0,index),pair.slice(index+1));
  }
  return response;
}
async function post(path, values) {
  const csrf = await (await request('/api/auth/csrf')).json();
  return request(path, {method:'POST',headers:{[csrf.headerName]:csrf.token,'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams(values)});
}
let vite;
try {
  const login = await post('/api/auth/login',{loginId:'user1',password:process.env.LOCAL_TEST_PASSWORD || '123456'});
  assert.equal(login.status,200,'Local test account login');
  const response=await request('/api/mypage');
  assert.equal(response.status,200);
  const snapshot=await response.json();
  const testBookings=snapshot.bookings.filter(row=>row.reservationNo.startsWith('TEST-')&&!row.courseId?.startsWith('draft-'));
  assert.ok(testBookings.length>0);
  vite=await createServer({server:{middlewareMode:true},appType:'custom'});
  const {default:Reservations}=await vite.ssrLoadModule('/src/pages/mypage/pages/Reservations.jsx');
  for(const booking of testBookings){
    assert.ok(booking.stayInfo?.imageUrl,`Photo URL missing for ${booking.id}`);
    assert.ok(booking.stayInfo.regionMatched);
    assert.ok(!booking.stay.includes('[테스트]'));
    const html=renderToStaticMarkup(React.createElement(Reservations,{bookings:snapshot.bookings,courses:snapshot.courses,reservationId:booking.id}));
    assert.ok(html.includes(booking.stay));
    assert.ok(html.includes(booking.stayInfo.imageUrl.replaceAll('&','&amp;')));
    assert.ok(html.includes(booking.stayInfo.address));
  }
  console.log(`PASS: live API and reservation detail render ${testBookings.length} visible region-matched lodgings, addresses and photo URLs`);
}finally{
  await post('/api/auth/logout',{});
  if(vite)await vite.close();
}
