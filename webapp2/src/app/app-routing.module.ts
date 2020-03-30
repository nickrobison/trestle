import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {DefaultRouteGuard} from "./user/DefaultRouteGuard";
import {NavigationComponent} from "./navigation/navigation/navigation.component";
import {LoginComponent} from "./navigation/login/login.component";


const routes: Routes = [
  {path: "", canActivate: [DefaultRouteGuard], component: NavigationComponent},
  {path: "login", component: LoginComponent},
  {path: "admin", loadChildren: () => import("./admin/admin.module").then(m => m.AdminModule)}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
