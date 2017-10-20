/**
 * Created by nrobison on 6/23/17.
 */
import {Component, OnInit} from "@angular/core";
import {MapService} from "./map.service";
import {ITrestleMapSource} from "../../UIModule/map/trestle-map.component";
import {animate, style, transition, trigger} from "@angular/animations";
import * as moment from "moment";
import {MatSliderChange} from "@angular/material";
import {IIndividualHistory} from "../../UIModule/history-graph/history-graph.component";
import {VisualizeService} from "../visualize/visualize.service";
import {TrestleIndividual} from "../visualize/individual/trestle-individual";
import {IEventData, IEventElement, IEventLink} from "../../UIModule/event-graph/event-graph.component";
import {Observable} from "rxjs/Observable";
import {TrestleEvent} from "../visualize/individual/trestle-event";
import {TrestleRelationType} from "../visualize/individual/trestle-relation";
import LngLatBounds = mapboxgl.LngLatBounds;

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
            start: individual.getTemporal().getFromAsDate(),
            end: individual.getTemporal().getToAsDate(),
            value: individual.getID()
        });
        // //    For all the other individuals, add them as well
        //    Now, build the individual events
        const individualEvents = this.buildObjectEvents(individual, filteredID);
        let events = individualEvents.events;
        let links = individualEvents.links;


        // Get all the related individuals, if necessary
        // If we don't need any individuals, then just plot our own events
        if (splitMerge.length > 0) {
            console.debug("Has some individuals:", splitMerge.length);
            const obsArray = splitMerge.map((relation) => {
                console.debug("Getting attributes for:", relation.getObject());
                return this.vs.getIndividualAttributes(relation.getObject());
            });
            Observable.forkJoin(obsArray)
                .subscribe((objects) => {
                    console.debug("Have all observables:", objects);
                    objects.forEach((object) => {
                        const relatedEvents = this.buildObjectEvents(object,
                            this.filterID(object.getID()),
                            splitMerge[0].getType());
                        events = events.concat(relatedEvents.events);
                        links = links.concat(relatedEvents.links);
                    });
                    const sortedEvents = this.sortEvents(events, links, filteredID);
                    console.debug("Sorted events:", sortedEvents)
                    this.eventData = sortedEvents;
                });
        } else {
            this.eventData = this.sortEvents(events, links, filteredID);
        }
    }

    private sortEvents(events: IEventElement[], links: IEventLink[], individualID: string): IEventData {

        // Sort any merged events
        let eventBins = events
            .filter((event) => (event.value === "from") || (event.value === "into")).length;
        // if we have an even number of merge events, add one, so that way
        if ((eventBins % 2) === 0) {
            eventBins += 1;
        }

        // We only have a single individual, add 2, just for giggles and spits
        if (eventBins === 1) {
            eventBins = 3;
        }

        // Set the individual equal to the middle value
        console.debug("Sorting data into %s bins", eventBins);
        let currentBin = Math.ceil(eventBins / 2);

        events
            .filter((event) => event.entity === individualID)
            .map((event) => {
                event.bin = currentBin;
            });
        // Now, the merged events, alternating high/low
        let sign = "+";
        let step = 1;

        events
            .filter((event) => (event.value === "from") || (event.value === "into"))
            .map((event) => {
                // Increment the current bin
                currentBin = currentBin + Number.parseInt(sign + step);
                event.bin = currentBin;

                // And do so for all the other events of the given individual
                events
                    .filter((iEvent) => iEvent.entity === event.entity)
                    .map((iEvent) => iEvent.bin = currentBin);

                // Increment the step and flip the sign
                step++;
                sign = sign === "+" ? "-" : "+";
            });

        return {
            nodes: events,
            links: links,
            bins: eventBins
        };
    }

    private buildObjectEvents(individual: TrestleIndividual,
                              entityName: string,
                              relationType?: TrestleRelationType,
                              rootIndividualID?: string): {
        events: IEventElement[],
        links: IEventLink[]
    } {
        console.debug("Build events for individual: %s with relation type: %s",
            individual, relationType);
        // Split merge first,
        // because it'll show us if we need to drop a created or destroyed event
        const events: IEventElement[] = [];
        const links: IEventLink[] = [];

        // Everything has a CREATED event, but not everything has destroyed
        //    If we have a split event, get the CREATED event
        // const created = {
        //     id: entityName + "-created",
        //     entity: entityName,
        //     bin: 1,
        //     value: "individual",
        //     temporal: individual
        //         .getEvents()
        //         .filter((event) => event.getType() === "CREATED")[0]
        //         .getTemporal().toDate()
        // };

        // Look for any other optional events

        // const destroyedEvents = individual
        //     .getEvents()
        //     .filter((event) => event.getType() === "DESTROYED");
        // const splitEvents = individual
        //     .getEvents()
        //     .filter((event) => event.getType() === "SPLIT");
        // const mergedEvents = individual
        //     .getEvents()
        //     .filter((event) => event.getType() === "MERGED");


        // Get the start event
        const startEvent = individual.getStartEvent();
        const started = {
            id: entityName + "-" + startEvent.getType(),
            entity: entityName,
            bin: 1,
            value: "individual",
            temporal: startEvent.getTemporal().toDate()
        };

        // If the root individual is a merged_from,
        // then we're looking for a link between the end event and an INTO relation
        if ((relationType === "MERGED_FROM") || (relationType === "SPLIT_FROM")) {
            console.debug("Has from");
            // const mergedEvent = events.filter((event) => (event.value === "merged_from")
            //     && (event.entity === entityName))[0];
            const fromEvent = {
                id: entityName + "-" + this.invertRelationship(relationType),
                entity: entityName,
                bin: 1,
                value: "into",
                // We can do this cast because if there's a merge event, there is some end point
                temporal: (individual.getTemporal().getToAsDate() as Date)
            };
            events.push(started, fromEvent);
            links.push({
                source: started,
                target: fromEvent
            });
            // We want the split event and the ending event
        } else if ((relationType === "SPLIT_INTO") || (relationType === "MERGED_INTO")) {
            console.debug("Has into");
            // const splitEvent = events.filter((event) => (event.value === "split_into")
            //     && (event.entity === entityName))[0];
            const endEvent = (individual.getEndEvent() as TrestleEvent);
            // We know that if there's a split going on, that there's an end event
            const end = {
                id: entityName + "-" + endEvent.getType(),
                entity: entityName,
                bin: 1,
                value: "individual",
                temporal: endEvent.getTemporal().toDate()
            };
            const intoEvent = {
                id: entityName + "-" + this.invertRelationship(relationType),
                entity: entityName,
                bin: 1,
                value: "from",
                temporal: startEvent.getTemporal().toDate()
            };
            events.push(end, intoEvent);
            links.push({
                source: intoEvent,
                target: end
            });
            //    Otherwise, link the start and end events
        } else {
            const endEvent = individual.getEndEvent();
            // If there is no end event, create a fake one and link it to the start event
            if (endEvent === null) {
                const destroyed = {
                    id: entityName + "-DESTROYED",
                    entity: entityName,
                    bin: 1,
                    value: "individual",
                    temporal: new Date("3001-01-01")
                };
                events.push(started, destroyed);
                links.push({
                    source: started,
                    target: destroyed
                });
                //    Otherwise, create the end event and move on
            } else {
                console.debug("Something else");
                const ended = {
                    id: entityName + "-" + (endEvent as TrestleEvent).getType(),
                    entity: entityName,
                    bin: 1,
                    value: "individual",
                    temporal: (endEvent as TrestleEvent).getTemporal().toDate()
                };
                events.push(started, ended);
                links.push({
                    source: started,
                    target: ended
                });
            }
        }
        //
        // if (mergedEvents.length > 0) {
        //     const merged = {
        //         id: entityName + "-merged",
        //         entity: entityName,
        //         bin: 1,
        //         value: "individual",
        //         temporal: mergedEvents[0].getTemporal().toDate()
        //     };
        //
        //     // Since we have a merged event, add all the MERGED_FROM events
        //     // Moments are mutable, so we have to clone it.
        //     const mergedTemporal = individual
        //         .getTemporal()
        //         .getFrom()
        //         .clone()
        //         .subtract(1, "year")
        //         .toDate();
        //     individual
        //         .getRelations()
        //         .filter((relation) => relation.getType() === "MERGED_FROM")
        //         .forEach((relation) => {
        //             const relationID = this.filterID(relation.getObject());
        //             const me = {
        //                 id: entityName + "-" + relationID,
        //                 entity: relationID,
        //                 bin: 1,
        //                 value: "merged_from",
        //                 temporal: mergedTemporal
        //             };
        //             events.push(me);
        //             links.push({
        //                 source: me,
        //                 target: merged
        //             });
        //         });
        //     // If we have both split/merge events, drop both CREATED and DESTROYED
        //     if (splitEvents.length > 0) {
        //         const split = {
        //             id: entityName + "-split",
        //             entity: entityName,
        //             bin: 1,
        //             value: "individual",
        //             temporal: splitEvents[0].getTemporal().toDate()
        //         };
        //         events.push(merged, split);
        //         links.push({
        //             source: merged,
        //             target: split
        //         });
        //         //    If we just have SPLIT, get the created event as well.
        //     } else if (destroyedEvents.length > 0) {
        //         const destroyed = {
        //             id: entityName + "-destroyed",
        //             entity: entityName,
        //             bin: 1,
        //             value: "individual",
        //             temporal: individual
        //                 .getEvents()
        //                 .filter((event) => event.getType() === "DESTROYED")[0]
        //                 .getTemporal().toDate()
        //         };
        //         events.push(merged, destroyed);
        //         links.push({
        //             source: merged,
        //             target: destroyed
        //         });
        //         //    If we don't have a DESTROYED event, create on in the far future
        //     } else {
        //         const destroyed = {
        //             id: entityName + "-destroyed",
        //             entity: entityName,
        //             bin: 1,
        //             value: "individual",
        //             temporal: new Date("3001-01-01")
        //         };
        //         events.push(merged, destroyed);
        //         links.push({
        //             source: merged,
        //             target: destroyed
        //         });
        //     }
        //     //    If we have a SPLIT event, don't bother with DESTROYED
        // } else if (splitEvents.length > 0) {
        //     const
        //         split = {
        //             id: entityName + "-split",
        //             entity: entityName,
        //             bin: 1,
        //             value: "individual",
        //             temporal: splitEvents[0].getTemporal().toDate()
        //         };
        //     events
        //         .push(created, split);
        //
        //     links
        //         .push({
        //             source: created,
        //             target: split
        //         });
        //
        //     //    Now CREATED/DESTROYED
        // } else if (destroyedEvents.length > 0) {
        //     const destroyed = {
        //         id: entityName + "-destroyed",
        //         entity: entityName,
        //         bin: 1,
        //         value: "individual",
        //         temporal: individual
        //             .getEvents()
        //             .filter((event) => event.getType() === "DESTROYED")[0]
        //             .getTemporal().toDate()
        //     };
        //     events.push(created, destroyed);
        //     links.push({
        //         source: created,
        //         target: destroyed
        //     });
        //     //    Create a far-future DESTROYED;
        // } else {
        //     const destroyed = {
        //         id: entityName + "-destroyed",
        //         entity: entityName,
        //         bin: 1,
        //         value: "individual",
        //         temporal: new Date("3001-01-01")
        //     };
        //     events.push(created, destroyed);
        //     links.push({
        //         source: created,
        //         target: destroyed
        //     });
        // }
        console.debug("Events for %s", individual.getID(), events);
        return {
            events: events,
            links: links
        };
    };

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

    /**
     * Invert the object relationship, because our event graphs works in reverse
     * @param {"MERGED_FROM" | "MERGED_INTO" | "SPLIT_FROM" | "SPLIT_INTO"} relationship
     * @returns {string} of inverted relationship
     */
    private invertRelationship(relationship: "MERGED_FROM" | "MERGED_INTO" | "SPLIT_FROM" | "SPLIT_INTO"): string {
        switch (relationship) {
            case "MERGED_FROM":
                return "MERGED_INTO";
            case "MERGED_INTO":
                return "MERGED_FROM";
            case "SPLIT_INTO":
                return "SPLIT_FROM";
            case "SPLIT_FROM":
                return "SPLIT_INTO";
        }
    }
}
