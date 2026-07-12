import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';

interface Hunt {
  id: string;
  startMs: number;
  durMin: number;
}

interface Extension {
  id: string;
  ms: number;
}

interface Filters {
  gains: boolean;
  hunts: boolean;
  exts: boolean;
  bonusOnly: boolean;
  offline: boolean;
}

type SimEventType = 'start' | 'huntStart' | 'huntEnd' | 'ext' | 'gain';

interface SimEvent {
  type: SimEventType;
  t: number;
  stamina: number;
  dur?: number;
  bonusMin?: number;
  added?: number;
  hour?: number;
}

interface SimResult {
  events: SimEvent[];
  fullT: number;
  finalStamina: number;
  refMs: number;
}

interface TimelineRow {
  kind: SimEventType;
  sMin: number;
  tag: string;
  tagColor: string;
  timeLabel: string;
  timeColor: string;
  timeWeight: string;
  offlineLabel: string;
  staminaLabel: string;
  staminaColor: string;
  zoneLabel: string;
  zoneColor: string;
  zoneBg: string;
  rowBg: string;
}

interface HuntRow {
  id: string;
  index: number;
  startLocal: string;
  hours: number;
  minutes: number;
  consumeLabel: string;
  endLabel: string;
  bonusText: string;
  bonusColor: string;
}

interface ExtRow {
  id: string;
  index: number;
  startLocal: string;
  tooSoon: boolean;
}

const MAX = 2520; // 42:00
const BONUS = 39 * 60; // 2340 — regen slows and the +50% XP zone begins here
const LOW = 14 * 60; // 840 — below this stamina is the −50% XP penalty zone

/** Semantic colors as theme variables so the timeline follows the app palette. */
const COLOR = {
  primary: 'var(--color-text-primary)',
  secondary: 'var(--color-text-secondary)',
  muted: 'var(--color-text-muted)',
  accent: 'var(--color-accent)',
  profit: 'var(--color-profit)',
  loss: 'var(--color-loss)',
};

const ZONE = {
  low: { label: '−50% XP', color: COLOR.loss, bg: 'rgba(248,113,113,0.12)' },
  bonus: { label: '+50% XP', color: COLOR.profit, bg: 'rgba(34,197,94,0.12)' },
  normal: { label: 'Normal', color: COLOR.secondary, bg: 'rgba(160,160,168,0.10)' },
};

/**
 * Stamina Recovery Simulator — models Tibia's offline stamina regeneration around
 * planned hunts and stamina-extension uses, producing an hour-by-hour timeline.
 */
