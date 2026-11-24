function uniquePhotos(photos) {
  const seen = new Set();
  return photos.filter(photo => {
    const urls = [photo.imageUrl, photo.thumbnailUrl].filter(Boolean).map(url => url.replace(/^http:/i, 'https:'));
    const duplicate = urls.some(url => seen.has(url));
    urls.forEach(url => seen.add(url));
    return urls.length > 0 && !duplicate;
  });
}

const roomName = value => String(value || '').normalize('NFKC').toLowerCase().replace(/[^\p{L}\p{N}]/gu, '');
const tourPhotos = photos => (photos || []).map(photo => ({ ...photo, tour: true }));

export function buildStayGallery(stay, rooms, tour) {
  const local = stay?.photos?.length ? stay.photos : [{ imageUrl: stay?.photoUrl, thumbnailUrl: stay?.thumbnailUrl }];
  const stayPhotos = uniquePhotos([...local, ...tourPhotos(tour?.photos)]);
  const apiRooms = tour?.rooms || [];
  const roomGalleries = rooms.map(room => {
    const key = roomName(room.name);
    const matches = apiRooms.filter(item => key && roomName(item.name) === key);
    const matched = matches.length === 1 ? tourPhotos(matches[0].photos) : [];
    return { photos: uniquePhotos([...(room.photos || []), ...matched]) };
  });
  const missingRooms = roomGalleries.filter(gallery => !gallery.photos.length);
  const sharedPhotos = stayPhotos.length > 1 ? stayPhotos.slice(1) : stayPhotos;
  if (sharedPhotos.length) {
    missingRooms.forEach((gallery, index) => {
      const assigned = sharedPhotos.filter((_, photoIndex) => photoIndex % missingRooms.length === index);
      gallery.photos = assigned.length ? assigned.slice(0, 3) : [sharedPhotos[index % sharedPhotos.length]];
    });
  }
  const [cover, ...photos] = uniquePhotos([
    ...stayPhotos, ...(stay?.roomPhotos || []), ...rooms.flatMap(room => room.photos || []),
    ...apiRooms.flatMap(room => tourPhotos(room.photos)),
  ]);
  return { cover, photos, roomGalleries };
}
