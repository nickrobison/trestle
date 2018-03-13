package com.nickrobison.trestle.reasoner;

import com.esri.core.geometry.Polygon;
import com.nickrobison.trestle.reasoner.annotations.*;
import com.nickrobison.trestle.reasoner.annotations.temporal.DefaultTemporal;
import com.nickrobison.trestle.reasoner.annotations.temporal.EndTemporal;
import com.nickrobison.trestle.reasoner.annotations.temporal.StartTemporal;
import com.nickrobison.trestle.types.TemporalType;
import com.vividsolutions.jts.geom.Geometry;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.io.Serializable;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Created by nrobison on 8/29/16.
 */
public class TestClasses {

    @DatasetClass(name = "odt-test")
    public static class OffsetDateTimeTest implements Serializable {
        private static final long serialVersionUID = 42L;

        @IndividualIdentifier
        public final Integer adm0_code;
        @StartTemporal
        public final OffsetDateTime startTemporal;
        @EndTemporal
        public final OffsetDateTime endTemporal;

        public OffsetDateTimeTest(Integer adm0_code, OffsetDateTime startTemporal, OffsetDateTime endTemporal) {
            this.adm0_code = adm0_code;
            this.startTemporal = startTemporal;
            this.endTemporal = endTemporal;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            OffsetDateTimeTest that = (OffsetDateTimeTest) o;

            return adm0_code.equals(that.adm0_code);
        }

        @Override
        public int hashCode() {
            return adm0_code.hashCode();
        }
    }

    @DatasetClass(name = "GAUL_JTS_Test")
    public static class JTSGeometryTest implements Serializable {
        private static final long serialVersionUID = 42L;

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

        @Spatial(projection = 4269)
        public Geometry getGeom() {
            return this.geom;
        }

        @DefaultTemporal(name = "date", type = TemporalType.INTERVAL, duration = 1, unit = ChronoUnit.YEARS)
        public LocalDate getDate() {
            return this.date;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            JTSGeometryTest that = (JTSGeometryTest) o;

            if (!getAdm0_code().equals(that.getAdm0_code())) return false;
            return getGeom().equalsExact(that.getGeom(), .01);
        }

        @Override
        public int hashCode() {
            int result = getAdm0_code().hashCode();
            result = 31 * result + getGeom().hashCode();
            return result;
        }
    }

    @DatasetClass(name = "GAUL_ESRI_Test")
    public static class ESRIPolygonTest implements Serializable {
        private static final long serialVersionUID = 42L;

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

        @DefaultTemporal(name = "date", type = TemporalType.INTERVAL, duration = 1, unit = ChronoUnit.YEARS)
        public LocalDate getDate() {
            return this.date;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ESRIPolygonTest that = (ESRIPolygonTest) o;

            if (!getAdm0_code().equals(that.getAdm0_code())) return false;
            return geom.equals(that.geom);
        }

        @Override
        public int hashCode() {
            int result = getAdm0_code().hashCode();
            result = 31 * result + geom.hashCode();
            return result;
        }
    }

    @DatasetClass(name = "GAUL_GeoTools_Test")
    public static class GeotoolsPolygonTest implements Serializable {
        private static final long serialVersionUID = 42L;

        @IndividualIdentifier
        public final UUID id;
        @Spatial
        public final org.opengis.geometry.coordinate.Polygon geom;
        @DefaultTemporal(name = "date", type = TemporalType.INTERVAL, duration = 1, unit = ChronoUnit.YEARS, timeZone = "America/Los_Angeles")
        public final LocalDate date;

        public GeotoolsPolygonTest(UUID id, org.opengis.geometry.coordinate.Polygon geom, LocalDate date) {
            this.id = id;
            this.geom = geom;
            this.date = date;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            GeotoolsPolygonTest that = (GeotoolsPolygonTest) o;

            if (!id.equals(that.id)) return false;
            return geom.equals(that.geom);
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + geom.hashCode();
            return result;
        }
    }

    @DatasetClass(name = "GAUL_Test")
    public static class GAULTestClass implements Serializable {
        private static final long serialVersionUID = 42L;

        @Fact(name = "adm0_code", datatype = OWL2Datatype.XSD_INTEGER)
        public int adm0_code;
        public String adm0_name;
        @IndividualIdentifier
        @Ignore
        public String test_name;
        @Spatial
        public String wkt;
        @StartTemporal
        public LocalDateTime time;
        @EndTemporal
        public LocalDateTime endTime;

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
            this.time = LocalDateTime.now().plusYears(5);
            this.wkt = "test_wkt";
        }

