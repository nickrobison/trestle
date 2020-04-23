import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {SizeProp} from '@fortawesome/fontawesome-svg-core';
import {faSignInAlt, faSignOutAlt} from '@fortawesome/free-solid-svg-icons';
import {Observable} from 'rxjs';
import {TrestleUser} from '../../user/trestle-user';
import {select, Store} from '@ngrx/store';
import {selectUserFromUser, State} from '../../reducers';
import {logout} from '../../actions/auth.actions';

@Component({
  selector: 'top-nav',
  templateUrl: './top-nav.component.html',
  styleUrls: ['./top-nav.component.scss']
})
export class TopNavComponent implements OnInit {

  public readonly iconSize: SizeProp = 'lg';
  public readonly loginIcon = faSignInAlt;
  public readonly logoutIcon = faSignOutAlt;
  public user: Observable<TrestleUser>;

  @Output()
  public headerClicked = new EventEmitter<void>();

  constructor(private store: Store<State>) {
  }

  ngOnInit(): void {
    this.user = this.store.pipe(select(selectUserFromUser));
  }

  /**
   * Get the Gravitar URL of the user
   * @returns {string}
   */
  public getGravatarURL(): string {
    return '';
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

  public logout(): void {
    this.store.dispatch(logout());
  }


}
