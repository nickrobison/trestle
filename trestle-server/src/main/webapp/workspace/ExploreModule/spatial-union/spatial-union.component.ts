import { Component, Input, OnChanges, SimpleChanges } from "@angular/core";
import { IIndividualHistory } from "../../UIModule/history-graph/history-graph.component";
import { TrestleIndividual } from "../../SharedModule/individual/TrestleIndividual/trestle-individual";
import { IEventData, IEventElement, IEventLink } from "../../UIModule/event-graph/event-graph.component";
import { Observable } from "rxjs/Observable";
import { IndividualService } from "../../SharedModule/individual/individual.service";
import { TrestleRelationType } from "../../SharedModule/individual/TrestleIndividual/trestle-relation";
import * as moment from "moment";
import { TrestleEvent } from "../../SharedModule/individual/TrestleIndividual/trestle-event";

@Component({
    selector: "spatial-union",
    templateUrl: "./spatial-union.component.html",
    styleUrls: ["./spatial-union.component.css"]
})
export class SpatialUnionComponent implements OnChanges {
    @Input()
    public individual: TrestleIndividual;
    @Input()
    public minDate: Date;
    @Input()
    public maxDate: Date;
    public eventData: IEventData;

    public constructor(private vs: IndividualService) { }

    public ngOnChanges(changes: SimpleChanges): void {
        const changedData = changes["individual"];
        if (changedData !== undefined &&
            changedData.currentValue !== changedData.previousValue) {
            console.debug("has new data?", changedData.currentValue !== changedData.previousValue);
            this.individual = changedData.currentValue;
            this.buildHistoryGraph(this.individual);
        }
    }

    /**
     * Filter individual label and only return the suffix
     * @param {IEventElement} input
     * @returns {string}
     */
    public filterLabel(input: IEventElement): string {
        return TrestleIndividual.extractSuffix(input.entity);
    }

