/**
 * Created by nrobison on 6/11/17.
 */
import {Component, OnInit} from "@angular/core";
import mapboxgl = require("mapbox-gl");

@Component({
    selector: "trestle-map",
    templateUrl: "./trestle-map.component.html",
    styleUrls: ["./trestle-map.component.css"]
})

export class TrestleMapComponent implements OnInit {

    private map: mapboxgl.Map;

    constructor() {
        mapboxgl.accessToken = "pk.eyJ1IjoibnJvYmlzb24iLCJhIjoiY2ozdDd5dmd2MDA3bTMxcW1kdHZrZ3ppMCJ9.YcJMRphQAfmZ0H8X9HnoKA";
    }

    ngOnInit(): void {
        this.map = new mapboxgl.Map({
            container: "map",
            style: "mapbox://styles/mapbox/dark-v9",
            center: [-74.50, 40], // starting position
            zoom: 9 // starting zoom
        })
    }
}