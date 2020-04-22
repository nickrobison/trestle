/**
 * Created by nrobison on 1/20/17.
 */
import {ActivatedRouteSnapshot, CanActivate, RouterStateSnapshot} from '@angular/router';
import {Observable} from 'rxjs';
import {Injectable} from '@angular/core';
import {selectUserFromUser, State} from '../reducers';
import {select, Store} from '@ngrx/store';
import {Privileges, TrestleUser} from './trestle-user';

@Injectable()
export class PermissionsGuard implements CanActivate {

  private user: TrestleUser;
  private subscription;

  public constructor(private store: Store<State>) {
    this.subscription = this.store.pipe(select(selectUserFromUser));
  }

  public canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> | Promise<boolean> | boolean {
    const roles = route.data['roles'] as Privileges[];
    return this.user.hasRequiredPrivileges(roles);
  }
}
