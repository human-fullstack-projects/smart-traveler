import { useEffect, useState } from 'react';

const emptyUrls = [];

export function useTourPhotos(load, needed) {
  const [result, setResult] = useState(null);
  useEffect(() => {
    if (!needed || !load) return;
    let active = true;
    load().then(urls => {
      if (active) setResult({ load, status: 'ready', urls });
    }).catch(() => {
      if (active) setResult({ load, status: 'error', urls: emptyUrls });
    });
    return () => { active = false; };
  }, [load, needed]);

  const current = result?.load === load ? result : null;
  return {
    status: needed && load ? current?.status || 'loading' : 'idle',
    urls: needed && load ? current?.urls || emptyUrls : emptyUrls,
  };
}

export function TourPhotoStatus({ status, missing }) {
  if (status === 'loading') return <span role="status">사진을 불러오는 중입니다.</span>;
  if (status === 'error') return '사진을 표시할 수 없습니다.';
  return missing;
}

export function TourPhotoCredit() {
  return <small>사진 출처{' '}
    <a href="https://www.data.go.kr/data/15101578/openapi.do" target="_blank" rel="noreferrer">한국관광공사 TourAPI</a>
  </small>;
}

export default function TourPhoto({ localUrl, load, alt, missing }) {
  const [failedUrls, setFailedUrls] = useState([]);
  const local = localUrl && !failedUrls.includes(localUrl) ? localUrl : null;
  const photos = useTourPhotos(load, !local);
  const url = local || photos.urls.find(item => !failedUrls.includes(item));
  if (!url) return <p className="tour-photo__message">
    <TourPhotoStatus status={photos.status} missing={photos.urls.length || localUrl ? '사진을 표시할 수 없습니다.' : missing} />
  </p>;
  return <div className="tour-photo">
    <img src={url} alt={alt} referrerPolicy="no-referrer" onError={() => setFailedUrls(previous => [...previous, url])} />
    {!local && <TourPhotoCredit />}
  </div>;
}
