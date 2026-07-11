import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
} from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { SessionApiService } from '../../core/services/session-api.service';
import { SessionDetail, SessionMember } from '../../core/models/session.model';
import { parseDurationToHours } from '../../core/utils/session-duration';
import { SignedNumberPipe } from '../../shared/pipes/signed-number.pipe';

type Metric = 'damage' | 'healing' | 'supplies';

/** One character occurrence inside one session; key is `${sessionIdx}:${memberId}`. */
interface CharRef {
  key: string;
  sessionIdx: number;
  member: SessionMember;
}

interface TrayModel {
  sessionIdx: number;
  name: string;
  chips: CharRef[];
}

interface BoardCell {
  sessionIdx: number;
  cellKey: string;
  char: CharRef | null;
  auto: boolean;
}

interface BoardGroup {
  id: string;
  label: string;
  sub: string;
  count: string;
  cells: BoardCell[];
}

interface MetricCell {
  value: number | null;
  best: boolean;
  pct: number;
}

interface MetricRow {
  key: string;
  name: string;
  sub: string;
  cells: MetricCell[];
  sortVal: number;
}

@Component({
  selector: 'app-compare-page',
  standalone: true,
  imports: [DatePipe, DecimalPipe, RouterLink, SignedNumberPipe],
  templateUrl: './compare.page.html',
  styleUrl: './compare.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ComparePage {
  readonly ids = input<string | undefined>(undefined);

  private readonly api = inject(SessionApiService);

  protected readonly sessions = signal<SessionDetail[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly showPerHour = signal(false);

  // Pairing state: each group is one "real player"; assign maps char key -> group id.
  protected readonly groups = signal<string[]>([]);
  protected readonly assign = signal<Record<string, string>>({});
  protected readonly draggingKey = signal<string | null>(null);
  protected readonly overCell = signal<string | null>(null);
  protected readonly overTray = signal<number | null>(null);
  protected readonly selectedKey = signal<string | null>(null);
  protected readonly boardCollapsed = signal(false);
  private nextGroupIdx = 0;

  protected readonly parsedIds = computed<string[]>(() => {
    const raw = this.ids();
    if (!raw) return [];
    return raw
      .split(',')
      .map((id) => id.trim())
      .filter((id) => id.length > 0);
  });

  protected readonly canCompare = computed(() => this.parsedIds().length >= 2);

  protected readonly gridCols = computed(
    () => `190px repeat(${this.sessions().length}, minmax(0, 1fr))`,
  );

  /** Min width (px) so the pairing board keeps usable columns and scrolls horizontally on narrow screens. */
  protected readonly boardMinWidth = computed(() => 190 + this.sessions().length * 180);

  protected readonly perHourSuffix = computed(() => (this.showPerHour() ? ' · per hour' : ''));

  private readonly charMap = computed(() => {
    const map = new Map<string, CharRef>();
    this.sessions().forEach((session, sessionIdx) =>
      session.members.forEach((member) => {
        const key = `${sessionIdx}:${member.id}`;
        map.set(key, { key, sessionIdx, member });
      }),
    );
    return map;
  });

  private readonly sessionHours = computed(() =>
    this.sessions().map((session) => parseDurationToHours(session.sessionDuration)),
  );

  private readonly groupChars = computed(() => {
    const map = new Map<string, (CharRef | null)[]>();
    const count = this.sessions().length;
    this.groups().forEach((group) => map.set(group, new Array<CharRef | null>(count).fill(null)));
    const chars = this.charMap();
    Object.entries(this.assign()).forEach(([key, group]) => {
      const cells = map.get(group);
      const ref = chars.get(key);
      if (cells && ref) cells[ref.sessionIdx] = ref;
    });
    return map;
  });

  protected readonly trays = computed<TrayModel[]>(() => {
    const assign = this.assign();
    const chars = this.charMap();
    return this.sessions().map((session, sessionIdx) => ({
      sessionIdx,
      name: session.name ?? 'Unnamed hunt',
      chips: session.members
        .filter((member) => assign[`${sessionIdx}:${member.id}`] === undefined)
        .map((member) => chars.get(`${sessionIdx}:${member.id}`)!),
    }));
  });

  protected readonly board = computed<BoardGroup[]>(() => {
    const charsByGroup = this.groupChars();
    const total = this.sessions().length;
    return this.groups().map((id) => {
      const chars = charsByGroup.get(id) ?? [];
      const names = chars.filter((c): c is CharRef => c !== null).map((c) => c.member.name);
      const primary = names[0] ?? '';
      const others = [
        ...new Set(names.slice(1).filter((n) => n.toLowerCase() !== primary.toLowerCase())),
      ];
      return {
        id,
        label: primary || 'New group',
        sub: others.length ? `= ${others.join(' · ')}` : '',
        count: `${names.length}/${total}`,
        cells: chars.map((char, sessionIdx) => ({
          sessionIdx,
          cellKey: `${id}|${sessionIdx}`,
          char,
          auto:
            char !== null &&
            sessionIdx > 0 &&
            primary !== '' &&
            char.member.name.toLowerCase() === primary.toLowerCase(),
        })),
      };
    });
  });

  protected readonly unassignedCount = computed(() => {
    const assign = this.assign();
    return this.sessions().reduce(
      (n, session, sessionIdx) =>
        n + session.members.filter((m) => assign[`${sessionIdx}:${m.id}`] === undefined).length,
      0,
    );
  });

  protected readonly pairingSummary = computed(
    () => `${this.groups().length} groups · ${this.unassignedCount()} unassigned`,
  );

  /** A mismatch is any character that auto-match could not place into a paired row. */
  protected readonly hasMismatch = computed(() => this.unassignedCount() > 0);

  protected readonly dragSession = computed(() => this.sessionOf(this.draggingKey()));
  protected readonly selectedSession = computed(() => this.sessionOf(this.selectedKey()));

  protected readonly damageRows = computed(() => this.buildRows('damage', false));
  protected readonly healingRows = computed(() => this.buildRows('healing', false));
  protected readonly suppliesRows = computed(() => this.buildRows('supplies', true));

  constructor() {
    effect(() => {
      const ids = this.parsedIds();
      if (ids.length < 2) {
        this.sessions.set([]);
        this.seed(false);
        return;
      }
      this.fetch(ids);
    });
  }

  protected balanceClass(value: number): string {
    if (value > 0) return 'text-[color:var(--color-profit)]';
    if (value < 0) return 'text-[color:var(--color-loss)]';
    return 'text-[color:var(--color-text-secondary)]';
  }

  protected togglePerHour(event: Event): void {
    this.showPerHour.set((event.target as HTMLInputElement).checked);
  }

  protected toggleBoard(): void {
    this.boardCollapsed.update((collapsed) => !collapsed);
  }

  protected cardBalance(session: SessionDetail): number {
    if (!this.showPerHour()) return session.balance;
    const hours = parseDurationToHours(session.sessionDuration);
    return hours ? Math.round(session.balance / hours) : session.balance;
  }

  // --- pairing interactions ---

  protected autoMatch(): void {
    this.seed(true);
  }

  protected clearAll(): void {
    this.seed(false);
  }

  protected addGroup(): void {
    this.groups.update((groups) => [...groups, `g${this.nextGroupIdx++}`]);
  }

  protected removeGroup(groupId: string): void {
    this.assign.update((assign) => {
      const next = { ...assign };
      Object.keys(next).forEach((key) => {
        if (next[key] === groupId) delete next[key];
      });
      return next;
    });
    this.groups.update((groups) => groups.filter((g) => g !== groupId));
    this.selectedKey.set(null);
  }

  protected toggleChipSelect(key: string): void {
    this.selectedKey.update((current) => (current === key ? null : key));
  }

  protected unassign(event: Event, key: string): void {
    event.stopPropagation();
    this.unassignChar(key);
  }

  protected onChipDragStart(event: DragEvent, key: string): void {
    event.dataTransfer?.setData('text/plain', key);
    if (event.dataTransfer) event.dataTransfer.effectAllowed = 'move';
    this.draggingKey.set(key);
  }

  protected onDragEnd(): void {
    this.draggingKey.set(null);
    this.overCell.set(null);
    this.overTray.set(null);
  }

  protected onCellDragOver(event: DragEvent, cellKey: string, sessionIdx: number): void {
    if (this.dragSession() !== sessionIdx) return;
    event.preventDefault();
    if (event.dataTransfer) event.dataTransfer.dropEffect = 'move';
    if (this.overCell() !== cellKey) this.overCell.set(cellKey);
  }

  protected onCellDragLeave(cellKey: string): void {
    if (this.overCell() === cellKey) this.overCell.set(null);
  }

  protected onCellDrop(event: DragEvent, groupId: string, sessionIdx: number): void {
    event.preventDefault();
    const key = this.draggingKey();
    if (key !== null && this.sessionOf(key) === sessionIdx) {
      this.assignChar(key, groupId, sessionIdx);
    }
  }

  protected onTrayDragOver(event: DragEvent, sessionIdx: number): void {
    if (this.dragSession() !== sessionIdx) return;
    event.preventDefault();
    if (this.overTray() !== sessionIdx) this.overTray.set(sessionIdx);
  }

  protected onTrayDragLeave(sessionIdx: number): void {
    if (this.overTray() === sessionIdx) this.overTray.set(null);
  }

  protected onTrayDrop(event: DragEvent, sessionIdx: number): void {
    event.preventDefault();
    const key = this.draggingKey();
    if (key !== null && this.sessionOf(key) === sessionIdx) this.unassignChar(key);
    this.overTray.set(null);
    this.draggingKey.set(null);
  }

  protected onSlotClick(groupId: string, sessionIdx: number): void {
    const key = this.selectedKey();
    if (key !== null && this.sessionOf(key) === sessionIdx) {
      this.assignChar(key, groupId, sessionIdx);
    }
  }

  // --- dynamic style helpers (drag/selection states) ---

  protected isSelected(key: string): boolean {
    return this.selectedKey() === key;
  }

  protected isTrayOver(sessionIdx: number): boolean {
    return this.overTray() === sessionIdx;
  }

  protected isCellOver(cell: BoardCell): boolean {
    return this.overCell() === cell.cellKey && this.dragSession() === cell.sessionIdx;
  }

  protected isInviting(cell: BoardCell): boolean {
    return cell.char === null && this.selectedSession() === cell.sessionIdx;
  }

  protected trayZoneClass(tray: TrayModel): string {
    const over = this.isTrayOver(tray.sessionIdx);
    const border = over
      ? 'border-[color:var(--color-accent)]'
      : 'border-[color:var(--color-border)]';
    const bg = over
      ? 'bg-[color:var(--color-accent-soft)]'
      : tray.chips.length === 0
        ? 'bg-transparent'
        : 'bg-[color:var(--color-bg)]';
    return `flex min-h-[66px] flex-col gap-2 rounded-[10px] border border-dashed p-2.5 transition-colors ${border} ${bg}`;
  }

  protected chipClass(key: string, inSlot: boolean): string {
    const border = this.isSelected(key)
      ? 'border-[color:var(--color-accent)] ring-2 ring-[color:var(--color-accent)]'
      : 'border-[color:var(--color-border)]';
    const bg = inSlot
      ? 'w-full min-w-0 overflow-hidden bg-[color:var(--color-surface)]'
      : 'bg-[color:var(--color-surface-2)]';
    return `inline-flex max-w-full cursor-grab select-none items-center gap-[7px] whitespace-nowrap rounded-lg border px-2.5 py-[7px] text-[13px] leading-none text-[color:var(--color-text-primary)] ${bg} ${border}`;
  }

  protected slotClass(cell: BoardCell): string {
    const has = cell.char !== null;
    const over = this.isCellOver(cell);
    const base = 'flex min-h-[44px] items-center rounded-lg transition-colors';

    // A filled slot lets the chip's own border be the only boundary — no doubled
    // border around it. Add an accent ring only while a chip is dragged over it.
    if (has) {
      return over
        ? `${base} border border-[color:var(--color-accent)] bg-[color:var(--color-accent-soft)]`
        : base;
    }

    const border =
      over || this.isInviting(cell)
        ? 'border-[color:var(--color-accent)]'
        : 'border-[color:var(--color-border-strong)]';
    const bg = over ? 'bg-[color:var(--color-accent-soft)]' : 'bg-transparent';
    return `${base} cursor-pointer border border-dashed px-1.5 ${border} ${bg}`;
  }

  protected cellTextClass(cell: MetricCell, cost: boolean): string {
    const weight = cell.best ? 'font-bold' : 'font-medium';
    const color = cost
      ? 'text-[color:var(--color-loss)]'
      : cell.best
        ? 'text-[color:var(--color-text-primary)]'
        : 'text-[color:var(--color-text-secondary)]';
    return `tabular-nums ${weight} ${color}`;
  }

  // --- internals ---

  private sessionOf(key: string | null): number | null {
    if (key === null) return null;
    return Number(key.split(':')[0]);
  }

  /** Rebuild groups from Hunt #1's roster; with auto, match other rosters by name. */
  private seed(auto: boolean): void {
    const sessions = this.sessions();
    const groups: string[] = [];
    const assign: Record<string, string> = {};
    const anchor = sessions[0]?.members ?? [];
    anchor.forEach((member, i) => {
      groups.push(`g${i}`);
      assign[`0:${member.id}`] = `g${i}`;
    });
    if (auto) {
      const taken = new Set<string>();
      for (let s = 1; s < sessions.length; s++) {
        sessions[s].members.forEach((member) => {
          const anchorIdx = anchor.findIndex(
            (a) => a.name.toLowerCase() === member.name.toLowerCase(),
          );
          if (anchorIdx >= 0 && !taken.has(`g${anchorIdx}|${s}`)) {
            assign[`${s}:${member.id}`] = `g${anchorIdx}`;
            taken.add(`g${anchorIdx}|${s}`);
          }
        });
      }
    }
    this.groups.set(groups);
    this.assign.set(assign);
    this.nextGroupIdx = anchor.length;
    this.draggingKey.set(null);
    this.overCell.set(null);
    this.overTray.set(null);
    this.selectedKey.set(null);
  }

  private assignChar(key: string, groupId: string, sessionIdx: number): void {
    this.assign.update((assign) => {
      const next = { ...assign };
      Object.keys(next).forEach((k) => {
        if (next[k] === groupId && this.sessionOf(k) === sessionIdx) delete next[k];
      });
      next[key] = groupId;
      return next;
    });
    this.draggingKey.set(null);
    this.overCell.set(null);
    this.overTray.set(null);
    this.selectedKey.set(null);
  }

  private unassignChar(key: string): void {
    this.assign.update((assign) => {
      const next = { ...assign };
      delete next[key];
      return next;
    });
    this.selectedKey.set(null);
    this.draggingKey.set(null);
    this.overTray.set(null);
  }

  private buildRows(metric: Metric, cost: boolean): MetricRow[] {
    const sessions = this.sessions();
    const charsByGroup = this.groupChars();
    const assign = this.assign();
    const chars = this.charMap();
    const rows: MetricRow[] = [];

    this.groups().forEach((groupId) => {
      const groupChars = charsByGroup.get(groupId) ?? [];
      if (groupChars.every((c) => c === null)) return;
      rows.push(this.makeRow(groupId, groupChars, metric, cost, null));
    });

    sessions.forEach((session, sessionIdx) => {
      session.members.forEach((member) => {
        const key = `${sessionIdx}:${member.id}`;
        if (assign[key] !== undefined) return;
        const solo: (CharRef | null)[] = sessions.map(() => null);
        solo[sessionIdx] = chars.get(key) ?? null;
        rows.push(this.makeRow(key, solo, metric, cost, sessionIdx));
      });
    });

    return rows.sort((a, b) => b.sortVal - a.sortVal);
  }

  private makeRow(
    key: string,
    chars: (CharRef | null)[],
    metric: Metric,
    cost: boolean,
    soloIdx: number | null,
  ): MetricRow {
    const hours = this.sessionHours();
    const perHour = this.showPerHour();
    const values = chars.map((char, i) => {
      if (!char) return null;
      const raw = char.member[metric];
      const h = hours[i];
      return perHour && h ? raw / h : raw;
    });
    const present = values.filter((v): v is number => v !== null);
    const rowMax = present.length ? Math.max(...present.map((v) => Math.abs(v))) : 0;

    // Highlight the best cell of the row: highest output, or lowest cost.
    let bestIdx = -1;
    if (present.length > 1) {
      let bestVal = cost ? Infinity : -Infinity;
      values.forEach((v, i) => {
        if (v === null) return;
        if (cost ? v < bestVal : v > bestVal) {
          bestVal = v;
          bestIdx = i;
        }
      });
    }

    const cells: MetricCell[] = values.map((value, i) => ({
      value,
      best: i === bestIdx,
      pct:
        value === null || rowMax === 0
          ? 0
          : Math.max(6, Math.round((Math.abs(value) / rowMax) * 100)),
    }));

    const names = chars.filter((c): c is CharRef => c !== null).map((c) => c.member.name);
    const name = names[0] ?? '—';
    let sub = '';
    if (soloIdx !== null) {
      sub = `Hunt #${soloIdx + 1} only`;
    } else {
      const others = [
        ...new Set(names.slice(1).filter((n) => n.toLowerCase() !== name.toLowerCase())),
      ];
      sub = others.length ? `= ${others.join(' · ')}` : '';
    }

    return {
      key,
      name,
      sub,
      cells,
      sortVal: present.reduce((sum, v) => sum + Math.abs(v), 0),
    };
  }

  private fetch(ids: string[]): void {
    this.loading.set(true);
    this.error.set(null);

    const requests = ids.map((id) =>
      this.api.getById(id).pipe(catchError(() => of(null as SessionDetail | null))),
    );

    forkJoin(requests).subscribe({
      next: (results) => {
        const ok = results.filter((s): s is SessionDetail => s !== null);
        if (ok.length < ids.length) {
          const missing = ids.length - ok.length;
          this.error.set(
            `${missing} session${missing > 1 ? 's' : ''} not found — showing the rest.`,
          );
        }
        this.sessions.set(ok);
        this.seed(true);
        // Start minimized when every character paired automatically; expand when
        // there are mismatches that need manual attention.
        this.boardCollapsed.set(!this.hasMismatch());
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load sessions.');
        this.loading.set(false);
      },
    });
  }
}
