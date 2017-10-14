import { AfterViewInit, Component, ElementRef, Input, ViewChild } from "@angular/core";
import {Selection, select, event, BaseType} from "d3-selection";
import { ScaleTime, scaleTime } from "d3-scale";
import { Moment } from "moment";
import { axisBottom } from "d3-axis";

interface ID3Margin {
    top: number;
    right: number;
    bottom: number;
    left: number;
}

@Component({
    selector: "object-events",
    templateUrl: "./object-events.component.html",
    styleUrls: ["./object-events.component.css"]
})
export class ObjectEventGraphComponent implements AfterViewInit {

    @ViewChild("container") public element: ElementRef;
    @Input() public minTime: Moment;
    @Input() public maxTime: Moment;

    private htmlElement: HTMLElement;
    private host: Selection<Element, {}, null, undefined>;
    private svg: Selection<BaseType, {}, null, undefined>;
    private margin: ID3Margin;
    private width: number;
    private height: number;
    private x: ScaleTime<number, number>;

    constructor() {}

    public ngAfterViewInit(): void {
        console.debug("event graph view-init");

        this.htmlElement = this.element.nativeElement;
        this.setupD3();
    }

    private setupD3() {
        this.host = select(this.htmlElement);
        this.margin = {top: 10, right: 20, bottom: 20, left: 10};
        this.width = this.htmlElement.offsetWidth - this.margin.left - this.margin.right;
        this.height = 500 - this.margin.top - this.margin.bottom;
        this.svg = this.host.html("")
            .append("svg")
            .attr("width", this.width + this.margin.left + this.margin.right)
            .attr("height", this.height + this.margin.top + this.margin.bottom)
            .append("g")
            .attr("transform", "translate(" + this.margin.left + "," + this.margin.top + ")");

        console.debug("Object event graph initialized");
    //    Setup the x axis
        this.x = scaleTime().range([0, this.width]);
        this.x.domain([this.minTime.toDate(), this.maxTime.toDate()]);
        console.debug("X range:", this.x.range());
        console.debug("X domain:", this.x.domain());
        this.svg
            .append("g")
            .attr("class", "axis axis-x")
            .attr("transform", "translate(0," + this.height + ")")
            .call(axisBottom(this.x));

        console.debug("D3 initialized");
    }
}