import { useRef, useState } from 'react';

const MAX_PHOTOS = 5;
const MAX_FILE_BYTES = 600 * 1024;
const MAX_TOTAL_BYTES = 2.5 * 1024 * 1024;
const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];

function readImage(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve({ name: file.name, dataUrl: String(reader.result) });
    reader.onerror = () => reject(new Error('사진을 읽지 못했습니다. 다시 선택해주세요.'));
    reader.readAsDataURL(file);
  });
}

export default function CourseForm({ course = {}, mode = 'create', onSubmit, cancelHref, disabled = false }) {
  const editing = mode === 'edit';
  const [preview, setPreview] = useState(() => ({
    title: course.title || '', category: course.category || '', companion: course.companion || '',
    summary: course.summary || '', body: course.body || '',
  }));
  const [selectedPhotos, setSelectedPhotos] = useState(null);
  const [photoError, setPhotoError] = useState('');
  const [reading, setReading] = useState(false);
  const photoRequest = useRef(0);
  const visiblePhotos = selectedPhotos || course.photoAssets || (course.photos || []).map((name) => ({ name }));

  async function changePhotos(event) {
    const request = ++photoRequest.current;
    const files = Array.from(event.target.files || []);
    setPhotoError('');
    if (!files.length) { setSelectedPhotos(null); setReading(false); return; }
    if (files.length > MAX_PHOTOS || files.some((file) => !IMAGE_TYPES.includes(file.type))
        || files.some((file) => file.size > MAX_FILE_BYTES)
        || files.reduce((total, file) => total + file.size, 0) > MAX_TOTAL_BYTES) {
      setSelectedPhotos(null);
      setReading(false);
      event.target.value = '';
      setPhotoError('JPG·PNG·WebP·GIF 사진을 최대 5개 선택해주세요. 파일당 600KB, 전체 2.5MB까지 저장할 수 있습니다.');
      return;
    }
    setReading(true);
    try {
      const photos = await Promise.all(files.map(readImage));
      if (request === photoRequest.current) setSelectedPhotos(photos);
    } catch (error) {
      if (request === photoRequest.current) { setSelectedPhotos(null); event.target.value = ''; setPhotoError(error.message); }
    } finally { if (request === photoRequest.current) setReading(false); }
  }

  return <form onReset={() => {
    photoRequest.current += 1;
    setReading(false);
    setPreview({ title: course.title || '', category: course.category || '', companion: course.companion || '', summary: course.summary || '', body: course.body || '' });
    setSelectedPhotos(null);
    setPhotoError('');
  }} onChange={(event) => {
    if (event.target.type === 'file') return;
    const data = new FormData(event.currentTarget);
    setPreview({ title: String(data.get('title') || ''), category: String(data.get('category') || ''), companion: String(data.get('companion') || ''), summary: String(data.get('summary') || ''), body: String(data.get('body') || '') });
  }} onSubmit={(event) => {
    event.preventDefault();
    if (reading || photoError) return;
    const data = new FormData(event.currentTarget);
    const photos = selectedPhotos ? selectedPhotos.map((photo) => photo.name) : course.photos || [];
    onSubmit?.({
      title: String(data.get('title') || '').trim(),
      category: String(data.get('category') || ''),
      companion: String(data.get('companion') || ''),
      summary: String(data.get('summary') || '').trim(),
      body: String(data.get('body') || '').trim(),
      photos,
      photoAssets: selectedPhotos || course.photoAssets || [],
    });
  }}>
    <fieldset disabled={disabled}>
      <legend>{editing ? '기존 코스 정보 수정' : '사진 / 후기 작성'}</legend>
      <div className="field"><label>게시글 제목 *<input name="title" type="text" maxLength={80} required defaultValue={course.title || ''} placeholder="예: 부산 맛집과 바다 여행 코스" /></label></div>
      <div className="field-row">
        <div className="field"><label>여행 유형 *<select name="category" required defaultValue={course.category || ''}><option value="">선택</option>{['맛집', '문화', '힐링', '액티비티'].map((value) => <option key={value}>{value}</option>)}</select></label></div>
        <div className="field"><label>동행 유형 *<select name="companion" required defaultValue={course.companion || ''}><option value="">선택</option>{['혼자', '친구', '연인', '가족'].map((value) => <option key={value}>{value}</option>)}</select></label></div>
      </div>
      <div className="field"><label>한 줄 소개 *<input name="summary" type="text" maxLength={150} required defaultValue={course.summary || ''} /></label></div>
      <div className="field"><label>여행 후기 *<textarea name="body" rows={6} maxLength={2000} required defaultValue={course.body || ''} /></label></div>
      {editing && <p className="note">기존 사진: {course.photos?.length ? course.photos.join(', ') : '없음'} · 새 사진을 선택하지 않으면 그대로 유지됩니다.</p>}
      <div className="field"><label>{editing ? '사진 변경 (선택 시 교체)' : '사진 첨부'}<input name="photos" type="file" accept="image/jpeg,image/png,image/webp,image/gif" multiple onChange={changePhotos} aria-describedby="photo-help" /></label></div>
      <p className="form-help" id="photo-help">사진은 최대 5개, 파일당 600KB, 전체 2.5MB까지 이 브라우저에 저장됩니다. 큰 사진은 크기를 줄인 뒤 선택해주세요.</p>
      {photoError && <p className="form-error" role="alert">{photoError}</p>}
      {reading && <p role="status">사진을 읽고 있습니다.</p>}
      <details className="course-preview" open><summary>게시글 미리보기</summary>
        <h3>{preview.title.trim() || '제목을 입력해주세요'}</h3>
        <p className="course-meta">{[preview.category, preview.companion].filter(Boolean).join(' · ') || '여행 유형 · 동행 유형'}</p>
        <p>{preview.summary.trim() || '한 줄 소개를 입력해주세요'}</p>
        <p className="writing-sample">{preview.body.trim() || '여행 후기를 입력해주세요'}</p>
        <ul className="photo-boxes">{visiblePhotos.length ? visiblePhotos.map((photo, index) => <li key={`${photo.name}-${index}`}>{photo.dataUrl ? <img src={photo.dataUrl} alt={`첨부 사진 미리보기: ${photo.name}`} /> : <span>{photo.name} (기존 파일명)</span>}</li>) : <li>첨부 사진 없음</li>}</ul>
      </details>
      <div className="actions"><button className="primary" type="submit" disabled={!onSubmit || reading || Boolean(photoError)}>{editing ? '수정 저장하기' : '코스 등록하기'}</button><button type="reset">입력 되돌리기</button>{cancelHref && <a href={cancelHref} className="button">{editing ? '수정 취소' : '취소'}</a>}</div>
    </fieldset>
  </form>;
}
