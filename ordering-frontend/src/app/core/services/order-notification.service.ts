import { Injectable, OnDestroy } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class OrderNotificationService implements OnDestroy {
  private ctx: AudioContext | null = null;
  private readonly unlockHandler = (): void => this.unlockContext();

  constructor() {
    document.addEventListener('click', this.unlockHandler, { once: true });
    document.addEventListener('keydown', this.unlockHandler, { once: true });
  }

  private getContext(): AudioContext {
    if (!this.ctx) {
      this.ctx = new AudioContext();
    }
    return this.ctx;
  }

  private unlockContext(): void {
    const ctx = this.getContext();
    if (ctx.state === 'suspended') {
      ctx.resume().catch(() => undefined);
    }
  }

  playNewOrder(): void {
    const ctx = this.getContext();
    const resume = ctx.state === 'suspended' ? ctx.resume() : Promise.resolve();
    resume.then(() => this.scheduleDing(ctx)).catch(() => undefined);
  }

  private scheduleDing(ctx: AudioContext): void {
    const now = ctx.currentTime;
    this.tone(ctx, 880, now, 0.40, 0.35);
    this.tone(ctx, 1108, now + 0.18, 0.40, 0.30);
  }

  private tone(ctx: AudioContext, freq: number, start: number, duration: number, gain: number): void {
    const osc = ctx.createOscillator();
    const env = ctx.createGain();
    osc.connect(env);
    env.connect(ctx.destination);
    osc.type = 'sine';
    osc.frequency.setValueAtTime(freq, start);
    env.gain.setValueAtTime(0, start);
    env.gain.linearRampToValueAtTime(gain, start + 0.01);
    env.gain.exponentialRampToValueAtTime(0.001, start + duration);
    osc.start(start);
    osc.stop(start + duration);
  }

  ngOnDestroy(): void {
    document.removeEventListener('click', this.unlockHandler);
    document.removeEventListener('keydown', this.unlockHandler);
    this.ctx?.close().catch(() => undefined);
    this.ctx = null;
  }
}
