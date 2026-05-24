import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { DatePipe } from '@angular/common';
import { SessionSummary } from '../../../../core/models/session.model';
import { CompactNumberPipe } from '../../../../shared/pipes/compact-number.pipe';
import { SignedNumberPipe } from '../../../../shared/pipes/signed-number.pipe';

@Component({
  selector: 'app-hunt-card',
  standalone: true,
  imports: [DatePipe, CompactNumberPipe, SignedNumberPipe],
  templateUrl: './hunt-card.html',
  styleUrl: './hunt-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HuntCard {
  readonly session = input.required<SessionSummary>();
  readonly selected = input<boolean>(false);
  readonly toggleSelect = output<string>();

  protected readonly balanceClass = computed(() => {
    const balance = this.session().balance;
    if (balance > 0) return 'text-[color:var(--color-profit)]';
    if (balance < 0) return 'text-[color:var(--color-loss)]';
    return 'text-[color:var(--color-text-secondary)]';
  });

  protected readonly displayName = computed(() => this.session().name ?? 'Unnamed hunt');

  protected onSelectClick(): void {
    this.toggleSelect.emit(this.session().id);
  }
}
