package com.nickrobison.trestle.reasoner.engines.collection;

import com.google.common.collect.ImmutableList;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.reasoner.AbstractReasonerTest;
import com.nickrobison.trestle.reasoner.TestClasses;
import com.nickrobison.trestle.types.relations.CollectionRelationType;
import com.nickrobison.trestle.types.relations.ObjectRelation;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.functions.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.nickrobison.trestle.common.StaticIRI.hasRelationIRI;
import static com.nickrobison.trestle.common.StaticIRI.trestleRelationIRI;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by nickrobison on 7/22/18.
 */
@Tag("integration")
public class CollectionEnginesTest extends AbstractReasonerTest {

    private static final String THIRD_COLLECTION = "third:collection";
    private static final String FIRST_COLLECTION = "first:collection";
    public static final String SECOND_COLLECTION = "second:collection";
    private TestClasses.JTSGeometryTest third;
    private TestClasses.JTSGeometryTest second;
    private TestClasses.JTSGeometryTest first;

    @BeforeEach
    public void setupClasses() throws ParseException {
        //        Write some objects
        final LocalDate date = LocalDate.of(2018, 1, 11);
        final Geometry jtsGeom = new WKTReader().read("POINT(4.0 6.0)");
        first = new TestClasses.JTSGeometryTest(100111, jtsGeom, date);
        final Geometry jtsGeom2 = new WKTReader().read("POINT(27.0 91.0)");
        second = new TestClasses.JTSGeometryTest(100112, jtsGeom2, date);
        third = new TestClasses.JTSGeometryTest(100113, jtsGeom2, date);
    }

    @Test
    public void testAdjacentCollections() {
//        Add all to collections
        this.reasoner.addObjectToCollection(FIRST_COLLECTION, first, CollectionRelationType.SEMANTIC, 1.0).blockingAwait();
        this.reasoner.addObjectToCollection(SECOND_COLLECTION, second, CollectionRelationType.SEMANTIC, 1.0).blockingAwait();
        this.reasoner.addObjectToCollection(THIRD_COLLECTION, third, CollectionRelationType.SEMANTIC, 1.0).blockingAwait();

//        Check that we have collections
        assertEquals(4, this.reasoner.getCollections().toList().blockingGet().size(), "Should have all the collections and the demo");

        //        Add a relation between one and two
        this.reasoner.writeObjectRelationship(first, second, ObjectRelation.SPATIAL_MEETS, null).blockingAwait();
//        And one and three
        this.reasoner.writeObjectRelationship(first, third, ObjectRelation.SPATIAL_MEETS, null).blockingAwait();

//        Check for adjacency
        assertAll(() -> assertTrue(this.reasoner.collectionsAreAdjacent(FIRST_COLLECTION, SECOND_COLLECTION, 0.5).blockingGet(), "First and second should be adjacent"),
                () -> assertTrue(this.reasoner.collectionsAreAdjacent(FIRST_COLLECTION, THIRD_COLLECTION, 0.5).blockingGet(), "First and third should be adjacent"),
                () -> assertFalse(this.reasoner.collectionsAreAdjacent(SECOND_COLLECTION, "third:collection", 0.5).blockingGet(), "Second and third should not be adjacent"));
    }

    @Test
    @Disabled // Disabling until TRESTLE-762 is resolved
    public void testObjectRemoval() {
        //        Add all to collections
        this.reasoner.addObjectToCollection(FIRST_COLLECTION, first, CollectionRelationType.SEMANTIC, 1.0).blockingAwait();
        this.reasoner.addObjectToCollection(FIRST_COLLECTION, second, CollectionRelationType.SEMANTIC, 1.0).blockingAwait();
        this.reasoner.addObjectToCollection(THIRD_COLLECTION, third, CollectionRelationType.SEMANTIC, 1.0).blockingAwait();

//        Verify third collection
        this.reasoner.removeObjectFromCollection(THIRD_COLLECTION, third, true).blockingAwait();
//        Verify that it's gone
        final ITrestleOntology ontology = this.reasoner.getUnderlyingOntology();
        final @NonNull List<TestClasses.JTSGeometryTest> thirdCollection = this.reasoner.getCollectionMembers(TestClasses.JTSGeometryTest.class, THIRD_COLLECTION, 0.1, null, null).toList().blockingGet();
        final List<OWLObjectPropertyAssertionAxiom> relationRelations = ontology.getIndividualObjectProperty(IRI.create(OVERRIDE_PREFIX, "100113"), hasRelationIRI).toList().blockingGet();
//        Make sure the Object doesn't have the relationship
        assertAll(() -> assertTrue(thirdCollection.isEmpty(), "Should not any members"),
                () -> assertTrue(relationRelations.isEmpty(), "Object should not have relationship relations"));

//        Verify first collection
        this.reasoner.removeObjectFromCollection(FIRST_COLLECTION, second, true).blockingAwait();
        @NonNull List<TestClasses.JTSGeometryTest> firstCollection = this.reasoner.getCollectionMembers(TestClasses.JTSGeometryTest.class, FIRST_COLLECTION, 0.1, null, null).toList().blockingGet();
        assertFalse(firstCollection.isEmpty(), "First collection should be there.");

//        First object should have relations
        final List<OWLObjectPropertyAssertionAxiom> firstRelations = ontology.getIndividualObjectProperty(df.getOWLNamedIndividual(IRI.create(OVERRIDE_PREFIX, "100111")), hasRelationIRI).toList().blockingGet();
        assertFalse(firstRelations.isEmpty(), "First object should still have relation");
        final List<OWLObjectPropertyAssertionAxiom> secondRelations = ontology.getIndividualObjectProperty(df.getOWLNamedIndividual(IRI.create(OVERRIDE_PREFIX, "100112")), hasRelationIRI).toList().blockingGet();
        assertTrue(secondRelations.isEmpty(), "Second object should not have relations");

//        Try to remove, but leave collection
        this.reasoner.removeObjectFromCollection(FIRST_COLLECTION, first, false).blockingAwait();
        final List<OWLObjectPropertyAssertionAxiom> firstEmptyRelations = ontology.getIndividualObjectProperty(df.getOWLNamedIndividual(IRI.create(OVERRIDE_PREFIX, "100111")), hasRelationIRI).toList().blockingGet();
//        assertTrue(firstEmptyRelations.isEmpty(), "First object should not have relations");
        firstCollection = this.reasoner.getCollectionMembers(TestClasses.JTSGeometryTest.class, FIRST_COLLECTION, 0.1, null, null).toList().blockingGet();
        assertTrue(firstCollection.isEmpty(), "Should have nothing in it");
    }

    @Test
    public void testCollectionRemoval() {
        //        Add all to collections
        this.reasoner.addObjectToCollection(FIRST_COLLECTION, first, CollectionRelationType.SEMANTIC, 1.0).blockingAwait();
        this.reasoner.addObjectToCollection(FIRST_COLLECTION, second, CollectionRelationType.SEMANTIC, 1.0).blockingAwait();
        this.reasoner.addObjectToCollection(FIRST_COLLECTION, third, CollectionRelationType.SEMANTIC, 1.0).blockingAwait();

//        Remove the collection
        this.reasoner.removeCollection(FIRST_COLLECTION).blockingAwait();

//        Test that there are no relations left
        final Set<OWLNamedIndividual> relations = this.reasoner.getUnderlyingOntology().getInstances(df.getOWLClass(trestleRelationIRI), true).collect((Supplier<HashSet<OWLNamedIndividual>>) HashSet::new, HashSet::add).blockingGet();
        assertEquals(1, relations.size(), "Should only have the demo relation");
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
