import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { LocationMenu } from './location-menu';
import { CustomerMenu } from '../../services/customer-menu';
import { CustomerMenuViewModel } from '../../models/customer.types';

describe('LocationMenu', () => {
  let component: LocationMenu;
  let fixture: ComponentFixture<LocationMenu>;

  const mockVm: CustomerMenuViewModel = {
    context: {
      businessName: 'Test',
      locationTitle: 'Postazione',
      statusLabel: 'Attivo',
      statusVariant: 'active',
    },
    categories: [{ id: 'c1', name: 'Cat', icon: '🍹' }],
    products: [
      { id: 'p1', categoryId: 'c1', name: 'Prodotto', priceCents: 100 },
    ],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LocationMenu],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            queryParamMap: of(convertToParamMap({}))
          }
        },
        {
          provide: CustomerMenu,
          useValue: {
            getMenu: () => of(mockVm)
          }
        }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LocationMenu);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
