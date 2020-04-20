import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {LoginComponent} from './login.component';
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {AuthService} from "../../user/authentication.service";
import {RouterTestingModule} from "@angular/router/testing";
import {HttpClientTestingModule} from "@angular/common/http/testing";
import {NO_ERRORS_SCHEMA} from "@angular/core";
import {NoopAnimationsModule} from "@angular/platform-browser/animations";
import {throwError} from "rxjs";
import {HttpResponse} from "@angular/common/http";

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authService: AuthService;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [FormsModule, ReactiveFormsModule, RouterTestingModule, HttpClientTestingModule, NoopAnimationsModule],
      providers: [AuthService],
      declarations: [LoginComponent],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();

    authService = TestBed.inject(AuthService);
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(fixture).toMatchSnapshot();
  });

  it('should render error message', () => {

      const resp = new HttpResponse({
        status: 401
      });
      spyOn(authService, "login").and.returnValue(throwError(resp));
      component.login({username: "hello", password: "password"});
      fixture.detectChanges();

      expect(component.errorMessage).toBe("Incorrect Username or Password");
      expect(component.errorState).toBe("active");
      expect(fixture).toMatchSnapshot();
    }
  )
});
