import { AfterViewInit, Component, ElementRef, EventEmitter, Input, OnChanges, Output, SimpleChanges, ViewChild } from "@angular/core";
import { ScaleLinear, scaleLinear } from "d3-scale";
import { BaseType, select, Selection } from "d3-selection";
import moment, { Moment } from "moment";
import { axisBottom, axisLeft } from "d3-axis";
import { IIndexLeafStatistics } from "../index.service";
import { interpolateHsl } from "d3-interpolate";
import {ID3Margin} from '../../../ui/common';
import {BehaviorSubject, Subject} from 'rxjs';
import {RoundingPipe} from '../../../shared/pipes/rounding-pipe.pipe';

export interface IGraphHeader {
    maxValue: number;
    offsetValue: number;
    leafs: IIndexLeafStatistics[];
}

@Component({
    selector: "tree-graph",
    templateUrl: "./tree-graph.component.html",
    styleUrls: ["./tree-graph.component.scss"]
})
export class TreeGraphComponent implements AfterViewInit, OnChanges {

    @ViewChild("graph")
    public element: ElementRef;
    @Input()
    public data: IGraphHeader;
    @Output()
    public hovered: EventEmitter<string>;
    private htmlElement: HTMLElement;
    private host: Selection<HTMLElement, IIndexLeafStatistics, null, undefined>;
    private svg: Selection<BaseType, IIndexLeafStatistics, null, undefined>;
    private width: number;
    private height: number;
    private margin: ID3Margin;
    private maxTime: Moment;
    private x: ScaleLinear<number, number>;
    private y: ScaleLinear<number, number>;
    private colorScale: (value: number) => string;
    private dataSubject: Subject<IGraphHeader | undefined>;
    private rounder: RoundingPipe;

    public constructor() {
        this.maxTime = moment("5000-01-01").startOf("year");
        this.colorScale = interpolateHsl("steelblue", "brown");
        this.rounder = new RoundingPipe();
        this.hovered = new EventEmitter<string>();
        this.dataSubject = new BehaviorSubject(undefined);
    }

    public ngAfterViewInit(): void {
        this.htmlElement = this.element.nativeElement;
        this.setupD3();

        //    Subscribe
        this.dataSubject
            .subscribe((value) => {
                if (value !== undefined) {
                    this.plotData(value);
                    this.data = value;
                }
            });
    }

    public ngOnChanges(changes: SimpleChanges): void {
        const dataChanges = changes["data"];
        if (dataChanges.currentValue !== dataChanges.previousValue) {
            console.debug("Plotting new changes");
            this.dataSubject.next(dataChanges.currentValue);
        }
    }

    private plotData(data: IGraphHeader): void {
        // Nuke everything, because I can't figure out the update pattern
        this.svg.selectAll("*").remove();
        // Calculate the max time, which need to adjust from the cache values
        this.maxTime = moment(data.maxValue - data.offsetValue);

        console.debug("Plotting with max: %s and offset: %s", data.maxValue, data.offsetValue);

        // Set the X/Y scales
        this.x = scaleLinear().range([0, this.width]);
        this.x.domain([0, data.maxValue]);
        // We need to invert this, in order to get the triangles to draw in the correct direction
        this.y = scaleLinear().range([this.height, 0]);
        this.y.domain([0, data.maxValue]);

        // Add the axises
        this.svg
            .append("g")
            .attr("class", "axis x-axis")
            .attr("transform", "translate(0," + this.height + ")")
            .call(axisBottom(this.x)
                .tickFormat((d) => TreeGraphComponent.adjustTemporals(d, data.offsetValue)))
            .selectAll("text")
            .attr("y", 0)
            .attr("x", 9)
            .attr("dy", ".35em")
            .attr("transform", "rotate(45)")
            .style("text-anchor", "start");

        this.svg
            .append("g")
            .attr("class", "axis axis-y")
            .call(axisLeft(this.y)
                .tickFormat((d) => TreeGraphComponent.adjustTemporals(d, data.offsetValue)))
            .selectAll("text")
            .attr("y", 0)
            .attr("x", -10)
            .attr("dy", ".35em")
            .style("text-anchor", "end");

        //    Draw a line
        // this.svg
        //     .append("g")
        //     .attr("class", "dividing-line")
        //     .append("line")
        //     .attr("x1", 0)
        //     .attr("x2", this.x(data.maxValue))
        //     // .attr("y1", this.height)
        //     .attr("y1", this.height)
        //     .attr("y2", 0)
        //     .attr("stroke-width", 3)
        //     .attr("stroke", "black");


        //    For each, leaf, draw the triangle
        const leafData = this.svg
            .selectAll<SVGPolygonElement, BaseType>(".leaf")
            // This index function needs to be a string, for some reason
            .data(data.leafs, (d: IIndexLeafStatistics) => d.binaryID);

        leafData
            .enter()
            .append("polygon")
            .attr("id", (d) => d.binaryID)
            .attr("direction", (d) => d.direction.toString())
            .attr("coords", (d) => d.coordinates.join(","))
            .attr("class", "leaf")
            .attr("points", (d) => this.normalizeTriangle(d.coordinates))
            .attr("stroke-width", 1)
            .attr("stroke", "black")
            // .attr("fill", (d) => this.colorScale((d.records / 20) + 0.01))
            .attr("fill", "blue")
            .style("fill-opacity", 0.7)
            // Hover handlers
            .on("mouseover", this.hoverHandler)
            .merge(leafData);

    }

    private hoverHandler = (d: IIndexLeafStatistics): void => {
        console.debug("Hovered", d.leafID.toString());
        this.hovered.next(d.leafID.toString());
    };

    /**
     * Normalize coordinates (represented as millis from UTC epoch) into D3 coordinates
     * @param {number[]} coordinates
     * @returns {string}
     */
    private normalizeTriangle(coordinates: number[]): string {
        const size = coordinates.length;
        const normalized = new Array(size);
        let isX = true;
        for (let idx = 0; idx < size; idx++) {
            // console.debug("Rounding %s to %s", coordinates[idx], this.rounder.transform(coordinates[idx], 0));
            const rounded = this.rounder.transform(coordinates[idx], 0);
            normalized[idx] = isX ? this.x(rounded) : this.y(rounded);
            isX = !isX;
        }
        return normalized.join(",");
    }

    private static adjustTemporals(domain: any, offsetValue: number): string {
        return new Date(domain - offsetValue).toISOString();
    }

    private setupD3(): void {
        this.host = select<HTMLElement, IIndexLeafStatistics>(this.htmlElement);
        this.margin = this.margin = {top: 20, right: 30, bottom: 100, left: 150};
        this.width = this.htmlElement.offsetWidth - this.margin.left - this.margin.right;
        this.height = 500 - this.margin.top - this.margin.bottom;

        this.svg = this.host.html("")
            .append("svg")
            .attr("width", this.width + this.margin.left + this.margin.right)
            .attr("height", this.height + this.margin.top + this.margin.bottom)
            .append("g")
            .attr("transform", "translate(" + this.margin.left + "," + this.margin.top + ")");
    }
}
