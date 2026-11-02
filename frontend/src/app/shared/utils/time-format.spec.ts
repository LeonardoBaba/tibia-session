import {
  clockLabel,
  dayLabel,
  durationLabel,
  hhmm,
  parseHHMM,
  roundToMinute,
  timeLabel,
  toLocalInput,
  utcOffsetLabel,
} from './time-format';

describe('time-format', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('formats minutes as hh:mm', () => {
    expect(hhmm(2520)).toBe('42:00');
    expect(hhmm(65)).toBe('01:05');
    expect(hhmm(0)).toBe('00:00');
    expect(hhmm(-5)).toBe('00:00');
  });

  it('parses hh:mm and its shorthands', () => {
    expect(parseHHMM('25:30')).toBe(1530);
    expect(parseHHMM('25h30')).toBe(1530);
    expect(parseHHMM(' 7:5 ')).toBe(425);
    expect(parseHHMM('38')).toBe(2280);
    expect(parseHHMM('abc')).toBeNull();
    expect(parseHHMM('')).toBeNull();
    expect(parseHHMM('123:00')).toBeNull();
  });

  it('formats durations with days, hours and minutes', () => {
    expect(durationLabel(2700)).toBe('1d 21h 0m');
    expect(durationLabel(45)).toBe('45m');
    expect(durationLabel(60)).toBe('1h 0m');
    expect(durationLabel(1440)).toBe('1d 0m');
  });

  it('rounds instants down to the minute', () => {
    const ms = new Date(2026, 10, 2, 10, 0, 59, 999).getTime();
    expect(roundToMinute(ms)).toBe(new Date(2026, 10, 2, 10, 0).getTime());
  });

  it('formats datetime-local input values in local time', () => {
    expect(toLocalInput(new Date(2026, 0, 5, 7, 3).getTime())).toBe('2026-01-05T07:03');
  });

  it('formats clock and time labels in 24h or 12h', () => {
    const ms = new Date(2026, 10, 3, 0, 5).getTime();
    expect(clockLabel(ms)).toBe('Tue, Nov 03, 00:05');
    expect(clockLabel(ms, false)).toBe('Tue, Nov 03, 12:05 AM');
    expect(timeLabel(ms)).toBe('00:05');
    expect(timeLabel(ms, false)).toBe('12:05 AM');
  });

  it('names the day relative to now', () => {
    const now = new Date(2026, 10, 2, 10, 0).getTime();
    expect(dayLabel(new Date(2026, 10, 2, 23, 0).getTime(), now)).toBe('Today, Nov 02');
    expect(dayLabel(new Date(2026, 10, 3, 1, 0).getTime(), now)).toBe('Tomorrow, Nov 03');
    expect(dayLabel(new Date(2026, 10, 1, 9, 0).getTime(), now)).toBe('Yesterday, Nov 01');
    expect(dayLabel(new Date(2026, 10, 5, 9, 0).getTime(), now)).toBe('Thursday, Nov 05');
  });

  it('labels the UTC offset with its sign', () => {
    const offset = vi.spyOn(Date.prototype, 'getTimezoneOffset');
    offset.mockReturnValue(180);
    expect(utcOffsetLabel(0)).toBe('UTC−03:00');
    offset.mockReturnValue(-330);
    expect(utcOffsetLabel(0)).toBe('UTC+05:30');
    offset.mockReturnValue(0);
    expect(utcOffsetLabel(0)).toBe('UTC+00:00');
  });
});
