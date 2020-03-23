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
@DatasetClass(name = "CensusState")
public class CovidState {

    private final MultiPolygon geom;
    private final String geoid;
    private final String name;
    private final String abbreviation;
    private final LocalDate start_date;

    public CovidState(MultiPolygon geom, String geoid, String name, String abbreviation, LocalDate start_date) {
        this.geom = geom;
        this.geoid = geoid;
        this.name = name;
        this.abbreviation = abbreviation;
        this.start_date = start_date;
    }

    @Spatial(projection = 4269)
    public MultiPolygon getGeom() {
        return geom;
    }

    @IndividualIdentifier
    public String getGeoid() { return geoid; }


    @StartTemporal(name = "start_date")
    public LocalDate getStartDate() {
        return start_date;
    }

    public String getName() {
        return name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }
}
