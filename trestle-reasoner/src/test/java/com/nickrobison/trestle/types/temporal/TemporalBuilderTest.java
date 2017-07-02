package com.nickrobison.trestle.types.temporal;

import com.nickrobison.trestle.types.TemporalScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by nrobison on 8/1/16.
 */
@SuppressWarnings({"unchecked", "OptionalGetWithoutIsPresent", "Duplicates", "initialization"})
public class TemporalBuilderTest {

    private static OWLNamedIndividual test_individual;
    static LocalDateTime valid_from_dt;
    static LocalDateTime valid_to_dt;
    static LocalDateTime exists_at_dt;

    static LocalDate valid_from_d;
    static LocalDate valid_to_d;
    static LocalDate exists_at_d;


    @BeforeAll
    public static void setup() {
        test_individual = OWLManager.getOWLDataFactory().getOWLNamedIndividual(IRI.create("trestle", "test"));
        valid_from_dt = LocalDateTime.of(1998, 3, 26, 18, 13, 14);
        valid_to_dt = LocalDateTime.of(2000, 3, 26, 18, 13, 14);
        exists_at_dt = LocalDateTime.of(2000, 3, 26, 18, 13, 14);

        valid_from_d = LocalDate.of(2007, 5, 19);
        valid_to_d = LocalDate.of(2015, 3, 26);
        exists_at_d = LocalDate.of(1990, 5, 14);
    }

    @Test
    public void testLocalDateTime() {
        final IntervalTemporal intervalTemporal = TemporalObjectBuilder.valid()
                .from(valid_from_dt)
                .to(valid_to_dt)
                .build();
//                .withRelations(test_individual);
        assertEquals(valid_from_dt, intervalTemporal.getFromTime(), "Wrong from time");
        assertEquals(valid_to_dt, intervalTemporal.getToTime().get(), "Wrong to time");
        assertTrue(intervalTemporal.getFromTime() instanceof LocalDateTime, "Should be LDT");
        assertFalse(intervalTemporal.getFromTime() instanceof LocalDate, "Should not be LD");

        final PointTemporal pointTemporal = TemporalObjectBuilder
                .exists()
                .at(exists_at_dt)
                .build();
//                .withRelations(test_individual);
        assertEquals(exists_at_dt, pointTemporal.getPointTime(), "Wrong at time");
        assertEquals(TemporalScope.EXISTS, pointTemporal.getScope(), "Wrong temporal scope");
    }

    @Test
    public void testLocalDate() {
        final IntervalTemporal intervalTemporal = TemporalObjectBuilder.valid()
                .from(valid_from_d)
                .to(valid_to_d)
                .build();
//                .withRelations(test_individual);
        assertEquals(valid_from_d, intervalTemporal.getFromTime(), "Wrong from time");
        assertEquals(valid_to_d, intervalTemporal.getToTime().get(), "Wrong to time");
        assertTrue(intervalTemporal.getFromTime() instanceof LocalDate, "Should be LDT");
        assertFalse(intervalTemporal.getFromTime() instanceof LocalDateTime, "Should not be LDT");

        final PointTemporal pointTemporal = TemporalObjectBuilder
                .exists()
                .at(exists_at_d)
                .build();
//                .withRelations(test_individual);
        assertEquals(exists_at_d, pointTemporal.getPointTime(), "Wrong at time");
        assertEquals(TemporalScope.EXISTS, pointTemporal.getScope(), "Wrong temporal scope");
    }


}
