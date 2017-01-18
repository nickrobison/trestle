/**
 * Created by nrobison on 1/17/17.
 */

import {NgModule} from "@angular/core";
import {HttpModule} from "@angular/http";
import {BrowserModule} from "@angular/platform-browser";
import {MaterialModule} from "@angular/material";
@NgModule({
    imports: [
        HttpModule,
        BrowserModule,
        MaterialModule.forRoot()
    ],

})
export class AppModule {}
