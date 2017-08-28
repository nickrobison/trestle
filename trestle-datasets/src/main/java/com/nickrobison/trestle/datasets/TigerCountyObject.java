package com.nickrobison.trestle.datasets;

import com.nickrobison.trestle.reasoner.annotations.DatasetClass;
import com.nickrobison.trestle.reasoner.annotations.IndividualIdentifier;
import com.nickrobison.trestle.reasoner.annotations.Spatial;
import com.nickrobison.trestle.reasoner.annotations.temporal.StartTemporal;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.LocalDate;

/**
 * Created by detwiler on 2/10/17.
 * Years that overlap between shapefiles and census data 2011-2015
 */
@DatasetClass(name = "TigerCountyCensus")
public class TigerCountyObject {

    /*
    // from Tiger shapefiles
    geoid
    geom

    // From census population data
    REGION
    DIVISION
    STATE
    COUNTY
    STNAME
    CTYNAME
    POPESTIMATE
    BIRTHS
    DEATHS
    NATURALINC
    INTERNATIONALMIG
    DOMESTICMIG
    RBIRTH
    RDEATH
    RNATURALINC
     */

    /*
    join criteria (note transform from srid 4269 to 4326):
    select *,ST_AsText(ST_Transform(geom,4326)) AS geotext from shp2012,population where CAST(shp2012.statefp AS NUMERIC) = population."STATE" and CAST(shp2012.countyfp AS NUMERIC) = population."COUNTY" and population."SUMLEV"=50;
     */

    // TIDER SRID = 4269, in these objects it is 4326

    // object attributes
    //private final UUID id;
    private final String geom;
    private final String geoid;
    private final String region;
    private final String division;
    private final String state;
    private final String county;
    private final int pop_estimate;
    private final int births;
    private final int deaths;
    private final int natural_increase;
    private final int international_migration;
    private final int domestic_migration;
    private final float rate_birth;
    private final float rate_death;
    private final float rate_natural_increase;
    private final LocalDate record_start_date;

    public TigerCountyObject(String geoid, String geom, String region, String division,
                             String state, String county, int pop_estimate, int births,
                             int deaths, int natural_increase, int international_migration,
                             int domestic_migration, float rate_birth, float rate_death,
                             float rate_natural_increase, LocalDate start_date) {
        this.geoid = geoid;
        this.geom = geom;
        this.region = region;
        this.division = division;
        this.state = state;
        this.county = county;
        this.pop_estimate = pop_estimate;
        this.births = births;
        this.deaths = deaths;
        this.natural_increase = natural_increase;
        this.international_migration = international_migration;
        this.domestic_migration = domestic_migration;
        this.rate_birth = rate_birth;
        this.rate_death = rate_death;
        this.rate_natural_increase = rate_natural_increase;
        this.record_start_date = start_date;
    }

    @Spatial
    public String getGeom() {
        return geom;
    }

    @IndividualIdentifier
    public String getGeoid() { return geoid; }

    public String getRegion() {
        return region;
    }

    public String getDivision() {
        return division;
    }

    public String getState() {
        return state;
    }

    public String getCounty() {
        return county;
    }

    public int getPop_estimate() {
        return pop_estimate;
    }

    public int getBirths() {
        return births;
    }

    public int getDeaths() {
        return deaths;
    }

    public int getNatural_increase() {
        return natural_increase;
    }

    public int getInternational_migration() {
        return international_migration;
    }

    public int getDomestic_migration() {
        return domestic_migration;
    }

    public float getRate_birth() {
        return rate_birth;
    }

    public float getRate_death() {
        return rate_death;
    }

    public float getRate_natural_increase() {
        return rate_natural_increase;
    }

    @StartTemporal(name = "start_date")
    public LocalDate getRecord_start_date() {
        return record_start_date;
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31). // two randomly chosen prime numbersappend(geom).
                append(geoid).
                append(region).
                append(division).
                append(state).
                append(county).
                append(pop_estimate).
                append(births).
                append(deaths).
                append(natural_increase).
                append(international_migration).
                append(domestic_migration).
                append(rate_birth).
                append(rate_death).
                append(rate_natural_increase).
                toHashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof TigerCountyObject))
            return false;
        if (obj == this)
            return true;

        TigerCountyObject rhs = (TigerCountyObject) obj;
        return new EqualsBuilder().
                //append(name, rhs.name).
                        append(geoid, rhs.geoid).
                        append(region, rhs.region).
                        append(division, rhs.division).
                        append(state, rhs.state).
                        append(county, rhs.county).
                        append(pop_estimate, rhs.pop_estimate).
                        append(births, rhs.births).
                        append(deaths, rhs.deaths).
                        append(natural_increase, rhs.natural_increase).
                        append(international_migration, rhs.international_migration).
                        append(domestic_migration, rhs.domestic_migration).
                        append(rate_birth, rhs.rate_birth).
                        append(rate_death, rhs.rate_death).
                        append(rate_natural_increase, rhs.rate_natural_increase).
                        isEquals();
    }
}
