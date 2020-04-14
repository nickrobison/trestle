/**
 * Created by nrobison on 5/30/17.
 */
import {ViewerComponent} from './viewer/viewer.component';
import {LoggedInGuard} from '../user/LoggedInGuard';
import {ITrestleRoute} from '../navigation/navigation.module';
import {QueryComponent} from './query/query.component';

export const ExploreRoutes: ITrestleRoute[] = [
  {path: '', redirectTo: 'viewer', pathMatch: 'full'},
  // {
  //     path: "visualize", component: VisualizeComponent, canActivate: [LoggedInGuard],
  //     children: [
  //         {path: ":id", component: VisualizeDetailsComponent}
  //
  //     ]
  // },
  {path: "query", component: QueryComponent, canActivate: [LoggedInGuard]},
  {path: 'viewer', component: ViewerComponent, canActivate: [LoggedInGuard]}
  // {path: "compare", component: CompareComponent, canActivate: [LoggedInGuard]},
  // {path: "aggregate", component: AggregateComponent, canActivate: [LoggedInGuard]}
];
