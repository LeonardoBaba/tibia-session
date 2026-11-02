import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
  type WritableSignal,
} from '@angular/core';
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
} from '../../shared/utils/time-format';
import { StaminaTabs } from './components/stamina-tabs/stamina-tabs';
import {
  STAMINA_BONUS,
  STAMINA_LOW,
  STAMINA_MAX,
  clampStamina,
  xpZone,
  type XpZone,
} from './stamina-rules';
import { planStaminaTarget } from './stamina-target';

interface ZoneStyle {
  /** Row name in the breakdown. */
  name: string;
  /** Chip label. */
  label: string;
  color: string;
  bg: string;
}

/** XP zone colors, matching the Recovery simulator's timeline. */
const ZONE: Record<XpZone, ZoneStyle> = {
  low: {
    name: '−50% XP zone',
    label: '−50% XP',
    color: 'var(--color-loss)',
    bg: 'rgba(248,113,113,0.12)',
  },
  normal: {
    name: 'Normal zone',
    label: 'Normal',
    color: 'var(--color-text-secondary)',
    bg: 'rgba(160,160,168,0.10)',
  },
  bonus: {
    name: '+50% XP bonus',
    label: '+50% XP',
    color: 'var(--color-profit)',
    bg: 'rgba(34,197,94,0.12)',
  },
};

interface StaminaChip {
  value: number;
  label: string;
  note?: string;
  active: boolean;
}

interface SegmentRow {
  zone: XpZone;
  name: string;
  color: string;
  range: string;
  rate: string;
  duration: string;
  reachedAt: string;
  offlineMin: number;
}

type TargetStatus = 'ok' | 'reached' | 'below';

/** How often the clock is re-read while the start time follows it. */
const NOW_TICK_MS = 15_000;

/**
 * Stamina Target Calculator — when offline regeneration takes the current stamina to a desired
 * value, starting now or at a chosen logout time. Same rates as the Recovery simulator.
 */
