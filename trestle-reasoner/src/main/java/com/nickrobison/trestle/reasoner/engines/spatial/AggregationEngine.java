package com.nickrobison.trestle.reasoner.engines.spatial;

import com.nickrobison.trestle.common.LambdaUtils;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.reasoner.engines.object.ITrestleObjectReader;
import com.nickrobison.trestle.reasoner.parser.IClassParser;
import com.nickrobison.trestle.reasoner.parser.TrestleParser;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorFactory;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorService;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.semanticweb.owlapi.model.OWLClass;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Created by nickrobison on 3/24/18.
 */
public class AggregationEngine {

    private final ITrestleObjectReader reader;
    private final IClassParser parser;
    private final QueryBuilder qb;
    private final ITrestleOntology ontology;
    private final TrestleExecutorService aggregationPool;

    @Inject
    public AggregationEngine(ITrestleObjectReader objectReader,
                             QueryBuilder queryBuilder,
                             ITrestleOntology ontology,
                             TrestleParser trestleParser,
                             TrestleExecutorFactory factory) {
        this.reader = objectReader;
        this.qb = queryBuilder;
        this.ontology = ontology;
        this.parser = trestleParser.classParser;
        this.aggregationPool = factory.create("aggregation-pool");

    }

//    public void aggregateDataset(String datasetClass, String wkt) {
//        final Class<?> registeredClass = this.objectEngineUtils.getRegisteredClass(datasetClass);
//        this.aggregateDataset(registeredClass, wkt);
//    }

    public <T extends @NonNull Object> Optional<List<T>> aggregateDataset(Class<T> clazz, String wkt) {
        final OffsetDateTime atTemporal = OffsetDateTime.now();
        final OffsetDateTime dbTemporal = OffsetDateTime.now();


        final OWLClass objectClass = this.parser.getObjectClass(clazz);
        final String aggregationQuery = this.qb.buildAggregationQuery(objectClass,
                wkt,
                LocalDate.of(1990, 1, 1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime(),
                LocalDate.of(2015, 1, 1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime());

        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        try {
            final TrestleResultSet trestleResultSet = this.ontology.executeSPARQLResults(aggregationQuery);
            final List<CompletableFuture<T>> objectFutures = trestleResultSet
                    .getResults()
                    .stream()
                    .map(result -> result.unwrapIndividual("m"))
                    .map(i -> i.asOWLNamedIndividual().getIRI())
                    .map(individual -> CompletableFuture.supplyAsync(() -> {
                        final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
                        try {
                            return this.reader.readTrestleObject(clazz, individual, false, atTemporal, dbTemporal);
                        } finally {
                            this.ontology.returnAndCommitTransaction(tt);
                        }
                    }))
                    .collect(Collectors.toList());

            final List<T> aggregateObjects = LambdaUtils.sequenceCompletableFutures(objectFutures).get();
            this.ontology.returnAndCommitTransaction(trestleTransaction);
            return Optional.of(aggregateObjects);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            return Optional.empty();
        } catch (ExecutionException e) {
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
