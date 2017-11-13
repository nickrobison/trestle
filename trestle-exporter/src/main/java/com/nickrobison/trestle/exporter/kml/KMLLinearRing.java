package com.nickrobison.trestle.exporter.kml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

@JacksonXmlRootElement(localName = "LinearRing")
public class KMLLinearRing extends KMLGeometry {

    @JacksonXmlProperty(localName = "LinearRing")
//        @JacksonXmlElementWrapper(useWrapping = false)
    private List<KMLCoordinate> coordinates;

//    private KMLCoordinate coordinates;
//    private KMLCoordinate coordinates;


    public KMLLinearRing() {

    }

//    public String getCoordinates() {
//        return coordinates;
//    }
//
//    public void setCoordinates(String coordinates) {
//        this.coordinates = coordinates;
//    }
//
//        public KMLCoordinate getCoordinates() {
//        return coordinates;
//    }
//
//    public void setCoordinates(KMLCoordinate coordinates) {
//        this.coordinates = coordinates;
//    }

        public List<KMLCoordinate> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<KMLCoordinate> coordinates) {
        this.coordinates = coordinates;
    }
}
