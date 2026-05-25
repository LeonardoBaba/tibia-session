import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { SessionApiService } from '../../core/services/session-api.service';
import { SessionDetail } from '../../core/models/session.model';
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
  /** Vem da rota `/hunts/:id` graças ao `withComponentInputBinding()`. */
  readonly id = input.required<string>();

  private readonly api = inject(SessionApiService);
  private readonly router = inject(Router);

  protected readonly session = signal<SessionDetail | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly saving = signal(false);

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
    // Permite limpar (string vazia).
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
        this.error.set('Falha ao salvar.');
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
          this.error.set('Hunt não encontrada.');
        } else {
          this.error.set('Falha ao carregar a sessão.');
        }
        this.loading.set(false);
      },
    });
  }
}
