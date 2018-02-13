package com.nickrobison.trestle.exporter.kml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "Point")
// Supressing this because I think an NPE would be better than simply returning bad data
@SuppressWarnings({"initialization.fields.uninitialized"})
public class KMLPoint extends KMLGeometry {

    @JacksonXmlProperty(localName = "coordinates")
    private String coordinates;

    public KMLPoint() {
    }

    public String getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(String coordinates) {
        this.coordinates = coordinates;
    }
}
