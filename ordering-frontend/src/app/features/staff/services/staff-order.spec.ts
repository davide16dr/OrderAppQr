import { TestBed } from '@angular/core/testing';

import { StaffOrder } from './staff-order';

describe('StaffOrder', () => {
  let service: StaffOrder;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(StaffOrder);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
