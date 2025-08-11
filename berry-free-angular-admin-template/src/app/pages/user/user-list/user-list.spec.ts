import { ComponentFixture, TestBed } from '@angular/core/testing';import { UserService } from 'src/app/services/user';
import { of } from 'rxjs';
import { UserListComponent } from './user-list';

describe('UserListComponent', () => {  // Changed describe name
  let component: UserListComponent;    // Changed type
  let fixture: ComponentFixture<UserListComponent>;  // Changed type
  let userService: jasmine.SpyObj<UserService>;

  beforeEach(async () => {
    // Create a spy for UserService
    const userServiceSpy = jasmine.createSpyObj('UserService', ['getAllUsers']);

    await TestBed.configureTestingModule({
      imports: [UserListComponent],  // Changed to UserListComponent
      providers: [
        { provide: UserService, useValue: userServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UserListComponent);  // Changed component
    component = fixture.componentInstance;
    userService = TestBed.inject(UserService) as jasmine.SpyObj<UserService>;
    
    // Mock the service response
    userService.getAllUsers.and.returnValue(of([]));
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // Add more test cases here
});