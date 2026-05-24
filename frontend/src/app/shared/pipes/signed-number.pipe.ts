import { Pipe, PipeTransform } from '@angular/core';

/**
 * Formata um número com separadores de milhar e prefixa `+` quando positivo,
 * `-` quando negativo. Ex.: `1450200` → `+1,450,200`.
 */
@Pipe({ name: 'signedNumber' })
export class SignedNumberPipe implements PipeTransform {
  private readonly formatter = new Intl.NumberFormat('en-US');

  transform(value: number | null | undefined): string {
    if (value === null || value === undefined || Number.isNaN(value)) {
      return '—';
    }
    const formatted = this.formatter.format(Math.abs(value));
    if (value > 0) return `+${formatted}`;
    if (value < 0) return `-${formatted}`;
    return formatted;
  }
}
