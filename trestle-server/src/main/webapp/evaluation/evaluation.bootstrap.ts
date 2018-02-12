import { platformBrowserDynamic } from "@angular/platform-browser-dynamic";
import { EvaluationModule } from "./evaluation.module";
import { enableProdMode } from "@angular/core";

if (ENV === "production") {
    enableProdMode();
}

platformBrowserDynamic().bootstrapModule(EvaluationModule);