import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { SessionMember } from '../../../../core/models/session.model';
import { SignedNumberPipe } from '../../../../shared/pipes/signed-number.pipe';

interface MemberRow extends SessionMember {
  damagePercentage: number;
}

@Component({
  selector: 'app-party-members',
  standalone: true,
  imports: [DecimalPipe, SignedNumberPipe],
  templateUrl: './party-members.html',
  styleUrl: './party-members.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PartyMembers {
  readonly members = input.required<SessionMember[]>();

  protected readonly rows = computed<MemberRow[]>(() => {
    const list = this.members();
    const totalDamage = list.reduce((sum, m) => sum + m.damage, 0);
    return [...list]
      .sort((a, b) => b.damage - a.damage)
      .map((m) => ({
        ...m,
        damagePercentage: totalDamage === 0 ? 0 : (m.damage / totalDamage) * 100,
      }));
  });

  protected balanceClass(balance: number): string {
    if (balance > 0) return 'text-[color:var(--color-profit)]';
    if (balance < 0) return 'text-[color:var(--color-loss)]';
    return 'text-[color:var(--color-text-secondary)]';
  }
}
