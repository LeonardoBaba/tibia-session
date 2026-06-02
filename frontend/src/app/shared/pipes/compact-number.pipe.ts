import { Pipe, PipeTransform } from '@angular/core';

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
