import { STAMINA_BONUS, STAMINA_LOW, STAMINA_MAX, clampStamina, xpZone } from './stamina-rules';

describe('stamina rules', () => {
  it('uses 42:00, 39:00 and 14:00 as the limits', () => {
    expect([STAMINA_MAX, STAMINA_BONUS, STAMINA_LOW]).toEqual([2520, 2340, 840]);
  });

  it('puts 14:00 in the normal zone and 39:00 in the bonus zone', () => {
    expect(xpZone(839)).toBe('low');
    expect(xpZone(840)).toBe('normal');
    expect(xpZone(2339)).toBe('normal');
    expect(xpZone(2340)).toBe('bonus');
  });

  it('clamps stamina to whole minutes between 00:00 and 42:00', () => {
    expect(clampStamina(-5)).toBe(0);
    expect(clampStamina(3000)).toBe(2520);
    expect(clampStamina(100.6)).toBe(101);
  });
});
