package com.nickrobison.trestle.reasoner.engines.exporter;

import com.nickrobison.trestle.common.IRIUtils;
import com.nickrobison.trestle.exporter.*;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.ReasonerPrefix;
import com.nickrobison.trestle.reasoner.engines.object.ITrestleObjectReader;
import com.nickrobison.trestle.reasoner.exceptions.NoValidStateException;
import com.nickrobison.trestle.reasoner.parser.*;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorFactory;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorService;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.nickrobison.trestle.types.temporal.IntervalTemporal;
import com.nickrobison.trestle.types.temporal.PointTemporal;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import com.vividsolutions.jts.geom.MultiPolygon;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.common.LambdaUtils.sequenceCompletableFutures;

/**
 * Created by nickrobison on 2/19/18.
 */
public class DataExportEngine implements ITrestleDataExporter {

    private final static Logger logger = LoggerFactory.getLogger(DataExportEngine.class);

    private final String reasonerPrefix;
    private final ITrestleOntology ontology;
    private final ITrestleObjectReader objectReader;
    private final IClassParser classParser;
    private final IClassBuilder classBuilder;
    private final ITypeConverter typeConverter;
    private final TemporalParser temporalParser;
    private final TrestleExecutorService dataExporterPool;

    @Inject
    public DataExportEngine(@ReasonerPrefix String reasonerPrefix,
                            ITrestleOntology ontology,
                            ITrestleObjectReader objectReader,
                            TrestleParser trestleParser,
                            TrestleExecutorFactory factory) {
        this.reasonerPrefix = reasonerPrefix;
        this.ontology = ontology;
        this.objectReader = objectReader;
        this.classParser = trestleParser.classParser;
        this.classBuilder = trestleParser.classBuilder;
        this.temporalParser = trestleParser.temporalParser;
        this.typeConverter = trestleParser.typeConverter;

        this.dataExporterPool = factory.create("data-exporter-pool");
    }

    @Override
    public <T> File exportDataSetObjects(Class<T> inputClass, List<String> objectID, ITrestleExporter.DataType exportType) throws IOException {
        return exportDataSetObjects(inputClass, objectID, null, null, exportType);
    }

