import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  ViewChild,
  computed,
  effect,
  input,
  output,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-editable-text',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './editable-text.html',
  styleUrl: './editable-text.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditableText implements AfterViewInit {
  readonly value = input<string | null>(null);
  readonly placeholder = input<string>('Click to edit');
  readonly multiline = input<boolean>(false);
  readonly displayClass = input<string>('');
  readonly inputClass = input<string>('');
  readonly allowEmpty = input<boolean>(true);

  readonly save = output<string>();

  protected readonly editing = signal(false);
  protected readonly draft = signal('');
  protected readonly hasValue = computed(() => {
    const v = this.value();
    return v !== null && v !== undefined && v.length > 0;
  });

  @ViewChild('field') private fieldRef?: ElementRef<HTMLInputElement | HTMLTextAreaElement>;

  constructor() {
    effect(() => {
      if (this.editing() && this.fieldRef) {
        queueMicrotask(() => {
          this.fieldRef!.nativeElement.focus();
          this.fieldRef!.nativeElement.select();
        });
      }
    });
  }

  ngAfterViewInit(): void {}

  protected startEdit(): void {
    this.draft.set(this.value() ?? '');
    this.editing.set(true);
  }

  protected cancel(): void {
    this.editing.set(false);
  }

  protected commit(): void {
    const next = this.draft();
    const current = this.value() ?? '';
    if (next === current) {
      this.editing.set(false);
      return;
    }
    if (!this.allowEmpty() && next.trim().length === 0) {
      this.editing.set(false);
      return;
    }
    this.save.emit(next);
    this.editing.set(false);
  }

  protected onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      event.preventDefault();
      this.cancel();
      return;
    }
    if (event.key === 'Enter') {
      if (this.multiline()) {
        if (event.ctrlKey || event.metaKey) {
          event.preventDefault();
          this.commit();
        }
      } else {
        event.preventDefault();
        this.commit();
      }
    }
  }

  protected onDraftChange(event: Event): void {
    const target = event.target as HTMLInputElement | HTMLTextAreaElement;
    this.draft.set(target.value);
  }
}
