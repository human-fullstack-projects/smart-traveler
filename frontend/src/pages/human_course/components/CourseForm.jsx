import { useEffect, useId, useRef, useState } from 'react';

const MAX_PHOTOS = 5;
const MAX_FILE_BYTES = 5 * 1024 * 1024;
const MAX_TOTAL_BYTES = 25 * 1024 * 1024;
const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
const IMAGE_EXTENSIONS = /\.(?:jpe?g|png|webp|gif)$/i;

function isImageFile(file) {
  return IMAGE_TYPES.includes(file.type.toLowerCase()) || IMAGE_EXTENSIONS.test(file.name);
}

function readImage(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve({ name: file.name, dataUrl: String(reader.result), size: file.size });
    reader.onerror = () => reject(new Error('사진을 읽지 못했습니다. 다시 선택해주세요.'));
    reader.readAsDataURL(file);
  });
}

function resizeReview(input) {
  input.style.height = 'auto';
  const borderHeight = input.offsetHeight - input.clientHeight;
  input.style.height = `${input.scrollHeight + borderHeight}px`;
}

export default function CourseForm({ course = {}, mode = 'create', onSubmit, onSaveDraft, cancelHref, disabled = false, serverMode = false, previewContext }) {
  const [submitError, setSubmitError] = useState('');
  const [pending, setPending] = useState(null);
  const dialog = useRef(null);
  const reviewInput = useRef(null);
  useEffect(() => { if (pending && !dialog.current?.open) dialog.current?.showModal(); }, [pending]);
  const editing = mode === 'edit';
  useEffect(() => {
    const input = reviewInput.current;
    if (!input) return;

    let width = input.clientWidth;
    resizeReview(input);
    const observer = new ResizeObserver(() => {
      if (input.clientWidth === width) return;
      width = input.clientWidth;
      resizeReview(input);
    });
    observer.observe(input);
    return () => observer.disconnect();
  }, [editing]);
  const [preview, setPreview] = useState(() => ({
    title: course.title || '', category: course.category || '', companion: course.companion || '',
    summary: course.summary || '', body: course.body || '',
  }));
  const [selectedPhotos, setSelectedPhotos] = useState(null);
  const photoInputId = useId();
  const [photoError, setPhotoError] = useState('');
  const [reading, setReading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const photoRequest = useRef(0);
  const visiblePhotos = selectedPhotos || course.photoAssets || (course.photos || []).map((name) => ({ name }));

  async function changePhotos(event) {
    const request = ++photoRequest.current;
    const input = event.currentTarget;
    const files = Array.from(input.files || []);
    const previousPhotos = editing ? [] : selectedPhotos || [];
    if (!editing) input.value = '';
    setPhotoError('');
    if (!files.length) {
      if (editing) setSelectedPhotos(null);
      setReading(false);
      return;
    }
    if (previousPhotos.length + files.length > MAX_PHOTOS || files.some((file) => !isImageFile(file))
        || files.some((file) => file.size > MAX_FILE_BYTES)
        || [...previousPhotos, ...files].reduce((total, photo) => total + photo.size, 0) > MAX_TOTAL_BYTES) {
      if (editing) setSelectedPhotos(null);
      setReading(false);
      input.value = '';
      setPhotoError('JPG·PNG·WebP·GIF 사진을 최대 5개 선택해주세요. 파일당 5MB, 전체 25MB까지 첨부할 수 있습니다.');
      return;
    }
    setReading(true);
    try {
      const photos = await Promise.all(files.map(readImage));
      if (request === photoRequest.current) setSelectedPhotos([...previousPhotos, ...photos]);
    } catch (error) {
      if (request === photoRequest.current) {
        if (editing) setSelectedPhotos(null);
        input.value = '';
        setPhotoError(error.message);
      }
    } finally { if (request === photoRequest.current) setReading(false); }
  }

  function removePhoto(index) {
    setSelectedPhotos(photos => photos.filter((_, photoIndex) => photoIndex !== index));
    setPhotoError('');
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
  }} onSubmit={async (event) => {
    event.preventDefault();
    if (reading || (editing && photoError) || submitting) return;
    const data = new FormData(event.currentTarget);
    const body = String(data.get('body') || '').trim();
    const photos = selectedPhotos ? selectedPhotos.map((photo) => photo.name) : course.photos || [];
    setSubmitError('');
    setPending({
      title: String(data.get('title') || '').trim(),
      category: String(data.get('category') || ''),
      companion: String(data.get('companion') || ''),
      summary: editing ? String(data.get('summary') || '').trim() : body.replace(/\s+/g, ' ').slice(0, 150).trim(),
      body,
      photos,
      photoAssets: selectedPhotos?.map(({ name, dataUrl }) => ({ name, dataUrl })) || course.photoAssets || [],
      replacePhotos: Boolean(selectedPhotos),
    });
  }}>
    <fieldset disabled={disabled}>
      {editing && <legend>기존 코스 정보 수정</legend>}
      <div className="field"><label>게시글 제목 *<input name="title" type="text" maxLength={80} required defaultValue={course.title || ''} placeholder="예: 부산 맛집과 바다 여행 코스" /></label></div>
      <div className="field-row">
        <div className="field"><label>여행 유형 *<select name="category" required defaultValue={course.category || ''}><option value="">선택</option>{['맛집', '문화', '힐링', '액티비티'].map((value) => <option key={value}>{value}</option>)}</select></label></div>
        <div className="field"><label>동행 유형 *<select name="companion" required defaultValue={course.companion || ''}><option value="">선택</option>{['혼자', '친구', '연인', '가족'].map((value) => <option key={value}>{value}</option>)}</select></label></div>
      </div>
      {editing && <div className="field"><label>한 줄 소개 *<input name="summary" type="text" maxLength={150} required defaultValue={course.summary || ''} /></label></div>}
      <div className="field"><label>여행 후기 *<textarea name="body" rows={editing ? 6 : 20} maxLength={2000} required defaultValue={course.body || ''}
        ref={editing ? null : reviewInput} className={editing ? undefined : 'review-input'}
        onInput={editing ? undefined : event => resizeReview(event.currentTarget)} /></label></div>
      {editing && <p className="note">기존 사진: {course.photos?.length ? course.photos.join(', ') : '없음'} · 새 사진을 선택하지 않으면 그대로 유지됩니다.</p>}
      <div className="field">
        <label htmlFor={photoInputId}>{editing ? '사진 변경 (선택 시 교체)' : '사진 첨부'}</label>
        <div className="photo-attachment">
          <input id={photoInputId} name="photos" type="file" className={editing ? undefined : 'attachment-file-input'}
            accept="image/jpeg,image/png,image/webp,image/gif" multiple disabled={!editing && reading} onChange={changePhotos} aria-describedby="photo-help" />
          {!editing && <>
            <label className="button attachment-file-button" htmlFor={photoInputId} aria-hidden="true">파일 선택</label>
            <span className="attachment-file-text">{selectedPhotos?.length > 0
              ? (selectedPhotos.length === 1 ? selectedPhotos[0].name : `파일 ${selectedPhotos.length}개`)
              : '선택한 파일 없음'}</span>
          </>}
          {!editing && selectedPhotos?.length > 0 && <div className="attachment-thumbnails">
            {selectedPhotos.map((photo, index) => <div className="attachment-thumbnail" key={index}>
              <img src={photo.dataUrl} alt={`첨부 사진 ${index + 1}: ${photo.name}`} />
              <button type="button" className="attachment-remove" disabled={reading || submitting}
                aria-label={`첨부 사진 ${index + 1} 제거: ${photo.name}`} onClick={() => removePhoto(index)}>×</button>
            </div>)}
          </div>}
        </div>
      </div>
      <p className="form-help" id="photo-help">JPG·PNG·WebP·GIF 사진은 최대 5개, 파일당 5MB, 전체 25MB까지 {serverMode ? '서버에 저장됩니다.' : '선택할 수 있습니다. 브라우저 저장 공간에 따라 저장이 제한될 수 있습니다.'}</p>
      {photoError && <p className="form-error" role="alert">{photoError}</p>}
      {reading && <p role="status">사진을 읽고 있습니다.</p>}
      {submitting && <p role="status">게시글을 저장하고 있습니다.</p>}
      <dialog ref={dialog} className="recommendation-preview" aria-labelledby="recommendation-preview-title" onCancel={(event) => { if (submitting) event.preventDefault(); else setPending(null); }}><h2 id="recommendation-preview-title">게시글 미리보기</h2>
        <h3>{preview.title.trim() || '제목을 입력해주세요'}</h3>
        <p className="course-meta">{[preview.category, preview.companion].filter(Boolean).join(' · ') || '여행 유형 · 동행 유형'}</p>
        {editing && <p>{preview.summary.trim() || '한 줄 소개를 입력해주세요'}</p>}
        <ul className="photo-boxes">{visiblePhotos.length ? visiblePhotos.map((photo, index) => <li key={`${photo.name}-${index}`}>{photo.dataUrl ? <img src={photo.dataUrl} alt={`첨부 사진 미리보기: ${photo.name}`} /> : <span>{photo.name} (기존 파일명)</span>}</li>) : <li>첨부 사진 없음</li>}</ul>
        <p className="writing-sample">{preview.body.trim() || '여행 후기를 입력해주세요'}</p>
        {pending && previewContext}
        {submitError && <p role="alert">{submitError}</p>}
        <div className="actions recommendation-preview-actions"><button type="button" disabled={submitting} onClick={() => { dialog.current?.close(); setPending(null); }}>수정</button><button className="primary" type="button" disabled={submitting} onClick={async () => { setSubmitting(true); try { const result = await onSubmit?.(pending); if (result?.error) setSubmitError(result.error); else { dialog.current?.close(); setPending(null); } } finally { setSubmitting(false); } }}>{submitting ? '등록 중…' : editing ? '수정 저장' : '등록'}</button></div>
      </dialog>
      <div className="actions course-form-actions"><button className="primary" type="submit" disabled={!onSubmit || reading || submitting || (editing && Boolean(photoError))}>{editing ? '수정 저장하기' : '등록'}</button>
        {onSaveDraft && <button type="button" disabled={submitting} onClick={() => onSaveDraft(preview)}>임시저장</button>}
        {cancelHref && <a href={cancelHref} className="button">목록으로</a>}</div>
    </fieldset>
  </form>;
}
