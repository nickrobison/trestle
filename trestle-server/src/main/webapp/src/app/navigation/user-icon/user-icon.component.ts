import {Component, OnInit} from '@angular/core';
import {SizeProp} from '@fortawesome/fontawesome-svg-core';
import {faSignInAlt, faSignOutAlt} from '@fortawesome/free-solid-svg-icons';
import {select, Store} from '@ngrx/store';
import {State} from '../../reducers';
import {Observable} from 'rxjs';
import {TrestleUser} from '../../user/trestle-user';
import {logout} from '../../actions/auth.actions';
import {MD5} from 'crypto-js';
import {selectUserFromUser} from "../../reducers/auth.reducers";

@Component({
  selector: 'user-icon',
  templateUrl: './user-icon.component.html',
  styleUrls: ['./user-icon.component.scss']
})
export class UserIconComponent implements OnInit {

  public readonly iconSize: SizeProp = 'lg';
  public readonly loginIcon = faSignInAlt;
  public readonly logoutIcon = faSignOutAlt;

  public user: Observable<TrestleUser>;

  constructor(private store: Store<State>) {
  }

  ngOnInit(): void {
    this.user = this.store.pipe(select(selectUserFromUser));
  }


  /**
   * Get the Gravitar URL of the user
   * @returns {string}
   */
  public getGravatarURL(user: TrestleUser): string {
    const hash = MD5(user.email.trim().toLowerCase()).toString();
    return 'https://www.gravatar.com/avatar/' + hash + '?d=identicon' + '&s=36';
  }

  public logout(): void {
    this.store.dispatch(logout());
  }

}
