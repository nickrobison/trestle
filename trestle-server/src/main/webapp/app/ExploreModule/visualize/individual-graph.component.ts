/**
 * Created by nrobison on 3/16/17.
 */
import {
    Component,
    AfterViewInit,
    ElementRef,
    ViewChild,
    Input,
    OnChanges,
    SimpleChange
} from "@angular/core";
import { Selection, select, event, BaseType } from "d3-selection";
import { ScaleOrdinal, schemeCategory20, scaleOrdinal } from "d3-scale";
import {
    SimulationNodeDatum,
    forceSimulation,
    forceManyBody,
    forceCenter,
    forceLink,
    SimulationLinkDatum, Simulation
} from "d3-force";
import { MatSlideToggleChange } from "@angular/material";
import { TrestleIndividual } from "./individual/trestle-individual";

export interface IIndividualConfig {
    data: TrestleIndividual;
}

const enum NodeType {
    INDIVIDUAL,
    VTEMPORAL,
    FACT,
    RELATION
}

interface ID3Margin {
    top: number;
    right: number;
    bottom: number;
    left: number;
}

interface IGraphLayout {
    nodes: IFactNode[];
    links: SimulationLinkDatum<IFactNode>[];
}

interface IFactNode extends SimulationNodeDatum {
    id: string;
    name: string;
    valid: boolean;
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

    factToggleName = "fact-toggle";
    relationToggleName = "relation-toggle";
    graphFacts = true;
    graphRelations = true;


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
    private nodeSize: number;
    private nodeSizeLarge: number;

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

    ngOnChanges(changes: { [propKey: string]: SimpleChange }): void {
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
        this.margin = {top: 10, right: 20, bottom: 10, left: 10};
        console.debug("offsetWidth", this.htmlElement.offsetWidth);
        this.width = this.htmlElement.offsetWidth - this.margin.left - this.margin.right;
        this.height = 500 - this.margin.top - this.margin.bottom;
        this.nodeSize = this.width / 75;
        this.nodeSizeLarge = this.width / 50;
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
        const force = forceManyBody();
        force.strength(-1000);
        this.simulation = forceSimulation<IFactNode>()
            .force("link", forceLink().id((d: IFactNode) => d.id))
            .force("charge", force)
            .force("center", forceCenter(this.width / 2, this.height / 2));

        const linkData = this.svg.selectAll(".link")
            .data(data.links, (d: any) => d.source.id + "_" + d.target.id);

        this.links = linkData.enter()
            .append("line")
            .attr("class", "link");

        const nodeData = this.svg.selectAll(".node")
            .data(data.nodes, (d: IFactNode) => d.id);

        this.nodes = nodeData
            .enter()
            .append("g")
            .attr("class", "node")
            .on("click", this.nodeClick)
            .on("mouseover", this.nodeMouseOver)
            .on("mouseout", this.nodeMouseOut);

        this.nodes
            .append("circle")
            .attr("r", this.nodeSize)
            .style("fill", (d) => this.color(d.group.toString(10)))
            .style("opacity", (d) => d.valid ? 1.0 : 0.5);

        this.nodes
            .append("text")
            .attr("x", 16)
            .attr("dy", ".35em")
            .text(d => d.name);

        //    Legend
        const legend = this.svg.selectAll(".legend")
            .data(this.color.domain())
            .enter()
            .append("g")
            .attr("class", "legend")
            .attr("transform", (d, i) => "translate(0," + (i * ((this.nodeSize) * 2) + 20) + ")");

        legend.append("circle")
            .attr("cx", this.width - 18)
            .attr("r", this.nodeSize)
            .attr("cy", this.nodeSize)
            .style("fill", this.color);

        legend
            .append("text")
            .attr("x", this.width - (this.nodeSize) * 2 - 12)
            .attr("y", this.nodeSize)
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

    private nodeClick = (d: IFactNode): void => {
        console.debug("Clicked", d);
    };

    private nodeMouseOver = (): void => {
        select(event.currentTarget).select("circle")
            .transition()
            .duration(750)
            .attr("r", this.nodeSizeLarge);
    };

    private nodeMouseOut = (): void => {
        select(event.currentTarget)
            .select("circle")
            .transition()
            .duration(750)
            .attr("r", this.nodeSize);
    };

    private forceTick = (): void => {
        this.nodes
            .attr("transform", (d) => "translate(" + d.x + "," + d.y + ")");


        this.links
            .attr("x1", (d: any) => d.source.x)
            .attr("y1", (d: any) => d.source.y)
            .attr("x2", (d: any) => d.target.x)
            .attr("y2", (d: any) => d.target.y);
    };

    private buildGraph(individual: TrestleIndividual): void {
        this.layout = {
            nodes: [],
            links: []
        };

        //    Add the individual as node 0
        const individualNode = {
            id: individual.getID(),
            name: IndividualGraph.parseIndividualID(individual.getID()),
            valid: true,
            group: NodeType.INDIVIDUAL
        };

        const individualTemporal = {
            id: individual.getTemporal().getID(),
            name: "individual-temporal",
            valid: true,
            group: NodeType.VTEMPORAL
        };

        this.layout.nodes.push(individualNode, individualTemporal);

        this.layout.links.push({
            source: individualNode,
            target: individualTemporal
        });

        if (this.graphFacts) {
            individual.getFacts().forEach(fact => {
                const factNode = {
                    id: fact.getID(),
                    name: fact.getName(),
                    // FIXME(nrobison): This won't work with times in the far future
                    valid: fact.getValidTemporal().getTo() === undefined && fact.getDatabaseTemporal().getTo() === undefined,
                    group: NodeType.FACT
                };
                this.layout.nodes.push(factNode);
                this.layout.links.push({
                    source: individualNode,
                    target: factNode
                });
            });
        }

        //    Relations
        if (this.graphRelations) {
            individual.getRelations().forEach(relation => {
                const relationNode = {
                    id: relation.getObject(),
                    name: relation.getType().toString() + ": " + IndividualGraph.parseIndividualID(relation.getObject()),
                    valid: true,
                    group: NodeType.RELATION
                };
                this.layout.nodes.push(relationNode);
                this.layout.links.push({
                    source: individualNode,
                    target: relationNode
                });
            });
        }
    }

    public changeGraphMembers(event: MatSlideToggleChange): void {
        if (event.source.id === this.factToggleName) {
            console.debug("Graph facts?", event.checked);
            this.graphFacts = event.checked;
            this.buildGraph(this.config.data);
            this.update({
                nodes: [],
                links: [],
            });
            this.update(this.layout);
        } else if (event.source.id === this.relationToggleName) {
            console.debug("Graph relations?", event.checked);
            this.graphRelations = event.checked;
            this.buildGraph(this.config.data);
            this.update({
                nodes: [],
                links: [],
            });
            this.update(this.layout);
        }
    }

    private static parseColorGroup(group: string): string {
        switch (parseInt(group, 10)) {
            case 0:
                return "Individual";
            case 1:
                return "Valid Temporal";
            case 2:
                return "Fact";
            case 3:
                return "Relation";
            default:
                return "unknown";
        }
    }

    private static parseIndividualID(id: string): string {
        const matches = id.match(/(#)(.*)/g);
        if (matches) {
            return matches[0].replace("#", "");
        }
        return id;
    }
}