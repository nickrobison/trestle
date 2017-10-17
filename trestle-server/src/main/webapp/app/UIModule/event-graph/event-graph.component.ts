import {
    AfterViewInit, Component, ElementRef, Input, OnChanges, SimpleChanges,
    ViewChild
} from "@angular/core";
import { BaseType, select, Selection } from "d3-selection";
import { scaleBand, scaleLinear, scaleOrdinal, scaleTime, schemeCategory10 } from "d3-scale";
import { axisBottom } from "d3-axis";

export interface IEventElement {
    id: string;
    entity: string;
    temporal: Date;
    bin: number;
    value: any;
    continuing?: boolean;
}

export interface IEventLink {
    source: IEventElement;
    target: IEventElement;
}

export interface IEventData {
    nodes: IEventElement[];
    links: IEventLink[];
}

@Component({
    selector: "event-graph",
    templateUrl: "./event-graph.component.html",
    styleUrls: ["./event-graph.component.css"]
})
export class EventGraphComponent implements AfterViewInit, OnChanges {
    @ViewChild("graph") public element: ElementRef;
    @Input() public data: IEventData;
    @Input() public graphHeight: number;
    @Input() public minDate: Date;
    @Input() public maxDate: Date;
    private htmlElement: HTMLElement;
    private host: Selection<HTMLElement, IEventElement | IEventLink, null, undefined>;
    private svg: Selection<BaseType, IEventElement | IEventLink, null, undefined>;
    private width: number;
    private height: number;
    private margin: ID3Margin;


    public constructor() {

    }

    public ngAfterViewInit(): void {
        this.htmlElement = this.element.nativeElement;
        this.setupD3();
    }

    public ngOnChanges(changes: SimpleChanges): void {
        const dataChange = changes["data"];
        if (dataChange != null
            && (dataChange.currentValue !== dataChange.previousValue)) {
            console.debug("Changed, plotting data", this.data);
            this.plotData();
        }
    }

    private plotData(): void {
        //    Setup the X/Y/Z values
        const entityNames = this.data.nodes.map((d) => d.entity);
        const bins = this.data.nodes.map((d) => d.bin);
        const x = scaleTime()
            .range([0, this.width])
            .domain([this.minDate, this.maxDate]);
        const y = scaleLinear()
            .range([this.height, 0])
            .domain([0, bins.length]);
        const z = scaleOrdinal(schemeCategory10)
            .domain(entityNames);

        // Filter down the events, to make sure they don't go off the screen
        this.data.nodes.map((node) => {
            if (node.temporal > this.maxDate) {
                node.continuing = true;
                node.temporal = this.maxDate;
            } else if (node.temporal < this.minDate) {
                node.continuing = true;
                node.temporal = this.minDate;
            }
        });

        //    Add the lines
        const links = this.svg
            .selectAll(".link")
            .data(this.data.links, (link: IEventLink) => link.source.id + "_" + link.target.id);

        links
            .enter()
            .append("line")
            .attr("class", "link")
            .attr("x1", (d) => x(d.source.temporal))
            .attr("y1", (d) => y(d.source.bin) || null)
            .attr("x2", (d) => x(d.target.temporal))
            .attr("y2", (d) => y(d.target.bin) || null)
            .merge(links);

        links
            .exit()
            .remove();

        //    Add the nodes to the graph
        const nodes = this.svg
            .selectAll(".node")
            .data(this.data.nodes, (data: IEventElement) => data.id);

        nodes
            .enter()
            .append("circle")
            .attr("class", "node")
            .attr("cx", (d) => x(d.temporal))
            .attr("cy", (d) => y(d.bin) || null)
            .attr("r", 9)
            .attr("name", (d) => d.bin)
            .attr("node-id", (d) => d.id)
            .attr("entity", (d) => d.entity)
            .style("fill", (d: IEventElement) => z(d.entity))
            .style("opacity", (d) => d.continuing ? 0.7 : 1.0)
            .merge(nodes);

        nodes
            .exit()
            .remove();

        //    Update the X-axis
        const xSelection = this.svg.selectAll("g.x-axis");
        if (xSelection.empty()) {
            this.svg
                .append("g")
                .attr("class", "axis x-axis")
                .attr("transform", "translate(0," + this.height + ")")
                .call(axisBottom(x));
        } else {
            xSelection
                .call(axisBottom(x));
        }
    }

    private setupD3(): void {
        this.host = select<HTMLElement, IEventElement | IEventLink>(this.htmlElement);
        this.margin = {top: 20, right: 30, bottom: 20, left: 20};
        this.width = this.htmlElement.offsetWidth - this.margin.left - this.margin.right;
        this.height = this.graphHeight - this.margin.top - this.margin.bottom;

        this.svg = this.host.html("")
            .append("svg")
            .attr("width", this.width + this.margin.left + this.margin.right)
            .attr("height", this.height + this.margin.top + this.margin.bottom)
            .append("g")
            .attr("transform", "translate(" + this.margin.left + "," + this.margin.top + ")");

        // this.initXAxis();

        console.debug("D3 Initialized");
    }

    private initXAxis(): void {
        const x = scaleTime()
            .range([0, this.width])
            .domain([this.minDate, this.maxDate]);

        //    Add the X-axis
        this.svg
            .append("g")
            .attr("class", "axis x-axis")
            .attr("transform", "translate(0," + this.height + ")")
            .call(axisBottom(x));
    }
}
