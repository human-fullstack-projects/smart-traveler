export default function CourseForm({ course = {}, mode = 'create', onSubmit, cancelHref, disabled = false }) {
  const editing = mode === 'edit';
  return <form onSubmit={(event) => {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const files = Array.from(event.currentTarget.elements.photos.files || []);
    if (files.length > 5 || files.some((file) => !file.type.startsWith('image/'))) {
      event.currentTarget.elements.photos.setCustomValidity('사진은 이미지 파일을 최대 5개 선택해주세요.');
      event.currentTarget.elements.photos.reportValidity();
      return;
    }
    onSubmit?.({
      title: String(data.get('title') || '').trim(),
      category: String(data.get('category') || ''),
      companion: String(data.get('companion') || ''),
      summary: String(data.get('summary') || '').trim(),
      body: String(data.get('body') || '').trim(),
      photos: files.length ? files.slice(0, 5).map((file) => file.name) : course.photos || [],
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
      {editing && <div className="field"><strong>현재 사진 (파일명)</strong><p className="note">{course.photos?.length ? course.photos.join(', ') : '없음'}</p></div>}
      <div className="field"><label>{editing ? '사진 변경 (선택 시 교체)' : '사진 첨부 (파일명만 저장)'}<input name="photos" type="file" accept="image/*" multiple onChange={(event) => event.target.setCustomValidity('')} /></label></div>
      <p className="form-help">사진은 최대 5개의 파일명만 저장합니다. 실제 이미지는 업로드되지 않습니다.{editing && ' 새 파일을 선택하지 않으면 기존 파일명이 유지됩니다.'}</p>
      <div className="actions"><button className="primary" type="submit" disabled={!onSubmit}>{editing ? '수정 저장하기' : '코스 등록하기'}</button>{cancelHref && <a href={cancelHref} className="button">{editing ? '수정 취소' : '취소'}</a>}</div>
    </fieldset>
  </form>;
}
