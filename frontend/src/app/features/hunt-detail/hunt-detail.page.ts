import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { SessionApiService } from '../../core/services/session-api.service';
import { SessionDetail } from '../../core/models/session.model';
import { perHour as toPerHour } from '../../core/utils/session-duration';
import { EditableText } from '../../shared/components/editable-text/editable-text';
import { HuntSummary } from './components/hunt-summary/hunt-summary';
import { PartyMembers } from './components/party-members/party-members';
import { PaymentDistribution } from './components/payment-distribution/payment-distribution';
import { RankingCard, RankingEntry } from './components/ranking-card/ranking-card';

@Component({
  selector: 'app-hunt-detail-page',
  standalone: true,
  imports: [
    RouterLink,
    EditableText,
    HuntSummary,
    PartyMembers,
    PaymentDistribution,
    RankingCard,
  ],
  templateUrl: './hunt-detail.page.html',
  styleUrl: './hunt-detail.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HuntDetailPage {
  readonly id = input.required<string>();

  private readonly api = inject(SessionApiService);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  protected readonly session = signal<SessionDetail | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly saving = signal(false);
  protected readonly showPerHour = signal(false);

  protected readonly isOwner = computed(() => {
    const user = this.auth.currentUser();
    const session = this.session();
    if (!user || !session?.ownerDiscordId) return false;
    return user.discordId === session.ownerDiscordId;
  });

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

  private buildEntries(metric: (member: SessionDetail['members'][number]) => number): RankingEntry[] {
    const session = this.session();
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

  constructor() {
    effect(() => {
      const id = this.id();
      if (!id) return;
      this.fetch(id);
    });
  }

  protected onNameSave(name: string): void {
    const trimmed = name.trim();
    if (trimmed.length === 0) return;
    this.patch({ name: trimmed });
  }

  protected onCommentSave(comment: string): void {
    this.patch({ comment });
  }

  protected back(): void {
    this.router.navigate(['/hunts']);
  }

  private patch(payload: { name?: string; comment?: string }): void {
    const id = this.id();
    if (!id) return;
    this.saving.set(true);
    this.api.update(id, payload).subscribe({
      next: (updated) => {
        this.session.set(updated);
        this.saving.set(false);
      },
      error: (err) => {
        console.error(err);
        this.error.set('Failed to save.');
        this.saving.set(false);
      },
    });
  }

  private fetch(id: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.getById(id).subscribe({
      next: (session) => {
        this.session.set(session);
        this.loading.set(false);
      },
      error: (err) => {
        console.error(err);
        if (err?.status === 404) {
          this.error.set('Hunt not found.');
        } else {
          this.error.set('Failed to load the session.');
        }
        this.loading.set(false);
      },
    });
  }
}
