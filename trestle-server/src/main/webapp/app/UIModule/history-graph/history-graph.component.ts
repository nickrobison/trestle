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
import { BaseType, select, Selection } from "d3-selection";
import { scaleBand, scaleOrdinal, ScaleTime, scaleTime, schemeCategory20 } from "d3-scale";
import { axisBottom, axisLeft } from "d3-axis";

export interface ITemporalEntity {
    label: string;
    start: Date;
    end: Date;
    value: any;
}

export interface IIndividualHistory {
    entities: ITemporalEntity[];
}

interface ID3Margin {
    top: number;
    right: number;
    bottom: number;
    left: number;
}

@Component({
    selector: "history-graph",
    templateUrl: "./history-graph.component.html",
    styleUrls: ["./history-graph.component.css"]
})
export class HistoryGraphComponent implements AfterViewInit, OnChanges {
    @ViewChild("graph") public element: ElementRef;
    @Input() public data: IIndividualHistory;
    @Input() public graphHeight: number;
    @Input() public minTime: Date;
    @Input() public maxTime: Date;
    private htmlElement: HTMLElement;
    private host: Selection<HTMLElement, ITemporalEntity, Document, ITemporalEntity>;
    private svg: Selection<BaseType, ITemporalEntity, Document, ITemporalEntity>;
    private width: number;
    private height: number;
    private margin: ID3Margin;
    private x: ScaleTime<number, number>;

    constructor() {}

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
        const entityNames = this.data.entities.map((d) => d.label);
        // const entityNames = this.data.getFacts().map((d) => d.getName());
        const y = scaleBand()
            .range([this.height, 0])
            .domain(entityNames);
        console.debug("Y values", y.range());
        console.debug("Y values", y.domain());

        const z = scaleOrdinal(schemeCategory20)
            .domain(entityNames);

        // Build the lane lines
        const laneLines = this.svg.selectAll(".laneLine")
            // .data(this.data.getFacts().map((fact) => fact.getName()))
            .data(this.data.entities.map((entity) => entity.label))
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
            .data(this.data.entities, (entity: ITemporalEntity) => entity.label);

        mainItems
            .enter()
            .append("rect")
            .attr("class", "fact")
            .attr("x", (d) => this.normalizeAxis("x", this.x(d.start)))
            .attr("y", (d) => y(d.label))
            .attr("width",
                (d) => {
                    const end = this.normalizeAxis("x",
                        this.x(d.end));
                    const start = this.normalizeAxis("x",
                        this.x(d.start));
                    return end - start;
                })
            .attr("height", (d) => y.bandwidth())
            // .style("fill", (d: TrestleFact) => z(d.getName()))
            .style("fill", (d) => z(d.label))
            .style("fill-opacity", 0.7)
            .merge(mainItems);

        // Labels
        const mainLabels = this.svg.selectAll(".mainLabels")
            .data(this.data.entities, (d: ITemporalEntity) => d.label);

        mainLabels
            .enter()
            .append("text")
            .text((d) => this.parseValue(d.value))
            .attr("class", "mainLabels")
            .attr("x", (d) => {
                const end = d.end;
                const start = d.start;
                const width = this.x(end) - this.x(start);
                return this.x(start) + width / 2;
            })
            .attr("y", (d) => y(d.label) + y.bandwidth() - 5)
            .attr("text-anchor", "middle")
            .attr("dy", ".1ex")
            .merge(mainLabels);

        mainItems.exit().remove();
        mainLabels.exit().remove();
        yAxis.exit().remove();
    }

    private maybeDate(date: string | Date): Date {
        if (date instanceof Date) {
            return date;
        }
        if (date === "") {
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

    private normalizeAxis(axis: "x" | "y", value: number): number {
        // Normalize X Axis
        if (axis === "x") {
            if (value < 0) {
                return 0;
            }
            if (value > this.width) {
                return this.width;
            }
            return value;
        } else {
            if (value < 0) {
                return 0;
            }
            if (value > this.height) {
                return this.height;
            }
            return value;
        }
    }

    private setupD3(): void {
        this.host = select<HTMLElement, ITemporalEntity>(this.htmlElement);
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
