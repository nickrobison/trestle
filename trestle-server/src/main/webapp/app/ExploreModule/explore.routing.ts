
import {VisualizeComponent} from "./visualize/visualize.component";
import {LoggedInGuard} from "../UserModule/LoggedInGuard";
import {QueryComponent} from "./query/query.component";
import {ITrestleRoute} from "../NavigationModule/navigation.module";
/**
 * Created by nrobison on 5/30/17.
 */

export const ExploreRoutes: Array<ITrestleRoute> = [
    {path: "", redirectTo: "visualize", pathMatch: "full"},
    {path: "visualize", component: VisualizeComponent, canActivate: [LoggedInGuard]},
    {path: "query", component: QueryComponent, canActivate: [LoggedInGuard]}
];
