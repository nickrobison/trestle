/**
 * Created by nrobison on 1/19/17.
 */
import {ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot} from '@angular/router';
import {Injectable} from '@angular/core';
import {Observable, Subscription} from 'rxjs';
import {select, Store} from '@ngrx/store';
import {State} from '../reducers';
import {TrestleUser} from './trestle-user';
import {selectUserFromUser} from "../reducers/auth.reducers";

@Injectable()
export class LoggedInGuard implements CanActivate {

  private user: TrestleUser | null;
  private readonly subscription: Subscription;

  constructor(private router: Router, private store: Store<State>) {
    this.subscription = this.store.pipe(select(selectUserFromUser)).subscribe(user => this.user = user);
  }


  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> | Promise<boolean> | boolean {
    console.debug('Route', route);
    console.debug('State', state);
    if (this.user) {
      return true;
    }
    this.router.navigate(['/login'], {queryParams: {returnUrl: state.url}});
    return false;
  }


}
