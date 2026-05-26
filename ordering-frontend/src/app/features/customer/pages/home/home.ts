import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router } from '@angular/router';

interface FeatureCard {
  icon: string;
  title: string;
  description: string;
  accent: 'blue' | 'green' | 'purple' | 'yellow' | 'red' | 'gray';
}

interface StepCard {
  number: string;
  title: string;
  description: string;
  accent: 'blue' | 'green' | 'purple' | 'yellow';
}

interface Testimonial {
  text: string;
  author: string;
  role: string;
}

interface PricingPlan {
  name: string;
  subtitle: string;
  price: string;
  period: string;
  features: string[];
  buttonLabel: string;
  highlighted?: boolean;
  badge?: string;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home.html',
  styleUrl: './home.scss'
})
export class HomeComponent {
  navItems = ['Funzionalità', 'Come funziona', 'Prezzi'];

  sectors = [
    'Lidi Balneari',
    'Ristoranti',
    'Bar & Cocktail',
    'Hotel & Resort'
  ];

  stats = [
    { value: '< 30s', label: 'Tempo medio ordine' },
    { value: '100%', label: 'Dati isolati per cliente' },
    { value: '0', label: 'App da scaricare' },
    { value: '24/7', label: 'Disponibilità' }
  ];

  tenants = [
    { name: 'Lido Azzurro', meta: '24 postazioni', orders: '142 ordini oggi', color: 'blue' },
    { name: 'Bar Centrale', meta: '12 tavoli', orders: '89 ordini oggi', color: 'green' },
    { name: 'VIP Cabana', meta: '8 cabane', orders: '56 ordini oggi', color: 'purple' },
    { name: 'Ristorante', meta: '18 tavoli', orders: '103 ordini oggi', color: 'yellow' }
  ];

  features: FeatureCard[] = [
    {
      icon: '⌁',
      title: 'Ordini via QR Code',
      description: 'Il cliente scansiona il QR sul tavolo o ombrellone e ordina direttamente dal telefono. Zero app da scaricare.',
      accent: 'blue'
    },
    {
      icon: '▥',
      title: 'Dashboard in tempo reale',
      description: 'Lo staff gestisce tutti gli ordini da un pannello centralizzato con aggiornamenti istantanei.',
      accent: 'green'
    },
    {
      icon: '◉',
      title: 'Multi-sede indipendente',
      description: 'Ogni locale ha il proprio spazio isolato: menu, postazioni, statistiche e staff completamente separati.',
      accent: 'purple'
    },
    {
      icon: '⚡',
      title: 'Setup in minuti',
      description: 'Crea il menu, configura le postazioni e genera i QR code. Operativo in meno di 30 minuti.',
      accent: 'yellow'
    },
    {
      icon: '▯',
      title: 'Mobile-first',
      description: 'Interfaccia cliente ottimizzata per smartphone. Funziona perfettamente anche in ambienti affollati.',
      accent: 'red'
    },
    {
      icon: '◌',
      title: 'Dati isolati e sicuri',
      description: 'Ogni agenzia accede solo ai propri dati. Nessuna condivisione accidentale tra clienti diversi.',
      accent: 'gray'
    }
  ];

  steps: StepCard[] = [
    {
      number: '01',
      title: 'Registra la tua attività',
      description: 'Crea il tuo account in pochi minuti. Ogni locale ottiene un ambiente completamente indipendente.',
      accent: 'blue'
    },
    {
      number: '02',
      title: 'Configura menu e postazioni',
      description: 'Inserisci i prodotti, crea le categorie, aggiungi le postazioni (tavoli, ombrelloni, cabane).',
      accent: 'green'
    },
    {
      number: '03',
      title: 'Genera i QR code',
      description: 'Ogni postazione riceve un QR univoco. Stampalo e posizionalo. Fatto.',
      accent: 'purple'
    },
    {
      number: '04',
      title: 'Gestisci in tempo reale',
      description: 'Lo staff riceve gli ordini istantaneamente e li gestisce dalla dashboard.',
      accent: 'yellow'
    }
  ];

  testimonials: Testimonial[] = [
    {
      text: '"In un\'estate abbiamo triplicato la velocità di servizio. I clienti ordinano direttamente dall\'ombrellone e lo staff non deve più girare a raccogliere comande."',
      author: 'Marco Ferretti',
      role: 'Gestore Lido Azzurro'
    },
    {
      text: '"Il setup è stato velocissimo. In meno di un\'ora avevamo menu, postazioni e QR code pronti. I nostri clienti si sono adattati subito."',
      author: 'Sofia Marini',
      role: 'Proprietaria Bar Centrale'
    },
    {
      text: '"Gestire 3 punti ristoro con un\'unica piattaforma è una svolta. Ogni area ha il suo menu e il suo staff, tutto separato."',
      author: 'Luca Bianchi',
      role: 'Responsabile Hotel Mare'
    }
  ];

  plans: PricingPlan[] = [
    {
      name: 'Starter',
      subtitle: 'Perfetto per iniziare e testare il sistema.',
      price: '€0',
      period: 'per sempre',
      features: [
        '1 locale',
        'Fino a 20 postazioni',
        'Menu illimitato',
        'QR code inclusi'
      ],
      buttonLabel: 'Inizia gratis'
    },
    {
      name: 'Pro',
      subtitle: 'Per chi vuole il massimo delle funzionalità.',
      price: '€49',
      period: '/ mese',
      features: [
        'Locali illimitati',
        'Postazioni illimitate',
        'Statistiche avanzate',
        'Staff multi-ruolo',
        'Supporto prioritario'
      ],
      buttonLabel: 'Prova 14 giorni',
      highlighted: true,
      badge: 'Più popolare'
    },
    {
      name: 'Agenzia',
      subtitle: 'Per chi gestisce molti clienti in white-label.',
      price: '€199',
      period: '/ mese',
      features: [
        'Multi-tenant illimitato',
        'Brand personalizzato',
        'API & Webhooks',
        'Account manager dedicato'
      ],
      buttonLabel: 'Contattaci'
    }
  ];

  constructor(private router: Router) {}

  navigateToBusinessSignup(): void {
    this.router.navigate(['/public/business-signup']);
  }

  navigateToStaffLogin(): void {
    this.router.navigate(['/public/login']);
  }
}
