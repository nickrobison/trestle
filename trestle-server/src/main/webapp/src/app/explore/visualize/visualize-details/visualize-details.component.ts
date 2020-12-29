import {AfterViewInit, Component} from "@angular/core";
import moment, {Moment} from "moment";
import {ActivatedRoute, Params} from '@angular/router';
import {TrestleIndividual} from '../../../shared/individual/TrestleIndividual/trestle-individual';
import {BehaviorSubject, combineLatest, Observable} from 'rxjs';
import {MapSource} from '../../../ui/trestle-map/trestle-map.component';
import {IIndividualHistory} from '../../../ui/history-graph/history-graph.component';
import {IndividualService} from '../../../shared/individual/individual.service';
import {map} from 'rxjs/operators';

interface IRouteObservable {
    route: Params;
    query: Params;
}

@Component({
    selector: "visualize-details",
    templateUrl: "./visualize-details.component.html",
    styleUrls: ["./visualize-details.component.scss"]
})
export class VisualizeDetailsComponent implements AfterViewInit {

    public individual: TrestleIndividual;
    public mapIndividual: BehaviorSubject<MapSource | undefined>;
    public individualFactHistory: IIndividualHistory;
    public minTime: Moment;
    public maxTime: Moment;
    public minGraphDate = new Date("1990-01-01");
    public maxGraphDate = new Date("2017-01-01");
    public validAt: Moment;
    public dbAt: Moment;
    private routeObservable: Observable<IRouteObservable>;

    constructor(private is: IndividualService,
                private route: ActivatedRoute) {
        this.mapIndividual = new BehaviorSubject(undefined);
        this.minTime = moment().year(1990).startOf("year");
        this.maxTime = moment().year(2016).endOf("year");
        const now = moment();
        this.validAt = now;
        this.dbAt = now;
    }

    public ngAfterViewInit(): void {
        this.routeObservable = combineLatest([this.route.params, this.route.queryParams]).pipe(map((results) => {
          return {
            route: results[0],
            query: results[1]
          }
        }));
        this.routeObservable
            .subscribe((combined) => {
                console.debug("has params: %O %O", combined.route, combined.query);
                if (combined.query["root"]) {
                    this.loadIndividual(combined.query["root"] + combined.route["id"]);
                } else {
                    this.loadIndividual(combined.route["id"]);
                }
            });
    }

    /**
     * Display function to filter down individual IDs
     * @param {string} name
     * @returns {string}
     */
    public displayFn(name: string): string {
        return TrestleIndividual.filterID(name);
    }

    /**
     * Gets the IRI suffix, since we can't access static methods in the Angular template
     * @param {string} object
     * @returns {string}
     */
    public getSuffix(object: string): string {
        return TrestleIndividual.extractSuffix(object);
    }

    /**
     * Gets the IRI hostname, since we can't access static methods in the Angular template
     * @param {string} object
     * @returns {string}
     */
    public getPrefix(object: string): string {
        return TrestleIndividual.extractPrefix(object);
    }

    private loadIndividual(value: string): void {
        console.debug("Loading individual:", value);
        this.is.getTrestleIndividual(value)
            .subscribe((results: TrestleIndividual) => {
                console.debug("has selection", results);
                this.individual = results;

                // Build fact history
                this.individualFactHistory = {
                    entities: results
                        .getFacts()
                        .filter((fact) => fact.getDatabaseTemporal().isContinuing())
                        .map((fact) => {
                            return {
                                label: fact.getName(),
                                start: fact.getValidTemporal().getFromAsDate(),
                                end: fact.getValidTemporal().getToAsDate(),
                                value: fact.getValue()
                            };
                        })
                };
                console.debug("Sending individual to map");
                this.mapIndividual.next({
                    id: results.getID(),
                    data: {
                        type: "Feature",
                        geometry: results.getSpatialValue(),
                        id: results.getIDAsInteger().toString(),
                        properties: results.getFactValues()
                    }
                });
            });
    }
}
