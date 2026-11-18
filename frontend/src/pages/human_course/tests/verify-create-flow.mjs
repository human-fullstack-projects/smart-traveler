import assert from 'node:assert/strict';
import React from 'react';
import {renderToStaticMarkup} from 'react-dom/server';
import {createServer} from 'vite';
const vite=await createServer({server:{middlewareMode:true},appType:'custom'});
try {
 const {default:Create}=await vite.ssrLoadModule('/src/pages/human_course/pages/HumanCourseCreate.jsx');
 const reservation={id:'42',name:'TEST HOTEL',region:'TEST REGION',dates:'2026-10-10 ~ 2026-10-11',status:'COMPLETED',itineraryFinal:true,courseVersion:1,plan:[{day:1,stops:['Palace','Museum']}],eligibilityError:''};
 for(const [from,back] of [['reservations','#/reservations'],['human-courses','#/human-courses']]){
  const html=renderToStaticMarkup(React.createElement(Create,{reservations:[reservation],reservationId:'42',from,serverMode:true,execute:async()=>({id:1})}));
  assert.ok(html.includes('TEST HOTEL'));assert.ok(html.includes('Palace'));assert.ok(html.includes('Museum'));
  assert.ok(!html.includes('<fieldset disabled'));assert.ok(html.includes('<dialog'));assert.ok(html.includes(`href="${back}"`));
  assert.ok(!html.includes('type="reset"'));console.log(`PASS: ${from} reservation preselection, hotel/itinerary, enabled form, preview dialog, return link`);
 }
 const blocked=renderToStaticMarkup(React.createElement(Create,{reservations:[{...reservation,status:'CONFIRMED',eligibilityError:'Not completed'}],reservationId:'42',serverMode:true}));
 assert.ok(blocked.includes('<fieldset disabled'));console.log('PASS: ineligible booking cannot publish');
} finally {await vite.close();}
