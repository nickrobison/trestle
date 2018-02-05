import { ErrorHandler, Injectable, Injector } from "@angular/core";
import { LocationStrategy, PathLocationStrategy } from "@angular/common";
import { fromError, StackFrame } from "stacktrace-js";
import { HttpClient } from "@angular/common/http";

interface IErrorReport {
    timestamp: number;
    location: string;
    message: string;
    stackTrace: StackFrame[];
}

@Injectable()
export class ErrorService extends ErrorHandler {

    public constructor(private injector: Injector) {
        super();
    }

    public handleError(error: any): void {

        const location = this.injector.get(LocationStrategy);
        const http = this.injector.get(HttpClient);

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
                console.debug("Frames:", frames);
                http.post("/error/report", {
                    timestamp: Date.now(),
                    message,
                    location: url,
                    stackTrace: frames
                } as IErrorReport)
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
