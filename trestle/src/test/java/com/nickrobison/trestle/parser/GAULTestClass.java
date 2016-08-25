package com.nickrobison.trestle.parser;

import com.nickrobison.trestle.annotations.*;
import com.nickrobison.trestle.annotations.temporal.DefaultTemporalProperty;
import com.nickrobison.trestle.types.TemporalType;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Created by nrobison on 6/27/16.
 */
@OWLClassName(className="GAUL_Test")
public class GAULTestClass {

    @DataProperty(name="ADM0_Code", datatype=OWL2Datatype.XSD_INTEGER)
    public int adm0_code;
    public String adm0_name;
    @IndividualIdentifier
    @Ignore
    public String test_name;
    @Spatial
    public String wkt;
    @DefaultTemporalProperty(type = TemporalType.INTERVAL, duration = 1, unit = ChronoUnit.YEARS)
    public LocalDateTime time;

    GAULTestClass() {
        this.adm0_code = 12;
        this.adm0_name = "test object";
        this.test_name = "test me";
        this.time = LocalDateTime.now();
        this.wkt = "test_wkt";
    }

    public GAULTestClass(int code, String name) {
        this.adm0_code = code;
        this.adm0_name = name;
        this.test_name = "test_me";
        this.time = LocalDateTime.now();
        this.wkt = "test_wkt";
    }

    @TrestleCreator
    public GAULTestClass(int adm0_code, String adm0_name, LocalDateTime time, String wkt) {
        this.test_name = adm0_name;
        this.adm0_code = adm0_code;
        this.adm0_name = adm0_name;
        this.wkt = wkt;
        this.time = time;

    }
}
