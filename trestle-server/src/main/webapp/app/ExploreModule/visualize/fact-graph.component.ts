/**
 * Created by nrobison on 4/19/17.
 */
import {
    AfterViewInit,
    Component,
    ElementRef,
    Input,
    OnChanges,
    SimpleChange,
    ViewChild
} from "@angular/core";
import { select, Selection } from "d3-selection";
import { scaleBand, scaleOrdinal, ScaleTime, scaleTime, schemeCategory20 } from "d3-scale";
import { axisBottom, axisLeft } from "d3-axis";
import { TrestleFact, TrestleIndividual } from "./visualize.service";

interface ID3Margin {
    top: number;
    right: number;
    bottom: number;
    left: number;
}

@Component({
    selector: "fact-history",
    templateUrl: "./fact-graph.component.html",
    styleUrls: ["./fact-graph.component.css"]
})

export class FactHistoryGraph implements AfterViewInit, OnChanges {
    @ViewChild("graph") public element: ElementRef;
    @Input() public data: TrestleIndividual;
    @Input() public graphHeight: number;
    @Input() public minTime: Date;
    @Input() public maxTime: Date;
    private htmlElement: HTMLElement;
    private host: Selection<HTMLElement, any, any, any>;
    private svg: Selection<any, any, any, any>;
    private width: number;
    private height: number;
    private margin: ID3Margin;
    private x: ScaleTime<number, number>;

    constructor() {

    }

    public ngAfterViewInit(): void {
        console.debug("Fact History view-init");
        this.htmlElement = this.element.nativeElement;
        this.setupD3();
    }

    public ngOnChanges(changes: { [propKey: string]: SimpleChange }): void {
        const dataChange = changes["data"];
        if (dataChange != null && !dataChange.isFirstChange()) {
            this.plotData();
        }
    }

    private plotData(): void {
        //    Build the domain values
        console.debug("Building with data:", this.data);
        const factNames = this.data.getFacts().map((d) => d.getName());
        const y = scaleBand()
            .range([this.height, 0])
            .domain(factNames);
        console.debug("Y values", y.range());
        console.debug("Y values", y.domain());

        const z = scaleOrdinal(schemeCategory20)
            .domain(factNames);

        // Build the lane lines
        const laneLines = this.svg.selectAll(".laneLine")
            .data(this.data.getFacts().map(fact => fact.getName()))
            .enter().append("line")
            .attr("class", "laneLine")
            .attr("x1", 0)
            .attr("y1", (d) => y(d))
            .attr("x2", this.width)
            .attr("y2", (d) => y(d));

        //    Build the Y-Axis
        const yAxis = this.svg
            .append("g")
            .attr("class", "axis axis-y")
            .call(axisLeft(y));

        //    Add the data
        const mainItems = this.svg.selectAll(".fact")
            .data(this.data
                .getFacts()
                .filter((f: TrestleFact) => f
                    .getDatabaseTemporal()
                    .isContinuing()),
                (d: TrestleFact) => d.getID());

        mainItems
            .enter()
            .append("rect")
            .attr("class", "fact")
            .attr("x", (d: TrestleFact) => this.x(d.getValidTemporal().getFrom().toDate()))
            .attr("y", (d: TrestleFact) => y(d.getName()))
            .attr("width", (d: TrestleFact) => this.x(this.maybeDate(d.getValidTemporal().getTo().toDate())) - this.x(this.maybeDate(d.getValidTemporal().getFrom().toDate())))
            .attr("height", (d) => y.bandwidth())
            .style("fill", (d: TrestleFact) => z(d.getName()))
            .style("fill-opacity", 0.7)
            .merge(mainItems);

        // Labels
        const mainLabels = this.svg.selectAll(".mainLabels")
            .data(this.data.getFacts().filter((f) => f.getDatabaseTemporal().isContinuing()), (d: TrestleFact) => d.getID());

        mainLabels
            .enter()
            .append("text")
            .text((d: TrestleFact) => this.parseValue(d.getValue()))
            .attr("class", "mainLabels")
            .attr("x", (d: TrestleFact) => {
                const end = this.maybeDate(d.getValidTemporal().getTo().toDate());
                const start = this.maybeDate(d.getValidTemporal().getFrom().toDate());
                const width = this.x(end) - this.x(start);
                return this.x(start) + width / 2;
            })
            .attr("y", (d: TrestleFact) => y(d.getName()) + y.bandwidth() - 5)
            .attr("text-anchor", "middle")
            .attr("dy", ".1ex")
            .merge(mainLabels);
            // .attr("fill", "transparent");

        mainItems.exit().remove();
        mainLabels.exit().remove();
        yAxis.exit().remove();
    }

    private maybeDate(date: string | Date): Date {
        if (date instanceof Date) {
            return date;
        }
        if (date == "") {
            return this.maxTime;
        }
        return new Date(date);
    }

    private parseValue(value: string | number): string {
        if (typeof value === "number") {
            return value.toString();
        }
        if (value.length > 20) {
            return value.substring(0, 20) + "...";
        }
        return value;
    }

    private setupD3(): void {
        this.host = select(this.htmlElement);
        this.margin = {top: 20, right: 30, bottom: 20, left: 150};
        this.width = this.htmlElement.offsetWidth - this.margin.left - this.margin.right;
        this.height = this.graphHeight - this.margin.top - this.margin.bottom;

        this.svg = this.host.html("")
            .append("svg")
            .attr("width", this.width + this.margin.left + this.margin.right)
            .attr("height", this.height + this.margin.top + this.margin.bottom)
            .append("g")
            .attr("transform", "translate(" + this.margin.left + "," + this.margin.top + ")");

        //    TODO(nrobison): Move this to the updateFunction
        this.x = scaleTime().range([0, this.width]);
        this.x.domain([this.minTime, this.maxTime]);
        console.debug("X range", this.x.range());
        console.debug("X domain", this.x.domain());
        this.svg
            .append("g")
            .attr("class", "axis axis-x")
            .attr("transform", "translate(0," + this.height + ")")
            .call(axisBottom(this.x));

        console.debug("D3 Initialized");
    }
}
