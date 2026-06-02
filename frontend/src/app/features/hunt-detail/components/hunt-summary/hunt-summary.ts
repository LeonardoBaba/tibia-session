import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { SessionDetail } from '../../../../core/models/session.model';
import { perHour } from '../../../../core/utils/session-duration';
import { SignedNumberPipe } from '../../../../shared/pipes/signed-number.pipe';

interface SummaryStat {
  label: string;
  value: number | string | null;
  formatted: string;
  tone: 'default' | 'profit' | 'loss';
}

@Component({
  selector: 'app-hunt-summary',
  standalone: true,
  imports: [DatePipe, DecimalPipe, SignedNumberPipe],
  templateUrl: './hunt-summary.html',
  styleUrl: './hunt-summary.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HuntSummary {
  readonly session = input.required<SessionDetail>();

  readonly perHour = model<boolean>(false);

  protected togglePerHour(event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    this.perHour.set(checked);
  }

  protected readonly topDamageMember = computed(() => {
    const members = this.session().members;
    if (members.length === 0) return null;
    return members.reduce((top, m) => (m.damage > top.damage ? m : top), members[0]);
  });

  protected readonly balancePerHour = computed(() => {
    const s = this.session();
    return perHour(s.balance, s.sessionDuration);
  });

  protected toneClass(tone: SummaryStat['tone']): string {
    if (tone === 'profit') return 'text-[color:var(--color-profit)]';
    if (tone === 'loss') return 'text-[color:var(--color-loss)]';
    return 'text-[color:var(--color-text-primary)]';
  }

  protected balanceTone(value: number): SummaryStat['tone'] {
    if (value > 0) return 'profit';
    if (value < 0) return 'loss';
    return 'default';
  }
}
