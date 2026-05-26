import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { CustomerMenuViewModel } from '../models/customer.types';

export interface CustomerMenuQuery {
  token?: string | null;
  tenant?: string | null;
  location?: string | null;
}

@Injectable({ providedIn: 'root' })
export class CustomerMenu {
  private readonly http = inject(HttpClient);

  getMenu(query: CustomerMenuQuery): Observable<CustomerMenuViewModel> {
    // Endpoint placeholder: when backend is ready, swap this to the real route.
    const url = `${environment.apiUrl}/api/public/customer/menu`;

    return this.http
      .get<CustomerMenuViewModel>(url, {
        params: {
          ...(query.token ? { token: query.token } : {}),
          ...(query.tenant ? { tenant: query.tenant } : {}),
          ...(query.location ? { location: query.location } : {}),
        },
      })
      .pipe(catchError(() => of(this.getMockMenu(query))));
  }

  private getMockMenu(query: CustomerMenuQuery): CustomerMenuViewModel {
    const businessName = query.tenant ? String(query.tenant) : 'Lido Azzurro';
    const locationTitle = query.location ? String(query.location) : 'Ombrellone 42';

    return {
      context: {
        businessName,
        businessAvatarText: businessName.trim().charAt(0).toUpperCase() || 'O',
        businessLogoDataUrl: null,
        locationTitle,
        locationSubtitle: 'Zona Spiaggia - Prima fila',
        statusLabel: 'Attivo',
        statusVariant: 'active',
      },
      categories: [
        { id: 'bevande', name: 'Bevande', icon: '🍹' },
        { id: 'cocktail', name: 'Cocktail', icon: '🍸' },
        { id: 'cibo', name: 'Cibo', icon: '🍕' },
        { id: 'snack', name: 'Snack', icon: '🥨' },
        { id: 'dolci', name: 'Dolci', icon: '🍰' },
      ],
      products: [
        {
          id: 'pizza',
          categoryId: 'cibo',
          name: 'Pizza Margherita',
          description: 'Pomodoro, mozzarella, basilico',
          priceCents: 800,
          icon: '🍕',
          modifierGroups: [
            {
              id: 100,
              name: 'Taglia',
              required: false,
              minSelectable: 0,
              maxSelectable: 1,
              options: [
                { id: 1, name: 'Piccola (20cm)', priceCents: 700 },
                { id: 2, name: 'Media (30cm)', priceCents: 900 },
                { id: 3, name: 'Grande (40cm)', priceCents: 1200 },
              ],
            },
            {
              id: 101,
              name: 'Extra',
              required: false,
              minSelectable: 0,
              maxSelectable: 5,
              options: [
                { id: 10, name: 'Mozzarella', priceDeltaCents: 100 },
                { id: 11, name: 'Pancetta', priceDeltaCents: 150 },
                { id: 12, name: 'Funghi', priceDeltaCents: 80 },
              ],
            },
          ],
        },
        {
          id: 'birra',
          categoryId: 'bevande',
          name: 'Birra',
          description: 'Birra artigianale',
          priceCents: 500,
          icon: '🍺',
          modifierGroups: [
            {
              id: 200,
              name: 'Taglia',
              required: false,
              minSelectable: 0,
              maxSelectable: 1,
              options: [
                { id: 50, name: 'Piccola (20cl)', priceCents: 300 },
                { id: 51, name: 'Media (40cl)', priceCents: 500 },
                { id: 52, name: 'Grande (66cl)', priceCents: 700 },
              ],
            },
          ],
        },
        {
          id: 'aperol-spriz',
          categoryId: 'cocktail',
          name: 'aperol Spriz',
          description: 'Aperitivo classico',
          priceCents: 800,
          icon: '🍹',
          modifierGroups: [
            {
              id: 201,
              name: 'Ghiaccio',
              required: false,
              minSelectable: 0,
              maxSelectable: 1,
              options: [
                { id: 52, name: 'No ghiaccio', priceDeltaCents: 0 },
                { id: 53, name: 'Con ghiaccio', priceDeltaCents: 0 },
              ],
            },
            {
              id: 202,
              name: 'Aggiunte',
              required: false,
              minSelectable: 0,
              maxSelectable: 2,
              options: [
                { id: 54, name: 'Fetta di limone', priceDeltaCents: 0 },
                { id: 55, name: 'Fetta di arancia', priceDeltaCents: 0 },
                { id: 56, name: 'Extra Prosecco', priceDeltaCents: 150 },
              ],
            },
          ],
        },
        {
          id: 'acqua-50',
          categoryId: 'bevande',
          name: 'Acqua Naturale',
          description: 'Bottiglia 50cl',
          priceCents: 200,
          icon: '🍽️',
        },
      ],
    };
  }
}
