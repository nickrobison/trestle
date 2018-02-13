package com.nickrobison.trestle.exporter.kml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.ArrayList;
import java.util.List;

@JacksonXmlRootElement(localName = "Polygon")
public class KMLPolygon extends KMLGeometry {

    @JacksonXmlProperty(localName = "outerBoundaryIs")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<KMLLinearRing> outerBoundaries;
    @JacksonXmlProperty(localName = "innerBoundaryIs")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<KMLLinearRing> innerBoundaries;


    public KMLPolygon() {
        this.innerBoundaries = new ArrayList<>();
        this.outerBoundaries = new ArrayList<>();
    }

    public List<KMLLinearRing> getOuterBoundaries() {
        return outerBoundaries;
    }

    public void setOuterBoundaries(List<KMLLinearRing> outerBoundaries) {
        this.outerBoundaries = outerBoundaries;
    }

    public List<KMLLinearRing> getInnerBoundaries() {
        return innerBoundaries;
    }

    public void setInnerBoundaries(List<KMLLinearRing> innerBoundaries) {
        this.innerBoundaries = innerBoundaries;
    }
}
