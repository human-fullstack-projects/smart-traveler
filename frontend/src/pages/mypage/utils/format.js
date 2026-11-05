export const money = (value) => `${Number(value).toLocaleString('ko-KR')}원`;

export function endDate(date, nights) {
  const value = new Date(`${date}T00:00:00Z`);
  value.setUTCDate(value.getUTCDate() + Number(nights));
  return value.toISOString().slice(0, 10);
}
