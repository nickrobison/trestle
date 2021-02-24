package com.nickrobison.trestle.reasoner.engines.spatial.containment;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Polygon;
import com.google.common.collect.ImmutableList;
import com.nickrobison.trestle.SharedTestUtils;
import com.nickrobison.trestle.reasoner.AbstractReasonerTest;
import com.nickrobison.trestle.reasoner.TestClasses;
import com.nickrobison.trestle.reasoner.annotations.DatasetClass;
import com.nickrobison.trestle.reasoner.annotations.IndividualIdentifier;
import com.nickrobison.trestle.reasoner.annotations.temporal.EndTemporal;
import com.nickrobison.trestle.reasoner.annotations.temporal.StartTemporal;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.union.EqualityTests;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Created by detwiler on 8/31/17.
 */
@Tag("integration")
public class ContainmentTests extends AbstractReasonerTest {

    @Test
    @SuppressWarnings({"dereference.of.nullable", "argument.type.incompatible"})
    public void containmentDirectionTest() throws IOException {
        final TestClasses.ESRIPolygonTest originalObject;

        // Read in the individual
        try (InputStream originalStream = EqualityTests.EqualityTestClass.class.getClassLoader().getResourceAsStream("98103.csv")) {
            assertNotNull(originalStream);
            try (BufferedReader originalReader = new BufferedReader(new InputStreamReader(originalStream, StandardCharsets.UTF_8))) {
                final String[] firstLine = originalReader.readLine().split(";");
                originalObject = new TestClasses.ESRIPolygonTest(Integer.parseInt(firstLine[0]), (Polygon) GeometryEngine.geometryFromWkt(firstLine[1], 0, Geometry.Type.Polygon), LocalDate.of(2001, 1, 1));
                //splitObjects.add(originalObject);
            } finally {
                originalStream.close();
            }
        }

        // Read in the dissolved ones
        final SharedTestUtils.ITestClassConstructor<TestClasses.ESRIPolygonTest, String> esriConstructor = (line -> {
            final String[] splitLine = line.split(";");
            return new TestClasses.ESRIPolygonTest(Integer.parseInt(splitLine[0]), (Polygon) GeometryEngine.geometryFromWkt(splitLine[1], 0, Geometry.Type.Polygon), LocalDate.of(2000, 1, 1));
        });

        List<TestClasses.ESRIPolygonTest> splitObjects = new ArrayList<>(SharedTestUtils.readFromCSV("98103_split.csv", esriConstructor));
        assertEquals(5, splitObjects.size(), "Should have 5 split objects");

        // test containment between original object and first split object
        ContainmentEngine engine = this.reasoner.getContainmentEngine();
        ContainmentEngine.ContainmentDirection containmentDir = engine.getApproximateContainment(originalObject, splitObjects.get(0), 0.9);
        assertEquals(ContainmentEngine.ContainmentDirection.CONTAINS, containmentDir, "Should have containment direction ContainmentDirection.CONTAINS");

        // test same two objects but in the opposite direction
        containmentDir = engine.getApproximateContainment(splitObjects.get(0), originalObject, 0.9);
        assertEquals(ContainmentEngine.ContainmentDirection.WITHIN, containmentDir, "Should have containment direction ContainmentDirection.WITHIN");

        //test between two split objects (neither should contain the other)
        containmentDir = engine.getApproximateContainment(splitObjects.get(0), splitObjects.get(1), 0.9);
        assertEquals(ContainmentEngine.ContainmentDirection.NONE, containmentDir, "Should have containment direction ContainmentDirection.NONE");
    }

    @Override
    protected String getTestName() {
        return "containment_tests";
    }

    @Override
    protected ImmutableList<Class<?>> registerClasses() {
        return ImmutableList.of(ContainmentTests.ContainmentTestClass.class, TestClasses.ESRIPolygonTest.class);
    }

    @DatasetClass(name = "containment-test")
    public static class ContainmentTestClass implements Serializable {
        private static final long serialVersionUID = 42L;

        private final String id;
        private final LocalDate startDate;
        private final LocalDate endDate;

        public ContainmentTestClass(String id, LocalDate startDate, LocalDate endDate) {
            this.id = id;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        @IndividualIdentifier
        public String getId() {
            return id;
        }

        @StartTemporal
        public LocalDate getStartDate() {
            return startDate;
        }

        @EndTemporal
        public LocalDate getEndDate() {
            return endDate;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ContainmentTests.ContainmentTestClass that = (ContainmentTests.ContainmentTestClass) o;

            if (!id.equals(that.id)) return false;
            if (!startDate.equals(that.startDate)) return false;
            return endDate.equals(that.endDate);
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + startDate.hashCode();
            result = 31 * result + endDate.hashCode();
            return result;
        }
    }
}
