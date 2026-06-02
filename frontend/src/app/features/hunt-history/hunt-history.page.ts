import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import {
  PagedResponse,
  SessionListFilter,
  SessionSummary,
} from '../../core/models/session.model';
import { SessionApiService } from '../../core/services/session-api.service';
import { HuntCard } from './components/hunt-card/hunt-card';
import { HuntFilters, HuntFiltersValue } from './components/hunt-filters/hunt-filters';

@Component({
  selector: 'app-hunt-history-page',
  standalone: true,
  imports: [HuntCard, HuntFilters],
  templateUrl: './hunt-history.page.html',
  styleUrl: './hunt-history.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HuntHistoryPage implements OnInit {
  private readonly service = inject(SessionApiService);
  private readonly router = inject(Router);

  protected readonly sessions = signal<SessionSummary[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly selectedIds = signal<Set<string>>(new Set());

  ngOnInit(): void {
    this.fetch();
  }

  protected onFiltersApplied(value: HuntFiltersValue): void {
    const filter: SessionListFilter = {
      name: value.name || undefined,
      player: value.player || undefined,
      startDate: value.startDate ? `${value.startDate}T00:00:00` : undefined,
      endDate: value.endDate ? `${value.endDate}T23:59:59` : undefined,
    };
    this.fetch(filter);
  }

  protected onToggleSelect(id: string): void {
    this.selectedIds.update((current) => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  protected isSelected(id: string): boolean {
    return this.selectedIds().has(id);
  }

  protected onCompare(): void {
    const ids = Array.from(this.selectedIds());
    if (ids.length < 2) {
      this.error.set('Select at least 2 sessions to compare.');
      return;
    }
    this.router.navigate(['/compare'], { queryParams: { ids: ids.join(',') } });
  }

  private fetch(filter: SessionListFilter = {}): void {
    this.loading.set(true);
    this.error.set(null);
    this.service.list(filter).subscribe({
      next: (response: PagedResponse<SessionSummary>) => {
        this.sessions.set(response.content);
        this.loading.set(false);
      },
      error: (err) => {
        console.error(err);
        this.error.set('Could not load sessions.');
        this.loading.set(false);
      },
    });
  }
}
