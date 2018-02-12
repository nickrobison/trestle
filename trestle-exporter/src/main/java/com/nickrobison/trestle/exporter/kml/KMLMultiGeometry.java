package com.nickrobison.trestle.exporter.kml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

@JacksonXmlRootElement(localName = "MultiGeometry")
public class KMLMultiGeometry extends KMLGeometry {

    @JacksonXmlProperty(localName = "Polygon")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<KMLPolygon> polygons;

    public KMLMultiGeometry() {
//        Not used
    }

    public List<KMLPolygon> getPolygons() {
        return polygons;
    }

    public void setPolygons(List<KMLPolygon> polygons) {
        this.polygons = polygons;
    }
}
