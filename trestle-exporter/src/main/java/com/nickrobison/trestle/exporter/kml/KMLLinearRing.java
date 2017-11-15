package com.nickrobison.trestle.exporter.kml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

@JacksonXmlRootElement(localName = "LinearRing")
public class KMLLinearRing extends KMLGeometry {

    @JacksonXmlProperty(localName = "LinearRing")
    private List<KMLCoordinate> coordinates;


    public KMLLinearRing() {
//        Not Used
    }

    public List<KMLCoordinate> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<KMLCoordinate> coordinates) {
        this.coordinates = coordinates;
    }
}
