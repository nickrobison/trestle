/**
 * Created by nrobison on 5/12/17.
 */
import {NgModule} from "@angular/core";
import {Http, HttpModule, RequestOptions, XHRBackend} from "@angular/http";
import {AuthService} from "./authentication.service";
import {LoggedInGuard} from "./LoggedInGuard";
import {PermissionsGuard} from "./PermissionsGuard";
import {Router} from "@angular/router";
import {TrestleHttp} from "./trestle-http.provider";
import {CommonModule} from "@angular/common";
import {UserService} from "./users.service";

@NgModule({
    imports: [
        HttpModule,
        CommonModule
    ],
    providers: [
        AuthService,
        LoggedInGuard,
        PermissionsGuard,
        UserService,
        {
            provide: TrestleHttp,
            useFactory: (backend: Http, defaultOptions: RequestOptions, router: Router) => new TrestleHttp(backend, defaultOptions, router),
            deps: [Http, RequestOptions, Router]
        }

    ]
})

export class UserModule {}

// export function authHttpServiceFactory(http: Http, options: RequestOptions) {
//     return new AuthHttp(new AuthConfig({
//         noTokenScheme: true
//     }), http, options);
// }