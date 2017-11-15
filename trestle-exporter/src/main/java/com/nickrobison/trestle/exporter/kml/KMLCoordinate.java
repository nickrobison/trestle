package com.nickrobison.trestle.exporter.kml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

//@JacksonXmlRootElement(localName = "coordinates")
public class KMLCoordinate extends KMLGeometry {

    @JacksonXmlProperty(localName = "coordinates")
    private final String coordinates;

    public KMLCoordinate(String coordinates) {
        this.coordinates = coordinates;
    }
}
