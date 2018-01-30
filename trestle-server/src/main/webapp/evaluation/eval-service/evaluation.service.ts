import { Injectable } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import "clientjs";
import { Observable } from "rxjs/Observable";
import { ITrestleIndividual, TrestleIndividual } from "../../workspace/SharedModule/individual/TrestleIndividual/trestle-individual";
import { CacheService } from "../../workspace/SharedModule/cache/cache.service";

export enum MapState {
    OVERLAY = 1,
    OPAQUE = 2,
    NO_CONTEXT = 4
}

export interface IResultSet {
    expNumber: number;
    expState: MapState;
    expTime: number;
    union: boolean;
    unionOf: string;
    sliderEvents: number;
    mapMoves: number;
}

export interface IUserDemographics {
    age: number;
    education: string;
    geospatial: boolean;
    publicHealth: boolean;
}

export interface IExperimentResponse {
    state: number;
    union: string;
    unionOf: string[];
    individuals: TrestleIndividual[];
}

export interface IExperimentResults {
    userId: string;
    experimentResults: IResultSet[];
    demographics: IUserDemographics;
}

@Injectable()
export class EvaluationService {

    private userId: string;
    private demographics: IUserDemographics;
    // private resultMap: { [expNumber: number]: IResultSet };
    private results: IResultSet[];
    private sliderEvents: number;
    private mapMoves: number;

    public constructor(private http: HttpClient,
                       private individualCache: CacheService<string, TrestleIndividual>) {
        // this.resultMap = {};
        this.sliderEvents = 0;
        this.mapMoves = 0;
        this.results = [];

    //    Remove this
        this.createUser();
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

    public addSliderChange(): void {
        this.sliderEvents++;
    }

    public addMapMove(): void {
        this.mapMoves++;
    }

    public submitResults(finish: boolean,
                         experimentNumber: number,
                         startTime: number,
                         state: MapState, union: boolean,
                         unionOf: string[]): void {

        const expDuration = Date.now() - startTime;

        console.debug("Took %s ms", expDuration);
        // this.resultMap[experimentNumber] = ;
        this.results.push({
            expNumber: experimentNumber,
            expState: state,
            expTime: expDuration,
            union,
            unionOf: unionOf.join(","),
            sliderEvents: this.sliderEvents,
            mapMoves: this.mapMoves
        });
        //    Reset everything
        this.sliderEvents = 0;
        this.mapMoves = 0;

        const body: IExperimentResults =  {
            userId: this.userId,
            experimentResults: this.results,
            demographics: this.demographics
        };
        if (finish) {
            console.debug("Results:", body);
            this.http.post("/experiment/submit", body)
                .subscribe((res) => {
                    console.debug("Result:", res);
                }, (err) => {
                    console.error("Error:", err);
                });
        }
    }

    public loadExperiment(experimentNumber: number): Observable<IExperimentResponse> {
        return this.http.get<IExperimentResponse>("/experiment/" + experimentNumber)
            .do(console.debug)
            .switchMap((response) => {
                const unions = response.unionOf.map((union: string) => {
                    return this.getTrestleIndividual(union);
                });
                unions.push(this.getTrestleIndividual(response.union));
                return Observable.forkJoin(unions);
            }, (first, second: TrestleIndividual[]) => {
                return {
                    union: first.union,
                    unionOf: first.unionOf,
                    state: first.state,
                    individuals: second
                };
            });
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

    private getTrestleIndividual(name: string): Observable<TrestleIndividual> {
        return this.individualCache.get(name, this.getIndividualAPI(name));
    }

    private getIndividualAPI(name: string): Observable<TrestleIndividual> {
        const params = new HttpParams()
            .set("name", name);
        return this.http.get<ITrestleIndividual>("/individual/retrieve", {
            params
        })
            .map((res) => {
                // const response = res.json();
                console.debug("Has response, building object", res);
                return new TrestleIndividual(res);
            })
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }
}
