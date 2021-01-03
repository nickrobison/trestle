import {ComponentFixture, TestBed} from '@angular/core/testing';

import {NotificationComponent} from './notification.component';

describe('ToastComponent', () => {
  let component: NotificationComponent;
  let fixture: ComponentFixture<NotificationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ NotificationComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(NotificationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
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
      state: 'notification',
      msg: 'Hello there',
    };
    fixture.detectChanges();
    expect(component).toMatchSnapshot();
  });
});
