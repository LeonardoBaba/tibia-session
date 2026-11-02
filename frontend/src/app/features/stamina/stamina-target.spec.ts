import { planStaminaTarget } from './stamina-target';

describe('planStaminaTarget', () => {
  it('splits 30:00 → 42:00 into the normal and bonus zones', () => {
    const plan = planStaminaTarget(30 * 60, 42 * 60);
    expect(plan.segments).toEqual([
      { zone: 'normal', from: 1800, to: 2340, offlinePerMin: 3, offlineMin: 1620, endsAtMin: 1620 },
      { zone: 'bonus', from: 2340, to: 2520, offlinePerMin: 6, offlineMin: 1080, endsAtMin: 2700 },
    ]);
    expect(plan.totalOfflineMin).toBe(2700);
  });

  it('recovers the −50% XP zone at the normal rate', () => {
    const plan = planStaminaTarget(10 * 60, 20 * 60);
    expect(plan.segments.map((s) => [s.zone, s.offlineMin])).toEqual([
      ['low', 720],
      ['normal', 1080],
    ]);
    expect(plan.totalOfflineMin).toBe(1800);
  });

  it('uses the slower rate inside the bonus zone', () => {
    expect(planStaminaTarget(40 * 60, 41 * 60).totalOfflineMin).toBe(360);
  });

  it('needs no time when the target is not above the current stamina', () => {
    expect(planStaminaTarget(30 * 60, 30 * 60)).toEqual({ segments: [], totalOfflineMin: 0 });
    expect(planStaminaTarget(30 * 60, 20 * 60)).toEqual({ segments: [], totalOfflineMin: 0 });
  });

  it('clamps values outside 00:00–42:00', () => {
    expect(planStaminaTarget(-30, 50 * 60).totalOfflineMin).toBe(8100);
  });
});
