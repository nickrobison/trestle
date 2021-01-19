import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';

import {NotificationCenterComponent} from './notification-center.component';
import {NotificationComponent} from '../notification/notification.component';
import {MaterialModule} from '../../../material/material.module';
import {MockStore, provideMockStore} from '@ngrx/store/testing';
import {MemoizedSelector} from '@ngrx/store';
import {State} from '../../../reducers';
import {
  selectNotificationsFromNotifications,
  TrestleError,
  TrestleMessage,
  TrestleNotification
} from '../../../reducers/notification.reducers';

describe('NotificationCenterComponent', () => {
  let component: NotificationCenterComponent;
  let fixture: ComponentFixture<NotificationCenterComponent>;
  let mockStore: MockStore;
  let mockNotificationSelector: MemoizedSelector<State, TrestleNotification[]>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ NotificationCenterComponent, NotificationComponent ],
      providers: [provideMockStore()],
      imports: [MaterialModule],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(NotificationCenterComponent);
    component = fixture.componentInstance;
    mockStore = TestBed.inject(MockStore);
    mockNotificationSelector = mockStore.overrideSelector(
      selectNotificationsFromNotifications,
      []
    );
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(fixture).toMatchSnapshot();
  });

  it('should have notifications', () => {
    const tError: TrestleError = {
      state: 'error',
      error: new Error('Cannot search for individuals'),
    };
    const tMessage: TrestleMessage = {
      state: 'notification',
      msg: 'Hello, Im a message'
    };
    const expected = [tError, tMessage];
    mockNotificationSelector.setResult(expected);
    fixture.detectChanges();
    component.notifications.subscribe(notifications => {
      expect(notifications).toEqual(expected);
    });
    expect(fixture).toMatchSnapshot();
  });
});
