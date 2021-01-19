import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';

import {TopNavComponent} from './top-nav.component';
import {provideMockStore} from '@ngrx/store/testing';
import {MaterialModule} from '../../material/material.module';
import {FontAwesomeModule} from '@fortawesome/angular-fontawesome';
import {UserIconComponent} from '../user-icon/user-icon.component';
import {NotificationCenterComponent} from '../notifications/notification-center/notification-center.component';
import {NotificationComponent} from '../notifications/notification/notification.component';
import {BrowserDynamicTestingModule} from '@angular/platform-browser-dynamic/testing';

describe('TopNavComponent', () => {
  let component: TopNavComponent;
  let fixture: ComponentFixture<TopNavComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [TopNavComponent, UserIconComponent, NotificationCenterComponent, NotificationComponent],
      imports: [MaterialModule, FontAwesomeModule],
      providers: [provideMockStore()]
    })
      .overrideModule(BrowserDynamicTestingModule, {
        set: {
          entryComponents: [NotificationCenterComponent]
        }
      })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TopNavComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should should be logged out', () => {
    expect(component).toBeTruthy();
  });
});
