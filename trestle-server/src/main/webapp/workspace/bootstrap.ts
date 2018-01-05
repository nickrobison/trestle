/**
 * Created by nrobison on 1/17/17.
 */
import {platformBrowserDynamic} from "@angular/platform-browser-dynamic";
import { enableProdMode } from "@angular/core";
import { AppModule } from "./app.module";

if (ENV === "production") {
    enableProdMode();
}
platformBrowserDynamic().bootstrapModule(AppModule);