import assert from 'node:assert/strict';
import { mapDays } from '../data/mapDays.js';

const points = [{ name: '경복궁', latitude: 37.5796, longitude: 126.977, address: '서울 종로구' }];
const saved = [{ day: 1, stops: ['10:00 경복궁', '사라진 옛 장소'] }];
const result = mapDays(saved, [{ mapPlaces: points }]);
assert.equal(result[0].places[0].latitude, 37.5796);
assert.equal(result[0].places[0].id, 'mypage-0-0');
assert.equal(result[0].places[1].name, '사라진 옛 장소');
assert.equal(result[0].places[1].latitude, undefined);
assert.equal(mapDays([{ places: ['경복궁', '서울숲'], mapPlaces: points }])[0].places.length, 2);
assert.equal(mapDays([{ mapPlaces: points }])[0].places[0].longitude, 126.977);
assert.equal(mapDays([{ places: ['경복궁'], mapPlaces: [{ name: '경복궁', latitude: null }] }], [{ mapPlaces: points }])[0].places[0].latitude, 37.5796);
assert.deepEqual(mapDays(), []);
console.log('PASS: draft coordinates, historical snapshot enrichment, unmatched stops, stable marker IDs, partial and empty data');
