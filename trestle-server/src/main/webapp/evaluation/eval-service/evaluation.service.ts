import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";

@Injectable()
export class EvaluationService {

    private userId: string;

    public constructor(private http: HttpClient) {

    }

    public createUser(): void {
        new Fingerprint2().get((result, components) => {
            console.debug("%s fingerprinted", result, components);
            this.userId = result;
        });
    }

    public setDemographics(): void {

    }

    public submitResults(): void {

    }
}
