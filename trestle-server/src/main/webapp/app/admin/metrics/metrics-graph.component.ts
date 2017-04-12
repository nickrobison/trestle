/**
 * Created by nrobison on 4/4/17.
 */
import {
    AfterViewInit, Component, ElementRef, Input, OnChanges, SimpleChange, SimpleChanges,
    ViewChild
} from "@angular/core";
import {Selection, select} from "d3-selection";
import {scaleLinear, scaleOrdinal, scaleTime, ScaleTime, schemeCategory10} from "d3-scale";
import {curveBasis, line} from "d3-shape";
import {max, min} from "d3-array";
import {axisBottom, axisLeft} from "d3-axis";
import {IMetricsData, IMetricsValue} from "./metrics.service";

interface ID3Margin {
    top: number;
    right: number;
    bottom: number;
    left: number;
}

@Component({
    selector: "metrics-graph",
    templateUrl: "./metrics-graph.component.html",
    styleUrls: ["./metrics-graph.component.css"]
})

export class MetricsGraph implements AfterViewInit, OnChanges {
    @ViewChild("container") element: ElementRef;
    @Input() data: IMetricsData;
    @Input() minTime: Date;
    @Input() maxTime: Date;
    private graphData: Array<IMetricsData> = [];
    private htmlElement: HTMLElement;
    private host: Selection<any, any, any, any>;
    private svg: Selection<any, any, any, any>;
    private width: number;
    private height: number;
    private margin: ID3Margin;
    private x: ScaleTime<number, number>;
    private visible: Map<string, boolean> = new Map();

    construct() {
    }

    ngAfterViewInit(): void {
        console.debug("Graph view-init");
        this.htmlElement = this.element.nativeElement;
        this.setupD3();

    }

    ngOnChanges(changes: { [propKey: string]: SimpleChange }): void {
        let dataChange = changes["data"];
        if (dataChange != null && !dataChange.isFirstChange() && (dataChange.currentValue !== dataChange.previousValue)) {
            console.debug("Updated, plotting");
            let currentValue: IMetricsData = changes["data"].currentValue;
            this.graphData.push(currentValue);
            console.debug("Adding as visible:", currentValue.metric);
            this.visible.set(currentValue.metric, true);
            this.plotData();
        }
    }

    getVisibleMetrics(): Array<string> {
        let metrics = Array<string>();
        this.visible.forEach((value, key, map) => {
            if (value) {
                metrics.push(key);
            }
        });
        return metrics;
    }

    private setupD3(): void {
        this.host = select(this.htmlElement);
        this.margin = {top: 20, right: 200, bottom: 20, left: 70};
        this.width = this.htmlElement.offsetWidth - this.margin.left - this.margin.right;
        this.height = 500 - this.margin.top - this.margin.bottom;
        console.debug("Creating D3 graph with width/height", this.width + "/" + this.height);
        this.svg = this.host.html("")
            .append("svg")
            .attr("width", this.width + this.margin.left + this.margin.right)
            .attr("height", this.height + this.margin.top + this.margin.bottom)
            .append("g")
            .attr("transform", "translate(" + this.margin.left + "," + this.margin.top + ")");

        // Setup the x axis
        this.x = scaleTime().range([0, this.width]);
        this.x.domain([this.minTime, this.maxTime]);
        this.svg
            .append("g")
            .attr("class", "axis axis-x")
            .attr("transform", "translate(0," + this.height + ")")
            .call(axisBottom(this.x));
        // this.x.domain(extent(this.graphData[0].values, (d: IMetricsValue)=> d.timestamp));
        console.debug("D3 initialized");

    }

    private plotData(): void {

        let y = scaleLinear().range([this.height, 0]);
        let z = scaleOrdinal(schemeCategory10);

        let metricsLine = line()
            .curve(curveBasis)
            .x((d: any) => this.x(d.timestamp))
            .y((d: any) => y(d.value));

        //Build domain values
        y.domain([
            min(this.graphData, (d: IMetricsData) => min(d.values, (d: IMetricsValue) => d.value)),
            max(this.graphData, (d: IMetricsData) => max(d.values, (d: IMetricsValue) => d.value))
        ]);

        z.domain(this.graphData.map((d) => d.metric));
        console.debug("Z-domain", z.domain());

        this.svg
            .append("g")
            .attr("class", "axis axis-y")
            .call(axisLeft(y))
            .append("text")
            .attr("transform", "rotate(-90)")
            .attr("y", 6)
            .attr("dy", "0.71em")
            .attr("fill", "#000")
            .text("Value");

        let metric = this.svg.selectAll(".metric")
            .data(this.graphData)
            .enter().append("g")
            .attr("class", "metric");

        metric
            .append("path")
            .attr("class", "line")
            .attr("id", (d) => d.metric.replace(/\./g, "-"))
            .attr("d", (d: any) => metricsLine(d.values))
            // .attr("data-legend", (d) => d.metric)
            .style("stroke", (d) => z(d.metric));

    //    Add the legend
        let legend = this.svg.selectAll(".legend")
            .data(z.domain())
            .enter()
            .append("g")
            .attr("class", "legend")
            .attr("id", (d) => "legend-" + d.replace(/\./g, "-"))
            .attr("transform", (d, i) => "translate(" + (this.width) + "," + (i * ((this.width / 100) * 2) + 15) + ")")
            .on("click", this.legendClickHandler);

        legend
            .append("circle")
            .attr("cx", 30)
            .attr("cy", 30)
            .attr("r", this.width / 150)
            .style("fill", z);

        legend
            .append("text")
            .attr("x", 40)
            .attr("y", 30)
            .attr("dy", "0.25em")
            .style("text-anchor", "start")
            .text(d => d);

    }

    private legendClickHandler = (d: string): void => {
        console.debug("Clicked", d);
        console.debug("Visible", this.visible);
        let isVisible = this.visible.get(d);
        console.debug("Metric: " + d + " is visible?", isVisible);
        if (isVisible) {
            console.debug("Going out");
            this.svg.selectAll("#" + d.replace(/\./g, "-"))
                .transition()
                .duration(1000)
                .style("opacity", 0);

            // Fade the legend
            this.svg.select("#legend-" + d.replace(/\./g, "-"))
                .transition()
                .duration(1000)
                .style("opacity", .2);

            // Fade the y-axis
            this.svg.select("#y-" + d.replace(/\./g, "-"))
                .transition()
                .duration(1000)
                .style("opacity", .2);

            this.visible.set(d, false);
        } else {
            console.debug("Coming back");
            this.svg.selectAll("#" + d.replace(/\./g, "-"))
                // .style("display", "block")
                .transition()
                .duration(1000)
                .style("opacity", 1);

            // Fade the legend
            this.svg.select("#legend-" + d.replace(/\./g, "-"))
                .transition()
                .duration(1000)
                .style("opacity", 1);

            // Fade the y-axis
            this.svg.select("#y-" + d.replace(/\./g, "-"))
                .transition()
                .duration(1000)
                .style("opacity", 1);

            this.visible.set(d, true);
        }
    }
}