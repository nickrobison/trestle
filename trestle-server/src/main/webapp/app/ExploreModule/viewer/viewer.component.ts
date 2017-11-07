/**
 * Created by nrobison on 6/23/17.
 */
import {Component, OnInit} from "@angular/core";
import {MapService} from "./map.service";
import {ITrestleMapSource, MapSource} from "../../UIModule/map/trestle-map.component";
import {animate, style, transition, trigger} from "@angular/animations";
import * as moment from "moment";
import {MatSliderChange} from "@angular/material";
import {IIndividualHistory} from "../../UIModule/history-graph/history-graph.component";
import {IndividualService} from "../../SharedModule/individual/individual.service";
import {TrestleIndividual} from "../../SharedModule/individual/TrestleIndividual/trestle-individual";
import {IEventData, IEventElement, IEventLink} from "../../UIModule/event-graph/event-graph.component";
import {Observable} from "rxjs/Observable";
import {TrestleEvent} from "../../SharedModule/individual/TrestleIndividual/trestle-event";
import {TrestleRelationType} from "../../SharedModule/individual/TrestleIndividual/trestle-relation";
import LngLatBounds = mapboxgl.LngLatBounds;
import {Subject} from "rxjs/Subject";
import {IDataExport} from "../exporter/exporter.component";

enum DatasetState {
    UNLOADED,
    LOADING,
    LOADED,
    ERROR
}

interface IDatasetState {
    name: string;
    state: DatasetState;
    error?: string;
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
    public minTime = moment("1990-01-01");
    public maxTime = moment("2016-01-01");
    public sliderValue = 2013;
    public selectedIndividual: TrestleIndividual;
    public selectedIndividualID: string;
    public objectHistory: IIndividualHistory;
    public eventData: IEventData;
    public dataChanges: Subject<MapSource>;
    public exportIndividuals: IDataExport[];
    private mapBounds: LngLatBounds;

    constructor(private mapService: MapService, private vs: IndividualService) {
        this.dataChanges = new Subject();
        this.exportIndividuals = [];
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
        console.debug("Loading:", dataset.name);
        dataset.state = DatasetState.LOADING;
        this.mapService.stIntersect(dataset.name,
            this.mapBounds, moment()
                .year(this.sliderValue)
                .startOf("year"))
            .subscribe((data) => {
                dataset.state = DatasetState.LOADED;
                console.debug("Data:", data);
                // Get the list of individuals, for exporting
                this.exportIndividuals.push({
                    dataset: this.availableDatasets[0].name,
                    individuals: (data.features
                        .filter((feature) => feature.id)
                        // We can do this cast, because we filter to make sure the features have an id
                        .map((feature) => feature.id) as string[])
                });
                this.dataChanges.next({
                    id: "intersection-query",
                    idField: "id",
                    data
                });
            }, (error) => {
                console.error("Error loading dataset:", error);
                dataset.state = DatasetState.ERROR;
                dataset.error = error;
            });
    }

