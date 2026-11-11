import Card from '../components/Card';
import PageHero from '../components/PageHero';
import ProfileAvatar from '../components/ProfileAvatar';
import useMemberResource from '../hooks/useMemberResource';
import { settingsApi } from '../api/settingsApi';
import '../../../css/mypage.css';
import '../mypage-settings.css';

export default function MyPage({ bookings = [], courses = [], posts = [], member, isGuest = true, status }) {
  const profile = useMemberResource(member?.memberId, settingsApi.profile);
  const name = profile.data?.nickname || member?.nickname || member?.loginId || '여행자';
  const memberLink = page => isGuest ? '#/login' : `#/${page}`;
  const summaries = [
    ['my-courses', '내 여행 코스', courses.length, '예약 없이 보관한 초안과 저장된 코스'],
      ['reservations', '내 예약', bookings.length, '본인 예약과 결제 상태'],
      ['my-posts', '내 여행 추천', posts.length, '본인이 등록한 여행 기록'],
  ];
  return <div className="mypage">
    <PageHero eyebrow="MY TRAVEL" title={<>나의 여행, <em>한곳에서.</em></>}>나의 정보와 여행 취향, 여행 일정을 한곳에서 관리하세요.</PageHero>
    <Card>
      <div className="mp-profile-line"><ProfileAvatar avatarData={isGuest ? null : profile.data?.avatarData} name={name} />
        <div className="mp-profile-summary"><span className="pill">{isGuest ? '비회원' : '회원'}</span>
          <h2>{isGuest ? '로그인이 필요합니다.' : `${name}님, 안녕하세요.`}</h2>
          <p className="muted">{isGuest ? '마이페이지의 모든 기능은 로그인 후 이용할 수 있습니다.' : '내 정보와 기본 취향을 관리하고 다음 여행을 준비하세요.'}</p>
          {!isGuest && profile.status === 'loading' && <p role="status" className="note">프로필을 불러오고 있습니다.</p>}
          {!isGuest && profile.error && <p role="alert" className="warning">프로필을 불러오지 못했습니다. <button type="button" onClick={profile.reload}>다시 확인</button></p>}
        </div>
        {isGuest && <div className="actions mp-profile-auth-actions"><a className="primary" href="#/login">로그인</a><a className="secondary" href="#/signup">회원가입</a></div>}
      </div>
      <div className="mp-home-actions"><a className="secondary" href={memberLink('my-profile')}>내 정보관리</a><a className="secondary" href={memberLink('my-travel-style')}>내 여행 스타일 관리</a></div>
    </Card>
    <div className="grid three" style={{ marginTop: 23 }}>{summaries.map(([page, title, count, description]) => <Card key={page}>
      <span className="pill">MY TRAVEL</span><h2><a className="card-title-link" href={memberLink(page)}>{title}</a></h2><p className="stats">{count}<span className="note"> 건</span></p><p className="muted">{description}</p><a className="text-button" href={memberLink(page)}>{title} 보기 →</a>
    </Card>)}</div>
  </div>;
}
