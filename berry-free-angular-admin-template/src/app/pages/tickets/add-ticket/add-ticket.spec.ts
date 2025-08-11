import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AddTicketComponent } from './add-ticket'; // <-- Correction ici

describe('AddTicketComponent', () => {
  let component: AddTicketComponent; // <-- Correction ici
  let fixture: ComponentFixture<AddTicketComponent>; // <-- Correction ici

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AddTicketComponent] // <-- Correction ici
    })
    .compileComponents();

    fixture = TestBed.createComponent(AddTicketComponent); // <-- Correction ici
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});