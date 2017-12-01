/**
 * Created by nrobison on 5/12/17.
 */
import { Injectable } from "@angular/core";
import { AuthConfig, AuthHttp } from "angular2-jwt";
import { Observable } from "rxjs/Observable";
import { Router } from "@angular/router";
import { Http, Request, RequestOptions, Response } from "@angular/http";

@Injectable()
export class TrestleHttp extends AuthHttp {
    constructor(private backend: Http,
                private defaultOptions: RequestOptions,
                private router: Router) {
        // super(backend, defaultOptions);
        super(new AuthConfig({noTokenScheme: true}), backend, defaultOptions);
    }

    public requestWithToken(req: Request, token: string): Observable<Response> {
        return super.requestWithToken(req, token)
            .map((res: Response) => res)
            .catch((error: any) => {
                console.error(error);
                if (error.status === 404) {
                    console.error("Unauthorized", error);
                    // this.router.navigate(["/login"[]);
                }
                return Observable.throw(error);
            });
    }

    public static factory(backend: Http,
                          defaultOptions: RequestOptions,
                          router: Router) {
        return new TrestleHttp(backend, defaultOptions, router);
    }
}
