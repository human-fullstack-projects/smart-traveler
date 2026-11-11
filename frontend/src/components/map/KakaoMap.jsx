import { useEffect, useRef, useState } from 'react';
import { loadKakaoMaps } from './loadKakaoMaps';
import MapFallback from './MapFallback';
import './kakao-map.css';

const DEFAULT_CENTER = { latitude: 37.5665, longitude: 126.9780 };
const EMPTY_PLACES = [];
const EMPTY_PATHS = [];

function hasCoordinates(place) {
  return Number.isFinite(Number(place?.latitude)) && Number.isFinite(Number(place?.longitude))
    && place.latitude !== null && place.longitude !== null
    && place.latitude !== '' && place.longitude !== '';
}

/** places: [{ id, name, latitude, longitude }]. center: { latitude, longitude }. */
export default function KakaoMap({ places = EMPTY_PLACES, center, level = 3, width = '100%', height = '400px', onMarkerClick, onStatusChange, selectedId, paths = EMPTY_PATHS, fitPoints, fitRequest = 0 }) {
  const containerRef = useRef(null);
  const mapRef = useRef(null);
  const mapsRef = useRef(null);
  const onMarkerClickRef = useRef(onMarkerClick);
  const onStatusChangeRef = useRef(onStatusChange);
  const [mapReady, setMapReady] = useState(false);
  const [error, setError] = useState('');
  const [retryKey, setRetryKey] = useState(0);

  onMarkerClickRef.current = onMarkerClick;
  onStatusChangeRef.current = onStatusChange;
  const firstPlace = places.find(hasCoordinates);
  const mapCenter = hasCoordinates(center) ? center : firstPlace || DEFAULT_CENTER;
  const latitude = Number(mapCenter.latitude);
  const longitude = Number(mapCenter.longitude);

  useEffect(() => {
    let cancelled = false;
    loadKakaoMaps().then((maps) => {
      if (cancelled || !containerRef.current) return;
      mapsRef.current = maps;
      mapRef.current = new maps.Map(containerRef.current, {
        center: new maps.LatLng(latitude, longitude),
        level,
      });
      setMapReady(true);
      onStatusChangeRef.current?.('ready');
    }).catch((loadError) => {
      if (!cancelled) {
        setError(loadError.message);
        onStatusChangeRef.current?.('error');
      }
    });

    return () => {
      cancelled = true;
      mapRef.current = null;
      mapsRef.current = null;
    };
    // A map belongs to its DOM element and is created only once per mount.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [retryKey]);

  useEffect(() => {
    if (!mapReady) return;
    mapRef.current.setCenter(new mapsRef.current.LatLng(latitude, longitude));
  }, [mapReady, latitude, longitude]);

  useEffect(() => {
    if (mapReady) mapRef.current.setLevel(level);
  }, [mapReady, level]);

  useEffect(() => {
    if (!mapReady) return undefined;
    const maps = mapsRef.current;
    const markers = places.filter(hasCoordinates).map((place) => {
      const position = new maps.LatLng(Number(place.latitude), Number(place.longitude));
      const handleClick = () => onMarkerClickRef.current?.(place);
      if (place.markerLabel != null) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = `kakao-map-marker kakao-map-marker--${place.kind === 'stay' ? 'stay' : 'course'}${String(place.id) === String(selectedId) ? ' is-selected' : ''}`;
        button.textContent = String(place.markerLabel);
        button.setAttribute('aria-label', place.name || String(place.markerLabel));
        button.addEventListener('click', handleClick);
        const marker = new maps.CustomOverlay({ map: mapRef.current, position, content: button, clickable: true, yAnchor: 1, zIndex: String(place.id) === String(selectedId) ? 4 : 2 });
        return { marker, handleClick, button };
      }
      const marker = new maps.Marker({
        map: mapRef.current,
        position,
        title: place.name || '',
      });
      maps.event.addListener(marker, 'click', handleClick);
      return { marker, handleClick };
    });

    return () => {
      markers.forEach(({ marker, handleClick, button }) => {
        if (button) button.removeEventListener('click', handleClick);
        else maps.event.removeListener(marker, 'click', handleClick);
        marker.setMap(null);
      });
    };
  }, [mapReady, places, selectedId]);

  useEffect(() => {
    if (!mapReady) return undefined;
    const maps = mapsRef.current;
    const lines = paths.filter((path) => path.points?.filter(hasCoordinates).length > 1).map((path) => new maps.Polyline({
      map: mapRef.current,
      path: path.points.filter(hasCoordinates).map((point) => new maps.LatLng(Number(point.latitude), Number(point.longitude))),
      strokeWeight: 3,
      strokeColor: path.color || '#247e67',
      strokeOpacity: 0.8,
      strokeStyle: path.dashed ? 'dashed' : 'solid',
    }));
    return () => lines.forEach((line) => line.setMap(null));
  }, [mapReady, paths]);

  useEffect(() => {
    if (!mapReady || !fitPoints?.length) return;
    const points = fitPoints.filter(hasCoordinates);
    if (!points.length) return;
    if (points.length === 1) {
      mapRef.current.setCenter(new mapsRef.current.LatLng(Number(points[0].latitude), Number(points[0].longitude)));
      return;
    }
    const bounds = new mapsRef.current.LatLngBounds();
    points.forEach((point) => bounds.extend(new mapsRef.current.LatLng(Number(point.latitude), Number(point.longitude))));
    mapRef.current.setBounds(bounds, 40, 40, 40, 40);
  }, [mapReady, fitPoints, fitRequest]);

  useEffect(() => {
    if (!mapReady || !containerRef.current || !window.ResizeObserver) return undefined;
    const observer = new ResizeObserver(() => mapRef.current?.relayout());
    observer.observe(containerRef.current);
    return () => observer.disconnect();
  }, [mapReady]);

  if (error) return <MapFallback
    onRetry={() => { setError(''); setMapReady(false); onStatusChangeRef.current?.('loading'); setRetryKey((value) => value + 1); }}
    reason="네트워크 연결을 확인한 뒤 다시 시도해 주세요."
    width={width}
    height={height}
  />;

  return <div ref={containerRef} role="application" aria-label="카카오 지도" style={{ width, height }} />;
}
