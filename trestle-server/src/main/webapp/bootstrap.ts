/**
 * Created by nrobison on 1/17/17.
 */
import {platformBrowserDynamic} from "@angular/platform-browser-dynamic";
import {AppModule} from "./app/app.module";
import { enableProdMode } from "@angular/core";

if (ENV === "production") {
    enableProdMode();
}
platformBrowserDynamic().bootstrapModule(AppModule);