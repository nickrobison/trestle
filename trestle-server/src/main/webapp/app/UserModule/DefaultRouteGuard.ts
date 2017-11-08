/**
 * Created by nrobison on 6/22/17.
 */
import {ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot} from "@angular/router";
import {Injectable} from "@angular/core";
import {AuthService} from "./authentication.service";
import {Observable} from "rxjs/Observable";

@Injectable()
export class DefaultRouteGuard implements CanActivate {

    public constructor(private authService: AuthService, private router: Router) {

    }

    public canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> | Promise<boolean> | boolean {
        if (this.authService.isAdmin()) {
            console.debug("Is Admin, routing to dashboard");
            this.router.navigate(["admin/dashboard"]);
            return false;
            // Navigate to admin dashboard
            // return this.router.navigate(["/"])
        } else if (this.authService.loggedIn()) {
        //    Navigate to dataset page
            console.debug("Logged in, routing to explore");
            this.router.navigate(["explore/viewer"]);
            return false;
        }
        // Just continue
        return true;
    }


}