import { TestBed } from '@angular/core/testing';

import { TenantContext } from './tenant-context';

describe('TenantContext', () => {
  let service: TenantContext;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TenantContext);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
