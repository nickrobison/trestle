/**
 * Created by nrobison on 6/23/17.
 */
import { Component, OnInit } from "@angular/core";
import { MapService } from "./map.service";
import { ITrestleMapSource } from "../../UIModule/map/trestle-map.component";
import { animate, style, transition, trigger } from "@angular/animations";
import * as moment from "moment";
import LngLatBounds = mapboxgl.LngLatBounds;
import { MatSliderChange } from "@angular/material";
import { IIndividualHistory } from "../../UIModule/history-graph/history-graph.component";
import { VisualizeService } from "../visualize/visualize.service";
import { TrestleIndividual } from "../visualize/individual/trestle-individual";
import {
    IEventData, IEventElement,
    IEventLink
} from "../../UIModule/event-graph/event-graph.component";

enum DatasetState {
    UNLOADED,
    LOADING,
    LOADED,
    ERROR
}

interface IDatasetState {
    name: string,
    state: DatasetState;
}

@Component({
    selector: "dataset-viewer",
    templateUrl: "./viewer.component.html",
    styleUrls: ["./viewer.component.css"],
    animations: [
        trigger("fadeInOut", [
            transition(":enter", [
                style({transform: "scale(0)", opacity: 0}),
                animate("500ms", style({transform: "scale(1)", opacity: 1}))
            ]),
        ])
    ]
})
export class DatsetViewerComponent implements OnInit {
    public availableDatasets: IDatasetState[] = [];
    public DatasetState = DatasetState;
    public loadedDataset: ITrestleMapSource;
    public minTime = moment("1990-01-01");
    public maxTime = moment("2016-01-01");
    public sliderValue = 2013;
    public selectedIndividual: TrestleIndividual;
    public objectHistory: IIndividualHistory;
    public eventData: IEventData;
    public mapLocked = false;
    private mapBounds: LngLatBounds;

    constructor(private mapService: MapService, private vs: VisualizeService) {
    }

    public ngOnInit(): void {
        this.objectHistory = {
            entities: []
        };
        this.mapService.getAvailableDatasets()
            .subscribe((results: string[]) => {
                results.forEach((ds) => {
                    this.availableDatasets.push({
                        name: ds,
                        state: DatasetState.UNLOADED
                    });
                });
            });
    }

    public loadDataset(dataset: IDatasetState): void {
        this.mapLocked = true;
        console.debug("Loading:", dataset.name);
        dataset.state = DatasetState.LOADING;
        this.mapService.stIntersect(dataset.name,
            this.mapBounds, moment()
                .year(this.sliderValue)
                .startOf("year"))
            .subscribe((data) => {
                dataset.state = DatasetState.LOADED;
                console.debug("Data:", data);
                this.loadedDataset = {
                    id: "intersection-query",
                    idField: "id",
                    data
                };
                this.mapLocked = false;
            }, (error) => {
                console.error("Error loading dataset:", error);
                dataset.state = DatasetState.ERROR;
                this.mapLocked = false;
            });
    }

    public updateBounds(bounds: LngLatBounds): void {
        console.debug("Moving, updating bounds", bounds);
        // If we've moved outside of the current bounds, get new data
        if (!this.mapLocked && this.needNewData(bounds)) {
            this.mapBounds = bounds;
            // this.availableDatasets
            //     .filter((ds) => ds.state === DatasetState.LOADED)
            //     .forEach((ds) => this.loadDataset(ds));
        } else {
            this.mapBounds = bounds;
        }
    }

    public sliderChanged = (event: MatSliderChange): void => {
        console.debug("Value changed to:", event);
        if (event.value) {
            this.sliderValue = event.value;
            //    Reload all the currently loaded datasets
            this.availableDatasets
                .filter((ds) => ds.state === DatasetState.LOADED)
                .forEach((ds) => this.loadDataset(ds));
        }
    };

    public mapClicked = (event: string): void => {
        console.debug("Clicked:", event);
        this.vs.getIndividualAttributes(event)
            .subscribe((data) => {
                console.debug("Has individual", data);
                this.buildHistoryGraph(data);
            });
    };

