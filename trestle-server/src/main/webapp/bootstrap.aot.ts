/**
 * Created by nrobison on 2/3/17.
 */
import {platformBrowser} from "@angular/platform-browser";
import {AppModuleNgFactory} from "../../../aot/src/main/webapp/app/app.module.ngfactory";

platformBrowser().bootstrapModuleFactory(AppModuleNgFactory).catch((err: Error) => console.error(err));