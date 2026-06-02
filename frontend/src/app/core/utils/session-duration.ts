export function parseDurationToHours(duration: string | null | undefined): number | null {
  if (!duration) return null;
  const match = duration.match(/^(\d{1,3}):(\d{2})h?$/);
  if (!match) return null;
  const hours = Number(match[1]);
  const minutes = Number(match[2]);
  return hours + minutes / 60;
}

export function perHour(value: number, duration: string | null | undefined): number | null {
  const hours = parseDurationToHours(duration);
  if (hours === null || hours === 0) return null;
  return value / hours;
}
