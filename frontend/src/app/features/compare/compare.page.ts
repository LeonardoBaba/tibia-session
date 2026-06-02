import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { SessionApiService } from '../../core/services/session-api.service';
import { SessionDetail } from '../../core/models/session.model';
import { parseDurationToHours } from '../../core/utils/session-duration';
import { SignedNumberPipe } from '../../shared/pipes/signed-number.pipe';

interface PlayerRow {
  name: string;
  values: (number | null)[];
  total: number;
}

@Component({
  selector: 'app-compare-page',
  standalone: true,
  imports: [DecimalPipe, RouterLink, SignedNumberPipe],
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

  protected readonly parsedIds = computed<string[]>(() => {
    const raw = this.ids();
    if (!raw) return [];
    return raw
      .split(',')
      .map((id) => id.trim())
      .filter((id) => id.length > 0);
  });

  protected readonly damageMatrix = computed<PlayerRow[]>(() =>
    this.buildPlayerMatrix('damage'),
  );

  protected readonly healingMatrix = computed<PlayerRow[]>(() =>
    this.buildPlayerMatrix('healing'),
  );

  protected readonly canCompare = computed(() => this.parsedIds().length >= 2);

  constructor() {
    effect(() => {
      const ids = this.parsedIds();
      if (ids.length < 2) {
        this.sessions.set([]);
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

  private buildPlayerMatrix(metric: 'damage' | 'healing'): PlayerRow[] {
    const list = this.sessions();
    const perHour = this.showPerHour();
    const playerMap = new Map<string, (number | null)[]>();

    list.forEach((session, huntIdx) => {
      const hours = perHour ? parseDurationToHours(session.sessionDuration) : null;

      session.members.forEach((member) => {
        const row = playerMap.get(member.name) ?? new Array(list.length).fill(null);
        const raw = member[metric];
        row[huntIdx] = perHour && hours ? Math.round(raw / hours) : raw;
        playerMap.set(member.name, row);
      });
    });

    return Array.from(playerMap.entries())
      .map<PlayerRow>(([name, values]) => ({
        name,
        values,
        total: values.reduce<number>((sum, v) => sum + (v ?? 0), 0),
      }))
      .sort((a, b) => b.total - a.total);
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
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load sessions.');
        this.loading.set(false);
      },
    });
  }
}
