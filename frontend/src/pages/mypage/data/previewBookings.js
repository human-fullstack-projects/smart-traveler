// 목업 화면을 확인하기 위한 데이터입니다. 저장 기능은 이후 단계에서 연결합니다.
export const previewBookings = [{
  id: 'sample-completed', region: '부산광역시', date: '2026-10-10', nights: 1,
  guests: 2, companion: '친구', transport: '대중교통', stay: '부산 여행 스테이',
  room: '스탠다드', price: 110000, status: '숙박완료',
  versions: [{ name: '바다와 골목 여행', v: 1, note: '최초 코스', time: '2026-10-10',
    places: ['광안리 해변', '부산시립미술관', '전포 카페거리'] }],
  post: { title: '친구와 다녀온 부산 여행', body: '해변을 걷고 골목에서 식사하며 여유롭게 보낸 여행입니다.', photo: '', version: 1 },
}];
