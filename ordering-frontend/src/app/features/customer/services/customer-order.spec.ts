import { TestBed } from '@angular/core/testing';

import { CustomerOrder } from './customer-order';

describe('CustomerOrder', () => {
  let service: CustomerOrder;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CustomerOrder);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
