package com.nickrobison.trestle.reasoner.engines.collection;

import com.google.common.collect.ImmutableList;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.AbstractReasonerTest;
import com.nickrobison.trestle.reasoner.TestClasses;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.types.relations.CollectionRelationType;
import com.nickrobison.trestle.types.relations.ObjectRelation;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by nickrobison on 7/22/18.
 */
@Tag("integration")
public class CollectionEnginesTest extends AbstractReasonerTest {

    @Test
    public void testAdjacentCollections() throws TrestleClassException, MissingOntologyEntity, ParseException {
//        Write two objects
        final LocalDate date = LocalDate.of(2018, 1, 11);
        final Geometry jtsGeom = new WKTReader().read("POINT(4.0 6.0)");
        final TestClasses.JTSGeometryTest first = new TestClasses.JTSGeometryTest(100111, jtsGeom, date);
        final Geometry jtsGeom2 = new WKTReader().read("POINT(27.0 91.0)");
        final TestClasses.JTSGeometryTest second = new TestClasses.JTSGeometryTest(100112, jtsGeom2, date);
        final TestClasses.JTSGeometryTest third = new TestClasses.JTSGeometryTest(100113, jtsGeom2, date);

//        Add all to collections
        this.reasoner.addObjectToCollection("first:collection", first, CollectionRelationType.SEMANTIC, 1.0);
        this.reasoner.addObjectToCollection("second:collection", second, CollectionRelationType.SEMANTIC, 1.0);
        this.reasoner.addObjectToCollection("third:collection", third, CollectionRelationType.SEMANTIC, 1.0);
        //        Add a relation between one and two
        this.reasoner.writeObjectRelationship(first, second, ObjectRelation.SPATIAL_MEETS);
//        And one and three
        this.reasoner.writeObjectRelationship(first, third, ObjectRelation.SPATIAL_MEETS);

//        Check for adjacency
        assertAll(() -> assertTrue(this.reasoner.collectionsAreAdjacent("first:collection", "second:collection", 0.5), "First and second should be adjacent"),
                () -> assertTrue(this.reasoner.collectionsAreAdjacent("first:collection", "third:collection", 0.5), "First and third should not be adjacent"),
                () -> assertFalse(this.reasoner.collectionsAreAdjacent("second:collection", "third:collection", 0.5), "Second and third should not be adjacent"));
    }

    @Test
    public void testCollectionRemoval() {

    }

    @Override
    protected String getTestName() {
        return "collections_test";
    }

    @Override
    protected ImmutableList<Class<?>> registerClasses() {
        return ImmutableList.of(TestClasses.JTSGeometryTest.class);
    }
}