    @Override
    public <T> File exportDataSetObjects(Class<T> inputClass, List<String> objectID, @Nullable Temporal validAt, @Nullable Temporal databaseAt, ITrestleExporter.DataType exportType) throws IOException {

        final Integer classProjection = this.classParser.getClassProjection(inputClass);

//        Build shapefile schema
//        TODO(nrobison): Extract type from wkt
//        FIXME(nrobison): Shapefile schema doesn't support multiple languages. Need to figure out how to flatten
        final ShapefileSchema shapefileSchema = new ShapefileSchema(MultiPolygon.class);
        final Optional<List<OWLDataProperty>> propertyMembers = this.classBuilder.getPropertyMembers(inputClass, true);
        propertyMembers.ifPresent(owlDataProperties -> owlDataProperties.forEach(property -> shapefileSchema.addProperty(this.classParser.matchWithClassMember(inputClass, property.asOWLDataProperty().getIRI().getShortForm()), this.typeConverter.lookupJavaClassFromOWLDataProperty(inputClass, property))));

//        Now the temporals
        final Optional<List<OWLDataProperty>> temporalProperties = this.temporalParser.getTemporalsAsDataProperties(inputClass);
        temporalProperties.ifPresent(owlDataProperties -> owlDataProperties.forEach(temporal -> shapefileSchema.addProperty(this.classParser.matchWithClassMember(inputClass, temporal.asOWLDataProperty().getIRI().getShortForm()), this.typeConverter.lookupJavaClassFromOWLDataProperty(inputClass, temporal))));


        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        final List<CompletableFuture<Optional<TSIndividual>>> completableFutures = objectID
                .stream()
                .map(id -> IRIUtils.parseStringToIRI(this.reasonerPrefix, id))
                .map(id -> CompletableFuture.supplyAsync(() -> {
                    final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
                    try {
                        final T object = this.objectReader.readTrestleObject(inputClass, id, false, validAt, databaseAt);
                        return Optional.of(object);
                    } catch (NoValidStateException e) {
                        this.ontology.returnAndAbortTransaction(tt);
                        return Optional.empty();
                    } finally {
                        this.ontology.returnAndCommitTransaction(tt);
                    }
                }, this.dataExporterPool))
                .map(objectFuture -> objectFuture.thenApply(object -> parseIndividualToShapefile(object, shapefileSchema)))
                .collect(Collectors.toList());

        final CompletableFuture<List<Optional<TSIndividual>>> sequencedFutures = sequenceCompletableFutures(completableFutures);

        try {
            final List<TSIndividual> individuals = sequencedFutures
                    .get()
                    .stream()
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            switch (exportType) {
                case SHAPEFILE: {
                    final ShapefileExporter shapeFileExporter = new ShapefileExporter.ShapefileExporterBuilder(shapefileSchema.getGeomName(), shapefileSchema.getGeomType(), shapefileSchema).setSRID(classProjection).build();
                    return shapeFileExporter.writePropertiesToByteBuffer(individuals, null);
                }
                case GEOJSON: {
                    return new GeoJsonExporter(classProjection).writePropertiesToByteBuffer(individuals, null);
                }
                case KML: {
                    return new KMLExporter(false).writePropertiesToByteBuffer(individuals, null);
                }
                case KMZ: {
                    return new KMLExporter(true).writePropertiesToByteBuffer(individuals, null);
                }
                default: {
                    throw new IllegalArgumentException(String.format("Cannot export to %s format", exportType.toString()));
                }
            }


        } catch (ExecutionException e) {
            logger.error("Error constructing object", e.getCause());
            return ExceptionUtils.rethrow(e.getCause());
        } catch (InterruptedException e) {
            logger.error("Object construction excepted", e.getCause());
            Thread.currentThread().interrupt();
            return ExceptionUtils.rethrow(e.getCause());
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private <T extends @NonNull Object> Optional<TSIndividual> parseIndividualToShapefile(Optional<T> objectOptional, ShapefileSchema shapefileSchema) {
        if (!objectOptional.isPresent()) {
            return Optional.empty();
        }
        final T object = objectOptional.get();
//        if (objectOptional.isPresent()) {
//            final T object = objectOptional.get();
        final Class<?> inputClass = object.getClass();
        final Optional<OWLDataPropertyAssertionAxiom> spatialProperty = this.classParser.getSpatialFact(object);
        if (!spatialProperty.isPresent()) {
            logger.error("Individual is not a spatial object");
            return Optional.empty();
        }
        final TSIndividual individual = new TSIndividual(spatialProperty.get().getObject().getLiteral(), shapefileSchema);
//                    Data properties, filtering out the spatial members
        final Optional<List<OWLDataPropertyAssertionAxiom>> owlDataPropertyAssertionAxioms = this.classParser.getFacts(object, true);
        owlDataPropertyAssertionAxioms.ifPresent(owlDataPropertyAssertionAxioms1 -> owlDataPropertyAssertionAxioms1.forEach(property -> {
            final Class<@NonNull ?> javaClass = this.typeConverter.lookupJavaClassFromOWLDatatype(property, object.getClass());
            final Object literal = this.typeConverter.reprojectSpatial(this.typeConverter.extractOWLLiteral(javaClass, property.getObject()), this.classParser.getClassProjection(inputClass));
            individual.addProperty(this.classParser.matchWithClassMember(inputClass, property.getProperty().asOWLDataProperty().getIRI().getShortForm()),
                    literal);
        }));
//                    Temporals
        final Optional<List<TemporalObject>> temporalObjects = this.temporalParser.getTemporalObjects(object);
        if (temporalObjects.isPresent()) {
            final TemporalObject temporalObject = temporalObjects.get().get(0);
            if (temporalObject.isInterval()) {
                final IntervalTemporal intervalTemporal = temporalObject.asInterval();
                individual.addProperty(this.classParser.matchWithClassMember(inputClass, intervalTemporal.getStartName()), intervalTemporal.getFromTime().toString());
                final Optional toTime = intervalTemporal.getToTime();
                if (toTime.isPresent()) {
                    final Temporal to = (Temporal) toTime.get();
                    individual.addProperty(this.classParser.matchWithClassMember(inputClass, intervalTemporal.getEndName()), to.toString());
                }
            } else {
                final PointTemporal pointTemporal = temporalObject.asPoint();
                individual.addProperty(pointTemporal.getParameterName(), pointTemporal.getPointTime().toString());
            }
        }
        return Optional.of(individual);
    }


}
