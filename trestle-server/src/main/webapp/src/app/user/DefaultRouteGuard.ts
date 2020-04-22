/**
 * Created by nrobison on 6/22/17.
 */
import {ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot} from '@angular/router';
import {Injectable} from '@angular/core';
import {Observable, Subscription} from 'rxjs';
import {TrestleUser} from './trestle-user';
import {select, Store} from '@ngrx/store';
import {selectUserFromUser, State} from '../reducers';

@Injectable()
export class DefaultRouteGuard implements CanActivate {

  private user: TrestleUser | null;
  private readonly subscription: Subscription;

  public constructor(private store: Store<State>, private router: Router) {
    this.subscription = this.store.pipe(select(selectUserFromUser)).subscribe(user => this.user = user);

  }

  public canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> | Promise<boolean> | boolean {
    if (this.user && this.user.isAdmin()) {
      console.debug('Is Admin, routing to dashboard');
      this.router.navigate(['admin/dashboard']);
      return false;
    } else if (this.user) {
      //    Navigate to dataset page
      console.debug('Logged in, routing to explore');
      this.router.navigate(['explore/viewer']);
      return false;
    }
    // Just continue
    return true;
  }


}
