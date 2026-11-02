import {
  BONUS_REGEN_OFFLINE_MIN,
  REGEN_OFFLINE_MIN,
  STAMINA_BONUS,
  STAMINA_LOW,
  STAMINA_MAX,
  clampStamina,
  type XpZone,
} from './stamina-rules';

/** One stretch of the recovery that stays inside a single XP zone. */
export interface TargetSegment {
  zone: XpZone;
  /** Stamina (minutes) when the stretch starts and ends. */
  from: number;
  to: number;
  /** Offline minutes that restore one minute of stamina in this zone. */
  offlinePerMin: number;
  /** Offline minutes the stretch takes. */
  offlineMin: number;
  /** Offline minutes from the start until the stretch is done. */
  endsAtMin: number;
}

export interface TargetPlan {
  segments: TargetSegment[];
  totalOfflineMin: number;
}

const ZONE_RANGES: { zone: XpZone; from: number; to: number; offlinePerMin: number }[] = [
  { zone: 'low', from: 0, to: STAMINA_LOW, offlinePerMin: REGEN_OFFLINE_MIN },
  { zone: 'normal', from: STAMINA_LOW, to: STAMINA_BONUS, offlinePerMin: REGEN_OFFLINE_MIN },
  { zone: 'bonus', from: STAMINA_BONUS, to: STAMINA_MAX, offlinePerMin: BONUS_REGEN_OFFLINE_MIN },
];

/**
 * Offline time to go from `current` to `target` stamina (minutes, clamped to 00:00–42:00), split
 * by XP zone. Whole-minute math, so it has no floating-point drift. A target at or below
 * `current` needs no time and has no segments.
 */
export function planStaminaTarget(current: number, target: number): TargetPlan {
  const from = clampStamina(current);
  const to = clampStamina(target);
  const segments: TargetSegment[] = [];
  let total = 0;
  for (const range of ZONE_RANGES) {
    const start = Math.max(from, range.from);
    const end = Math.min(to, range.to);
    if (end <= start) continue;
    const offlineMin = (end - start) * range.offlinePerMin;
    total += offlineMin;
    segments.push({
      zone: range.zone,
      from: start,
      to: end,
      offlinePerMin: range.offlinePerMin,
      offlineMin,
      endsAtMin: total,
    });
  }
  return { segments, totalOfflineMin: total };
}
