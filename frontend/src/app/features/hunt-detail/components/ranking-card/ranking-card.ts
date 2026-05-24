import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';

export interface RankingEntry {
  name: string;
  value: number;
}

export type RankingTone = 'accent' | 'profit' | 'loss';

interface ComputedEntry extends RankingEntry {
  percentage: number;
}

@Component({
  selector: 'app-ranking-card',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './ranking-card.html',
  styleUrl: './ranking-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RankingCard {
  readonly title = input.required<string>();
  readonly entries = input.required<RankingEntry[]>();
  readonly tone = input<RankingTone>('accent');
  /** Texto curto opcional (ex.: "gp"). */
  readonly valueSuffix = input<string | null>(null);

  protected readonly computed = computed<ComputedEntry[]>(() => {
    const list = this.entries();
    const total = list.reduce((sum, e) => sum + e.value, 0);
    if (total === 0) {
      return list.map((e) => ({ ...e, percentage: 0 }));
    }
    return [...list]
      .sort((a, b) => b.value - a.value)
      .map((e) => ({ ...e, percentage: (e.value / total) * 100 }));
  });

  protected readonly barColorClass = computed(() => {
    switch (this.tone()) {
      case 'profit':
        return 'bg-[color:var(--color-profit)]';
      case 'loss':
        return 'bg-[color:var(--color-loss)]';
      default:
        return 'bg-[color:var(--color-accent)]';
    }
  });

  protected readonly valueColorClass = computed(() => {
    switch (this.tone()) {
      case 'profit':
        return 'text-[color:var(--color-profit)]';
      case 'loss':
        return 'text-[color:var(--color-loss)]';
      default:
        return 'text-[color:var(--color-text-primary)]';
    }
  });
}
