import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ListTicketComponent } from './list-ticket';
import { HttpClientTestingModule } from '@angular/common/http/testing'; // si tu as un service HTTP
import { NO_ERRORS_SCHEMA } from '@angular/core';

describe('ListTicketComponent', () => {
  let component: ListTicketComponent;
  let fixture: ComponentFixture<ListTicketComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ListTicketComponent],
      imports: [HttpClientTestingModule],  // si ton composant utilise HTTP
      schemas: [NO_ERRORS_SCHEMA] // pour ignorer les erreurs de sous-composants inconnus (optionnel)
    }).compileComponents();

    fixture = TestBed.createComponent(ListTicketComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
