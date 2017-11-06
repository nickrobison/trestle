/**
 * Created by nrobison on 5/30/17.
 */
import {VisualizeComponent} from "./visualize/visualize.component";
import {LoggedInGuard} from "../UserModule/LoggedInGuard";
import {QueryComponent} from "./query/query.component";
import {ITrestleRoute} from "../NavigationModule/navigation.module";
import {DatsetViewerComponent} from "./viewer/viewer.component";
import {CompareComponent} from "./compare/compare.component";
import {VisualizeDetailsComponent} from "./visualize/details/visualize-details.component";

export const ExploreRoutes: ITrestleRoute[] = [
    {path: "", redirectTo: "viewer", pathMatch: "full"},
    {
        path: "visualize", component: VisualizeComponent, canActivate: [LoggedInGuard],
        children: [
            {path: ":id", component: VisualizeDetailsComponent}

        ]
    },
    {path: "query", component: QueryComponent, canActivate: [LoggedInGuard]},
    {path: "viewer", component: DatsetViewerComponent, canActivate: [LoggedInGuard]},
    {path: "compare", component: CompareComponent, canActivate: [LoggedInGuard]}
];
