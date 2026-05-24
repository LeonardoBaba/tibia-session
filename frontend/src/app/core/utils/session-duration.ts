/**
 * Converte uma string no formato "HH:MMh" (ex.: "02:11h") em horas decimais.
 * Retorna `null` se a string for inválida ou nula.
 */
export function parseDurationToHours(duration: string | null | undefined): number | null {
  if (!duration) return null;
  const match = duration.match(/^(\d{1,3}):(\d{2})h?$/);
  if (!match) return null;
  const hours = Number(match[1]);
  const minutes = Number(match[2]);
  return hours + minutes / 60;
}

/**
 * Calcula "por hora" — útil pra balance/h, xp/h, etc.
 * Retorna `null` se a duração for inválida ou zero.
 */
export function perHour(value: number, duration: string | null | undefined): number | null {
  const hours = parseDurationToHours(duration);
  if (hours === null || hours === 0) return null;
  return value / hours;
}
