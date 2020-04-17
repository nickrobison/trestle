/// <reference lib="webworker" />

import { parse } from "wellknown";
import { Feature, FeatureCollection, GeometryObject } from "geojson";
import { IMapWorkerRequest, IMapWorkerResponse, wktType } from "./map.service";

const ctx: Worker = self as any;

ctx.addEventListener("message", (message) => {
    console.debug("Message:", message);
    const data = (message.data as IMapWorkerRequest);
    const msg: IMapWorkerResponse = {
        id: data.id,
        geom: parseObjectToGeoJSON(data.response)
    };
    ctx.postMessage(msg);
});

function parseObjectToGeoJSON(responseObject: object[]): FeatureCollection<GeometryObject> {
    const features: Array<Feature<GeometryObject>> = [];
    responseObject.forEach((obj: any) => {
        const properties: { [key: string]: {} } = {};
        let geometry: GeometryObject | null = null;
        let id = "";
        Object.keys(obj).forEach((key: string) => {
            const value: any = obj[key];
            if (isSpatial(value)) {
                geometry = parse(value);
            } else if (typeof value === "string") {
                if (isID(key)) {
                    id = value;
                    properties["id"] = value;
                } else {
                    properties[key] = value;
                }
            } else if (typeof value === "number" ||
                typeof value === "boolean") {
                properties[key] = value;
            }
        });
        if (geometry) {
            features.push({
                type: "Feature",
                id,
                geometry,
                properties
            });
        }
    });

    return {
        type: "FeatureCollection",
        features
    };
}

/**
 * Determines if a given {string} property name represents the object ID
 * It does so by peaking at the last 2 characters to see if their lowercase representation equals 'id'
 * @param x - Property name
 * @returns {boolean} is id
 */
function isID(x: string): boolean {
    if (x.toLowerCase() === "id") {
        return true;
    }
    if (x.length >= 2) {
        const sub = x.substring(x.length - 3, x.length - 1);
        return sub.toLowerCase() === "id";
    }
    return false;
}

/**
 * Crummy regex function to determine if a provided value is a WKT literal
 * @param x - any object at all
 * @returns {boolean} is wkt
 */
function isSpatial(x: any): x is wktType {
    if (typeof x === "string") {
        const matches = x.match(/^([\w\-]+)/);
        if (matches != null &&
            (matches[0] === "MULTIPOLYGON" ||
                matches[0] === "POLYGON" ||
                matches[0] === "POINT" ||
                matches[0] === "MULTIPOINT" ||
                matches[0] === "LINESTRING" ||
                matches[0] === "MULTILINESTRING")) {
            return true;
        }
    }
    return false;
}
