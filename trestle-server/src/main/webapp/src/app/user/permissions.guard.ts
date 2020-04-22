/**
 * Created by nrobison on 1/20/17.
 */
import {ActivatedRouteSnapshot, CanActivate, RouterStateSnapshot} from '@angular/router';
import {Observable, Subscription} from 'rxjs';
import {Injectable, OnDestroy} from '@angular/core';
import {selectUserFromUser, State} from '../reducers';
import {select, Store} from '@ngrx/store';
import {Privileges, TrestleUser} from './trestle-user';

@Injectable()
export class PermissionsGuard implements CanActivate, OnDestroy {

  private user: TrestleUser;
  private subscription: Subscription;

  public constructor(private store: Store<State>) {
    this.subscription = this.store.pipe(select(selectUserFromUser)).subscribe(user => {
      this.user = user;
    });
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  public canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> | Promise<boolean> | boolean {
    const roles = route.data['roles'] as Privileges[];
    return this.user.hasRequiredPrivileges(roles);
  }
}
