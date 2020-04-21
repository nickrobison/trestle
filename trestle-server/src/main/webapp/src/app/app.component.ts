/**
 * Created by nrobison on 1/19/17.
 */
import {ChangeDetectorRef, Component, OnDestroy, OnInit, ViewEncapsulation} from '@angular/core';
import {TrestleUser} from './user/trestle-user';
import {AuthService, Privileges} from './user/authentication.service';
import {Router} from '@angular/router';
import {MD5} from 'crypto-js';
import {faSignInAlt, faSignOutAlt} from '@fortawesome/free-solid-svg-icons';
import {SizeProp} from '@fortawesome/fontawesome-svg-core';
import {MediaMatcher} from '@angular/cdk/layout';
import {select, Store} from '@ngrx/store';
import {State} from './reducers';
import {Observable} from 'rxjs';
import {tap} from 'rxjs/operators';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class AppComponent implements OnInit, OnDestroy {
  public readonly iconSize: SizeProp = 'lg';
  public readonly loginIcon = faSignInAlt;
  public readonly logoutIcon = faSignOutAlt;
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
    this.user = this.store.pipe(select("user"), select("user"), tap(user => console.log("User: ", user)));
  }

  public ngOnDestroy(): void {
    this.mobileQuery.removeEventListener('change', this.mobileQueryListener);
    // this.loginSubscription.unsubscribe();
  }

  // /**
  //  * Attempt to login the user
  //  */
  // public login(): void {
  //   if (this.user == null) {
  //     this.router.navigate(['/login']);
  //   }
  // }

  /**
   * Does the user have the required permissions?
   * @param {TrestleUser} user to verify permisisons on
   * @param {Privileges[]} requiredPrivs
   * @returns {boolean}
   */
  public userHasRequiredPermissions(user: TrestleUser, requiredPrivs: Privileges[]): boolean {
    return user.hasRequiredPrivileges(requiredPrivs);
  }

  /**
   * Get the Gravitar URL of the user
   * @returns {string}
   */
  public getGravatarURL(): string {
    return "";
    // if (this.gravatarURL == null) {
    //   const user = this.authService.getUser();
    //   if (user !== null) {
    //     const hash = MD5(user.email.trim().toLowerCase()).toString();
    //     this.gravatarURL = 'https://www.gravatar.com/avatar/' + hash + '?d=identicon' + '&s=36';
    //     return this.gravatarURL;
    //   }
    // }
    // return this.gravatarURL;
  }

  /**
   * Logout the currently logged in user
   */
  public logout(): void {
    this.authService.logout();
    this.user = null;
  }
}
