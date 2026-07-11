import { Component, Input, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { SubscriptionService, SubscriptionDto } from '../../services/subscription.service';
import { AuthUser } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-subscription-management',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './subscription-management.component.html',
  styleUrls: ['./subscription-management.component.scss']
})
export class SubscriptionManagementComponent implements OnInit {
  @Input() currentUser: AuthUser | null = null;

  sub = signal<SubscriptionDto | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);
  actionLoading = signal(false);
  confirmAction = signal<'cancel' | 'changeBilling' | null>(null);

  constructor(private subscriptionService: SubscriptionService) {}

  ngOnInit(): void {
    this.loadSubscription();
  }

  loadSubscription(): void {
    const tid = this.currentUser?.tenantId;
    if (!tid) { this.loading.set(false); return; }
    this.loading.set(true);
    this.error.set(null);
    this.subscriptionService.getCurrent(tid).subscribe({
      next: (dto) => { this.sub.set(dto); this.loading.set(false); },
      error: () => { this.error.set('Impossibile caricare i dati dell\'abbonamento.'); this.loading.set(false); }
    });
  }

  get isActive(): boolean    { return this.sub()?.status === 'ACTIVE'; }
  get isPastDue(): boolean   { return this.sub()?.status === 'PAST_DUE'; }
  get isCancelled(): boolean { return this.sub()?.status === 'CANCELLED'; }
  get willCancel(): boolean  { return this.sub()?.cancelAtPeriodEnd === true; }

  get currentPrice(): number | null {
    const s = this.sub();
    if (!s) return null;
    return s.billingCycle === 'YEARLY' ? s.priceYearly : s.priceMonthly;
  }

  get targetCycle(): string {
    return this.sub()?.billingCycle === 'MONTHLY' ? 'YEARLY' : 'MONTHLY';
  }

  get targetPrice(): number | null {
    const s = this.sub();
    if (!s) return null;
    return s.billingCycle === 'MONTHLY' ? s.priceYearly : s.priceMonthly;
  }

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      ACTIVE: 'Attivo', PAST_DUE: 'Scaduto', CANCELLED: 'Cancellato',
      PENDING: 'In attesa', PAST_DUE_GRACE: 'In ritardo'
    };
    return map[status] ?? status;
  }

  openPortal(): void {
    const tid = this.currentUser?.tenantId;
    if (!tid) return;
    const returnUrl = window.location.href;
    this.actionLoading.set(true);
    this.subscriptionService.createPortalSession(tid, returnUrl).subscribe({
      next: ({ url }) => { window.location.href = url; },
      error: () => { this.actionLoading.set(false); this.error.set('Impossibile aprire il portale di pagamento.'); }
    });
  }

  confirmCancel(): void { this.confirmAction.set('cancel'); }
  confirmChangeBilling(): void { this.confirmAction.set('changeBilling'); }
  dismissConfirm(): void { this.confirmAction.set(null); }

  executeAction(): void {
    const action = this.confirmAction();
    const tid = this.currentUser?.tenantId;
    if (!action || !tid) return;
    this.actionLoading.set(true);
    this.confirmAction.set(null);
    const request$ = action === 'cancel'
      ? this.subscriptionService.cancel(tid)
      : this.subscriptionService.changeBilling(tid, this.targetCycle);
    request$.subscribe({
      next: (dto) => { this.sub.set(dto); this.actionLoading.set(false); },
      error: (err) => {
        this.error.set(err.error?.message ?? 'Operazione non riuscita.');
        this.actionLoading.set(false);
      }
    });
  }

  reactivate(): void {
    const tid = this.currentUser?.tenantId;
    if (!tid) return;
    this.actionLoading.set(true);
    this.subscriptionService.reactivate(tid).subscribe({
      next: (dto) => { this.sub.set(dto); this.actionLoading.set(false); },
      error: (err) => { this.error.set(err.error?.message ?? 'Riattivazione non riuscita.'); this.actionLoading.set(false); }
    });
  }
}
