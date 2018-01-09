import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import "clientjs";
import { Observable } from "rxjs/Observable";

export enum MapState {
    OVERLAY = 1,
    OPAQUE = 2,
    NO_CONTEXT = 4
}

export interface IUserDemographics {
    test: number;
}

export interface IExperimentResponse {
    state: number;
    union: string;
    unionOf: string[];
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

    public loadExperiment(experimentNumber: number): Observable<IExperimentResponse> {
        return this.http.get<IExperimentResponse>("/experiment/" + experimentNumber)
            .do(console.debug);
    }

    public isOverlay(mapState: number): boolean {
        // tslint:disable-next-line:no-bitwise
        return (mapState & MapState.OVERLAY) > 0;
    }

    public isOpaque(mapState: number): boolean {
        // tslint:disable-next-line:no-bitwise
        return (mapState & MapState.OPAQUE) > 0;
    }

    public noContext(mapState: number): boolean {
        // tslint:disable-next-line:no-bitwise
        return (mapState & MapState.NO_CONTEXT) > 0;
    }
}
