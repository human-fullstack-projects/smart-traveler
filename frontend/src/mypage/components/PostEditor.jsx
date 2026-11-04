import { useState } from 'react';
import Button from './Button';

export default function PostEditor({ booking, dispatch }) {
  const [title, setTitle] = useState(booking.post?.title || '');
  const [body, setBody] = useState(booking.post?.body || '');
  const [photo, setPhoto] = useState(booking.post?.photo || '');
  const [reading, setReading] = useState(false);
  const [notice, setNotice] = useState('');
  async function readPhoto(event) {
    const file = event.target.files[0];
    if (!file) return;
    if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type) || file.size > 1024 * 1024) { setNotice('PNG·JPEG·WebP 사진을 1MB 이하로 선택해주세요.'); event.target.value = ''; return; }
    setReading(true);
    try {
      const data = await new Promise((resolve, reject) => { const reader = new FileReader(); reader.onload = () => resolve(reader.result); reader.onerror = reject; reader.readAsDataURL(file); });
      setPhoto(data); setNotice('사진을 선택했습니다. 저장 버튼을 눌러 반영해주세요.');
    } catch { setNotice('사진을 읽지 못했습니다. 다시 선택해주세요.'); }
    finally { setReading(false); }
  }
  function submit(event) {
    event.preventDefault();
    if (!title.trim() || !body.trim()) { setNotice('제목과 여행 이야기를 입력해주세요.'); return; }
    dispatch?.({ type: 'post', id: booking.id, post: { title: title.trim(), body: body.trim(), photo } });
    setNotice('여행 기록을 저장했습니다. 브라우저 안에만 보관됩니다.');
  }
  return <details><summary>{booking.post ? '작성 코스 수정' : '휴먼 추천 코스 작성'}</summary><form onSubmit={submit}>
    <label>제목<input name="title" required maxLength={100} value={title} onChange={(event) => setTitle(event.target.value)} /></label>
    <label>여행 이야기<textarea name="body" required maxLength={10000} value={body} onChange={(event) => setBody(event.target.value)} /></label>
    <label>여행 사진 <span className="note">(선택)</span><input name="photo" type="file" accept="image/png,image/jpeg,image/webp" disabled={reading} onChange={readPhoto} /></label>
    {photo && <><img className="photo-preview" src={photo} alt="저장할 여행 사진 미리보기" /><Button variant="secondary" onClick={() => setPhoto('')}>사진 제거</Button></>}
    <p className="note">최종 저장 코스 버전 {booking.versions.at(-1).v}를 첨부합니다. 사진은 1MB 이하로 선택해주세요.</p>
    <Button type="submit" disabled={reading}>{reading ? '사진 읽는 중' : booking.post ? '수정 내용 저장' : '코스 저장하기'}</Button>
    {notice && <p role="status">{notice}</p>}
  </form></details>;
}
