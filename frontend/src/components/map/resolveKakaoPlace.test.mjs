import test from 'node:test';
import assert from 'node:assert/strict';
import { resolveKakaoPlace } from './resolveKakaoPlace.js';

function mapsFor({ address = [], reverse = [], keyword = [] } = {}) {
  const calls = [];
  const maps = { services: {
    Status: { OK: 'OK', ZERO_RESULT: 'ZERO_RESULT' },
    AnalyzeType: { EXACT: 'EXACT' },
    Geocoder: class {
      addressSearch(value, callback, options) {
        calls.push(['address', value, options.analyze_type]);
        callback(address, address.length ? 'OK' : 'ZERO_RESULT');
      }
      coord2Address(x, y, callback) {
        calls.push(['reverse', x, y]);
        callback(reverse, reverse.length ? 'OK' : 'ZERO_RESULT');
      }
    },
    Places: class {
      keywordSearch(value, callback, options) {
        calls.push(['keyword', value, options]);
        callback(keyword, keyword.length ? 'OK' : 'ZERO_RESULT');
      }
    },
  } };
  return { maps, calls };
}

test('address resolves to coordinates before keyword search', async () => {
  const { maps, calls } = mapsFor({ address: [{ x: '126.9749', y: '37.5658', road_address: { address_name: '서울특별시 중구 세종대로 99' } }] });
  const result = await resolveKakaoPlace(maps, { name: '덕수궁', address: '서울특별시 중구 세종대로 99' }, '서울');
  assert.equal(result.latitude, 37.5658);
  assert.equal(result.locationStatus, 'ready');
  assert.deepEqual(calls, [['keyword', '덕수궁', undefined], ['address', '서울특별시 중구 세종대로 99', 'EXACT']]);
});

test('coordinates resolve a missing address', async () => {
  const { maps } = mapsFor({ reverse: [{ road_address: { address_name: '서울 중구 세종대로 110' } }] });
  const result = await resolveKakaoPlace(maps, { name: '서울시청', latitude: 37.5665, longitude: 126.978 });
  assert.equal(result.address, '서울 중구 세종대로 110');
  assert.equal(result.locationStatus, 'ready');
});

test('a unique exact name in the region resolves to coordinates', async () => {
  const { maps } = mapsFor({ keyword: [
    { place_name: '광화문', address_name: '서울 종로구 세종로', x: '126.9769', y: '37.5759' },
    { place_name: '광화문 카페', address_name: '서울 종로구 세종로', x: '126.9', y: '37.5' },
  ] });
  const result = await resolveKakaoPlace(maps, { name: '광화문' }, '서울');
  assert.equal(result.longitude, 126.9769);
  assert.equal(result.locationStatus, 'ready');
});

test('ambiguous names and mismatched addresses remain without markers', async () => {
  const duplicate = { place_name: '중앙공원', address_name: '서울 중구 공원로 1', x: '127', y: '37' };
  const { maps } = mapsFor({ keyword: [duplicate, { ...duplicate, x: '128' }] });
  const ambiguous = await resolveKakaoPlace(maps, { name: '중앙공원' }, '서울');
  assert.equal(ambiguous.locationStatus, 'unresolved');
  assert.equal(ambiguous.latitude, undefined);
  const mismatched = await resolveKakaoPlace(maps, { name: '중앙공원', address: '서울 중구 다른로 9' }, '서울');
  assert.equal(mismatched.locationStatus, 'unresolved');
});

test('a single keyword result is accepted even when its name is not identical', async () => {
  const { maps } = mapsFor({ keyword: [
    { place_name: '국립중앙박물관 본관', address_name: '서울 용산구 서빙고로 137', x: '126.9804', y: '37.5239' },
  ] });
  const result = await resolveKakaoPlace(maps, { name: '국립중앙박물관' }, '서울');
  assert.equal(result.locationStatus, 'ready');
  assert.equal(result.latitude, 37.5239);
});

test('valid coordinates retain a marker when reverse lookup fails', async () => {
  const { maps } = mapsFor();
  const result = await resolveKakaoPlace(maps, { name: '장소', latitude: 37.5, longitude: 127 });
  assert.equal(result.locationStatus, 'ready');
  assert.equal(result.addressStatus, 'unavailable');
});

test('a shortened beach name resolves only when unique in the region', async () => {
  const { maps } = mapsFor({ keyword: [
    { place_name: '광안리해수욕장', address_name: '부산 수영구 광안해변로 219', x: '129.118', y: '35.153' },
    { place_name: '광안리 카페', address_name: '부산 수영구 광안해변로 200', x: '129.12', y: '35.15' },
  ] });
  const result = await resolveKakaoPlace(maps, { name: '광안리' }, '부산');
  assert.equal(result.locationStatus, 'ready');
  assert.equal(result.longitude, 129.118);
});
