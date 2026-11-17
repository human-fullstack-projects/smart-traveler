import { useState } from 'react';
import { chatApi } from '../../../chatApi';

export default function CourseRegionPicker({ busy, onSelect }) {
  const [open, setOpen] = useState(false);
  const [regions, setRegions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function toggle() {
    if (open) { setOpen(false); return; }
    setOpen(true); setLoading(true); setError('');
    try { setRegions(await chatApi.courseRegions()); }
    catch { setError('지역 목록을 불러오지 못했습니다. 다시 열어 주세요.'); }
    finally { setLoading(false); }
  }

  async function select(region) {
    setError('');
    try { await onSelect(region); setOpen(false); }
    catch { setError('지역을 적용하지 못했습니다. 다시 선택해 주세요.'); }
  }

  return <div className="cb-region-picker">
    <button type="button" className="cb-region-toggle" disabled={busy || loading} onClick={toggle} aria-expanded={open} aria-controls="course-region-list">관광지 찾아보기</button>
    {open && <div id="course-region-list" className="cb-region-list">
      <p className="cb-status">여행할 지역을 선택해 주세요.</p>
      {loading ? <p role="status">지역을 불러오는 중입니다.</p>
        : regions.length ? regions.map(region => <button type="button" key={region.regionId} disabled={busy} onClick={() => select(region)}>{region.fullName}</button>)
          : !error && <p>저장된 코스가 있는 지역이 없습니다.</p>}
    </div>}
    {error && <p className="cb-error" role="alert">{error}</p>}
  </div>;
}
