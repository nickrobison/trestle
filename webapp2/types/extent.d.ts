declare module "@mapbox/geojson-extent" {
  import { GeoJsonObject, Polygon } from "geojson";

  function extent(geojson: GeoJsonObject): number[];

  function polygon(geojson: GeoJsonObject): Polygon;

  function bboxify(geojson: GeoJsonObject): void;

  export { polygon, bboxify };
  export default extent;
}
