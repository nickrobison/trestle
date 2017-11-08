// Some environment variables
declare const ENV: string;

// GeoJson extent
declare module "@mapbox/geojson-extent" {
    import { GeoJsonObject, Polygon } from "geojson";

    function extent(geojson: GeoJsonObject): number[];

    // function polygon(geojson: GeoJsonObject): Polygon;
    // function bboxify(geojson: GeoJsonObject): void;
    // export {polygon, bboxify};
    export default extent;
}

declare module "wellknown" {
    import { GeoJsonObject } from "geojson";

    function parse(input: string): GeoJsonObject;

    function stringify(input: GeoJsonObject): string;

    export { stringify, parse };
}
