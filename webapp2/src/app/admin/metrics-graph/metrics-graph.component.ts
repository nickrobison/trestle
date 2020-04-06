/**
 * Created by nrobison on 4/4/17.
 */
import {AfterViewInit, Component, ElementRef, Input, OnChanges, SimpleChange, ViewChild} from '@angular/core';
import {BaseType, select, Selection} from 'd3-selection';
import {scaleLinear, scaleOrdinal, scaleTime, ScaleTime} from 'd3-scale';
import {curveBasis, line} from 'd3-shape';
import {max, min} from 'd3-array';
import {axisBottom, axisLeft} from 'd3-axis';
import {schemeCategory10} from 'd3';
import {IMetricsData} from './metrics.service';

interface ID3Margin {
  top: number;
  right: number;
  bottom: number;
  left: number;
}

@Component({
  selector: 'metrics-graph',
  templateUrl: './metrics-graph.component.html',
  styleUrls: ['./metrics-graph.component.scss']
})

export class MetricsGraphComponent implements AfterViewInit, OnChanges {
  @ViewChild('container') private element: ElementRef;
  @Input() public data: IMetricsData;
  @Input() public minTime: Date;
  @Input() public maxTime: Date;
  private graphData: IMetricsData[] = [];
  private htmlElement: HTMLElement;
  private host: Selection<HTMLElement, IMetricsData, null, undefined>;
  private svg: Selection<BaseType, IMetricsData, null, undefined>;
  private width: number;
  private height: number;
  private margin: ID3Margin;
  private x: ScaleTime<number, number>;
  private visible: Map<string, boolean> = new Map();

  constructor() {
  }

  public ngAfterViewInit(): void {
    console.debug('Graph view-init');
    this.htmlElement = this.element.nativeElement;
    this.setupD3();

  }

  public ngOnChanges(changes: { [propKey: string]: SimpleChange }): void {
    const dataChange = changes['data'];
    if (dataChange != null
      && !dataChange.isFirstChange()
      && (dataChange.currentValue !== dataChange.previousValue)) {
      console.debug('Updated, plotting');
      const currentValue: IMetricsData = changes['data'].currentValue;
      this.graphData.push(currentValue);
      console.debug('Adding as visible:', currentValue.metric);
      this.visible.set(currentValue.metric, true);
      this.plotData();
    }
  }

  /**
   * Get all metrics that aren't currently disabled
   * @returns {string[]}
   */
  public getVisibleMetrics(): string[] {
    const metrics = Array<string>();
    this.visible.forEach((value, key) => {
      if (value) {
        metrics.push(key);
      }
    });
    return metrics;
  }

  private setupD3(): void {
    this.host = select<HTMLElement, IMetricsData>(this.htmlElement);
    this.margin = {top: 20, right: 200, bottom: 20, left: 70};
    this.width = this.htmlElement.offsetWidth - this.margin.left - this.margin.right;
    this.height = 500 - this.margin.top - this.margin.bottom;
    console.debug('Creating D3 graph with width/height', this.width + '/' + this.height);
    this.svg = this.host.html('')
      .append('svg')
      .attr('width', this.width + this.margin.left + this.margin.right)
      .attr('height', this.height + this.margin.top + this.margin.bottom)
      .append('g')
      .attr('transform', 'translate(' + this.margin.left + ',' + this.margin.top + ')');

    // Setup the x axis
    this.x = scaleTime().range([0, this.width]);
    this.x.domain([this.minTime, this.maxTime]);
    this.svg
      .append('g')
      .attr('class', 'axis axis-x')
      .attr('transform', 'translate(0,' + this.height + ')')
      .call(axisBottom(this.x));
    // this.x.domain(extent(this.graphData[0].values, (d: IMetricsValue)=> d.timestamp));
    console.debug('D3 initialized');

  }

  private plotData(): void {

    const y = scaleLinear().range([this.height, 0]);
    const z = scaleOrdinal(schemeCategory10);

    const metricsLine = line()
      .curve(curveBasis)
      .x((d: any) => this.x(d.timestamp))
      .y((d: any) => y(d.value));

    // Build domain values
    y.domain([
      (min(this.graphData,
        (d) => min(d.values,
          (mv) => mv.value) || 0) || 0),
      (max(this.graphData,
        (d) => max(d.values,
          (mv) => mv.value) || 0) || 0)
    ]);

    z.domain(this.graphData.map((d) => d.metric));
    console.debug('Z-domain', z.domain());

    this.svg
      .append('g')
      .attr('class', 'axis axis-y')
      .call(axisLeft(y))
      .append('text')
      .attr('transform', 'rotate(-90)')
      .attr('y', 6)
      .attr('dy', '0.71em')
      .attr('fill', '#000')
      .text('Value');

    const metric = this.svg.selectAll('.metric')
      .data(this.graphData)
      .enter().append('g')
      .attr('class', 'metric');

    metric
      .append('path')
      .attr('class', 'line')
      .attr('id', (d) => d.metric.replace(/\./g, '-'))
      .attr('d', (d: any) => metricsLine(d.values))
      // .attr("data-legend", (d) => d.metric)
      .style('stroke', (d) => z(d.metric));

    //    Add the legend
    const legend = this.svg.selectAll('.legend')
      .data(z.domain())
      .enter()
      .append('g')
      .attr('class', 'legend')
      .attr('id', (d) => 'legend-' + d.replace(/\./g, '-'))
      .attr('transform',
        (d, i) => 'translate('
          + (this.width) + ','
          + (i * ((this.width / 100) * 2) + 15) + ')')
      .on('click', this.legendClickHandler);

    legend
      .append('circle')
      .attr('cx', 30)
      .attr('cy', 30)
      .attr('r', this.width / 150)
      .style('fill', z);

    legend
      .append('text')
      .attr('x', 40)
      .attr('y', 30)
      .attr('dy', '0.25em')
      .style('text-anchor', 'start')
      .text(d => d);

  }

  private legendClickHandler = (d: string): void => {
    console.debug('Clicked', d);
    console.debug('Visible', this.visible);
    const isVisible = this.visible.get(d);
    console.debug('Metric: ' + d + ' is visible?', isVisible);
    if (isVisible) {
      console.debug('Going out');
      this.svg.selectAll('#' + d.replace(/\./g, '-'))
        .transition()
        .duration(1000)
        .style('opacity', 0);

      // Fade the legend
      this.svg.select('#legend-' + d.replace(/\./g, '-'))
        .transition()
        .duration(1000)
        .style('opacity', .2);

      // Fade the y-axis
      this.svg.select('#y-' + d.replace(/\./g, '-'))
        .transition()
        .duration(1000)
        .style('opacity', .2);

      this.visible.set(d, false);
    } else {
      console.debug('Coming back');
      this.svg.selectAll('#' + d.replace(/\./g, '-'))
        // .style("display", "block")
        .transition()
        .duration(1000)
        .style('opacity', 1);

      // Fade the legend
      this.svg.select('#legend-' + d.replace(/\./g, '-'))
        .transition()
        .duration(1000)
        .style('opacity', 1);

      // Fade the y-axis
      this.svg.select('#y-' + d.replace(/\./g, '-'))
        .transition()
        .duration(1000)
        .style('opacity', 1);

      this.visible.set(d, true);
    }
  };
}
