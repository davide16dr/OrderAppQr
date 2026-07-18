import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

interface Testimonial {
  text: string;
  author: string;
  role: string;
  initials: string;
}

interface PricingPlan {
  name: string;
  subtitle: string;
  price: string;
  period: string;
  annualNote?: string;
  features: string[];
  buttonLabel: string;
  highlighted?: boolean;
  badge?: string;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.scss'
})
export class HomeComponent {
  stats = [
    { value: '< 30s', label: 'Velocizza il tuo servizio' },
    { value: '100%',  label: 'Aumenta il numero di ordini' },
    { value: '0',     label: 'App da scaricare' },
    { value: '24/7',  label: 'Disponibilità' }
  ];

  tenants = [
    { name: 'Lido Azzurro', meta: '24 postazioni', orders: '142 ordini oggi', color: 'blue' },
    { name: 'Bar Centrale',  meta: '12 tavoli',     orders: '89 ordini oggi',  color: 'green' },
    { name: 'The Fox Pub',   meta: '10 tavoli',     orders: '56 ordini oggi',  color: 'purple' },
    { name: 'Ristorante',    meta: '18 tavoli',     orders: '103 ordini oggi', color: 'yellow' }
  ];

  steps = [
    { number: '01', title: 'Registra la tua attività',    description: 'Crea il tuo account in pochi minuti. Ogni locale ottiene un ambiente completamente indipendente.', accent: 'blue' },
    { number: '02', title: 'Configura menu e postazioni', description: 'Inserisci i prodotti, crea le categorie, aggiungi le postazioni (tavoli, ombrelloni, cabane).', accent: 'green' },
    { number: '03', title: 'Genera i QR code',            description: 'Ogni postazione riceve un QR univoco. Stampalo e posizionalo. Fatto.', accent: 'purple' },
    { number: '04', title: 'Gestisci in tempo reale',     description: 'Lo staff riceve gli ordini istantaneamente e li gestisce dalla dashboard.', accent: 'yellow' }
  ];

  testimonials: Testimonial[] = [
    {
      text: 'In un\'estate abbiamo triplicato la velocità di servizio. I clienti ordinano direttamente dall\'ombrellone e lo staff non deve più girare a raccogliere comande.',
      author: 'Marco Ferretti', role: 'Gestore Lido Azzurro', initials: 'MF'
    },
    {
      text: 'Il setup è stato velocissimo. In meno di un\'ora avevamo menu, postazioni e QR code pronti. I nostri clienti si sono adattati subito.',
      author: 'Sofia Marini', role: 'Proprietaria Bar Centrale', initials: 'SM'
    },
    {
      text: 'Gestire 3 punti ristoro con un\'unica piattaforma è una svolta. Ogni area ha il suo menu e il suo staff, tutto separato.',
      author: 'Luca Bianchi', role: 'Responsabile Hotel Mare', initials: 'LB'
    }
  ];

  plans: PricingPlan[] = [
    {
      name: 'OrderApp Pro',
      subtitle: 'Tutto il necessario per il tuo locale. Nessun costo nascosto.',
      price: '€59',
      period: '/ mese',
      annualNote: 'oppure €590/anno · risparmi €118',
      features: [
        'Postazioni illimitate',
        'Menu illimitato',
        'QR code inclusi',
        'Dashboard in tempo reale',
        'Statistiche avanzate',
        'Staff multi-ruolo',
        'Supporto dedicato'
      ],
      buttonLabel: 'Inizia la prova',
      highlighted: true,
      badge: 'Tutto incluso'
    }
  ];

  private readonly authService = inject(AuthService);
  constructor(private router: Router) {}

  navigateToBusinessSignup(): void {
    if (this.authService.isAuthenticatedSync() && !this.authService.currentUser()?.isDemo) {
      this.router.navigate([this.authService.getDefaultRouteForCurrentUser()]);
    } else {
      this.router.navigate(['/public/business-signup']);
    }
  }

  navigateToStaffLogin(): void {
    if (this.authService.isAuthenticatedSync()) {
      this.router.navigate([this.authService.getDefaultRouteForCurrentUser()]);
    } else {
      this.router.navigate(['/public/login']);
    }
  }

  navigateToDemo(): void { this.router.navigate(['/public/demo']); }
}
