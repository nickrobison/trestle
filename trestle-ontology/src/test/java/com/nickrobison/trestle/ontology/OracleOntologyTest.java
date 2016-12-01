package com.nickrobison.trestle.ontology;

import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.typesafe.config.Config;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by nrobison on 6/24/16.
 */
@SuppressWarnings({"OptionalGetWithoutIsPresent", "initialization", "Duplicates", "unchecked"})
public class OracleOntologyTest extends OntologyTest {


    @Override
    public void setupOntology() throws OWLOntologyCreationException {
        final Config localConf = config.getConfig("trestle.ontology.oracle");

        ontology = new OntologyBuilder()
                .withDBConnection(localConf.getString("connectionString"),
                        localConf.getString("username"),
                        localConf.getString("password"))
                .fromInputStream(inputStream)
                .name("trestle_test3")
                .build().get();

        ontology.initializeOntology();
    }

    @Test
    @Override
    public void testByteParsing() throws MissingOntologyEntity {

        int smallInt = 4321;
        int bigInt = Integer.MAX_VALUE;
        int negativeInt = Integer.MIN_VALUE;
        long smallLong = 4321;
        long negativeLong = -4321;
        long negativeBigLong = Long.MIN_VALUE;
        long bigLong = Long.MAX_VALUE;
        Double bigFloat = 4321.43;

        final OWLNamedIndividual long_test = df.getOWLNamedIndividual(IRI.create("trestle:", "long_test"));
        OWLDataProperty aLong = df.getOWLDataProperty(IRI.create("trestle:", "long_small"));
        OWLLiteral owlLiteral = df.getOWLLiteral(Long.toString(smallLong), OWL2Datatype.XSD_LONG);
        final OWLClass owlCl = df.getOWLClass(IRI.create("trestle:", "test"));
        OWLClassAssertionAxiom owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        Optional<Set<OWLLiteral>> individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong);
        assertEquals(OWL2Datatype.XSD_INTEGER, individualDataProperty.get().stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");

//        Big long
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "long_big"));
        owlLiteral = df.getOWLLiteral(Long.toString(bigLong), OWL2Datatype.XSD_LONG);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong);
        assertEquals(Long.toString(bigLong), individualDataProperty.get().stream().findFirst().get().getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_LONG, individualDataProperty.get().stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");

//        Int
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "int_small"));
        owlLiteral = df.getOWLLiteral(Integer.toString(smallInt), OWL2Datatype.XSD_INTEGER);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong);
        assertEquals(Integer.toString(smallInt), individualDataProperty.get().stream().findFirst().get().getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_INTEGER, individualDataProperty.get().stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");

        //        Big Int
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "int_big"));
        owlLiteral = df.getOWLLiteral(Integer.toString(bigInt), OWL2Datatype.XSD_INTEGER);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong);
        assertEquals(Integer.toString(bigInt), individualDataProperty.get().stream().findFirst().get().getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_INTEGER, individualDataProperty.get().stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");

        //        Negative Int
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "neg_int"));
        owlLiteral = df.getOWLLiteral(Integer.toString(negativeInt), OWL2Datatype.XSD_INTEGER);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong);
        assertEquals(Integer.toString(negativeInt), individualDataProperty.get().stream().findFirst().get().getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_INTEGER, individualDataProperty.get().stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");

        //        Double
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "float"));
        owlLiteral = df.getOWLLiteral(Double.toString(bigFloat), OWL2Datatype.XSD_DECIMAL);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong);
        assertEquals(Double.toString(bigFloat), individualDataProperty.get().stream().findFirst().get().getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_DECIMAL, individualDataProperty.get().stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");

        //        Negative Long
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "neg_long"));
        owlLiteral = df.getOWLLiteral(Long.toString(negativeLong), OWL2Datatype.XSD_LONG);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong);
        assertEquals(Long.toString(negativeLong), individualDataProperty.get().stream().findFirst().get().getLiteral(), "Wrong long value");
