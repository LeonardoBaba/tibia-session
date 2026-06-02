import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { SessionTransfer } from '../../../../core/models/session.model';

interface GroupedTransfers {
  payer: string;
  total: number;
  transfers: SessionTransfer[];
}

@Component({
  selector: 'app-payment-distribution',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './payment-distribution.html',
  styleUrl: './payment-distribution.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentDistribution {
  readonly transfers = input.required<SessionTransfer[]>();

  protected readonly grouped = computed<GroupedTransfers[]>(() => {
    const map = new Map<string, GroupedTransfers>();
    for (const t of this.transfers()) {
      const entry = map.get(t.fromPlayer) ?? {
        payer: t.fromPlayer,
        total: 0,
        transfers: [] as SessionTransfer[],
      };
      entry.total += t.amount;
      entry.transfers.push(t);
      map.set(t.fromPlayer, entry);
    }
    return Array.from(map.values()).sort((a, b) => b.total - a.total);
  });

  protected copyCommand(transfer: SessionTransfer): void {
    const command = `transfer ${transfer.amount} to ${transfer.toPlayer}`;
    navigator.clipboard?.writeText(command).catch(() => {});
  }
}
