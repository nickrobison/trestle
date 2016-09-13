package com.nickrobison.trestle;

import com.esri.core.geometry.Polygon;
import com.nickrobison.trestle.annotations.*;
import com.nickrobison.trestle.annotations.temporal.DefaultTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.EndTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.StartTemporalProperty;
import com.nickrobison.trestle.types.TemporalType;
import com.vividsolutions.jts.geom.Geometry;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Created by nrobison on 8/29/16.
 */
public class TestClasses {

    @OWLClassName(className = "odt-test")
    protected static class OffsetDateTimeTest {

        @IndividualIdentifier
        public final Integer adm0_code;
        @StartTemporalProperty
        public final OffsetDateTime startTemporal;
        @EndTemporalProperty
        public final OffsetDateTime endTemporal;

        public OffsetDateTimeTest(Integer adm0_code, OffsetDateTime startTemporal, OffsetDateTime endTemporal) {
            this.adm0_code = adm0_code;
            this.startTemporal = startTemporal;
            this.endTemporal = endTemporal;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            OffsetDateTimeTest that = (OffsetDateTimeTest) o;

            if (!adm0_code.equals(that.adm0_code)) return false;
            if (!startTemporal.equals(that.startTemporal)) return false;
            return endTemporal.equals(that.endTemporal);

        }

        @Override
        public int hashCode() {
            int result = adm0_code.hashCode();
            result = 31 * result + startTemporal.hashCode();
            result = 31 * result + endTemporal.hashCode();
            return result;
        }
    }

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
        @Spatial
        public final Polygon geom;
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

        @DefaultTemporalProperty(name = "date", type = TemporalType.INTERVAL, duration = 1, unit = ChronoUnit.YEARS)
        public LocalDate getDate() {
            return this.date;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ESRIPolygonTest that = (ESRIPolygonTest) o;

//            There are very subtle differences in the way WKTs are stored and managed, so we do a bit of munging to figure out the differences in area
            if (!getAdm0_code().equals(that.getAdm0_code())) return false;
            if (!(Math.round(geom.calculateArea2D() - that.geom.calculateArea2D()) == 0)) return false;
            return getDate().equals(that.getDate());

        }

        @Override
        public int hashCode() {
            int result = getAdm0_code().hashCode();
            result = 31 * result + geom.hashCode();
            result = 31 * result + getDate().hashCode();
            return result;
        }
    }

    @OWLClassName(className = "GAUL_Test")
    public static class GeotoolsPolygonTest {
        @IndividualIdentifier
        public final UUID id;
        @Spatial
        public final org.opengis.geometry.coordinate.Polygon geom;
        @DefaultTemporalProperty(name = "date", type = TemporalType.INTERVAL, duration = 1, unit = ChronoUnit.YEARS)
        public final LocalDate date;

        public GeotoolsPolygonTest(UUID id, org.opengis.geometry.coordinate.Polygon geom, LocalDate date) {
            this.id = id;
            this.geom = geom;
            this.date = date;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            GeotoolsPolygonTest that = (GeotoolsPolygonTest) o;

            if (!id.equals(that.id)) return false;
            if (!geom.equals(that.geom)) return false;
            return date != null ? date.equals(that.date) : that.date == null;

        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + geom.hashCode();
            result = 31 * result + (date != null ? date.hashCode() : 0);
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


    @OWLClassName(className = "gaul-complex")
    public static class GAULComplexClassTest {

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

    /**
     * Created by nrobison on 7/29/16.
     */
    @OWLClassName(className = "GAUL_Test")
    public static class GAULMethodTest {

        public int adm0_code;
        private String adm0_name;
        @Spatial
        public String test_name;

        @Ignore
        public LocalDateTime defaultTime;
        private String privateField;
        //        @DefaultTemporalProperty(type = TemporalType.POINT, scope= TemporalScope.EXISTS, duration = 0, unit = ChronoUnit.YEARS)
        private LocalDateTime intervalStart;
        private LocalDateTime intervalEnd;

        public GAULMethodTest() {
            this.adm0_code = 4326;
            this.test_name = "new_test";
            this.adm0_name = "test region";
            this.defaultTime = LocalDateTime.of(1998, 3, 26, 0, 0);
            this.privateField = "don't read me";
            this.intervalStart = LocalDateTime.of(1989, 3, 26, 0, 0);
            this.intervalEnd = this.intervalStart.plusYears(5);
        }

        public GAULMethodTest(int adm0_code, String adm0_name, String test_name, LocalDateTime defaultTime, LocalDateTime intervalStart, LocalDateTime intervalEnd) {
            this.adm0_code = adm0_code;
            this.adm0_name = adm0_name;
            this.test_name = test_name;
            this.defaultTime = defaultTime;
            this.intervalStart = intervalStart;
            this.privateField = "don't read me";
            this.intervalEnd = intervalEnd;
        }

        @IndividualIdentifier
        public String getName() {
            return "string_from_method";
        }

        public int getAdm0_code1() {
            return this.adm0_code;
        }

        public String getadm0_name() {
            return this.adm0_name;
        }

        @DefaultTemporalProperty(type = TemporalType.INTERVAL, duration = 1, unit = ChronoUnit.YEARS)
        public LocalDateTime getTime() {
            return this.defaultTime;
        }

        @StartTemporalProperty(type = TemporalType.INTERVAL)
        public LocalDateTime getStart() {
            return this.intervalStart;
        }

        @EndTemporalProperty()
        public LocalDateTime getEnd() {
            return this.intervalEnd;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            GAULMethodTest that = (GAULMethodTest) o;

            if (getAdm0_code1() != that.getAdm0_code1()) return false;
            if (!adm0_name.equals(that.adm0_name)) return false;
            if (!test_name.equals(that.test_name)) return false;
            if (!defaultTime.equals(that.defaultTime)) return false;
            if (!privateField.equals(that.privateField)) return false;
            if (!intervalStart.equals(that.intervalStart)) return false;
            return intervalEnd.equals(that.intervalEnd);

        }

        @Override
        public int hashCode() {
            int result = getAdm0_code1();
            result = 31 * result + adm0_name.hashCode();
            result = 31 * result + test_name.hashCode();
            result = 31 * result + defaultTime.hashCode();
            result = 31 * result + privateField.hashCode();
            result = 31 * result + intervalStart.hashCode();
            result = 31 * result + intervalEnd.hashCode();
            return result;
        }
    }
}
