import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { UserIconComponent } from './user-icon.component';
import {MockStore, provideMockStore} from '@ngrx/store/testing';
import {MemoizedSelector} from '@ngrx/store';
import * as fromState from '../../reducers';
import {Privileges, TrestleUser} from '../../user/trestle-user';
import {createMockUser} from '../../../test.helpers';
import {FontAwesomeModule} from '@fortawesome/angular-fontawesome';
import {logout} from '../../actions/auth.actions';

describe('UserIconComponent', () => {
  let component: UserIconComponent;
  let fixture: ComponentFixture<UserIconComponent>;
  let mockStore: MockStore;
  let mockUsernameSelector: MemoizedSelector<fromState.State, TrestleUser>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ UserIconComponent ],
      imports: [FontAwesomeModule],
      providers: [provideMockStore()]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(UserIconComponent);
    component = fixture.componentInstance;
    mockStore = TestBed.inject(MockStore);
    mockUsernameSelector = mockStore.overrideSelector(
      fromState.selectUserFromUser,
      null
    );
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toMatchSnapshot();
  });

  it('should be logged in', () => {
    mockUsernameSelector.setResult(createMockUser(Privileges.ADMIN + Privileges.USER));
    mockStore.refreshState();
    fixture.detectChanges();
    expect(component).toMatchSnapshot();
  })

  it('should dispatch on logout', () => {
    const dispatchSpy = spyOn(mockStore, 'dispatch');
    component.logout();

    expect(dispatchSpy).toBeCalledWith(logout());
  })
});