@Component({
  selector: 'app-stamina-page',
  standalone: true,
  templateUrl: './stamina.page.html',
  styleUrl: './stamina.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StaminaPage {
  private readonly clock24h = true;

  // --- state ---
  protected readonly staminaMin = signal(MAX); // 42:00 — opens full so the screen starts "clean"
  protected readonly startMs = signal(this.roundMin(Date.now()));
  protected readonly hunts = signal<Hunt[]>([]);
  protected readonly extensions = signal<Extension[]>([]);
  protected readonly filters = signal<Filters>({
    gains: true,
    hunts: true,
    exts: true,
    bonusOnly: false,
    offline: true,
  });
  private huntSeq = 1;
  private extSeq = 1;

  // --- formatting helpers ---
  private roundMin(ms: number): number {
    return Math.floor(ms / 60000) * 60000;
  }

  private pad(n: number): string {
    return String(n).padStart(2, '0');
  }

  private hhmm(min: number): string {
    const m = Math.max(0, Math.floor(min + 1e-9));
    return this.pad(Math.floor(m / 60)) + ':' + this.pad(m % 60);
  }

  private toLocalInput(ms: number): string {
    const d = new Date(ms);
    return (
      d.getFullYear() +
      '-' +
      this.pad(d.getMonth() + 1) +
      '-' +
      this.pad(d.getDate()) +
      'T' +
      this.pad(d.getHours()) +
      ':' +
      this.pad(d.getMinutes())
    );
  }

  private clockLabel(ms: number): string {
    return new Date(ms).toLocaleString('en-US', {
      weekday: 'short',
      day: '2-digit',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit',
      hour12: !this.clock24h,
    });
  }

  private durLabel(mins: number): string {
    const m = Math.round(mins);
    const d = Math.floor(m / 1440);
    const h = Math.floor((m % 1440) / 60);
    const mm = m % 60;
    const out: string[] = [];
    if (d) out.push(d + 'd');
    if (h) out.push(h + 'h');
    out.push(mm + 'm');
    return out.join(' ');
  }

  private zoneFor(staminaMin: number): { label: string; color: string; bg: string } {
    if (staminaMin < LOW) return ZONE.low;
    if (staminaMin >= BONUS) return ZONE.bonus;
    return ZONE.normal;
  }

  /** Minutes of a hunt (which drains 1:1) spent in the +50% XP bonus zone. */
  private huntBonusMin(stamina: number, dur: number): number {
    return Math.max(0, Math.min(dur, stamina - BONUS));
  }

  // --- core simulation (minute-by-minute) ---
  private readonly sim = computed<SimResult>(() => {
    const refMs = this.startMs();
    let stamina = Math.min(MAX, Math.max(0, this.staminaMin()));

    const hunts = this.hunts()
      .map((h) => ({
        start: Math.round((h.startMs - refMs) / 60000),
        dur: Math.max(0, Math.round(h.durMin)),
      }))
      .filter((h) => h.dur > 0)
      .map((h) => ({ start: h.start, end: h.start + h.dur, dur: h.dur }))
      .sort((a, b) => a.start - b.start);
    const lastHuntEnd = hunts.reduce((m, h) => Math.max(m, h.end), 0);

    const exts = this.extensions()
      .map((e) => Math.round((e.ms - refMs) / 60000))
      .filter((t) => t >= 0)
      .sort((a, b) => a - b);
    const extAt = new Map<number, number>();
    exts.forEach((t) => extAt.set(t, (extAt.get(t) ?? 0) + 1));
    const lastExtT = exts.length ? exts[exts.length - 1] : 0;

    const events: SimEvent[] = [];
    let lastWhole = Math.floor(stamina / 60 + 1e-9);
    events.push({ type: 'start', t: 0, stamina });

    const HORIZON = 60 * 24 * 21; // 21-day safety cap
    const startedSet = new Set<number>();
    const endedSet = new Set<number>();
    let t = 0;

    while (t < HORIZON) {
      // Already full with nothing left to process — stop at t=0 rather than running a
      // spurious extra minute, so "total offline needed" reads 0 when starting at 42:00.
      if (stamina >= MAX && t >= lastHuntEnd && t >= lastExtT) break;

      hunts.forEach((h, i) => {
        if (t === h.start && !startedSet.has(i)) {
          startedSet.add(i);
          events.push({
            type: 'huntStart',
            t,
            stamina,
            dur: h.dur,
            bonusMin: this.huntBonusMin(stamina, h.dur),
          });
        }
      });

      const inHunt = hunts.some((h) => t >= h.start && t < h.end);
      if (inHunt) {
        stamina = Math.max(0, stamina - 1);
      } else if (stamina < MAX) {
        stamina = Math.min(MAX, stamina + (stamina < BONUS ? 1 / 3 : 1 / 6));
      }
      t += 1;

      if (extAt.has(t)) {
        const before = stamina;
        stamina = Math.min(MAX, stamina + 60 * (extAt.get(t) ?? 0));
        events.push({ type: 'ext', t, stamina, added: stamina - before });
        lastWhole = Math.floor(stamina / 60 + 1e-9);
      }

      hunts.forEach((h, i) => {
        if (t === h.end && !endedSet.has(i)) {
          endedSet.add(i);
          events.push({ type: 'huntEnd', t, stamina });
          lastWhole = Math.floor(stamina / 60 + 1e-9);
        }
      });

      if (inHunt) {
        lastWhole = Math.floor(stamina / 60 + 1e-9);
      } else {
        const wh = Math.floor(stamina / 60 + 1e-9);
        if (wh > lastWhole) {
          events.push({ type: 'gain', t, stamina: wh * 60, hour: wh });
          lastWhole = wh;
        }
      }

      if (stamina >= MAX && t >= lastHuntEnd && t >= lastExtT) break;
    }

    return { events, fullT: t, finalStamina: stamina, refMs };
  });

  // --- derived view state ---
  protected readonly staminaHHMM = computed(() => this.hhmm(this.staminaMin()));
  protected readonly startLocal = computed(() => this.toLocalInput(this.startMs()));
  protected readonly progressPct = computed(() => Math.round((this.staminaMin() / MAX) * 100) + '%');
  protected readonly noHunts = computed(() => this.hunts().length === 0);
  protected readonly noExts = computed(() => this.extensions().length === 0);
  protected readonly showOffline = computed(() => this.filters().offline);

  protected readonly summary = computed(() => {
    const sim = this.sim();
    const full = sim.finalStamina >= MAX;
    return {
      fullRecoveryLabel: full
        ? this.clockLabel(sim.refMs + sim.fullT * 60000)
        : 'never (hunts drain it)',
      fullRecoveryRelative: full
        ? 'in ' + this.durLabel(sim.fullT) + ' of offline time from start'
        : 'add more offline time between hunts',
      totalOfflineLabel: full ? this.durLabel(sim.fullT) : '—',
    };
  });

  protected readonly rows = computed<TimelineRow[]>(() => {
    const sim = this.sim();
    const f = this.filters();

    const bonusLabel = (bonusMin: number, dur: number): string => {
      if (bonusMin <= 0) return 'no bonus stamina';
      if (bonusMin >= dur - 0.5) return 'entire hunt at +50% XP';
      return this.hhmm(bonusMin) + ' of ' + this.hhmm(dur) + ' at +50% XP';
    };

    const allRows = sim.events.map((ev): TimelineRow => {
      const zone = this.zoneFor(ev.stamina);
      if (ev.type === 'start') {
        return {
          kind: 'start',
          sMin: ev.stamina,
          tag: 'NOW',
          tagColor: COLOR.accent,
          timeLabel: this.clockLabel(sim.refMs + ev.t * 60000),
          timeColor: COLOR.primary,
          timeWeight: '600',
          offlineLabel: '—',
          staminaLabel: this.hhmm(ev.stamina),
          staminaColor: COLOR.primary,
          zoneLabel: zone.label,
          zoneColor: zone.color,
          zoneBg: zone.bg,
          rowBg: 'rgba(245,166,35,0.05)',
        };
      }
      if (ev.type === 'huntStart') {
        const bonusMin = ev.bonusMin ?? 0;
        return {
          kind: 'huntStart',
          sMin: ev.stamina,
          tag: 'HUNT',
          tagColor: COLOR.accent,
          timeLabel: 'Hunt starts · ' + bonusLabel(bonusMin, ev.dur ?? 0),
          timeColor: COLOR.accent,
          timeWeight: '600',
          offlineLabel: this.durLabel(ev.t),
          staminaLabel: this.hhmm(ev.stamina),
          staminaColor: COLOR.secondary,
          zoneLabel: bonusMin > 0 ? '+50% XP' : 'online',
          zoneColor: bonusMin > 0 ? COLOR.profit : COLOR.accent,
          zoneBg: bonusMin > 0 ? 'rgba(34,197,94,0.12)' : 'rgba(245,166,35,0.12)',
          rowBg: 'rgba(245,166,35,0.07)',
        };
      }
      if (ev.type === 'huntEnd') {
        return {
          kind: 'huntEnd',
          sMin: ev.stamina,
          tag: 'HUNT',
          tagColor: COLOR.loss,
          timeLabel: 'Hunt ends',
          timeColor: COLOR.loss,
          timeWeight: '600',
          offlineLabel: this.durLabel(ev.t),
          staminaLabel: this.hhmm(ev.stamina),
          staminaColor: COLOR.loss,
          zoneLabel: zone.label,
          zoneColor: zone.color,
          zoneBg: zone.bg,
          rowBg: 'rgba(248,113,113,0.06)',
        };
      }
      if (ev.type === 'ext') {
        return {
          kind: 'ext',
          sMin: ev.stamina,
          tag: 'EXT',
          tagColor: COLOR.profit,
          timeLabel: 'Stamina Extension used (+' + this.hhmm(ev.added ?? 0) + ')',
          timeColor: COLOR.profit,
          timeWeight: '600',
          offlineLabel: this.durLabel(ev.t),
          staminaLabel: this.hhmm(ev.stamina),
          staminaColor: COLOR.profit,
          zoneLabel: zone.label,
          zoneColor: zone.color,
          zoneBg: zone.bg,
          rowBg: 'rgba(34,197,94,0.07)',
        };
      }
      // gain
      const bonus = ev.stamina > BONUS;
      return {
        kind: 'gain',
        sMin: ev.stamina,
        tag: bonus ? 'BONUS' : '',
        tagColor: COLOR.profit,
        timeLabel: this.clockLabel(sim.refMs + ev.t * 60000),
        timeColor: COLOR.primary,
        timeWeight: '400',
        offlineLabel: this.durLabel(ev.t),
        staminaLabel: this.hhmm(ev.stamina),
        staminaColor: ev.stamina >= BONUS ? COLOR.profit : COLOR.primary,
        zoneLabel: zone.label,
        zoneColor: zone.color,
        zoneBg: zone.bg,
        rowBg: 'transparent',
      };
    });

    return allRows.filter((r) => {
      if (r.kind === 'start') return true;
      if (r.kind === 'gain') return f.gains && !(f.bonusOnly && r.sMin < BONUS);
      if (r.kind === 'huntStart' || r.kind === 'huntEnd') return f.hunts;
      if (r.kind === 'ext') return f.exts;
      return true;
    });
  });

  protected readonly huntRows = computed<HuntRow[]>(() => {
    const sim = this.sim();
    const startMs = this.startMs();
    const staminaMin = this.staminaMin();

    // Stamina at each hunt's start, from the simulation, for an accurate bonus preview.
    const huntStartStamina: Record<number, number> = {};
    sim.events.forEach((ev) => {
      if (ev.type === 'huntStart') huntStartStamina[ev.t] = ev.stamina;
    });

    return this.hunts().map((h, idx) => {
      const tStart = Math.round((h.startMs - startMs) / 60000);
      const sAtStart = huntStartStamina[tStart];
      const bMin =
        sAtStart == null
          ? this.huntBonusMin(Math.min(MAX, staminaMin), h.durMin)
          : this.huntBonusMin(sAtStart, h.durMin);
      const bonusNote =
        bMin <= 0
          ? { text: 'No bonus — starts below 39:00', color: COLOR.muted }
          : bMin >= h.durMin - 0.5
            ? { text: 'Entire hunt at +50% XP bonus', color: COLOR.profit }
            : { text: this.hhmm(bMin) + ' of ' + this.hhmm(h.durMin) + ' at +50% XP', color: COLOR.profit };
      return {
        id: h.id,
        index: idx + 1,
        startLocal: this.toLocalInput(h.startMs),
        hours: Math.floor(h.durMin / 60),
        minutes: h.durMin % 60,
        consumeLabel: this.hhmm(h.durMin),
        endLabel: this.clockLabel(h.startMs + h.durMin * 60000),
        bonusText: bonusNote.text,
        bonusColor: bonusNote.color,
      };
    });
  });

  protected readonly extRows = computed<ExtRow[]>(() => {
    const sorted = [...this.extensions()].sort((a, b) => a.ms - b.ms);
    return sorted.map((e, idx) => {
      const prev = idx > 0 ? sorted[idx - 1] : null;
      const tooSoon = prev ? e.ms - prev.ms < 24 * 3600 * 1000 : false;
      return {
        id: e.id,
        index: idx + 1,
        startLocal: this.toLocalInput(e.ms),
        tooSoon,
      };
    });
  });

  /** Tailwind classes for a filter chip in its active / inactive state. */
  protected chipClass(active: boolean): string {
    const base = 'cursor-pointer rounded-md border px-3 py-1.5 text-[11px] font-semibold transition';
    return active
      ? `${base} border-[color:var(--color-accent)] bg-[color:var(--color-accent-soft)] text-[color:var(--color-accent)]`
      : `${base} border-[color:var(--color-border-strong)] bg-transparent text-[color:var(--color-text-muted)]`;
  }

  // --- stamina input ---
  private setStamina(min: number): void {
    this.staminaMin.set(Math.min(MAX, Math.max(0, Math.round(min))));
  }

  protected onStaminaSlider(e: Event): void {
    this.setStamina(parseInt((e.target as HTMLInputElement).value, 10) || 0);
  }

  protected onStaminaText(e: Event): void {
    const m = String((e.target as HTMLInputElement).value).match(
      /^\s*(\d{1,2})\s*[:hH]?\s*(\d{1,2})?\s*$/,
    );
    if (m) this.setStamina((parseInt(m[1], 10) || 0) * 60 + (parseInt(m[2], 10) || 0));
  }

  protected setStamina14(): void {
    this.setStamina(14 * 60);
  }
  protected setStamina30(): void {
    this.setStamina(30 * 60);
  }
  protected setStamina39(): void {
    this.setStamina(39 * 60);
  }

  // --- start time ---
  protected onStartChange(e: Event): void {
    const ms = new Date((e.target as HTMLInputElement).value).getTime();
    if (!isNaN(ms)) this.startMs.set(ms);
  }

  protected setNow(): void {
    this.startMs.set(this.roundMin(Date.now()));
  }

  // --- hunts ---
  protected addHunt(): void {
    const nextStart = this.roundMin(this.startMs() + 30 * 60000);
    this.hunts.update((hs) => [...hs, { id: 'h' + this.huntSeq, startMs: nextStart, durMin: 120 }]);
    this.huntSeq += 1;
  }

  protected removeHunt(id: string): void {
    this.hunts.update((hs) => hs.filter((x) => x.id !== id));
  }

  protected onHuntStartChange(id: string, e: Event): void {
    const ms = new Date((e.target as HTMLInputElement).value).getTime();
    if (!isNaN(ms)) this.hunts.update((hs) => hs.map((x) => (x.id === id ? { ...x, startMs: ms } : x)));
  }

  protected onHuntHoursChange(id: string, e: Event): void {
    const hh = Math.max(0, parseInt((e.target as HTMLInputElement).value, 10) || 0);
    this.hunts.update((hs) =>
      hs.map((x) => (x.id === id ? { ...x, durMin: hh * 60 + (x.durMin % 60) } : x)),
    );
  }

  protected onHuntMinutesChange(id: string, e: Event): void {
    const mm = Math.min(59, Math.max(0, parseInt((e.target as HTMLInputElement).value, 10) || 0));
    this.hunts.update((hs) =>
      hs.map((x) => (x.id === id ? { ...x, durMin: Math.floor(x.durMin / 60) * 60 + mm } : x)),
    );
  }

  // --- stamina extensions ---
  protected addExt(): void {
    const last = this.extensions().reduce((m, e) => Math.max(m, e.ms), this.startMs());
    const when = this.roundMin(last + 24 * 3600 * 1000);
    this.extensions.update((es) => [...es, { id: 'e' + this.extSeq, ms: when }]);
    this.extSeq += 1;
  }

  protected removeExt(id: string): void {
    this.extensions.update((es) => es.filter((x) => x.id !== id));
  }

  protected onExtChange(id: string, e: Event): void {
    const ms = new Date((e.target as HTMLInputElement).value).getTime();
    if (!isNaN(ms)) this.extensions.update((es) => es.map((x) => (x.id === id ? { ...x, ms } : x)));
  }

  // --- timeline filters ---
  protected toggleGains(): void {
    this.filters.update((f) => ({ ...f, gains: !f.gains }));
  }
  protected toggleHunts(): void {
    this.filters.update((f) => ({ ...f, hunts: !f.hunts }));
  }
  protected toggleExts(): void {
    this.filters.update((f) => ({ ...f, exts: !f.exts }));
  }
  protected toggleOffline(): void {
    this.filters.update((f) => ({ ...f, offline: !f.offline }));
  }
  protected toggleBonusOnly(): void {
    this.filters.update((f) => ({ ...f, bonusOnly: !f.bonusOnly }));
  }
}