        @TrestleCreator
        public GAULTestClass(int adm0_code, String adm0_name, LocalDateTime time, LocalDateTime endTime, String wkt) {
            this.test_name = adm0_name;
            this.adm0_code = adm0_code;
            this.adm0_name = adm0_name;
            this.wkt = wkt;
            this.time = time;
            this.endTime = endTime;

        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            GAULTestClass that = (GAULTestClass) o;

            if (adm0_code != that.adm0_code) return false;
            if (!adm0_name.equals(that.adm0_name)) return false;
            if (!test_name.equals(that.test_name)) return false;
            return wkt.equals(that.wkt);
        }

        @Override
        public int hashCode() {
            int result = adm0_code;
            result = 31 * result + adm0_name.hashCode();
            result = 31 * result + test_name.hashCode();
            result = 31 * result + wkt.hashCode();
            return result;
        }
    }

    @DatasetClass(name = "gaul-complex")
    public static class GAULComplexClassTest implements Serializable {
        private static final long serialVersionUID = 42L;

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
            this.testDouble = Double.valueOf("3.14");
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

        @StartTemporal
        public LocalDate getAtDate() {
            return this.atDate;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            GAULComplexClassTest that = (GAULComplexClassTest) o;

            if (testPrimitiveInt != that.testPrimitiveInt) return false;
            if (Double.compare(that.testPrimitiveDouble, testPrimitiveDouble) != 0) return false;
            if (!id.equals(that.id)) return false;
            if (!testBigInt.equals(that.testBigInt)) return false;
            if (!testInteger.equals(that.testInteger)) return false;
            if (!getTestDouble().equals(that.getTestDouble())) return false;
            return getWkt().equals(that.getWkt());
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            result = id.hashCode();
            result = 31 * result + testBigInt.hashCode();
            result = 31 * result + testPrimitiveInt;
            result = 31 * result + testInteger.hashCode();
            result = 31 * result + getTestDouble().hashCode();
            temp = Double.doubleToLongBits(testPrimitiveDouble);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            result = 31 * result + getWkt().hashCode();
            return result;
        }
    }

    @DatasetClass(name = "GAUL_Test1")
    public static class GAULMethodTest implements Serializable {
        private static final long serialVersionUID = 42L;

        public int adm0_code;
        private String adm0_name;
        @Spatial
        public String test_name;

        @Ignore
        public LocalDateTime defaultTime;
        private String privateField;
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

        @StartTemporal(type = TemporalType.INTERVAL)
        public LocalDateTime getStart() {
            return this.intervalStart;
        }

        @EndTemporal()
        public LocalDateTime getEnd() {
            return this.intervalEnd;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            GAULMethodTest that = (GAULMethodTest) o;

            if (adm0_code != that.adm0_code) return false;
            if (!adm0_name.equals(that.adm0_name)) return false;
            if (!test_name.equals(that.test_name)) return false;
            return privateField.equals(that.privateField);
        }

        @Override
        public int hashCode() {
            int result = adm0_code;
            result = 31 * result + adm0_name.hashCode();
            result = 31 * result + test_name.hashCode();
            result = 31 * result + privateField.hashCode();
            return result;
        }

    }

    @DatasetClass(name = "multiLang-test")
    public static class MultiLangTest implements Serializable {
        private static final long serialVersionUID = 42L;

        @Fact(name = "testString")
        @Language(language = "en")
        public final String englishString;
        @Fact(name = "testString")
        @Language(language = "heb")
        public final String hebrewString;
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
            this.frenchString = "Chaîne d'essai";
            this.englishGBString = "test string";
            this.hebrewString = "מחרוזת בדיקה";
            this.defaultTime = LocalDate.now();
            this.id = "test-multilang";
            this.testString2 = "second string";
            this.testString2cs = "second string";
        }

        @TrestleCreator
        public MultiLangTest(String englishString, String englishGBString, String frenchString, String hebrewString, LocalDate defaultTime, String id, String testString2, String testString2cs) {
            this.defaultTime = defaultTime;
            this.frenchString = frenchString;
            this.englishGBString = englishGBString;
            this.englishString = englishString;
            this.id = id;
            this.testString2 = testString2;
            this.testString2cs = testString2cs;
            this.hebrewString = hebrewString;
        }

        @Fact(name = "testString")
        @Language(language = "fr")
        public String getFrenchString() {
            return frenchString;
        }

        @Fact(name = "testString")
        @Language(language = "en-GB")
        public String getEnglishGBString() {
            return englishGBString;
        }

        @DefaultTemporal(type = TemporalType.INTERVAL, duration = 1, unit = ChronoUnit.YEARS)
        public LocalDate getDefaultTime() {
            return defaultTime;
        }

        @Fact(name = "testString2")
        @Language(language = "cs")
        public String getTestString2cs() {
            return testString2cs;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MultiLangTest that = (MultiLangTest) o;

            if (!englishString.equals(that.englishString)) return false;
            if (!hebrewString.equals(that.hebrewString)) return false;
            if (!getFrenchString().equals(that.getFrenchString())) return false;
            if (!getEnglishGBString().equals(that.getEnglishGBString())) return false;
            if (!testString2.equals(that.testString2)) return false;
            if (!getTestString2cs().equals(that.getTestString2cs())) return false;
            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            int result = englishString.hashCode();
            result = 31 * result + hebrewString.hashCode();
            result = 31 * result + getFrenchString().hashCode();
            result = 31 * result + getEnglishGBString().hashCode();
            result = 31 * result + testString2.hashCode();
            result = 31 * result + getTestString2cs().hashCode();
            result = 31 * result + id.hashCode();
            return result;
        }
    }

    @DatasetClass(name = "VersionTest")
    public static class FactVersionTest implements Serializable {
        private static final long serialVersionUID = 42L;

        @IndividualIdentifier
        public final String id;
        private final LocalDate validFrom;
        private final String wkt;
        public final String testValue;


        public FactVersionTest(String id, LocalDate validFrom, String wkt, String testValue) {
            this.id = id;
            this.validFrom = validFrom;
            this.wkt = wkt;
            this.testValue = testValue;
        }

        @Spatial
        public String getWkt() {
            return this.wkt;
        }

        @StartTemporal
        public LocalDate getValidFrom() {
            return this.validFrom;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FactVersionTest that = (FactVersionTest) o;

            if (!id.equals(that.id)) return false;
            if (!getWkt().equals(that.getWkt())) return false;
            return testValue.equals(that.testValue);
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + getWkt().hashCode();
            result = 31 * result + testValue.hashCode();
            return result;
        }
    }

    public interface ICensusTract {
        String getName();
    }

    @DatasetClass(name = "census")
    public static class CensusProjectionTestClass implements ICensusTract {
        private static final long serialVersionUID = 42L;

        private final LocalDate startTemporal;
        private final Long objectid;
        private final String  name;
        private final Geometry geom;


        public CensusProjectionTestClass(Long objectid, String name, Geometry geom) {
            this.objectid = objectid;
            this.name = name;
            this.geom = geom;
            this.startTemporal = LocalDate.of(2010, 1, 1);
        }

        @DefaultTemporal(duration = 20, unit = ChronoUnit.YEARS, type = TemporalType.INTERVAL)
        public LocalDate getStartTemporal() {
            return startTemporal;
        }

        @IndividualIdentifier
        public Long getObjectid() {
            return objectid;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Spatial(projection = 4269)
        public Geometry getGeom() {
            return geom;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CensusProjectionTestClass that = (CensusProjectionTestClass) o;
            return Objects.equals(startTemporal, that.startTemporal) &&
                    Objects.equals(objectid, that.objectid) &&
                    Objects.equals(geom, that.geom);
        }

        @Override
        public int hashCode() {

            return Objects.hash(startTemporal, objectid, geom);
        }
    }

    @DatasetClass(name = "king-county")
    public static class KCProjectionTestClass implements ICensusTract {
        private static final long serialVersionUID = 42L;

        private final LocalDate startTemporal;
        private final Long objectid;
        private final String name;
        private final Geometry geom;


        public KCProjectionTestClass(Long objectid, String name, Geometry geom) {
            this.objectid = objectid;
            this.name = name;
            this.geom = geom;
            this.startTemporal = LocalDate.of(2010, 1, 1);
        }

        @DefaultTemporal(duration = 20, unit = ChronoUnit.YEARS, type = TemporalType.INTERVAL)
        public LocalDate getStartTemporal() {
            return startTemporal;
        }

        @IndividualIdentifier
        public Long getObjectid() {
            return objectid;
        }

        @Language(language = "en-US")
        @Override
        public String getName() {
            return this.name;
        }

        //        Watch out! QGIS suggests that this EPSG code is 102748, but Geotools doesn't know that one
        @Spatial(projection = 2285)
        public Geometry getGeom() {
            return geom;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            KCProjectionTestClass that = (KCProjectionTestClass) o;
            return Objects.equals(startTemporal, that.startTemporal) &&
                    Objects.equals(objectid, that.objectid) &&
                    Objects.equals(geom, that.geom);
        }

        @Override
        public int hashCode() {

            return Objects.hash(startTemporal, objectid, geom);
        }
    }
}
