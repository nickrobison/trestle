package com.nickrobison.trestle.datasets;

import com.nickrobison.trestle.reasoner.annotations.DatasetClass;
import com.nickrobison.trestle.reasoner.annotations.IndividualIdentifier;
import com.nickrobison.trestle.reasoner.annotations.Spatial;
import com.nickrobison.trestle.reasoner.annotations.temporal.StartTemporal;
import org.locationtech.jts.geom.MultiPolygon;

import java.time.LocalDate;

/**
 * Created by nickrobison on 3/22/20.
 */
@DatasetClass(name = "CensusCounty")
public class CensusCounty {
    private final MultiPolygon geom;
    private final String geoid;
    private final String name;
    private final String state;
    private final LocalDate start_date;

    public CensusCounty(MultiPolygon geom, String geoid, String name, String state, LocalDate start_date) {
        this.geom = geom;
        this.geoid = geoid;
        this.name = name;
        this.state = state;
        this.start_date = start_date;
    }

    @Spatial(projection = 4269)
    public MultiPolygon getGeom() {
        return geom;
    }

    @IndividualIdentifier
    public String getGeoid() {
        return geoid;
    }

    public String getName() {
        return name;
    }

    public String getState() {
        return state;
    }

    @StartTemporal(name = "start_date")
    public LocalDate getStartDate() {
        return start_date;
    }
}
