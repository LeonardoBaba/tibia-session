import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { AppShell } from './app-shell';

@Component({ template: '' })
class BlankPage {}

describe('AppShell', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          {
            path: '',
            component: AppShell,
            children: [
              { path: 'hunts', component: BlankPage },
              { path: 'compare', component: BlankPage },
              { path: 'stamina', component: BlankPage },
              { path: 'stamina/target', component: BlankPage },
            ],
          },
        ]),
      ],
    });
  });

  // The active styles hang off aria-current, so it must land on exactly the current section.
  function currentLinks(root: HTMLElement): string[] {
    return Array.from(root.querySelectorAll('a[aria-current="page"]')).map((a) =>
      a.textContent!.trim(),
    );
  }

  it('marks only the current section in the main nav', async () => {
    const harness = await RouterTestingHarness.create('/compare');
    expect(currentLinks(harness.fixture.nativeElement)).toEqual(['Comparisons']);
  });

  it('keeps Stamina current on its sub-pages, in the mobile menu too', async () => {
    const harness = await RouterTestingHarness.create('/stamina/target');
    const root = harness.fixture.nativeElement as HTMLElement;
    root.querySelector<HTMLButtonElement>('button[aria-label="Toggle navigation menu"]')!.click();
    harness.detectChanges();
    // RouterLinkActive sets aria-current on newly rendered links in a microtask.
    await harness.fixture.whenStable();
    expect(currentLinks(root)).toEqual(['Stamina', 'Stamina']);
  });
});
