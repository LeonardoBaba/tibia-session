import { ChangeDetectionStrategy, Component, inject, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';

export interface HuntFiltersValue {
  name: string;
  player: string;
  startDate: string;
  endDate: string;
}

@Component({
  selector: 'app-hunt-filters',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './hunt-filters.html',
  styleUrl: './hunt-filters.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HuntFilters {
  readonly apply = output<HuntFiltersValue>();

  protected readonly form = inject(FormBuilder).nonNullable.group({
    name: '',
    player: '',
    startDate: '',
    endDate: '',
  });

  protected onApply(): void {
    this.apply.emit(this.form.getRawValue());
  }
}
