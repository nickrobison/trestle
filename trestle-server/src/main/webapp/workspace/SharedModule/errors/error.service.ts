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

    private sourceCache: {[file: string]: string};

    public constructor(private injector: Injector) {
        super();

        this.sourceCache = {
            "ng:///WorkspaceModule/WorkspaceComponent.ngfactory.js": "{}",
            "http://localhost:8080/static/toSubscriber.js.map": '{"version": 3, "sources": []}'
        };
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
            sourceCache: this.sourceCache,
            offline: true
        })
            .then((frames) => {
                console.debug("Frames:", frames);
                http.post("/error/report", {
                    timestamp: Date.now(),
                    message,
                    location: url,
                    stackTrace: frames.slice(0, 10)
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
