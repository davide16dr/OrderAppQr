import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class DashboardSyncService {
  private readonly versionSignal = signal(0);
  readonly version = this.versionSignal.asReadonly();

  notifyDataChanged(): void {
    this.versionSignal.update((v) => v + 1);
  }
}
