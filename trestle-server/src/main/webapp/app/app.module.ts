/**
 * Created by nrobison on 1/17/17.
 */
import {NgModule} from "@angular/core";
import {HttpModule} from "@angular/http";
import {BrowserModule} from "@angular/platform-browser";
import {MaterialModule} from "@angular/material";
import {RouterModule} from "@angular/router";
import {AppRoutes} from "./app.routes";
import {AppComponent} from "./app.component";
import {MaterializeModule} from "angular2-materialize";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {NavigationModule} from "./NavigationModule/navigation.module";

@NgModule({
    imports: [
        HttpModule,
        BrowserModule,
        BrowserAnimationsModule,
        MaterialModule,
        RouterModule.forRoot(AppRoutes),
        MaterializeModule,
        NavigationModule
    ],
    declarations: [AppComponent],
    // providers: [AuthService,
    //     LoggedInGuard,
    //     PermissionsGuard,
    //     {
    //         provide: AuthHttp,
    //         useFactory: authHttpServiceFactory,
    //         deps: [Http, RequestOptions]
    //     }],
    bootstrap: [AppComponent]

})
export class AppModule {}

// export function authHttpServiceFactory(http: Http, options: RequestOptions) {
//     return new AuthHttp(new AuthConfig({
//         noTokenScheme: true
//     }), http, options);
// }