//        Oracle can't tell the difference between an int and a small long, so it treats them as longs.
        assertEquals(OWL2Datatype.XSD_INTEGER, individualDataProperty.get().stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");

        aLong = df.getOWLDataProperty(IRI.create("trestle:", "neg_big_long"));
        owlLiteral = df.getOWLLiteral(Long.toString(negativeBigLong), OWL2Datatype.XSD_LONG);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong);
        assertEquals(Long.toString(negativeBigLong), individualDataProperty.get().stream().findFirst().get().getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_LONG, individualDataProperty.get().stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");

    }

//    @Test
//    public void testRelationAssociation() {
//
////        Ensure ontology has correct relational classes
//        final OWLNamedIndividual muni1_muni2 = df.getOWLNamedIndividual(IRI.create("trestle:", "muni1_muni2"));
//        final Set<OWLDataPropertyAssertionAxiom> muniProperties = ontology.getAllDataPropertiesForIndividual(muni1_muni2);
//        assertEquals(1, muniProperties.size(), "Wrong number of properties");
//
//        final Set<OWLObjectPropertyAssertionAxiom> allObjectPropertiesForIndividual = ontology.getAllObjectPropertiesForIndividual(muni1_muni2);
//        assertEquals(2, allObjectPropertiesForIndividual.size(), "Wrong number of object properties");
//
////        Check to ensure the relation is transitive and inferred
//        final OWLNamedIndividual test_muni4 = df.getOWLNamedIndividual(IRI.create("trestle:", "test_muni4"));
//        final OWLObjectProperty owlObjectProperty = df.getOWLObjectProperty(hasRelationIRI);
//        final Optional<Set<OWLObjectPropertyAssertionAxiom>> individualObjectProperty = ontology.getIndividualObjectProperty(test_muni4, owlObjectProperty);
//        assertTrue(individualObjectProperty.isPresent(), "Should have related_to properties");
//        assertEquals(7, individualObjectProperty.get().size(), "Wrong number of related to properties");
//
//        final OWLClass gaulClass = df.getOWLClass(IRI.create("trestle:", "GAUL"));
//        final QueryBuilder queryBuilder = new QueryBuilder(QueryBuilder.DIALECT.ORACLE, ontology.getUnderlyingPrefixManager());
//        final String builtString = queryBuilder.buildRelationQuery(test_muni4, gaulClass, 0.6);
//
//        //        Now for the sparql query
//
//        List<QuerySolution> resultSet = ResultSetFormatter.toList(ontology.executeSPARQL(builtString));
//        assertEquals(4, resultSet.size(), "Wrong number of relations");
//
////        If we have the right number of relations, let's build a set of individuals and get their validity intervals;
//        final Set<OWLNamedIndividual> individuals = resultSet.stream()
//                .map(result -> df.getOWLNamedIndividual(result.getResource("f").getURI()))
//                .collect(Collectors.toSet());
//        assertEquals(4, individuals.size(), "Should have the same number of individuals");
//
//        Map<OWLNamedIndividual, TemporalObject> intervals = new HashMap<>();
//        individuals.forEach(individual -> {
//            final Optional<Set<OWLObjectPropertyAssertionAxiom>> individualObjectProperty1 = ontology.getIndividualObjectProperty(individual, hasTemporalIRI);
//            if (individualObjectProperty1.isPresent()) {
//                final Optional<Set<OWLLiteral>> validFrom = ontology.getIndividualDataProperty(individualObjectProperty1.get().stream().findFirst().get().getObject().asOWLNamedIndividual(), temporalValidFromIRI);
//                final Optional<Set<OWLLiteral>> validTo = ontology.getIndividualDataProperty(individualObjectProperty1.get().stream().findFirst().get().getObject().asOWLNamedIndividual(), temporalValidToIRI);
//                final IntervalTemporal intervalTemporal = TemporalObjectBuilder.valid().from(LocalDateTime.parse(validFrom.get().stream().findFirst().get().getLiteral()))
//                        .to(LocalDateTime.parse(validTo.get().stream().findFirst().get().getLiteral()))
//                        .withRelations(individual);
//                intervals.put(individual, intervalTemporal);
//            }
//        });
//
//        assertEquals(4, intervals.size(), "Should have 4 intervals");
//
//
//    }

    @Override
    public void shutdownOntology() {
        ontology.close(true);
    }


}
