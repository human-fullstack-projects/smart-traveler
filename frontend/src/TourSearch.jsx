import { useEffect, useState } from 'react';
import { tourApi } from './tourApi.js';

// 대화에서 나온 이름은 후보가 하나일 때만 시도 선택에 반영한다.
function findProvince(regions, regionName) {
  const name = (regionName || '').replace(/\s/g, '');
  if (name.length < 2) return '';
  const matches = regions.filter((item) => {
    const candidate = item.name.replace(/\s/g, '');
    return candidate === name || (name.length >= 2 && candidate.startsWith(name));
  });
  return matches.length === 1 ? matches[0].code : '';
}

/** 한국관광공사의 실제 지역 코드와 관광지 목록을 조회한다. */
export default function TourSearch({ conditions, onConditionsChange }) {
  const [open, setOpen] = useState(false);
  const [regions, setRegions] = useState([]);
  const [province, setProvince] = useState('');
  const [districts, setDistricts] = useState([]);
  const [district, setDistrict] = useState('');
  const [loadingRegions, setLoadingRegions] = useState(false);
  const [loadingDistricts, setLoadingDistricts] = useState(false);
  const [loadingPlaces, setLoadingPlaces] = useState(false);
  const [error, setError] = useState('');
  const [districtError, setDistrictError] = useState('');
  const [places, setPlaces] = useState(null);
  const [page, setPage] = useState(1);
  const [applied, setApplied] = useState(false);

  useEffect(() => {
    if (!open || regions.length) return;
    const controller = new AbortController();
    setLoadingRegions(true); setError('');
    tourApi.regions('', controller.signal).then((data) => {
      if (!Array.isArray(data.items)) throw new Error('지역 목록 형식 오류');
      setRegions(data.items);
      setProvince(findProvince(data.items, conditions.regionName));
      if (!data.items.length) setError('조회된 지역이 없습니다.');
    }).catch((error) => { if (error.name !== 'AbortError') setError('지역을 불러오지 못했습니다. 관광정보 서버와 인증키 설정을 확인해 주세요.'); })
      .finally(() => { if (!controller.signal.aborted) setLoadingRegions(false); });
    return () => controller.abort();
  }, [open]);

  useEffect(() => {
    setDistrict(''); setDistricts([]); setDistrictError(''); setPlaces(null); setPage(1); setApplied(false);
    if (!open || !province) return;
    const controller = new AbortController();
    setLoadingDistricts(true);
    tourApi.regions(province, controller.signal).then((data) => {
      if (!Array.isArray(data.items)) throw new Error('시군구 목록 형식 오류');
      setDistricts(data.items);
    }).catch((error) => { if (error.name !== 'AbortError') setDistrictError('시군구 목록을 불러오지 못했습니다. 시도 전체 관광지는 조회할 수 있습니다.'); })
      .finally(() => { if (!controller.signal.aborted) setLoadingDistricts(false); });
    return () => controller.abort();
  }, [province, open]);

  async function search(nextPage) {
    if (!province || loadingPlaces) return;
    setLoadingPlaces(true); setError('');
    try {
      const result = await tourApi.places(province, district, nextPage);
      if (!Array.isArray(result.items)) throw new Error('관광지 목록 형식 오류');
      setPlaces(result); setPage(nextPage);
    } catch { setError('관광지를 불러오지 못했습니다. 관광정보 서버와 인증키 설정을 확인해 주세요.'); }
    finally { setLoadingPlaces(false); }
  }

  const provinceName = regions.find((item) => item.code === province)?.name || '';
  const districtName = districts.find((item) => item.code === district)?.name || '';
  function applyRegion() {
    if (!provinceName) return;
    // 관광지를 가져온 뒤에만 서버가 발급한 내부 지역 번호를 사용한다.
    onConditionsChange({ ...conditions, regionName: [provinceName, districtName].filter(Boolean).join(' '), regionId: places?.regionId ?? null });
    setApplied(true);
  }

  return <section className="tour-search" aria-labelledby="tour-search-title">
    <div className="tour-search-head"><div><span className="greeting-label">EXPLORE</span><h2 id="tour-search-title">지역 관광지 둘러보기</h2></div></div>
    <p>선택한 지역의 관광지를 가져오면 조회한 정보가 저장됩니다.</p>
    <button className="conditions-toggle" type="button" aria-expanded={open} onClick={() => setOpen(!open)}>{open ? '접기' : '관광지 찾아보기'} <span aria-hidden="true">{open ? '−' : '+'}</span></button>
    {open && <div className="tour-search-body">
      <label>시도<select value={province} disabled={loadingRegions || loadingPlaces} onChange={(event) => { setProvince(event.target.value); setError(''); }}><option value="">{loadingRegions ? '지역을 불러오는 중...' : '지역을 선택해 주세요'}</option>{regions.map((item) => <option key={item.code} value={item.code}>{item.name}</option>)}</select></label>
      <label>시군구 <span className="optional-label">선택</span><select value={district} disabled={!province || loadingDistricts || loadingPlaces} onChange={(event) => { setDistrict(event.target.value); setPlaces(null); setPage(1); setApplied(false); }}><option value="">{loadingDistricts ? '불러오는 중...' : '전체'}</option>{districts.map((item) => <option key={item.code} value={item.code}>{item.name}</option>)}</select></label>
      {districtError && <p role="status">{districtError}</p>}
      <div className="tour-search-actions"><button type="button" className="primary" disabled={!province || loadingPlaces || loadingDistricts} onClick={() => search(1)}>{loadingPlaces ? '불러오는 중...' : '관광지 가져오기'}</button><button type="button" className="tour-apply" disabled={!province || loadingPlaces} onClick={applyRegion}>대화 조건에 반영</button></div>
      {applied && <p role="status">선택한 지역을 대화 조건에 반영했습니다.</p>}
      {error && <p role="alert">{error}{!regions.length && <button className="tour-retry" type="button" onClick={() => { setOpen(false); setTimeout(() => setOpen(true), 0); }}>다시 시도</button>}</p>}
      {places && <div aria-live="polite"><p>{places.totalCount ? `관광지 ${places.totalCount}곳` : '조회된 관광지가 없습니다.'}</p><ul className="tour-results">{places.items.map((place) => <li key={place.id}><strong>{place.name}</strong>{place.address && <span>{place.address}</span>}</li>)}</ul><div className="tour-pages"><button type="button" disabled={page <= 1 || loadingPlaces} onClick={() => search(page - 1)}>이전</button><span>{page}쪽</span><button type="button" disabled={page >= 100 || page * places.pageSize >= places.totalCount || loadingPlaces} onClick={() => search(page + 1)}>다음</button></div></div>}
      <p className="tour-source">조회한 관광지만 저장됩니다. 자료 출처: 한국관광공사 TourAPI</p>
    </div>}
  </section>;
}
