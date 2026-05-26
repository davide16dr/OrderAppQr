import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminGlobalCatalog } from './admin-global-catalog';

describe('AdminGlobalCatalog', () => {
  let component: AdminGlobalCatalog;
  let fixture: ComponentFixture<AdminGlobalCatalog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminGlobalCatalog]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminGlobalCatalog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
