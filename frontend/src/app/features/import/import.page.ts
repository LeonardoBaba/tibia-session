import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { SessionApiService } from '../../core/services/session-api.service';
import { SessionDetail } from '../../core/models/session.model';
import { perHour as toPerHour } from '../../core/utils/session-duration';
import { HuntSummary } from '../hunt-detail/components/hunt-summary/hunt-summary';
import { PartyMembers } from '../hunt-detail/components/party-members/party-members';
import { PaymentDistribution } from '../hunt-detail/components/payment-distribution/payment-distribution';
import { RankingCard, RankingEntry } from '../hunt-detail/components/ranking-card/ranking-card';

@Component({
  selector: 'app-import-page',
  standalone: true,
  imports: [
    FormsModule,
    RouterLink,
    HuntSummary,
    PartyMembers,
    PaymentDistribution,
    RankingCard,
  ],
  templateUrl: './import.page.html',
  styleUrl: './import.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportPage {
  private readonly api = inject(SessionApiService);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);

  protected readonly input = signal('');
  protected readonly processing = signal(false);
  protected readonly preview = signal<SessionDetail | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly showPerHour = signal(false);

  protected readonly canSubmit = computed(
    () => this.input().trim().length > 0 && !this.processing(),
  );

  protected readonly damageEntries = computed<RankingEntry[]>(() =>
    this.buildEntries((m) => m.damage),
  );

  protected readonly healingEntries = computed<RankingEntry[]>(() =>
    this.buildEntries((m) => m.healing),
  );

  protected readonly wasteEntries = computed<RankingEntry[]>(() =>
    this.buildEntries((m) => m.supplies),
  );

  protected readonly valueSuffix = computed(() => (this.showPerHour() ? '/h' : null));

  protected onInputChange(value: string): void {
    this.input.set(value);
  }

  protected onSubmit(): void {
    if (!this.canSubmit()) return;
    const text = this.input().trim();
    this.processing.set(true);
    this.error.set(null);

    if (this.auth.isAuthenticated()) {
      this.api.create({ input: text }).subscribe({
        next: (saved) => {
          this.processing.set(false);
          this.router.navigate(['/hunts', saved.id]);
        },
        error: (err) => this.handleError(err),
      });
    } else {
      this.api.preview(text).subscribe({
        next: (result) => {
          this.preview.set(result);
          this.processing.set(false);
        },
        error: (err) => this.handleError(err),
      });
    }
  }

  protected reset(): void {
    this.input.set('');
    this.preview.set(null);
    this.error.set(null);
  }

  private handleError(err: unknown): void {
    console.error(err);
    const message =
      typeof err === 'object' && err && 'error' in err
        ? // @ts-expect-error: shape do payload de erro da API
          (err.error?.message as string | undefined)
        : undefined;
    this.error.set(message ?? 'Failed to process the session. Check the input format.');
    this.processing.set(false);
  }

  private buildEntries(metric: (member: SessionDetail['members'][number]) => number): RankingEntry[] {
    const session = this.preview();
    if (!session) return [];
    const list = session.members.filter((m) => metric(m) > 0);

    if (!this.showPerHour()) {
      return list.map((m) => ({ name: m.name, value: metric(m) }));
    }

    return list.map((m) => {
      const ph = toPerHour(metric(m), session.sessionDuration);
      return { name: m.name, value: ph === null ? metric(m) : Math.round(ph) };
    });
  }
}
