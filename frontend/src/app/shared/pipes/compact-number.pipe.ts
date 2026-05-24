import { Pipe, PipeTransform } from '@angular/core';

/**
 * Formata um número em notação compacta — `1500000` → `1.5M`.
 * Mantém o sinal negativo.
 */
@Pipe({ name: 'compactNumber' })
export class CompactNumberPipe implements PipeTransform {
  private readonly formatter = new Intl.NumberFormat('en-US', {
    notation: 'compact',
    maximumFractionDigits: 2,
  });

  transform(value: number | null | undefined): string {
    if (value === null || value === undefined || Number.isNaN(value)) {
      return '—';
    }
    return this.formatter.format(value);
  }
}
