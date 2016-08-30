package com.nickrobison.trestle;

import com.esri.core.geometry.Polygon;
import com.nickrobison.trestle.annotations.*;
import com.nickrobison.trestle.annotations.temporal.DefaultTemporalProperty;
import com.nickrobison.trestle.types.TemporalType;
import com.vividsolutions.jts.geom.Geometry;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Created by nrobison on 8/29/16.
 */
public class TestClasses {

    @OWLClassName(className = "GAUL_Test")
    protected static class JTSGeometryTest {

        private final Integer adm0_code;
        private final Geometry geom;
        private LocalDate date;

        public JTSGeometryTest(Integer adm0_code, Geometry geom, LocalDate date) {
            this.adm0_code = adm0_code;
            this.geom = geom;
            this.date = date;
        }

        @IndividualIdentifier
        public Integer getAdm0_code() {
            return this.adm0_code;
        }

        @Spatial
        public Geometry getGeom() {
            return this.geom;
        }

        @DefaultTemporalProperty(name = "date", type = TemporalType.INTERVAL, duration = 1, unit = ChronoUnit.YEARS)
        public LocalDate getDate() {
            return this.date;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            JTSGeometryTest that = (JTSGeometryTest) o;

            if (!getAdm0_code().equals(that.getAdm0_code())) return false;
            if (!getGeom().equals(that.getGeom())) return false;
            return getDate().equals(that.getDate());

        }

        @Override
        public int hashCode() {
            int result = getAdm0_code().hashCode();
            result = 31 * result + getGeom().hashCode();
            result = 31 * result + getDate().hashCode();
            return result;
        }
    }

    @OWLClassName(className = "GAUL_Test")
    protected static class ESRIPolygonTest {

        private final Integer adm0_code;
        private final Polygon geom;
        private LocalDate date;

        public ESRIPolygonTest(Integer adm0_code, Polygon geom, LocalDate date) {
            this.adm0_code = adm0_code;
            this.geom = geom;
            this.date = date;
        }

        @IndividualIdentifier
        public Integer getAdm0_code() {
            return this.adm0_code;
        }

        @Spatial
        public Polygon getGeom() {
            return this.geom;
        }

        @DefaultTemporalProperty(name = "date", type = TemporalType.INTERVAL, duration = 1, unit = ChronoUnit.YEARS)
        public LocalDate getDate() {
            return this.date;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            JTSGeometryTest that = (JTSGeometryTest) o;

            if (!getAdm0_code().equals(that.getAdm0_code())) return false;
            if (!getGeom().equals(that.getGeom())) return false;
            return getDate().equals(that.getDate());

        }

        @Override
        public int hashCode() {
            int result = getAdm0_code().hashCode();
            result = 31 * result + getGeom().hashCode();
            result = 31 * result + getDate().hashCode();
            return result;
        }
    }

    /**
     * Created by nrobison on 6/27/16.
     */
    @OWLClassName(className="GAUL_Test")
    public static class GAULTestClass {

        @DataProperty(name="ADM0_Code", datatype= OWL2Datatype.XSD_INTEGER)
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
}
