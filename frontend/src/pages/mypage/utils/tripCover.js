import palace from '../profile/avatars/palace.png';
import beach from '../profile/avatars/beach.png';
import village from '../profile/avatars/village.png';

export default function tripCover(region) {
  if (/부산|제주|울산|포항|여수|인천/.test(region)) return beach;
  return /서울|광주|대구/.test(region) ? palace : village;
}
