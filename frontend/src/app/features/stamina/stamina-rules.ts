/**
 * Tibia stamina rules shared by the stamina tools. Stamina values are minutes (42:00 = 2520).
 */

/** Full stamina, 42:00. */
export const STAMINA_MAX = 42 * 60;

/** 39:00 — from here up hunting earns +50% XP and offline regeneration slows down. */
export const STAMINA_BONUS = 39 * 60;

/** 14:00 — below this hunting earns −50% XP. */
export const STAMINA_LOW = 14 * 60;

/** Offline minutes that restore one minute of stamina below 39:00. */
export const REGEN_OFFLINE_MIN = 3;

/** Offline minutes that restore one minute of stamina from 39:00 to 42:00. */
export const BONUS_REGEN_OFFLINE_MIN = 6;

export type XpZone = 'low' | 'normal' | 'bonus';

/** XP zone a stamina value falls in: −50% below 14:00, +50% from 39:00. */
export function xpZone(staminaMin: number): XpZone {
  if (staminaMin < STAMINA_LOW) return 'low';
  if (staminaMin >= STAMINA_BONUS) return 'bonus';
  return 'normal';
}

/** Whole minutes between 00:00 and 42:00. */
export function clampStamina(min: number): number {
  return Math.min(STAMINA_MAX, Math.max(0, Math.round(min)));
}
