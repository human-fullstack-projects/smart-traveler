import lounge from './sample-photos/lounge.webp';
import twinRoom from './sample-photos/twin-room.webp';
import kitchen from './sample-photos/kitchen.webp';
import desk from './sample-photos/desk.webp';
import bathroom from './sample-photos/bathroom.webp';
import courtyard from './sample-photos/courtyard.webp';

const samplePhotos = [lounge, twinRoom, kitchen, desk, bathroom, courtyard]
  .map(imageUrl => ({ imageUrl }));

export function getStaySamplePhotos(stayId) {
  const offset = (Number(stayId) || 0) % samplePhotos.length;
  return [...samplePhotos.slice(offset), ...samplePhotos.slice(0, offset)];
}
