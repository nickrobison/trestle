import { AfterViewInit, Component, ElementRef, Input, OnChanges, SimpleChanges, ViewChild } from "@angular/core";
import { ScaleLinear, scaleLinear } from "d3-scale";
import { BaseType, select, Selection } from "d3-selection";
import * as moment from "moment";
import { Moment } from "moment";
import { axisBottom, axisLeft } from "d3-axis";
import { IIndexLeafStatistics } from "../index.service";

@Component({
    selector: "tree-graph",
    templateUrl: "./tree-graph.component.html",
    styleUrls: ["./tree-graph.component.css"]
})
export class TreeGraphComponent implements AfterViewInit, OnChanges {

    @ViewChild("graph")
    public element: ElementRef;
    @Input()
    public data: IIndexLeafStatistics[];
    private htmlElement: HTMLElement;
    private host: Selection<HTMLElement, IIndexLeafStatistics, null, undefined>;
    private svg: Selection<BaseType, IIndexLeafStatistics, null, undefined>;
    private width: number;
    private height: number;
    private margin: ID3Margin;
    private maxTime: Moment;
    private x: ScaleLinear<number, number>;
    private y: ScaleLinear<number, number>;

    public constructor() {
        this.maxTime = moment("3001-01-01").startOf("year");
    }

    public ngAfterViewInit(): void {
        this.htmlElement = this.element.nativeElement;
        this.setupD3();
    }

    public ngOnChanges(changes: SimpleChanges): void {
        const dataChanges = changes["data"];
        if (dataChanges.currentValue !== dataChanges.previousValue) {
            console.debug("Plotting new changes");
            this.plotData(dataChanges.currentValue);
        }
    }

    private plotData(data: IIndexLeafStatistics[]): void {
    //    For each, leaf, draw the triangle
        const leafData = this.svg
            .selectAll(".leaf")
            .data(data, (d: any) => d.leafID);

        leafData
            .enter()
            .append("polygon")
            .attr("class", "leaf")
            .attr("points", (d: any) => this.normalizeTriangle(d.coordinates))
            .merge(leafData);
    }

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
            normalized[idx] = isX ? this.x(coordinates[idx]) : this.y(coordinates[idx]);
            isX = !isX;
        }
        return normalized.join(",");
    }

    private setupD3(): void {
        this.host = select<HTMLElement, IIndexLeafStatistics>(this.htmlElement);
        this.margin = this.margin = {top: 20, right: 30, bottom: 20, left: 30};
        this.width = this.htmlElement.offsetWidth - this.margin.left - this.margin.right;
        this.height = 500 - this.margin.top - this.margin.bottom;

        // Set the X/Y axis
        this.x = scaleLinear().range([0, this.width]);
        this.x.domain([0, this.maxTime.valueOf()]);
        this.y = scaleLinear().range([0, this.height]);
        this.y.domain([0, this.maxTime.valueOf()]);

        console.debug("Y:", this.y);
        console.debug("Yd:", this.y.domain());
        console.debug("Yr:", this.y.range());

        console.debug("X:", this.x);
        console.debug("Xd:", this.x.domain());
        console.debug("Xr:", this.x.range());

        this.svg = this.host.html("")
            .append("svg")
            .attr("width", this.width + this.margin.left + this.margin.right)
            .attr("height", this.height + this.margin.top + this.margin.bottom)
            .append("g")
            .attr("transform", "translate(" + this.margin.left + "," + this.margin.top + ")");

        // Add the axises
        this.svg
            .append("g")
            .attr("class", "axis x-axis")
            .attr("transform", "translate(0," + this.height + ")")
            .call(axisBottom(this.x));

        this.svg
            .append("g")
            .attr("class", "axis axis-y")
            .append("text")
            .attr("transform", "rotate(-90)")
            .attr("y", 6)
            .attr("dy", "0.71em")
            .attr("fill", "#000")
            .text("Value")
            .call(axisLeft(this.y));

        //    Draw a line
        this.svg
            .append("g")
            .attr("class", "dividing-line")
            .append("line")
            .attr("x1", 0)
            .attr("x2", this.x(this.maxTime.valueOf()))
            // .attr("y1", this.height)
            .attr("y1", this.y(this.maxTime.valueOf()))
            .attr("y2", 0)
            .attr("stroke-width", 2)
            .attr("stroke", "black");
    }
}
