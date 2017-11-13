package com.nickrobison.trestle.exporter.kml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

//@JacksonXmlRootElement(localName = "coordinates")
public class KMLCoordinate extends KMLGeometry {

    @JacksonXmlProperty(localName = "coordinates")
    private String coordinates;
//    private String coordinates;

    public KMLCoordinate(String coordinates) {
//        Not used
        this.coordinates = coordinates;
    }


//    public String getCoordinates() {
//        return coordinates;
//    }

//    public void setCoordinates(Object coordinates) {
//        this.coordinates = coordinates.toString();
//    }
//
//    public void setCoordinates(String coordinate) {
//        this.coordinates = coordinate;
//    }
}
