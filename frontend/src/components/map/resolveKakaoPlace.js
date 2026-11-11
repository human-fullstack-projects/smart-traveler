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
  return String(value || '').replace(/\s+/g, '').toLowerCase();
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

function addressSearch(maps, address) {
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

function keywordSearch(maps, query) {
  return new Promise((resolve) => {
    new maps.services.Places().keywordSearch(query, (results, status) => {
      resolve(status === maps.services.Status.OK ? results || [] : []);
    });
  });
}

export async function resolveKakaoPlace(maps, place, region = '') {
  const name = String(place.name || '').trim();
  const address = String(place.address || '').trim();

  if (hasValidCoordinates(place)) {
    if (address) return { ...place, locationStatus: 'ready' };
    const foundAddress = await coord2Address(maps, place);
    return { ...place, address: foundAddress, locationStatus: 'ready', addressStatus: foundAddress ? 'ready' : 'unavailable' };
  }

  if (address) {
    const result = await addressSearch(maps, address);
    if (hasValidCoordinates({ latitude: result?.y, longitude: result?.x })) {
      return { ...place, address: addressOf(result) || address,
        latitude: Number(result.y), longitude: Number(result.x), locationStatus: 'ready' };
    }
  }

  if (name) {
    const results = await keywordSearch(maps, [region, name].filter(Boolean).join(' '));
    const matches = results.filter((result) => normalizedName(result.place_name) === normalizedName(name))
      .filter((result) => !region || [result.road_address_name, result.address_name].some((value) => String(value || '').includes(region)))
      .filter((result) => !address || [result.road_address_name, result.address_name].some((value) => addressMatches(address, value)))
      .filter((result) => hasValidCoordinates({ latitude: result.y, longitude: result.x }));
    if (matches.length === 1) {
      const result = matches[0];
      return { ...place, address: addressOf(result) || address,
        latitude: Number(result.y), longitude: Number(result.x), locationStatus: 'ready' };
    }
  }

  return { ...place, locationStatus: 'unresolved', addressStatus: address ? 'provided' : 'unavailable' };
}
