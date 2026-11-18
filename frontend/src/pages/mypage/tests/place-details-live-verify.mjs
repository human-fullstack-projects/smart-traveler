// frontend에서 실행. 로컬 서버와 user1 테스트 계정 필요. DB 자료는 변경하지 않는다.
import assert from 'node:assert/strict';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { createServer } from 'vite';
import { mapDays } from '../data/mapDays.js';
const cookies=new Map();
async function request(path,options={}) {
  const response=await fetch('http://127.0.0.1:5173'+path,{...options,redirect:'manual',headers:{Cookie:[...cookies].map(([k,v])=>`${k}=${v}`).join('; '),...options.headers}});
  for(const cookie of response.headers.getSetCookie()) {const pair=cookie.split(';')[0],i=pair.indexOf('=');cookies.set(pair.slice(0,i),pair.slice(i+1));}
  return response;
}
async function post(path,body) {
  const csrf=await(await request('/api/auth/csrf')).json();
  return request(path,{method:'POST',headers:{[csrf.headerName]:csrf.token,'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams(body)});
}
let vite;
try {
  assert.equal((await request('/api/mypage/places/detail?placeId=1')).status,401);
  assert.equal((await post('/api/auth/login',{loginId:'user1',password:process.env.LOCAL_TEST_PASSWORD||'123456'})).status,200);
  const snapshot=await(await request('/api/mypage')).json();
  let count=0,photos=0,descriptions=0;const missing=[];
  for(const course of snapshot.courses) {
    const response=await request(`/api/mypage/courses/${encodeURIComponent(course.id)}`);
    assert.equal(response.status,200);
    const detail=await response.json();
    for(const day of mapDays(detail.days))for(const place of day.places){
      assert.ok(place.placeId,`${course.region}/${place.name}: missing DB place ID`);
      assert.ok(Number.isFinite(Number(place.latitude))&&place.latitude!=null,`${place.name}: missing latitude`);
      assert.ok(Number.isFinite(Number(place.longitude))&&place.longitude!=null,`${place.name}: missing longitude`);
      const query=new URLSearchParams({name:place.name,region:course.region});
      if(place.placeId)query.set('placeId',place.placeId);
      const response=await request(`/api/mypage/places/detail?${query}`);
      if(response.status===404){missing.push(`${course.region}/${place.name}`);continue;}
      assert.equal(response.status,200);
      const data=await response.json();assert.ok(data.placeId);assert.ok(data.name);
      assert.equal(data.placeId,place.placeId);
      assert.equal(data.name,place.name);
      assert.ok(data.address?.includes(course.region),`${place.name}: region mismatch (${course.region})`);
      count++;if(data.imageUrl)photos++;if(data.description?.trim())descriptions++;
    }
  }
  assert.ok(count>0);assert.ok(photos>0);assert.ok(descriptions>0);
  assert.equal((await request('/api/mypage/places/detail?name=NO_SUCH_PLACE_TEST_123456&region=서울')).status,404);
  vite=await createServer({server:{middlewareMode:true},appType:'custom'});
  const {default:Cards}=await vite.ssrLoadModule('/src/components/map/MapPlaceCards.jsx');
  const props={places:[{id:'place-1',name:'경복궁',number:'1-1'}],onSelect(){},loadDetails:()=>Promise.resolve({}),searchRegion:'서울'};
  const opened=renderToStaticMarkup(React.createElement(Cards,{...props,selectedId:'place-1'}));
  const closed=renderToStaticMarkup(React.createElement(Cards,{...props,selectedId:null}));
  assert.ok(opened.includes('aria-expanded="true"'));assert.ok(opened.includes('경복궁 상세 정보'));
  assert.ok(closed.includes('aria-expanded="false"'));assert.ok(!closed.includes('role="region"'));
  console.log(JSON.stringify({matchedPlaces:count,photos,descriptions,unmatched:missing,expandedCardRender:true}));
}finally{await post('/api/auth/logout',{});if(vite)await vite.close();}
