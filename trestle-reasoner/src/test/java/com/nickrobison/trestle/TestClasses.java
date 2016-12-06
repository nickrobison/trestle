package com.nickrobison.trestle;

import com.esri.core.geometry.Polygon;
import com.nickrobison.trestle.annotations.*;
import com.nickrobison.trestle.annotations.temporal.DefaultTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.EndTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.StartTemporalProperty;
import com.nickrobison.trestle.types.TemporalType;
import com.vividsolutions.jts.geom.Geometry;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
            if (!startTemporal.atZoneSameInstant(ZoneOffset.UTC).equals(that.startTemporal.atZoneSameInstant(ZoneOffset.UTC))) return false;
            return endTemporal.atZoneSameInstant(ZoneOffset.UTC).equals(that.endTemporal.atZoneSameInstant(ZoneOffset.UTC));

        }

        @Override
        public int hashCode() {
            int result = adm0_code.hashCode();
            result = 31 * result + startTemporal.hashCode();
            result = 31 * result + endTemporal.hashCode();
            return result;
        }
    }

    @OWLClassName(className = "GAUL_JTS_Test")
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

    @OWLClassName(className = "GAUL_ESRI_Test")
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

    @OWLClassName(className = "GAUL_GeoTools_Test")
    public static class GeotoolsPolygonTest {
        @IndividualIdentifier
        public final UUID id;
        @Spatial
        public final org.opengis.geometry.coordinate.Polygon geom;
        @DefaultTemporalProperty(name = "date", type = TemporalType.INTERVAL, duration = 1, unit = ChronoUnit.YEARS, timeZone = "America/Los_Angeles")
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            GAULTestClass that = (GAULTestClass) o;

            if (adm0_code != that.adm0_code) return false;
            if (!adm0_name.equals(that.adm0_name)) return false;
            if (!test_name.equals(that.test_name)) return false;
            if (!wkt.equals(that.wkt)) return false;
            return time.equals(that.time);

        }

        @Override
        public int hashCode() {
            int result = adm0_code;
            result = 31 * result + adm0_name.hashCode();
            result = 31 * result + test_name.hashCode();
            result = 31 * result + wkt.hashCode();
            result = 31 * result + time.hashCode();
            return result;
        }
    }

    @OWLClassName(className = "gaul-complex")
    public static class GAULComplexClassTest {

        @IndividualIdentifier
        public UUID id;
        public final BigInteger testBigInt;
        public final int testPrimitiveInt;
        public final Integer testInteger;
        private final Double testDouble;
        public final double testPrimitiveDouble;
        private final String wkt;
        private final LocalDate atDate;

        public GAULComplexClassTest() {
            this.id = UUID.randomUUID();
            this.wkt = "POINT(4.0 6.0)";
            this.atDate = LocalDate.of(1989, 3, 26);
            this.testBigInt = new BigInteger("10");
            this.testPrimitiveInt = 14;
            this.testDouble = new Double("3.14");
            this.testPrimitiveDouble = 3.141592654;
            this.testInteger = 42;
        }

        public GAULComplexClassTest(UUID id, int testPrimitiveInt, String wkt, Double testDouble, Integer testInteger, LocalDate atDate, BigInteger testBigInt, double testPrimitiveDouble) {
            this.id = id;
            this.testPrimitiveInt = testPrimitiveInt;
            this.wkt = wkt;
            this.testDouble = testDouble;
            this.atDate = atDate;
            this.testBigInt = testBigInt;
            this.testPrimitiveDouble = testPrimitiveDouble;
            this.testInteger = testInteger;
        }

        public Double getTestDouble() {
            return this.testDouble;
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

            if (testPrimitiveInt != that.testPrimitiveInt) return false;
            if (!id.equals(that.id)) return false;
            if (!testBigInt.equals(that.testBigInt)) return false;
            if (!getTestDouble().equals(that.getTestDouble())) return false;
            if (!getWkt().equals(that.getWkt())) return false;
            return getAtDate().equals(that.getAtDate());
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + testBigInt.hashCode();
            result = 31 * result + testPrimitiveInt;
            result = 31 * result + getTestDouble().hashCode();
            result = 31 * result + getWkt().hashCode();
            result = 31 * result + getAtDate().hashCode();
            return result;
        }
    }

    @OWLClassName(className = "GAUL_Test1")
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

    @OWLClassName(className = "multiLang-test")
    public static class MultiLangTest {

        @DataProperty(name = "testString")
        @Language(language = "en")
        public final String englishString;
        private final String frenchString;
        private final String englishGBString;
        private final LocalDate defaultTime;
//        @Language(language = "kk")
        public final String testString2;
        private final String testString2cs;
        @IndividualIdentifier
        @NoMultiLanguage
        public final String id;

        public MultiLangTest() {
            this.englishString = "test string";
            this.frenchString = "test string";
            this.englishGBString = "test string";
            this.defaultTime = LocalDate.now();
            this.id = "test-multilang";
            this.testString2 = "second string";
            this.testString2cs = "second string";
        }

        @TrestleCreator
        public MultiLangTest(String englishString, String englishGBString, String frenchString, LocalDate defaultTime, String id, String testString2, String testString2cs) {
            this.defaultTime = defaultTime;
            this.frenchString = frenchString;
            this.englishGBString = englishGBString;
            this.englishString = englishString;
            this.id = id;
            this.testString2 = testString2;
            this.testString2cs = testString2cs;
        }

        @DataProperty(name = "testString")
        @Language(language = "fr")
        public String getFrenchString() {
            return frenchString;
        }

        @DataProperty(name = "testString")
        @Language(language = "en-GB")
        public String getEnglishGBString() {
            return englishGBString;
        }

        @DefaultTemporalProperty(type = TemporalType.INTERVAL, duration = 1, unit = ChronoUnit.YEARS)
        public LocalDate getDefaultTime() {
            return defaultTime;
        }

        @DataProperty(name = "testString2")
        @Language(language = "cs")
        public String getTestString2cs() {
            return testString2cs;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MultiLangTest that = (MultiLangTest) o;

            if (!englishString.equals(that.englishString)) return false;
            if (!getFrenchString().equals(that.getFrenchString())) return false;
            if (!getEnglishGBString().equals(that.getEnglishGBString())) return false;
            if (!getDefaultTime().equals(that.getDefaultTime())) return false;
            if (!testString2.equals(that.testString2)) return false;
            if (!getTestString2cs().equals(that.getTestString2cs())) return false;
            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            int result = englishString.hashCode();
            result = 31 * result + getFrenchString().hashCode();
            result = 31 * result + getEnglishGBString().hashCode();
            result = 31 * result + getDefaultTime().hashCode();
            result = 31 * result + testString2.hashCode();
            result = 31 * result + getTestString2cs().hashCode();
            result = 31 * result + id.hashCode();
            return result;
        }
    }
}
