import {ChangeDetectorRef, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {MediaMatcher} from '@angular/cdk/layout';
import {selectUserFromUser, State} from '../../reducers';
import {select, Store} from '@ngrx/store';
import {Observable} from 'rxjs';
import {TrestleUser, Privileges} from '../../user/trestle-user';
import {tap} from 'rxjs/operators';
import {MatDrawerToggleResult, MatSidenav} from '@angular/material/sidenav';

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent implements OnInit, OnDestroy {

  // We need this in order to access the Privileges enum from the template
  public Privileges = Privileges;
  public user: Observable<TrestleUser>;
  public mobileQuery: MediaQueryList;
  private readonly mobileQueryListener: () => void;
  @ViewChild("sidenav")
  private sideNav: MatSidenav;

  constructor(changeDetectorRef: ChangeDetectorRef,
              media: MediaMatcher, private store: Store<State>) {
    this.mobileQuery = media.matchMedia('(max-width: 800px)');
    this.mobileQueryListener = () => changeDetectorRef.detectChanges();
    this.mobileQuery.addEventListener('change', this.mobileQueryListener);
  }

  ngOnInit(): void {
    this.user = this.store.pipe(select(selectUserFromUser), tap(user => console.log("User: ", user)));
  }

  ngOnDestroy(): void {
    this.mobileQuery.removeEventListener('change', this.mobileQueryListener);
  }

  public async toggle(): Promise<MatDrawerToggleResult> {
    return this.sideNav.toggle();
  }
}
