import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import {SidebarComponent} from './sidebar.component';
import {MemoizedSelector} from '@ngrx/store';
import {MockStore, provideMockStore} from '@ngrx/store/testing';
import {Privileges, TrestleUser} from '../../user/trestle-user';
import {By} from '@angular/platform-browser';
import {MaterialModule} from '../../material/material.module';
import {RouterTestingModule} from '@angular/router/testing';
import {NoopAnimationsModule} from '@angular/platform-browser/animations';
import {createMockUser} from '../../../test.helpers';
import {State} from '../../reducers';
import {selectUserFromUser} from '../../reducers/auth.reducers';

describe('SidebarComponent', () => {
  let component: SidebarComponent;
  let fixture: ComponentFixture<SidebarComponent>;
  let mockStore: MockStore;
  let mockUsernameSelector: MemoizedSelector<State, TrestleUser>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [MaterialModule, RouterTestingModule, NoopAnimationsModule],
      declarations: [SidebarComponent],
      providers: [
        provideMockStore()
      ]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SidebarComponent);
    component = fixture.componentInstance;
    mockStore = TestBed.inject(MockStore);
    mockUsernameSelector = mockStore.overrideSelector(
      selectUserFromUser,
      null
    );
    fixture.detectChanges();
  });

  it('should should have no routes', () => {
    // expect(component).toMatchSnapshot();
  });

  it('should have user options', () => {
    mockUsernameSelector.setResult(createMockUser(Privileges.USER));
    mockStore.refreshState();
    fixture.detectChanges();
    expect(fixture.debugElement.queryAll(By.css('.mat-list-item-content'))).toHaveLength(5);
  });

  it('should have admin options', () => {
    mockUsernameSelector.setResult(createMockUser(Privileges.ADMIN + Privileges.USER));
    mockStore.refreshState();
    fixture.detectChanges();
    expect(fixture.debugElement.queryAll(By.css('.mat-list-item-content'))).toHaveLength(8);
  });

  it('should have dba options', () => {
    mockUsernameSelector.setResult(createMockUser(Privileges.DBA + Privileges.ADMIN + Privileges.USER));
    mockStore.refreshState();
    fixture.detectChanges();
    expect(fixture.debugElement.queryAll(By.css('.mat-list-item-content'))).toHaveLength(9);
  });
});
