export function hasValidCoordinates(place) {
  const latitude = Number(place?.latitude);
  const longitude = Number(place?.longitude);
  return place?.latitude != null && place?.longitude != null
    && place.latitude !== '' && place.longitude !== ''
    && Number.isFinite(latitude) && Number.isFinite(longitude)
    && latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180;
}

function addressOf(result) {
  return result?.road_address?.address_name || result?.road_address_name
    || result?.address?.address_name || result?.address_name || '';
}

function normalizedName(value) {
  return String(value || '').replace(/[^0-9a-z가-힣]/gi, '').toLowerCase();
}

function addressMatches(given, candidate) {
  const parts = (value) => String(value || '').trim().split(/\s+/).filter(Boolean);
  const a = parts(given);
  const b = parts(candidate);
  if (!a.length || !b.length) return false;
  if (a.join('') === b.join('')) return true;
  const tailSize = Math.min(3, a.length);
  return b.length >= tailSize && a.slice(-tailSize).join('') === b.slice(-tailSize).join('');
}

export function addressSearch(maps, address) {
  return new Promise((resolve) => {
    new maps.services.Geocoder().addressSearch(address, (results, status) => {
      resolve(status === maps.services.Status.OK ? results?.[0] || null : null);
    }, { analyze_type: maps.services.AnalyzeType.EXACT });
  });
}

function coord2Address(maps, place) {
  return new Promise((resolve) => {
    new maps.services.Geocoder().coord2Address(Number(place.longitude), Number(place.latitude), (results, status) => {
      resolve(status === maps.services.Status.OK ? addressOf(results?.[0]) : '');
    });
  });
}

function keywordSearch(maps, query, options) {
  return new Promise((resolve) => {
    new maps.services.Places().keywordSearch(query, (results, status) => {
      resolve(status === maps.services.Status.OK ? results || [] : []);
    }, options);
  });
}

function uniqueCandidate(results, name) {
  const usable = (results || []).filter((result) => hasValidCoordinates({ latitude: result.y, longitude: result.x }));
  const exact = usable.filter((result) => normalizedName(result.place_name) === normalizedName(name));
  if (exact.length === 1) return exact[0];
  return usable.length === 1 ? usable[0] : null;
}

function resolved(place, result, address = '') {
  return { ...place, address: addressOf(result) || address,
    latitude: Number(result.y), longitude: Number(result.x), locationStatus: 'ready' };
}

export async function resolveKakaoPlace(maps, place, region = '') {
  const name = String(place.name || '').trim();
  const address = String(place.address || '').trim();

  // 1. 장소명만으로 정확히 하나를 고를 수 있으면 바로 사용한다.
  if (name) {
    const result = uniqueCandidate(await keywordSearch(maps, name), name);
    if (result) return resolved(place, result, address);
  }

  // 주소는 DB에 저장된 경우 가장 신뢰할 수 있는 보조 식별자로 사용한다.
  if (address) {
    const result = await addressSearch(maps, address);
    if (hasValidCoordinates({ latitude: result?.y, longitude: result?.x })) {
      return resolved(place, result, address);
    }
  }

  // 2. 장소명만으로 모호하면 지역명과 함께 다시 검색해 한 건을 특정한다.
  if (name) {
    const results = await keywordSearch(maps, [region, name].filter(Boolean).join(' '));
    const regionResult = uniqueCandidate(results, name);
    if (regionResult && (!address || addressMatches(address, addressOf(regionResult))))
      return resolved(place, regionResult, address);
  }

  // 3. DB 좌표가 있으면 그 주변을 중심으로 마지막 장소 검색을 시도한다.
  // 검색이 실패하더라도 DB 좌표 자체는 버리지 않아 마커가 사라지지 않게 한다.
  if (hasValidCoordinates(place)) {
    if (name && maps.LatLng) {
      const near = await keywordSearch(maps, name, {
        location: new maps.LatLng(Number(place.latitude), Number(place.longitude)),
        radius: 20000,
        sort: maps.services.SortBy?.DISTANCE,
      });
      const nearResult = uniqueCandidate(near, name);
      if (nearResult) return resolved(place, nearResult, address);
    }
    if (address) return { ...place, locationStatus: 'ready' };
    const foundAddress = await coord2Address(maps, place);
    return { ...place, address: foundAddress, locationStatus: 'ready', addressStatus: foundAddress ? 'ready' : 'unavailable' };
  }

  return { ...place, locationStatus: 'unresolved', addressStatus: address ? 'provided' : 'unavailable' };
}

/** destination: free-text region/address string (e.g. course.destination). Returns {latitude, longitude} or null. */
export async function resolveDestinationCenter(maps, destination) {
  const query = String(destination || '').trim();
  if (!query) return null;

  const addressResult = await addressSearch(maps, query);
  if (hasValidCoordinates({ latitude: addressResult?.y, longitude: addressResult?.x })) {
    return { latitude: Number(addressResult.y), longitude: Number(addressResult.x) };
  }

  const keywordResults = await keywordSearch(maps, query);
  const match = keywordResults.find((result) => hasValidCoordinates({ latitude: result.y, longitude: result.x }));
  if (match) return { latitude: Number(match.y), longitude: Number(match.x) };

  return null;
}
