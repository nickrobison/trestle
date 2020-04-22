import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {LoginComponent} from './login.component';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {RouterTestingModule} from '@angular/router/testing';
import {HttpClientTestingModule} from '@angular/common/http/testing';
import {NO_ERRORS_SCHEMA} from '@angular/core';
import {NoopAnimationsModule} from '@angular/platform-browser/animations';
import {MockStore, provideMockStore} from '@ngrx/store/testing';
import {MemoizedSelector} from '@ngrx/store';
import * as fromState from '../../reducers';
import {AuthService} from '../../user/authentication.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let mockStore: MockStore;
  let mockUsernameSelector: MemoizedSelector<fromState.State, Error>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [FormsModule, ReactiveFormsModule, RouterTestingModule, HttpClientTestingModule, NoopAnimationsModule],
      providers: [AuthService, provideMockStore()],
      declarations: [LoginComponent],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    mockStore = TestBed.inject(MockStore);
    mockUsernameSelector = mockStore.overrideSelector(
      fromState.selectErrorFromUser,
      null
    );

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(fixture).toMatchSnapshot();
  });

  it('should login correctly', () => {
    expect(component.errorMessage).toBe('');
    expect(component.errorState).toBe('inactive');
  });

  it('should render error message', () => {
      mockUsernameSelector.setResult(new Error('Incorrect Username or Password'));
      mockStore.refreshState();
      fixture.detectChanges();

      expect(component.errorMessage).toBe('Incorrect Username or Password');
      expect(component.errorState).toBe('active');
    }
  );
});
