import { AfterViewInit, Component, ElementRef, ViewChild } from "@angular/core";
import { ScaleLinear, scaleLinear } from "d3-scale";
import { BaseType, select, Selection } from "d3-selection";
import * as moment from "moment";
import { Moment } from "moment";
import { axisBottom, axisLeft } from "d3-axis";

@Component({
    selector: "tree-graph",
    templateUrl: "./tree-graph.component.html",
    styleUrls: ["./tree-graph.component.css"]
})
export class TreeGraphComponent implements AfterViewInit {

    @ViewChild("graph")
    public element: ElementRef;
    private htmlElement: HTMLElement;
    private host: Selection<HTMLElement, any, null, undefined>;
    private svg: Selection<BaseType, any, null, undefined>;
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

    private setupD3(): void {
        this.host = select<HTMLElement, any>(this.htmlElement);
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
