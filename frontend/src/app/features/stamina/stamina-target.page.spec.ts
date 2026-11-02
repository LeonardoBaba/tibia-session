import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { StaminaTargetPage } from './stamina-target.page';

describe('StaminaTargetPage', () => {
  let fixture: ComponentFixture<StaminaTargetPage>;
  let el: HTMLElement;
  // Protected signals are read through a cast.
  let page: any;

  beforeEach(async () => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date(2026, 10, 2, 10, 0)); // Mon, Nov 02 2026, 10:00 local time

    await TestBed.configureTestingModule({
      imports: [StaminaTargetPage],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(StaminaTargetPage);
    fixture.detectChanges();
    el = fixture.nativeElement;
    page = fixture.componentInstance;
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  function enter(selector: string, value: string): HTMLInputElement {
    const input = el.querySelector<HTMLInputElement>(selector)!;
    input.value = value;
    input.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    return input;
  }

  function buttonLabelled(label: string): HTMLButtonElement {
    return Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.trim() === label)!;
  }

  it('defaults to 30:00 → 42:00 counted from now', () => {
    // 9:00 below 39:00 at 3 min each + 3:00 above it at 6 min each = 45h offline.
    expect(page.result().offline).toBe('1d 21h 0m');
    expect(el.textContent).toContain('Reaches 42:00 at');
    expect(el.textContent).toContain('07:00');
    expect(el.textContent).toContain('Wednesday, Nov 04');
    expect(el.textContent).toContain('Live — follows the current time');
  });

  it('recalculates from typed stamina and reformats the boxes', () => {
    const current = enter('#current-stamina', '38');
    enter('#target-stamina', '39:00');
    expect(current.value).toBe('38:00');
    expect(page.result().offline).toBe('3h 0m');
    expect(el.textContent).toContain('13:00');
    expect(el.textContent).toContain('Today, Nov 02');
  });

  it('keeps the previous value when the text does not parse', () => {
    const current = enter('#current-stamina', 'abc');
    expect(current.value).toBe('30:00');
    expect(page.currentMin()).toBe(1800);
  });

  it('flags a target below the current stamina', () => {
    enter('#target-stamina', '20:00');
    expect(el.querySelector('[role="alert"]')?.textContent).toContain(
      'Must be above your current 30:00',
    );
    expect(el.querySelector('#target-stamina')?.getAttribute('aria-invalid')).toBe('true');
    expect(el.textContent).toContain('Target is below current');
  });

  it('says when the target is already reached', () => {
    enter('#target-stamina', '30:00');
    expect(el.textContent).toContain('Already at 30:00');
  });

  it('counts from an edited start time until Now is pressed', () => {
    enter('#offline-from', '2026-11-02T08:00');
    expect(page.followingNow()).toBe(false);
    expect(el.textContent).toContain('2h 0m ago');
    expect(page.result().time).toBe('05:00');
    expect(page.result().fromNow).toBe('in 1d 19h 0m');

    buttonLabelled('Now').click();
    fixture.detectChanges();
    expect(page.followingNow()).toBe(true);
    expect(page.result().time).toBe('07:00');
  });

  it('starts with the breakdown collapsed and opens it on click', () => {
    const toggle = el.querySelector<HTMLButtonElement>('[aria-controls="target-breakdown"]')!;
    const body = el.querySelector<HTMLElement>('#target-breakdown')!;
    expect(toggle.getAttribute('aria-expanded')).toBe('false');
    expect(body.hidden).toBe(true);

    toggle.click();
    fixture.detectChanges();
    expect(toggle.getAttribute('aria-expanded')).toBe('true');
    expect(body.hidden).toBe(false);
    expect(body.textContent).toContain('Normal zone');
    expect(body.textContent).toContain('+50% XP bonus');
    expect(body.textContent).toContain('1d 3h 0m');
  });
});
