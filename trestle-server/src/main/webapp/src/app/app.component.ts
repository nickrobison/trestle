/**
 * Created by nrobison on 1/19/17.
 */
import {ChangeDetectorRef, Component, OnDestroy, OnInit, ViewEncapsulation} from '@angular/core';
import {Privileges, TrestleUser} from './user/trestle-user';
import {AuthService} from './user/authentication.service';
import {Router} from '@angular/router';
import {MediaMatcher} from '@angular/cdk/layout';
import {select, Store} from '@ngrx/store';
import {selectUserFromUser, State} from './reducers';
import {Observable} from 'rxjs';
import {tap} from 'rxjs/operators';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class AppComponent implements OnInit, OnDestroy {

  public gravatarURL: string;
  // We need this in order to access the Privileges enum from the template
  public Privileges = Privileges;
  public user: Observable<TrestleUser>;

  public mobileQuery: MediaQueryList;
  private readonly mobileQueryListener: () => void;

  constructor(private authService: AuthService,
              private router: Router,
              private store: Store<State>,
              changeDetectorRef: ChangeDetectorRef,
              media: MediaMatcher
  ) {
    this.mobileQuery = media.matchMedia('(max-width: 800px)');
    this.mobileQueryListener = () => changeDetectorRef.detectChanges();
    this.mobileQuery.addEventListener('change', this.mobileQueryListener);
  }


  public ngOnInit(): void {
    // Get the current user, if it exists
    this.user = this.store.pipe(select(selectUserFromUser), tap(user => console.log('User: ', user)));
  }

  public ngOnDestroy(): void {
    this.mobileQuery.removeEventListener('change', this.mobileQueryListener);
  }
}