@Component({
  selector: 'app-stamina-target-page',
  standalone: true,
  imports: [StaminaTabs],
  templateUrl: './stamina-target.page.html',
  styleUrl: './stamina-target.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StaminaTargetPage {
  private readonly clock24h = true;

  // --- state ---
  protected readonly currentMin = signal(30 * 60);
  protected readonly targetMin = signal(STAMINA_MAX);
  /** Start time the user picked; null while it follows the current time. */
  protected readonly customStartMs = signal<number | null>(null);
  protected readonly breakdownOpen = signal(false); // starts collapsed
  private readonly nowMs = signal(Date.now());

  constructor() {
    const timer = setInterval(() => this.nowMs.set(Date.now()), NOW_TICK_MS);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  // --- derived view state ---
  protected readonly followingNow = computed(() => this.customStartMs() === null);
  protected readonly startMs = computed(() => this.customStartMs() ?? roundToMinute(this.nowMs()));
  protected readonly startLocal = computed(() => toLocalInput(this.startMs()));
  protected readonly currentHHMM = computed(() => hhmm(this.currentMin()));
  protected readonly targetHHMM = computed(() => hhmm(this.targetMin()));

  protected readonly status = computed<TargetStatus>(() => {
    const diff = this.targetMin() - this.currentMin();
    return diff > 0 ? 'ok' : diff === 0 ? 'reached' : 'below';
  });

  private readonly plan = computed(() => planStaminaTarget(this.currentMin(), this.targetMin()));

  protected readonly startStatus = computed(() => {
    const start = this.startMs();
    if (this.followingNow()) return 'Live — follows the current time · ' + utcOffsetLabel(start);
    const rel = this.relative(start - this.nowMs());
    return rel.charAt(0).toUpperCase() + rel.slice(1) + ' · ' + utcOffsetLabel(start);
  });

  protected readonly result = computed(() => {
    const plan = this.plan();
    const now = this.nowMs();
    const etaMs = this.startMs() + plan.totalOfflineMin * 60000;
    const gain = Math.max(0, this.targetMin() - this.currentMin());
    return {
      time: timeLabel(etaMs, this.clock24h),
      day: dayLabel(etaMs, now),
      full: clockLabel(etaMs, this.clock24h),
      offline: durationLabel(plan.totalOfflineMin),
      gain: hhmm(gain),
      fromNow: this.followingNow() ? null : this.relative(etaMs - now),
      zone: ZONE[xpZone(this.targetMin())],
      currentPct: this.pct(this.currentMin()),
      gapPct: this.pct(gain),
    };
  });

  protected readonly segmentRows = computed<SegmentRow[]>(() => {
    const start = this.startMs();
    return this.plan().segments.map((s) => ({
      zone: s.zone,
      name: ZONE[s.zone].name,
      color: ZONE[s.zone].color,
      range: hhmm(s.from) + ' → ' + hhmm(s.to),
      rate: '+1 min / ' + s.offlinePerMin + ' min',
      duration: durationLabel(s.offlineMin),
      reachedAt: clockLabel(start + s.endsAtMin * 60000, this.clock24h),
      offlineMin: s.offlineMin,
    }));
  });

  protected readonly currentChips = computed<StaminaChip[]>(() =>
    [STAMINA_LOW, 30 * 60, STAMINA_BONUS].map((value) => ({
      value,
      label: hhmm(value),
      active: this.currentMin() === value,
    })),
  );

  protected readonly targetChips = computed<StaminaChip[]>(() =>
    [
      { value: STAMINA_LOW, note: 'no penalty' },
      { value: STAMINA_BONUS, note: 'bonus' },
      { value: STAMINA_MAX, note: 'full' },
    ].map((c) => ({ ...c, label: hhmm(c.value), active: this.targetMin() === c.value })),
  );

  protected readonly nowButtonClass = computed(() => {
    const base =
      'inline-flex min-h-11 cursor-pointer items-center gap-2 rounded-lg border px-3.5 text-xs font-semibold transition';
    return this.followingNow()
      ? `${base} border-[color:var(--color-accent)] bg-[color:var(--color-accent-soft)] text-[color:var(--color-accent)]`
      : `${base} border-[color:var(--color-border-strong)] bg-transparent text-[color:var(--color-text-secondary)] hover:border-[color:var(--color-accent)] hover:text-[color:var(--color-accent)]`;
  });

  /** Tailwind classes for a quick-pick chip in its active / inactive state. */
  protected chipClass(active: boolean): string {
    const base =
      'min-h-11 cursor-pointer rounded-md border px-3 text-xs font-semibold transition sm:min-h-8';
    return active
      ? `${base} border-[color:var(--color-accent)] bg-[color:var(--color-accent-soft)] text-[color:var(--color-accent)]`
      : `${base} border-[color:var(--color-border-strong)] bg-transparent text-[color:var(--color-text-secondary)] hover:border-[color:var(--color-accent)] hover:text-[color:var(--color-accent)]`;
  }

  private pct(min: number): string {
    return ((min / STAMINA_MAX) * 100).toFixed(2) + '%';
  }

  /** "in 2h 0m", "2h 0m ago" or "right now". */
  private relative(diffMs: number): string {
    const min = Math.round(diffMs / 60000);
    if (min === 0) return 'right now';
    return min < 0 ? durationLabel(-min) + ' ago' : 'in ' + durationLabel(min);
  }

  // --- stamina inputs ---
  protected onCurrentText(e: Event): void {
    this.applyStaminaText(e, this.currentMin);
  }

  protected onTargetText(e: Event): void {
    this.applyStaminaText(e, this.targetMin);
  }

  private applyStaminaText(e: Event, field: WritableSignal<number>): void {
    const input = e.target as HTMLInputElement;
    const min = parseHHMM(input.value);
    if (min !== null) field.set(clampStamina(min));
    // Re-sync the box: the signal may not change (same value, or text that didn't parse).
    input.value = hhmm(field());
  }

  protected onCurrentSlider(e: Event): void {
    this.currentMin.set(clampStamina(parseInt((e.target as HTMLInputElement).value, 10) || 0));
  }

  protected onTargetSlider(e: Event): void {
    this.targetMin.set(clampStamina(parseInt((e.target as HTMLInputElement).value, 10) || 0));
  }

  protected setCurrent(min: number): void {
    this.currentMin.set(min);
  }

  protected setTarget(min: number): void {
    this.targetMin.set(min);
  }

  // --- start time ---
  protected onStartChange(e: Event): void {
    const input = e.target as HTMLInputElement;
    const ms = new Date(input.value).getTime();
    if (isNaN(ms)) {
      input.value = this.startLocal();
      return;
    }
    this.customStartMs.set(ms);
  }

  protected useNow(): void {
    this.customStartMs.set(null);
    this.nowMs.set(Date.now());
  }

  protected toggleBreakdown(): void {
    this.breakdownOpen.update((open) => !open);
  }
}
