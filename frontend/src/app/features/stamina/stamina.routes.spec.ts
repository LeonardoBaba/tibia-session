import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { StaminaTargetPage } from './stamina-target.page';
import { StaminaPage } from './stamina.page';
import { STAMINA_ROUTES } from './stamina.routes';

describe('STAMINA_ROUTES', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([{ path: 'stamina', children: STAMINA_ROUTES }])],
    });
  });

  it('opens the target calculator from /stamina', async () => {
    const harness = await RouterTestingHarness.create();
    const page = await harness.navigateByUrl('/stamina', StaminaTargetPage);
    expect(page).toBeInstanceOf(StaminaTargetPage);
    expect(TestBed.inject(Router).url).toBe('/stamina/target');
  });

  it('serves the recovery simulator at /stamina/simulator', async () => {
    const harness = await RouterTestingHarness.create();
    const page = await harness.navigateByUrl('/stamina/simulator', StaminaPage);
    expect(page).toBeInstanceOf(StaminaPage);
  });
});