    public updateBounds(bounds: LngLatBounds): void {
        console.debug("Moving, updating bounds", bounds);
        // If we've moved outside of the current bounds, get new data
        if (this.needNewData(bounds)) {
            this.mapBounds = bounds;
            this.availableDatasets
                .filter((ds) => ds.state === DatasetState.LOADED)
                .forEach((ds) => this.loadDataset(ds));
        }
        // On the first time around, set the map bounds
        if (!this.mapBounds) {
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
        this.vs.getTrestleIndividual(event)
            .subscribe((data) => {
                console.debug("Has selection", data);
                this.selectedIndividualID = data.getFilteredID();
                this.buildHistoryGraph(data);
            });
    };

    public filterLabel(input: IEventElement): string {
        return input.entity.split(":")[1];
    }

    public getError(ds: IDatasetState): string {
        return ds.error === undefined ? "Error" : ds.error;
    }

    private buildHistoryGraph(individual: TrestleIndividual): void {
        console.debug("has events", individual.getEvents());
        //    Get the split/merged/component relations
        const additionalRelations = individual
            .getRelations()
            .filter((relation) => (relation.getType() === "MERGED_FROM")
                || (relation.getType() === "MERGED_INTO")
                || (relation.getType() === "SPLIT_FROM")
                || (relation.getType() === "SPLIT_INTO")
                || (relation.getType() === "COMPONENT_WITH"));
        console.debug("Individual %s has relations %o",
            individual.getFilteredID(), additionalRelations);
        const history: IIndividualHistory = {
            entities: []
        };
        const filteredID = individual.getFilteredID();
        history.entities.push({
            label: filteredID,
            start: individual.getTemporal().getFromAsDate(),
            end: individual.getTemporal().getToAsDate(),
            value: individual.getID()
        });
        // //    For all the other individuals, add them as well
        //    Now, build the selection events
        const individualEvents = this.buildObjectEvents(individual, filteredID);
        let events = individualEvents.events;
        let links = individualEvents.links;

        // Get all the related individuals, if necessary
        // If we don't need any individuals, then just plot our own events
        if (additionalRelations.length > 0) {
            console.debug("Has some individuals:", additionalRelations.length);
            // Figure out what to link to.
            let rootEvent: IEventElement;
            const splitMergeType = additionalRelations[0].getType();

            // If we have a COMPONENT_WITH relationship,
            // then we need the individual being split/merged from/into
            const hasComponent = additionalRelations
                .filter((relation) => relation.getType() === "COMPONENT_WITH");
            if (hasComponent) {
                console.debug("Has component with relationship");
                rootEvent = events[0];
            } else if ((splitMergeType === "MERGED_FROM") || (splitMergeType === "SPLIT_FROM")) {
                // Link to the start event
                rootEvent = events[0];
            } else {
                rootEvent = events[1];
            }
            // } else if ((splitMergeType === "SPLIT_INTO") || (splitMergeType === "SPLIT_FROM")) {
            //     // Link to the end event
            //     rootEvent = events[1];
            // } else {
            //     rootEvent = undefined;
            // }
            console.debug("Linking to root event:", rootEvent);

            const obsArray = additionalRelations.map((relation) => {
                console.debug("Getting attributes for:", relation.getObject());
                return this.vs.getTrestleIndividual(relation.getObject());
            });
            Observable.forkJoin(obsArray)
                .subscribe((objects) => {
                    console.debug("Have all observables:", objects);
                    objects.forEach((object) => {
                        const relatedEvents = this.buildObjectEvents(object,
                            object.getFilteredID(),
                            splitMergeType,
                            rootEvent);
                        events = events.concat(relatedEvents.events);
                        links = links.concat(relatedEvents.links);
                    });
                    const sortedEvents = this.sortEvents(events, links, filteredID);
                    console.debug("Sorted events:", sortedEvents);
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

        // We only have a single selection, add 2, just for giggles and spits
        if (eventBins === 1) {
            eventBins = 3;
        }

        // Set the selection equal to the middle value
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

                // And do so for all the other events of the given selection
                events
                    .filter((iEvent) => iEvent.entity === event.entity)
                    .map((iEvent) => iEvent.bin = currentBin);

                // Increment the step and flip the sign
                step++;
                sign = sign === "+" ? "-" : "+";
            });

        return {
            nodes: events,
            links,
            bins: eventBins
        };
    }

    private buildObjectEvents(individual: TrestleIndividual,
                              entityName: string,
                              relationType?: TrestleRelationType,
                              rootEvent?: IEventElement): {
        events: IEventElement[],
        links: IEventLink[]
    } {
        console.debug("Build events for selection: %s with relation type: %s",
            individual, relationType);
        // Split merge first,
        // because it'll show us if we need to drop a created or destroyed event
        const events: IEventElement[] = [];
        const links: IEventLink[] = [];

        // Get the start event
        const startEvent = individual.getStartEvent();
        const started = {
            id: entityName + "-" + startEvent.getType(),
            entity: entityName,
            bin: 1,
            value: "data",
            temporal: startEvent.getTemporal().toDate()
        };

        // If the root selection is a merged_from or split_from,
        // then we're looking for a link between the end event and an INTO relation
        if ((relationType === "MERGED_FROM") || (relationType === "SPLIT_FROM")) {
            console.debug("Has from");
            const fromEvent = {
                id: entityName + "-" + this.invertRelationship(relationType),
                entity: entityName,
                bin: 1,
                value: "into",
                // We can do this cast because if there's a merge event, there is some end point
                // We need to roll it back by 1 year, to make it look better
                temporal: (individual.getTemporal().getTo() as moment.Moment).add(-1, "year").toDate()
            };
            events.push(started, fromEvent);
            links.push({
                source: started,
                target: fromEvent
            });
            // If we also have a root event, draw a link between it and the split/merge event
            if (rootEvent) {
                links.push({
                    source: fromEvent,
                    target: rootEvent
                });
            }
            // We want the split event and the ending event
        } else if ((relationType === "SPLIT_INTO") || (relationType === "MERGED_INTO")) {
            console.debug("Has into");
            const endEvent = (individual.getEndEvent() as TrestleEvent);
            // We know that if there's a split going on, that there's an end event
            const end = {
                id: entityName + "-" + endEvent.getType(),
                entity: entityName,
                bin: 1,
                value: "data",
                temporal: endEvent.getTemporal().toDate()
            };
            const intoEvent = {
                id: entityName + "-" + this.invertRelationship(relationType),
                entity: entityName,
                bin: 1,
                value: "from",
                // We need to roll it forward by 1 year, for art's sake
                temporal: startEvent.getTemporal().add(1, "year").toDate()
            };
            events.push(end, intoEvent);
            links.push({
                source: intoEvent,
                target: end
            });
            // If we also have a root event, draw a link between it and the split/merge event
            if (rootEvent) {
                links.push({
                    source: intoEvent,
                    target: rootEvent
                });
            }
            //    Otherwise, link the start and end events
        } else {
            const endEvent = individual.getEndEvent();
            // If there is no end event, create a fake one and link it to the start event
            if (endEvent === null) {
                const destroyed = {
                    id: entityName + "-DESTROYED",
                    entity: entityName,
                    bin: 1,
                    value: "data",
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
                    value: "data",
                    temporal: (endEvent as TrestleEvent).getTemporal().toDate()
                };
                events.push(started, ended);
                links.push({
                    source: started,
                    target: ended
                });
            }
        }
        console.debug("Events for %s", individual.getID(), events);
        return {
            events,
            links
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
