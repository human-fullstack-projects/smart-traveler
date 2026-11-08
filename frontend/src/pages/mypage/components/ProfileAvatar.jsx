export default function ProfileAvatar({ avatarData, name = '여행자' }) {
  return avatarData ? <img className="mp-profile-avatar" src={avatarData} alt={`${name} 프로필 이미지`} />
    : <span className="mp-profile-avatar mp-profile-avatar-default" role="img" aria-label="기본 프로필 이미지">
      <svg width="52" height="52" viewBox="0 0 48 48" fill="none" aria-hidden="true"><circle cx="24" cy="17" r="8" fill="currentColor" /><path d="M8 42c0-10 6-15 16-15s16 5 16 15" fill="currentColor" /></svg>
    </span>;
}
