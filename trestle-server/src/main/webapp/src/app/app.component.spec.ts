import { TestBed, waitForAsync } from '@angular/core/testing';
import {RouterTestingModule} from '@angular/router/testing';
import {AppComponent} from './app.component';
import {MaterialModule} from "./material/material.module";
import {AuthService} from "./user/authentication.service";
import {HttpClientTestingModule} from "@angular/common/http/testing";
import {NoopAnimationsModule} from "@angular/platform-browser/animations";
import {FontAwesomeModule} from '@fortawesome/angular-fontawesome';
import {NavigationModule} from './navigation/navigation.module';
import {provideMockStore} from '@ngrx/store/testing';

describe('AppComponent', () => {
  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        HttpClientTestingModule,
        NoopAnimationsModule,
        MaterialModule,
        FontAwesomeModule,
        NavigationModule
      ],
      providers: [AuthService, provideMockStore()],
      declarations: [
        AppComponent
      ],
    }).compileComponents();
  }));

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    expect(fixture).toMatchSnapshot();
  });
});
