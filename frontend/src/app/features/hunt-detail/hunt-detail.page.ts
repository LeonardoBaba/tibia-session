import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { SessionApiService } from '../../core/services/session-api.service';
import { SessionDetail } from '../../core/models/session.model';
import { HuntSummary } from './components/hunt-summary/hunt-summary';
import { PartyMembers } from './components/party-members/party-members';
import { PaymentDistribution } from './components/payment-distribution/payment-distribution';
import { RankingCard, RankingEntry } from './components/ranking-card/ranking-card';

@Component({
  selector: 'app-hunt-detail-page',
  standalone: true,
  imports: [RouterLink, HuntSummary, PartyMembers, PaymentDistribution, RankingCard],
  templateUrl: './hunt-detail.page.html',
  styleUrl: './hunt-detail.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HuntDetailPage {
  /** Vem da rota `/hunts/:id` graças ao `withComponentInputBinding()`. */
  readonly id = input.required<string>();

  private readonly api = inject(SessionApiService);
  private readonly router = inject(Router);

  protected readonly session = signal<SessionDetail | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly damageEntries = computed<RankingEntry[]>(() =>
    (this.session()?.members ?? [])
      .filter((m) => m.damage > 0)
      .map((m) => ({ name: m.name, value: m.damage })),
  );

  protected readonly healingEntries = computed<RankingEntry[]>(() =>
    (this.session()?.members ?? [])
      .filter((m) => m.healing > 0)
      .map((m) => ({ name: m.name, value: m.healing })),
  );

  protected readonly wasteEntries = computed<RankingEntry[]>(() =>
    (this.session()?.members ?? [])
      .filter((m) => m.supplies > 0)
      .map((m) => ({ name: m.name, value: m.supplies })),
  );

  constructor() {
    // Sempre que o `id` da rota mudar, refaz o fetch. Effects são o jeito
    // idiomático de reagir a inputs no Angular moderno.
    effect(() => {
      const id = this.id();
      if (!id) return;
      this.fetch(id);
    });
  }

  protected back(): void {
    this.router.navigate(['/hunts']);
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
          this.error.set('Hunt não encontrada.');
        } else {
          this.error.set('Falha ao carregar a sessão.');
        }
        this.loading.set(false);
      },
    });
  }
}
