import { useEffect, useState } from 'react';
import { request } from '../../api/auth';

const mockReviews = [
  {
    id: 'mock-yeosu',
    region: '여수',
    title: '케이블카보다 오래 기억난 여수 밤바다',
    summary: '오동도 → 이순신광장 → 여수 해상케이블카',
        photoAssets: [{ dataUrl: 'https://tong.visitkorea.or.kr/cms/resource/64/3422964_image2_1.png' }],
  },
  {
    id: 'mock-mokpo',
    region: '목포',
    title: '시장부터 유달산까지, 목포 한 바퀴',
    summary: '목포자유시장 → 유달산 → 목포근대역사관',
        photoAssets: [{ dataUrl: 'https://tong.visitkorea.or.kr/cms/resource/11/4067011_image2_1.JPG' }],
  },
  {
    id: 'mock-gwangju',
    region: '광주 광산구',
    title: '송정에서 떡갈비 먹고 느긋하게 걷기',
    summary: '송정떡갈비거리 → 1913송정역시장',
        photoAssets: [{ dataUrl: 'https://tong.visitkorea.or.kr/cms/resource/52/4143652_image2_1.jpg' }],
  },
];

export default function HomeReviewCards({ access }) {
  const showMock =
    import.meta.env.DEV &&
    new URLSearchParams(window.location.search).get('mockReviews') === '1';

  const [reviews, setReviews] = useState([]);
  const [status, setStatus] = useState('loading');

  useEffect(() => {
      if (showMock || access !== 'traveler') return;

    const controller = new AbortController();

    request('/api/human-courses/home-reviews', { signal: controller.signal })
      .then((data) => {
        if (controller.signal.aborted) return;
        setReviews(data);
        setStatus('ready');
      })
      .catch(() => {
        if (!controller.signal.aborted) setStatus('error');
      });

    return () => controller.abort();
  }, [showMock, access]);

  const cards = showMock ? mockReviews : reviews;

  return (
    <section className="landing-recommendations" aria-labelledby="courses-title">
      <div className="landing-section-heading">
        <h2 id="courses-title">이번 여행은 어디로 갈까요사우르스?</h2>
        <a href="#/human-courses">전체 보기 →</a>
      </div>

         {!showMock && access === 'checking' && (
     <p role="status">로그인 상태를 확인하는 중이에요.</p>
   )}
   {!showMock && access === 'guest' && (
     <p><a href="#/login">로그인</a>하면 비슷한 취향의 여행 후기를 볼 수 있어요.</p>
   )}
   {!showMock && access === 'other' && (
     <p>여행자 계정으로 로그인하면 맞춤 후기를 볼 수 있어요.</p>
   )}
   {!showMock && (access === 'error' || (access === 'traveler' && status === 'error')) && (
     <p role="alert">후기를 불러오지 못했어요. 새로고침 후 다시 시도해 주세요.</p>
   )}
   {!showMock && access === 'traveler' && status === 'loading' && (
     <p role="status">여행 후기를 불러오는 중이에요.</p>
   )}
   {!showMock && access === 'traveler' && status === 'ready' && cards.length === 0 && (
     <p>아직 추천할 후기가 없어요. 여행 취향을 설정하고 다시 확인해 보세요.</p>
   )}

      <div className="landing-course-grid">
        {cards.map((review) => {
          const photo = review.photoAssets?.[0]?.dataUrl;
          const body = (
            <>
              <div className="landing-course-image hanok">
                <span className="landing-region">{review.region}</span>
                {photo && (
                  <img
                    src={photo}
                    alt=""
                    loading="lazy"
                    onError={(event) => {
                      event.currentTarget.style.display = 'none';
                    }}
                  />
                )}
                {showMock && <span className="landing-example">테스트 후기</span>}
              </div>
              <div className="landing-course-body">
                <span className="landing-course-tag">{review.region}</span>
                <h3>{review.title}</h3>
                {review.summary && <p>{review.summary}</p>}
              </div>
            </>
          );

          return showMock ? (
            <article key={review.id} className="landing-course">{body}</article>
          ) : (
            <a
              key={review.id}
              className="landing-course"
              href={`#/human-courses/${encodeURIComponent(review.id)}`}
            >
              {body}
            </a>
          );
        })}
      </div>
    </section>
  );
}