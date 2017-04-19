/**
 * Created by nrobison on 3/16/17.
 */
import {Component, AfterViewInit, ElementRef, ViewChild, Input, OnChanges, SimpleChange} from "@angular/core";
import {Selection, select, BaseType} from "d3-selection";
import {ScaleOrdinal, schemeCategory20, scaleOrdinal} from "d3-scale";
import {
    SimulationNodeDatum,
    forceSimulation,
    forceManyBody,
    forceCenter,
    forceLink,
    SimulationLinkDatum, Simulation
} from "d3-force";
import {ITrestleIndividual} from "./visualize.service";

export interface IIndividualConfig {
    data: ITrestleIndividual;
}

const enum NodeType {
    INDIVIDUAL,
    VTEMPORAL,
    DTEMPORAL,
    FACT
}

interface ID3Margin {
    top: number;
    right: number;
    bottom: number;
    left: number;
}

interface IGraphLayout {
    nodes: Array<IFactNode>;
    links: Array<SimulationLinkDatum<IFactNode>>;
}

interface IFactNode extends SimulationNodeDatum {
    id: string;
    group: number;
}

@Component({
    selector: "individual-graph",
    templateUrl: "./individual-graph.component.html",
    styleUrls: ["./individual-graph.component.css"]
})

export class IndividualGraph implements AfterViewInit, OnChanges {

    @ViewChild("container") element: ElementRef;
    @Input() config: IIndividualConfig;


    private htmlElement: HTMLElement;
    private host: Selection<any, any, any, any>;
    private svg: Selection<any, any, any, any>;
    private margin: ID3Margin;
    private height: number;
    private width: number;
    private color: ScaleOrdinal<string, string>;
    private layout: IGraphLayout;
    private links: Selection<BaseType, SimulationLinkDatum<IFactNode>, any, any>;
    private nodes: Selection<any, IFactNode, any, any>;
    private simulation: Simulation<IFactNode, any>;

    constructor() {
    }

    ngAfterViewInit(): void {
        console.debug("graph view-init");
        this.htmlElement = this.element.nativeElement;
        this.setupD3();
        this.layout = {
            nodes: [],
            links: []
        }
    }

    ngOnChanges(changes: {[propKey: string]: SimpleChange}): void {
        let configChange = changes["config"];
        if (!configChange.isFirstChange() && (configChange.currentValue !== configChange.previousValue)) {
            console.debug("Config changed", configChange);
            this.buildGraph(configChange.currentValue.data);
            this.update({
                nodes: [],
                links: [],
            });
            this.update(this.layout);
        }
    }

    private setupD3() {
        this.host = select(this.htmlElement);
        this.margin = {top: 10, right: 10, bottom: 10, left: 10};
        console.debug("offsetWidth", this.htmlElement.offsetWidth);
        this.width = this.htmlElement.offsetWidth - this.margin.left - this.margin.right;
        this.height = 500 - this.margin.top - this.margin.bottom;
        console.debug("Creating D3 graph with width/height", this.width + "/" + this.height);
        this.svg = this.host.html("")
            .append("svg")
            .attr("width", this.width + this.margin.left + this.margin.right)
            .attr("height", this.height + this.margin.top + this.margin.bottom)
            .append("g")
            .attr("transform", "translate(" + this.margin.left + "," + this.margin.top + ")");

        this.color = scaleOrdinal(schemeCategory20);
        console.debug("D3 initialized");
    }

    private update(data: IGraphLayout): void {
        console.debug("Data in update function", data);
        let force = forceManyBody();
        force.strength(-200);
        this.simulation = forceSimulation<IFactNode>()
            .force("link", forceLink().id((d: IFactNode) => d.id))
            .force("charge", force)
            .force("center", forceCenter(this.width / 2, this.height / 2));

        let linkData = this.svg.selectAll(".link")
            .data(data.links, (d: any) => d.source.id + "_" + d.target.id);

        this.links = linkData.enter()
            .append("line")
            .attr("class", "link");

        let nodeData = this.svg.selectAll(".node")
            .data(data.nodes, (d: IFactNode) => d.id);

        this.nodes = nodeData.enter()
            .append("circle")
            .attr("class", "node")
            .style("fill", (d) => this.color(d.group.toString(10)));

        this.nodes
            .append("title")
            .text((d: IFactNode) => d.id);

        //    Click handler
        this.nodes.on("click", (d: any) => console.debug("clicked", d));

        //    Legend
        let legend = this.svg.selectAll(".legend")
            .data(this.color.domain())
            .enter()
            .append("g")
            .attr("class", "legend")
            .attr("transform", (d, i) => "translate(0," + (i * ((this.width / 100) * 2) + 10) + ")");

        legend.append("circle")
            .attr("cx", this.width - 18)
            .attr("r", this.width / 100)
            .attr("cy", this.width / 100)
            .style("fill", this.color);

        legend
            .append("text")
            .attr("x", this.width - (this.width / 100) * 2 - 20)
            .attr("y", this.width / 100)
            .attr("dy", "0.35em")
            .style("text-anchor", "end")
            .text((d) => IndividualGraph.parseColorGroup(d));
        // Force setup
        this.simulation
            .nodes(data.nodes)
            .on("tick", this.forceTick);

        // For some reason, the links() function doesn't exist on the simulation type, so we do a simple cast to get around it.
        // Seems to work, and the only other option is to lose all type checking for the simulation object
        (this.simulation.force("link") as any).links(data.links);

        linkData.exit().remove();
        nodeData.exit().remove();
    }

    private forceTick = (): void => {
        this.nodes.attr("r", this.width / 75)
            .attr("cx", (d) => d.x)
            .attr("cy", (d) => d.y);

        this.links
            .attr("x1", (d: any) => d.source.x)
            .attr("y1", (d: any) => d.source.y)
            .attr("x2", (d: any) => d.target.x)
            .attr("y2", (d: any) => d.target.y);
    };

    private buildGraph(individual: ITrestleIndividual): void {
        this.layout = {
            nodes: [],
            links: []
        };

        //    Add the individual as node 0
        let individualNode = {
            id: individual.individualID,
            group: NodeType.INDIVIDUAL
        };

        let individualTemporal = {
            id: individual.individualTemporal.validID,
            group: NodeType.VTEMPORAL
        };

        this.layout.nodes.push(individualNode, individualTemporal);

        this.layout.links.push({
            source: individualNode,
            target: individualTemporal
        });

        individual.facts.forEach(fact => {
            let factNode = {
                id: fact.identifier,
                group: NodeType.FACT
            };
            this.layout.nodes.push(factNode);
            this.layout.links.push({
                    source: individualNode,
                    target: factNode
                });
        });
    }

    private static parseColorGroup(group: string): string {
        switch (parseInt(group, 10)) {
            case 0:
                return "Individual";
            case 1:
                return "Valid Temporal";
            case 2:
                return "Database Temporal";
            case 3:
                return "Fact";
            default:
                return "unknown";
        }
    }
}