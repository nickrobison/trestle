import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import "clientjs";

export interface IUserDemographics {
    test: number;
}

@Injectable()
export class EvaluationService {

    private userId: string;
    private demographics: IUserDemographics;

    public constructor(private http: HttpClient) {

    }

    public createUser(): void {
        const fingerprint = new ClientJS().getFingerprint();
        console.debug("FP:", fingerprint);
        this.userId = fingerprint;
    }

    public setDemographics(demographics: IUserDemographics): void {
        console.debug("Setting demographics", demographics);
        this.demographics = demographics;
    }

    public submitResults(): void {

    }
}
