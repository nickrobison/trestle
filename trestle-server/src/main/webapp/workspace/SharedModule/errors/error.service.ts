import { ErrorHandler, Injectable, Injector } from "@angular/core";
import { LocationStrategy, PathLocationStrategy } from "@angular/common";
import { fromError } from "stacktrace-js";
import { Http } from "@angular/http";

@Injectable()
export class ErrorService extends ErrorHandler {

    public constructor(private injector: Injector) {
        super();
    }

    public handleError(error: any): void {

        const location = this.injector.get(LocationStrategy);
        const http = this.injector.get(Http);

        const message = error.message ? error.message : error.toString();
        let url = "";
        if (location instanceof PathLocationStrategy) {
            url = location.path();
        }

        console.debug("Location: %s, Message: %s", url, message);

        fromError(error, {
            offline: true
        })
            .then((frames) => {
                http.post("/error/report", {
                    message,
                    location: url,
                    stack: JSON.stringify(frames)
                })
                    .subscribe((success) => {
                            console.debug("Error logged as:", success);
                        },
                        (rejected) => {
                            console.error("Cannot log error", rejected);
                        });
            });
        throw error;
    }
}
