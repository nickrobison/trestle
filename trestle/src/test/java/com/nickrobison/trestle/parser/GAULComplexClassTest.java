package com.nickrobison.trestle.parser;

import com.nickrobison.trestle.annotations.IndividualIdentifier;
import com.nickrobison.trestle.annotations.OWLClassName;
import com.nickrobison.trestle.annotations.Spatial;
import com.nickrobison.trestle.annotations.temporal.StartTemporalProperty;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Created by nrobison on 8/24/16.
 */
@OWLClassName(className = "gaul-complex")
public class GAULComplexClassTest {

    @IndividualIdentifier
    public UUID id;
    private final String wkt;
    private final LocalDate atDate;

    public GAULComplexClassTest() {
        this.id = UUID.randomUUID();
        this.wkt = "POINT(4.0 6.0)";
        this.atDate = LocalDate.of(1989, 3, 26);
    }

    public GAULComplexClassTest(UUID id, String wkt, LocalDate atDate) {
        this.id = id;
        this.wkt = wkt;
        this.atDate = atDate;
    }

    @Spatial
    public String getWkt() {
        return this.wkt;
    }

    @StartTemporalProperty
    public LocalDate getAtDate() {
        return this.atDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GAULComplexClassTest that = (GAULComplexClassTest) o;

        if (!id.equals(that.id)) return false;
        if (!getWkt().equals(that.getWkt())) return false;
        return getAtDate().equals(that.getAtDate());

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + getWkt().hashCode();
        result = 31 * result + getAtDate().hashCode();
        return result;
    }
}
