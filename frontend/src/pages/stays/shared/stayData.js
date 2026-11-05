export const stayTypes = ['호텔', '리조트', '펜션', '게스트하우스'];

export const stays = [
  {
    id: 'haeundae-wave',
    name: '해운대 웨이브 호텔',
    type: '호텔',
    region: '부산',
    area: '해운대구',
    address: '부산 해운대구 달맞이길 42',
    phone: '051-000-1200',
    website: 'wave-hotel.example.com',
    description: '해운대 바다와 달맞이길을 함께 즐길 수 있는 도심형 호텔입니다.',
    rating: 4.8,
    reviewCount: 328,
    distanceKm: 0.8,
    price: 148000,
    source: '업주 등록',
    saleable: true,
    remaining: 3,
    checkIn: '15:00',
    checkOut: '11:00',
    parking: '투숙객 1대 무료',
    amenities: ['오션뷰', '주차', '조식', '와이파이', '짐 보관'],
    theme: 'ocean',
    marker: { left: 61, top: 32 },
    cancellation: '체크인 5일 전까지 무료 취소, 이후 날짜별 환불 비율 적용',
    rooms: [
      { id: 'wave-standard', name: '스탠다드 더블', basis: 2, maxGuests: 2, area: '24㎡', beds: '더블 1', price: 148000, remaining: 2, amenities: ['샤워실', '스마트 TV', '냉장고'] },
      { id: 'wave-ocean', name: '오션 디럭스 트윈', basis: 2, maxGuests: 4, area: '34㎡', beds: '더블 2', price: 196000, remaining: 1, amenities: ['오션뷰', '욕조', '미니바'] },
    ],
  },
  {
    id: 'gwangalli-morrow',
    name: '광안 모로우 스테이',
    type: '게스트하우스',
    region: '부산',
    area: '수영구',
    address: '부산 수영구 광안해변로 188',
    phone: '051-000-2200',
    website: 'morrow-stay.example.com',
    description: '광안리 해변 산책과 야경을 가까이에서 누리는 감각적인 스테이입니다.',
    rating: 4.6,
    reviewCount: 191,
    distanceKm: 1.7,
    price: 89000,
    source: '업주 등록',
    saleable: true,
    remaining: 5,
    checkIn: '16:00',
    checkOut: '11:00',
    parking: '인근 공영주차장 이용',
    amenities: ['해변 인접', '공용 주방', '와이파이', '짐 보관'],
    theme: 'night',
    marker: { left: 38, top: 51 },
    cancellation: '체크인 7일 전까지 무료 취소, 3일 전부터 취소 수수료 발생',
    rooms: [
      { id: 'morrow-double', name: '프라이빗 더블', basis: 2, maxGuests: 2, area: '18㎡', beds: '더블 1', price: 89000, remaining: 4, amenities: ['전용 욕실', '테이블', '냉장고'] },
      { id: 'morrow-family', name: '패밀리 로프트', basis: 3, maxGuests: 5, area: '31㎡', beds: '더블 2 · 싱글 1', price: 154000, remaining: 1, amenities: ['복층', '전용 욕실', '간이 주방'] },
    ],
  },
  {
    id: 'songjeong-slow',
    name: '송정 슬로우 하우스',
    type: '펜션',
    region: '부산',
    area: '해운대구',
    address: '부산 해운대구 송정광어골로 71',
    phone: '051-000-3300',
    website: 'slow-house.example.com',
    description: '조용한 송정 골목에서 넉넉한 객실과 작은 테라스를 이용할 수 있습니다.',
    rating: 4.7,
    reviewCount: 84,
    distanceKm: 3.2,
    price: 176000,
    source: '업주 등록',
    saleable: true,
    remaining: 1,
    checkIn: '15:00',
    checkOut: '11:00',
    parking: '객실당 1대 무료',
    amenities: ['테라스', '주차', '취사', '바비큐', '와이파이'],
    theme: 'forest',
    marker: { left: 73, top: 58 },
    cancellation: '체크인 7일 전까지 무료 취소, 이후 숙소 환불 규정 적용',
    rooms: [
      { id: 'slow-terrace', name: '테라스 패밀리', basis: 4, maxGuests: 6, area: '46㎡', beds: '더블 2', price: 176000, remaining: 1, amenities: ['개별 테라스', '주방', '바비큐'] },
    ],
  },
  {
    id: 'busan-archive',
    name: '부산 아카이브 호텔',
    type: '호텔',
    region: '부산',
    area: '중구',
    address: '부산 중구 중앙대로 25',
    phone: '051-000-4400',
    website: '',
    description: '관광정보에서 수집한 숙소 정보입니다. 현재 내부 판매 재고는 확인되지 않았습니다.',
    rating: 4.3,
    reviewCount: 52,
    distanceKm: 5.4,
    price: 112000,
    source: 'TourAPI 정보',
    saleable: false,
    remaining: 0,
    checkIn: '15:00',
    checkOut: '11:00',
    parking: '숙소 문의',
    amenities: ['와이파이', '엘리베이터'],
    theme: 'city',
    marker: { left: 22, top: 70 },
    cancellation: '내부 판매 정보가 확인되지 않았습니다.',
    rooms: [],
  },
];

export function formatWon(value) {
  return `${new Intl.NumberFormat('ko-KR').format(value)}원`;
}

export function toDateInputValue(date) {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
}

export function getDefaultStayDates() {
  const checkIn = new Date();
  checkIn.setDate(checkIn.getDate() + 7);
  const checkOut = new Date(checkIn);
  checkOut.setDate(checkOut.getDate() + 2);
  return { checkIn: toDateInputValue(checkIn), checkOut: toDateInputValue(checkOut) };
}

export function getNightCount(checkIn, checkOut) {
  const start = new Date(`${checkIn}T00:00:00`);
  const end = new Date(`${checkOut}T00:00:00`);
  const nights = Math.round((end - start) / 86400000);
  return Number.isFinite(nights) && nights > 0 ? nights : 0;
}
