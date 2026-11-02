import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

/** Switches between the stamina tools: the target calculator and the recovery simulator. */
@Component({
  selector: 'app-stamina-tabs',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './stamina-tabs.html',
  host: { class: 'block' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StaminaTabs {}
