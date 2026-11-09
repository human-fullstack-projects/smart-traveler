let sdkPromise;

export function loadKakaoMaps() {
  if (window.kakao?.maps?.Map) return Promise.resolve(window.kakao.maps);
  if (sdkPromise) return sdkPromise;

  const appKey = import.meta.env.VITE_KAKAO_MAP_APP_KEY;
  if (!appKey) return Promise.reject(new Error('VITE_KAKAO_MAP_APP_KEY를 설정해 주세요.'));

  sdkPromise = new Promise((resolve, reject) => {
    const scriptId = 'kakao-maps-sdk';
    let script = document.getElementById(scriptId);

    const onLoad = () => {
      if (!window.kakao?.maps?.load) {
        onError();
        return;
      }
      window.kakao.maps.load(() => resolve(window.kakao.maps));
    };
    const onError = () => {
      script?.removeEventListener('load', onLoad);
      script?.removeEventListener('error', onError);
      script?.remove();
      sdkPromise = undefined;
      reject(new Error('카카오맵 SDK를 불러오지 못했습니다. 도메인과 JavaScript 키를 확인해 주세요.'));
    };

    if (!script) {
      script = document.createElement('script');
      script.id = scriptId;
      script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(appKey)}&autoload=false&libraries=services`;
      script.async = true;
      script.addEventListener('load', onLoad, { once: true });
      script.addEventListener('error', onError, { once: true });
      document.head.appendChild(script);
    } else {
      script.addEventListener('load', onLoad, { once: true });
      script.addEventListener('error', onError, { once: true });
    }
  });

  return sdkPromise;
}