    private buildHistoryGraph(individual: TrestleIndividual): void {
        console.debug("Individual has events", individual.getEvents());
        //    Get the split/merged/component relations
        const additionalRelations = individual
            .getRelations()
            .filter((relation) => (relation.getType() === "MERGED_FROM")
                || (relation.getType() === "MERGED_INTO")
                || (relation.getType() === "SPLIT_FROM")
                || (relation.getType() === "SPLIT_INTO")
                || (relation.getType() === "COMPONENT_WITH"));
        console.debug("Individual %s has relations %O",
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
            // const hasComponent = additionalRelations
            //     .filter((relation) => relation.getType() === "COMPONENT_WITH");
            // if (hasComponent) {
            //     console.debug("Has component with relationship");
            //     rootEvent = events[0];
            if ((splitMergeType === "MERGED_FROM") || (splitMergeType === "SPLIT_FROM")) {
                // Link to the start event
                rootEvent = events[0];
            } else {
                rootEvent = events[1];
            }

            const obsArray = additionalRelations.map((relation) => {
                console.debug("Getting attributes for:", relation.getObject());
                return this.vs.getTrestleIndividual(relation.getObject());
            });
            Observable.forkJoin(obsArray)
                .subscribe((objects) => {
                    console.debug("Adding related events for:", individual);
                    console.debug("Have all observables:", objects);

                    // Do we have a component with relationship?
                    // If so, find who we're supposed to split/merge with, and use that as the root event
                    const hasComponent = additionalRelations
                        .filter((relation) => relation.getType() === "COMPONENT_WITH");
                    if (hasComponent.length > 0) {
                        console.debug("has component with:", hasComponent);
                        const splitMergeID = additionalRelations[0].getObject();
                        console.debug("Using %s as root individual", splitMergeID);
                        const splitMergeIndividual = objects
                            .filter((obj) => obj.getID() === splitMergeID);
                        console.debug("Found individual:", splitMergeIndividual[0]);
                        if (!splitMergeIndividual) {
                            throw new Error("Can't find who to split/merge with during component_with");
                        }
                        const smiEvents = this.buildObjectEvents(splitMergeIndividual[0], splitMergeIndividual[0].getFilteredID(),
                            undefined, undefined, true);
                        if ((splitMergeType === "MERGED_FROM") || (splitMergeType === "SPLIT_FROM")) {
                            // Link to the start event
                            rootEvent = smiEvents.events[1];
                        } else {
                            rootEvent = smiEvents.events[0];
                        }
                        console.debug("Using as root event:", rootEvent);
                        //    Add the events
                        events = events.concat(smiEvents.events);
                        links = links.concat(smiEvents.links);
                    }


                    objects.forEach((object) => {
                        // If you're the root event, don't process yourself
                        if (object.getFilteredID() !== rootEvent.entity) {
                            const relatedEvents = this.buildObjectEvents(object,
                                object.getFilteredID(),
                                // If it's a component with relationship, don't deal with the split/merges
                                splitMergeType,
                                rootEvent,
                                hasComponent.length > 0);
                            events = events.concat(relatedEvents.events);
                            links = links.concat(relatedEvents.links);
                        }
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
        console.debug("Sorting with %s in the middle", individualID);

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
            .filter((event) => (event.value === "from") || (event.value === "into") || (event.value === "component"))
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



    /**
     * Build events for individual
     *
     * @param {TrestleIndividual} individual
     * @param {string} entityName
     * @param {TrestleRelationType} relationType
     * @param {IEventElement} rootEvent
     * @param {boolean} componentWith
     * @returns {{events: IEventElement[]; links: IEventLink[]}}
     */
    private buildObjectEvents(individual: TrestleIndividual,
                              entityName: string,
                              relationType?: TrestleRelationType,
                              rootEvent?: IEventElement,
                              componentWith = false): {
        events: IEventElement[],
        links: IEventLink[]
    } {
        console.debug("Build events for selection: %s with relation type: %s",
            individual.getID(), relationType);
        console.debug("Using root event:", rootEvent);
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
            const adjustedTemporal = (individual.getTemporal().getTo() as moment.Moment)
                .clone().add(-1, "year").toDate();
            const fromEvent = {
                id: entityName + "-" + this.invertRelationship(relationType),
                entity: entityName,
                bin: 1,
                value: "into",
                // We can do this cast because if there's    a merge event, there is some end point
                // We need to roll it back by 1 year, to make it look better
                temporal: adjustedTemporal
            };
            events.push(started, fromEvent);
            links.push({
                source: started,
                target: fromEvent
            });
            // If we also have a root event, draw a link between it and the split/merge event
            if (rootEvent && !componentWith) {
                console.debug("Writing link between %O and %O", fromEvent, rootEvent);
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
            const adjustedDate = startEvent.getTemporal().clone()
                .add(1, "year").toDate();
            const intoEvent = {
                id: entityName + "-" + this.invertRelationship(relationType),
                entity: entityName,
                bin: 1,
                value: "from",
                // We need to roll it forward by 1 year, for art's sake
                temporal: adjustedDate
            };
            console.debug("Writing link between %O and %O", end, intoEvent);
            events.push(end, intoEvent);
            links.push({
                source: intoEvent,
                target: end
            });
            // If we also have a root event, draw a link between it and the end event
            if (rootEvent && !componentWith) {
                console.debug("Writing link between %O and %O", intoEvent, rootEvent);
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
                console.debug("Something else:", relationType);
                const ended = {
                    id: entityName + "-" + (endEvent as TrestleEvent).getType(),
                    entity: entityName,
                    bin: 1,
                    value: componentWith === true ? "component" : "data",
                    temporal: (endEvent as TrestleEvent).getTemporal().toDate()
                };
                events.push(started, ended);
                links.push({
                    source: started,
                    target: ended
                });

                //    Add root event?
                if (rootEvent) {
                    console.debug("Should add root event link");
                }
            }
        }
        console.debug("Events for %s", individual.getID(), events);
        return {
            events,
            links
        };
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
