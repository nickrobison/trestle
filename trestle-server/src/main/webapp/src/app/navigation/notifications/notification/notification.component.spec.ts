import {ComponentFixture, fakeAsync, TestBed, tick, waitForAsync} from '@angular/core/testing';

import {NotificationComponent} from './notification.component';
import {MaterialModule} from '../../../material/material.module';
import {MockStore, provideMockStore} from '@ngrx/store/testing';
import {NoopAnimationsModule} from '@angular/platform-browser/animations';
import {removeNotification} from '../../../actions/notification.actions';
import {initialAppState} from '../../../reducers';

describe('NotificationComponent', () => {
  let component: NotificationComponent;
  let fixture: ComponentFixture<NotificationComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ NotificationComponent ],
      providers: [provideMockStore({
        initialState: initialAppState
      })],
      imports: [MaterialModule, NoopAnimationsModule],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(NotificationComponent);
    component = fixture.componentInstance;
  });

  it('should be notification', () => {
    component.notification = {
      state: 'notification',
      msg: 'Hello there',
    };
    fixture.detectChanges();
    expect(component).toMatchSnapshot();
  });

  it('should be error', () => {
    component.notification = {
      state: 'error',
      error: new Error("I'm an error")
    };
    fixture.detectChanges();
    expect(component).toMatchSnapshot();
  });

  it('should dismiss', fakeAsync(() => {
    const triggerSpy = spyOn(component, 'triggerClose');
    component.notification = {
      state: 'notification',
      msg: 'Hello there',
    };
    fixture.detectChanges();
    tick(5000);
    expect(triggerSpy).toBeCalledTimes(1);
  }));
});
