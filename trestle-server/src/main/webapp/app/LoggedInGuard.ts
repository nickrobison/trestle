/**
 * Created by nrobison on 1/19/17.
 */
import {CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router} from "@angular/router";
import {Injectable} from "@angular/core";
import {Observable} from "rxjs";
import {AuthService} from "./authentication.service";

@Injectable()
export class LoggedInGuard implements CanActivate {

    constructor(private authService: AuthService, private router: Router) {}

    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean>|Promise<boolean>|boolean {
        console.debug("Route", route);
        console.debug("State", state);
        if (this.authService.loggedIn()) {
            return true;
        }
        this.router.navigate(["/login"], { queryParams: { returnUrl: state.url}});
        return false;
    }


}
