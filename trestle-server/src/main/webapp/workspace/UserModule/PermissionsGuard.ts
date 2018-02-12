/**
 * Created by nrobison on 1/20/17.
 */
import {CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot} from "@angular/router";
import {Observable} from "rxjs";
import {Privileges, AuthService} from "./authentication.service";
import {Injectable} from "@angular/core";

@Injectable()
export class PermissionsGuard implements CanActivate {

    public constructor(private authService: AuthService) {
    }

    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean>|Promise<boolean>|boolean {
        let roles = route.data["roles"] as Array<Privileges>;
        console.debug("Needs roles", roles);
        return this.authService.hasRequiredRoles(roles);
    }
}