    private buildHistoryGraph(individual: TrestleIndividual): void {
        console.debug("has events", individual.getEvents());
        //    Get the split/merged events
        const splitMerge = individual
            .getRelations()
            .filter((relation) => (relation.getType() === "MERGED_FROM")
                || (relation.getType() === "MERGED_INTO")
                || (relation.getType() === "SPLIT_FROM")
                || (relation.getType() === "SPLIT_INTO"));
        const history: IIndividualHistory = {
            entities: []
        };
        const filteredID = this.filterID(individual.getID());
        history.entities.push({
            label: filteredID,
            start: individual.getTemporal().getFromDate(),
            end: individual.getTemporal().getToDate(),
            value: individual.getID()
        });
        // //    For all the other individuals, add them as well
        // console.debug("Has some individuals:", splitMerge.length);
        // const obsArray = splitMerge.map((relation) => {
        //     console.debug("Getting attributes for:", relation.getObject());
        //     return this.vs.getIndividualAttributes(relation.getObject());
        // });
        // Observable.forkJoin(obsArray)
        //     .subscribe((objects) => {
        //         console.debug("Have all observables:", objects);
        //         objects.forEach((object) => {
        //             history.entities.push({
        //                 label: this.filterID(object.getID()),
        //                 start: object.getTemporal().getFromDate(),
        //                 end: object.getTemporal().getToDate(),
        //                 value: object.getID()
        //             });
        //         });
        //         console.debug("History", history);
        //         this.objectHistory = history;
        //     });

        //    Now, build the object events


        // Split merge first,
        // because it'll show us if we need to drop a created or destroyed event
        const events: IEventElement[] = [];
        const links: IEventLink[] = [];

        // Everything has a CREATED event, but not everything has destroyed
        //    If we have a split event, get the CREATED event
        const created = {
            id: filteredID + "-created",
            entity: filteredID,
            bin: 1,
            value: "individual",
            temporal: individual
                .getEvents()
                .filter((event) => event.getType() === "CREATED")[0]
                .getTemporal().toDate()
        };

        const destroyedEvents = individual
            .getEvents()
            .filter((event) => event.getType() === "DESTROYED");
        const splitEvents = individual
            .getEvents()
            .filter((event) => event.getType() === "SPLIT");
        const mergedEvents = individual
            .getEvents()
            .filter((event) => event.getType() === "MERGED");

        if (mergedEvents.length > 0) {
            const merged = {
                id: filteredID + "-merged",
                entity: filteredID,
                bin: 1,
                value: "individual",
                temporal: mergedEvents[0].getTemporal().toDate()
            };

            // Since we have a merged event, add all the MERGED_FROM events
            // Moments are mutable, so we have to clone it.
            const mergedTemporal = individual
                .getTemporal()
                .getFrom()
                .clone()
                .subtract(1, "year")
                .toDate();
            individual
                .getRelations()
                .filter((relation) => relation.getType() === "MERGED_FROM")
                .forEach((relation) => {
                    const me = {
                        id: filteredID + "-" + relation.getObject(),
                        entity: relation.getObject(),
                        bin: 1,
                        value: "merged_from",
                        temporal: mergedTemporal
                    };
                    events.push(me);
                    links.push({
                        source: me,
                        target: merged
                    });
                });
            // If we have both split/merge events, drop both CREATED and DESTROYED
            if (splitEvents.length > 0) {
                const split = {
                    id: filteredID + "-split",
                    entity: filteredID,
                    bin: 1,
                    value: "individual",
                    temporal: splitEvents[0].getTemporal().toDate()
                };
                events.push(merged, split);
                links.push({
                    source: merged,
                    target: split
                });
                //    If we just have SPLIT, get the created event as well.
            } else if (destroyedEvents.length > 0) {
                const destroyed = {
                    id: filteredID + "-destroyed",
                    entity: filteredID,
                    bin: 1,
                    value: "individual",
                    temporal: individual
                        .getEvents()
                        .filter((event) => event.getType() === "DESTROYED")[0]
                        .getTemporal().toDate()
                };
                events.push(merged, destroyed);
                links.push({
                    source: merged,
                    target: destroyed
                });
                //    If we don't have a DESTROYED event, create on in the far future
            } else {
                const destroyed = {
                    id: filteredID + "-destroyed",
                    entity: filteredID,
                    bin: 1,
                    value: "individual",
                    temporal: new Date("3001-01-01")
                };
                events.push(merged, destroyed);
                links.push({
                    source: merged,
                    target: destroyed
                });
            }
            //    If we have a SPLIT event, don't bother with DESTROYED
        } else if (splitEvents.length > 0) {
            const
                split = {
                    id: filteredID + "-split",
                    entity: filteredID,
                    bin: 1,
                    value: "individual",
                    temporal: splitEvents[0].getTemporal().toDate()
                };
            events
                .push(created, split);

            links
                .push({
                    source: created,
                    target: split
                });

            //    Now CREATED/DESTROYED
        } else if (destroyedEvents.length > 0) {
            const destroyed = {
                id: filteredID + "-destroyed",
                entity: filteredID,
                bin: 1,
                value: "individual",
                temporal: individual
                    .getEvents()
                    .filter((event) => event.getType() === "DESTROYED")[0]
                    .getTemporal().toDate()
            };
            events.push(created, destroyed);
            links.push({
                source: created,
                target: destroyed
            });
            //    Create a far-future DESTROYED;
        } else {
            const destroyed = {
                id: filteredID + "-destroyed",
                entity: filteredID,
                bin: 1,
                value: "individual",
                temporal: new Date("3001-01-01")
            };
            events.push(created, destroyed);
            links.push({
                source: created,
                target: destroyed
            });
        }
        // Sort any merged events
        let mergeCount = events.filter((event) => event.value === "merged_from").length;
        // if we have an even number of merge events, add one, so that way
        if ((mergeCount % 2) === 0) {
            mergeCount += 1;
        }

        // Set the individual equal to the middle value
        let currentBin = Math.ceil(mergeCount / 2);
        events
            .filter((event) => event.value === "individual")
            .map((event) => {
                event.bin = currentBin;
            });
        // Now, the merged events, alternating high/low
        let sign = "+";
        let step = 1;
        events
            .filter((event) => event.value === "merged_from")
            .map((event) => {
                // Increment the current bin
                currentBin = currentBin + Number.parseInt(sign + step);
                event.bin = currentBin;
                // Increment the step and flip the sign
                step++;
                sign = sign === "+" ? "-" : "+";
            });

        this.eventData = {
            nodes: events,
            links: links
        };
    }

    private needNewData(newBounds: mapboxgl.LngLatBounds) {
        console.debug("Need new data", newBounds, "old Data", this.mapBounds);
        // This short circuits the checks to avoid loading data on the first go 'round.
        if (newBounds === null || this.mapBounds === undefined) {
            return false;
        }
        // Moved up/down
        if ((newBounds.getNorth() > this.mapBounds.getNorth())
            || (newBounds.getSouth() < this.mapBounds.getSouth())) {
            console.debug(newBounds.getNorth() + ", " + this.mapBounds.getNorth());
            console.debug(newBounds.getSouth() + ", " + this.mapBounds.getSouth());
            console.debug("Moved north/south, so true");
            return true;
            //    Moved east/west
        } else if ((newBounds.getEast() > this.mapBounds.getEast())
            || (newBounds.getWest() < this.mapBounds.getWest())) {
            console.debug(newBounds.getEast() + ", " + this.mapBounds.getEast());
            console.debug(newBounds.getWest() + ", " + this.mapBounds.getWest());
            console.debug("Moved east/west, so true");
        }
        return false;
    }

    private filterID(id: string): string {
        const strings = id.split("#");
        const idStrings = strings[1].split(":");
        return idStrings[0] + ":" + idStrings[1];
    }
}
