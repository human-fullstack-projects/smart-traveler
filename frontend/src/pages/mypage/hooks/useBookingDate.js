import { useEffect, useState } from 'react';

const bookingDate = () => new Date().toLocaleDateString('sv-SE', { timeZone: 'Asia/Seoul' });

export default function useBookingDate() {
  const [today, setToday] = useState(bookingDate);
  useEffect(() => {
    const update = () => setToday(bookingDate());
    const timer = window.setInterval(update, 30_000);
    window.addEventListener('focus', update);
    return () => {
      window.clearInterval(timer);
      window.removeEventListener('focus', update);
    };
  }, []);
  return today;
}
