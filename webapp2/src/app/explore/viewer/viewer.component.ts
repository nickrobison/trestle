/**
 * Created by nrobison on 6/23/17.
 */
import {Component, OnInit, ViewChild} from '@angular/core';
import {MapService} from './map.service';
import {animate, style, transition, trigger} from '@angular/animations';
import * as moment from 'moment';
import {TrestleIndividual} from '../../shared/individual/TrestleIndividual/trestle-individual';
import {Subject} from 'rxjs';
import {LngLatBounds} from 'mapbox-gl';
import {IndividualService} from '../../shared/individual/individual.service';
import {MatSliderChange} from '@angular/material/slider';
import {DatasetService} from '../../shared/dataset/dataset.service';
import {IIndividualHistory} from '../../ui/history-graph/history-graph.component';
import {IDataExport} from '../exporter/exporter.component';
import {MapSource, TrestleMapComponent} from '../../ui/trestle-map/trestle-map.component';

enum DatasetState {
  UNLOADED,
  LOADING,
  LOADED,
  ERROR
}

interface IDatasetState {
  name: string;
  state: DatasetState;
  error?: string;
}

@Component({
  selector: 'dataset-viewer',
  templateUrl: './viewer.component.html',
  styleUrls: ['./viewer.component.scss'],
  animations: [
    trigger('fadeInOut', [
      transition(':enter', [
        style({transform: 'scale(0)', opacity: 0}),
        animate('500ms', style({transform: 'scale(1)', opacity: 1}))
      ]),
    ])
  ]
})
export class ViewerComponent implements OnInit {
  public availableDatasets: IDatasetState[] = [];
  public DatasetState = DatasetState;
  public minTime = moment('1990-01-01');
  public maxTime = moment('2016-01-01');
  public sliderValue = 2013;
  public selectedIndividual: TrestleIndividual;
  public objectHistory: IIndividualHistory;
  public dataChanges: Subject<MapSource>;
  public exportIndividuals: IDataExport[];
  @ViewChild('map')
  public map: TrestleMapComponent;
  private mapBounds: LngLatBounds;

  constructor(private mapService: MapService,
              private is: IndividualService,
              private ds: DatasetService) {
    this.dataChanges = new Subject();
    this.exportIndividuals = [];
  }

  public ngOnInit(): void {
    this.objectHistory = {
      entities: []
    };
    this.ds.getAvailableDatasets()
      .subscribe((results: string[]) => {
        results.forEach((ds) => {
          this.availableDatasets.push({
            name: ds,
            state: DatasetState.UNLOADED
          });
        });
      });
  }

  /**
   * Load new data (or an entirely new dataset)
   * If one's already loaded, unload it, unless we mark the keep flag
   * @param {IDatasetState} dataset - dataset to load
   * @param {boolean} keepLoaded - true if we need to keep the dataset loaded, false to unload it
   */
  public loadDataset(dataset: IDatasetState, keepLoaded = false): void {
    if (dataset.state === DatasetState.LOADED && !keepLoaded) {
      console.debug('Unloading dataset:', dataset.name);
      this.map.removeIndividual('intersection-query');
      dataset.state = DatasetState.UNLOADED;
    } else {
      console.debug('Loading:', dataset.name);
      dataset.state = DatasetState.LOADING;
      this.mapService.stIntersect(dataset.name,
        this.mapBounds, moment()
          .year(this.sliderValue)
          .startOf('year'))
        .subscribe((data) => {
          dataset.state = DatasetState.LOADED;
          console.debug('Data:', data);
          // Get the list of individuals, for exporting
          this.exportIndividuals.push({
            dataset: this.availableDatasets[0].name,
            individuals: (data.features
              .filter((feature) => feature.id)
              // We can do this cast, because we filter to make sure the features have an id
              .map((feature) => feature.id) as string[])
          });
          this.dataChanges.next({
            id: 'intersection-query',
            idField: 'id',
            labelField: 'objectName',
            data
          });
        }, (error) => {
          console.error('Error loading dataset:', error);
          dataset.state = DatasetState.ERROR;
          dataset.error = error;
        });
    }
  }

  /**
   * Update map bounds, and fetch new data, if necessary
   * @param {mapboxgl.LngLatBounds} bounds
   */
  public updateBounds(bounds: LngLatBounds): void {
    console.debug('Moving, updating bounds', bounds);
    // If we haven't loaded any datasets, keep resetting the map bounds
    if (!this.availableDatasets.some((ds) => ds.state === DatasetState.LOADED)) {
      this.mapBounds = bounds;
    }

    // If we've moved outside of the current bounds, get new data
    if (this.needNewData(bounds)) {
      this.mapBounds = bounds;
      this.availableDatasets
        .filter((ds) => ds.state === DatasetState.LOADED)
        .forEach((ds) => this.loadDataset(ds, true));
    }
    // On the first time around, set the map bounds
    if (!this.mapBounds) {
      this.mapBounds = bounds;
    }
  }

  /**
   * Handler to update time slider, and fetch new data
   * @param {MatSliderChange} event
   */
  public sliderChanged = (event: MatSliderChange): void => {
    console.debug('Value changed to:', event);
    if (event.value) {
      this.sliderValue = event.value;
      //    Reload all the currently loaded datasets
      this.availableDatasets
        .filter((ds) => ds.state === DatasetState.LOADED)
        .forEach((ds) => this.loadDataset(ds, true));
    }
  };

  /**
   * Map click handler, which currently fetches the object as a {TrestleIndividual}
   * @param {string} event
   */
  public mapClicked = (event: string): void => {
    console.debug('Clicked:', event);
    this.is.getTrestleIndividual(event)
      .subscribe((data) => {
        console.debug('Has selection', data);
        this.selectedIndividual = data;
      });
  };

  /**
   * Get error from {IDatasetState}
   * @param {IDatasetState} ds
   * @returns {string}
   */
  public getError(ds: IDatasetState): string {
    return ds.error === undefined ? 'Error' : ds.error;
  }

  private needNewData(newBounds: mapboxgl.LngLatBounds) {
    console.debug('Need new data', newBounds, 'old Data', this.mapBounds);
    // This short circuits the checks to avoid loading data on the first go 'round.
    if (newBounds === null || this.mapBounds === undefined) {
      return false;
    }
    // Moved up/down
    if ((newBounds.getNorth() > this.mapBounds.getNorth())
      || (newBounds.getSouth() < this.mapBounds.getSouth())) {
      console.debug(newBounds.getNorth() + ', ' + this.mapBounds.getNorth());
      console.debug(newBounds.getSouth() + ', ' + this.mapBounds.getSouth());
      console.debug('Moved north/south, so true');
      return true;
      //    Moved east/west
    } else if ((newBounds.getEast() > this.mapBounds.getEast())
      || (newBounds.getWest() < this.mapBounds.getWest())) {
      console.debug(newBounds.getEast() + ', ' + this.mapBounds.getEast());
      console.debug(newBounds.getWest() + ', ' + this.mapBounds.getWest());
      console.debug('Moved east/west, so true');
    }
    return false;
  }
}
