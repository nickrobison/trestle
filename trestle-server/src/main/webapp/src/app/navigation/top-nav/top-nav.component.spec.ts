import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import {TopNavComponent} from './top-nav.component';
import {MockStore, provideMockStore} from '@ngrx/store/testing';
import {MemoizedSelector} from '@ngrx/store';
import * as fromState from '../../reducers';
import {Privileges, TrestleUser} from '../../user/trestle-user';
import {MaterialModule} from '../../material/material.module';
import {FontAwesomeModule} from '@fortawesome/angular-fontawesome';
import {createMockUser} from '../../../test.helpers';
import {UserIconComponent} from '../user-icon/user-icon.component';

describe('TopNavComponent', () => {
  let component: TopNavComponent;
  let fixture: ComponentFixture<TopNavComponent>;
  let mockStore: MockStore;
  let mockUsernameSelector: MemoizedSelector<fromState.State, TrestleUser>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [TopNavComponent, UserIconComponent],
      imports: [MaterialModule, FontAwesomeModule],
      providers: [provideMockStore()]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TopNavComponent);
    component = fixture.componentInstance;
    mockStore = TestBed.inject(MockStore);
    mockUsernameSelector = mockStore.overrideSelector(
      fromState.selectUserFromUser,
      null
    );
    fixture.detectChanges();
  });

  it('should should be logged out', () => {
    expect(component).toMatchSnapshot();
  });

  it('should should be logged in', () => {
    mockUsernameSelector.setResult(createMockUser(Privileges.ADMIN + Privileges.USER));
    mockStore.refreshState();
    fixture.detectChanges();
    expect(component).toMatchSnapshot();
  });
});
