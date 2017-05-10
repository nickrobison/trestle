package com.nickrobison.trestle.reasoner;

import com.nickrobison.trestle.reasoner.annotations.DatasetClass;
import com.nickrobison.trestle.reasoner.annotations.IndividualIdentifier;
import com.nickrobison.trestle.reasoner.annotations.TrestleCreator;
import com.nickrobison.trestle.reasoner.annotations.temporal.DefaultTemporal;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.reasoner.types.TemporalType;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiLineString;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;

import java.io.File;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nrobison on 9/19/16.
 */
@Disabled
public class RoadLoader {


    private static TrestleReasoner reasoner;
    private static OWLDataFactory df;

    @BeforeAll
    public static void setup() {
        final Config config = ConfigFactory.load(ConfigFactory.parseResources("application.conf"));
        reasoner = new TrestleBuilder()
                .withDBConnection(config.getString("trestle.ontology.connectionString"),
                        config.getString("trestle.ontology.username"),
                        config.getString("trestle.ontology.password"))
                .withName("api_test")
                .withOntology(IRI.create(config.getString("trestle.ontology.location")))
                .withInputClasses(gROADS.class)
                .withoutCaching()
                .initialize()
                .build();

        df = OWLManager.getOWLDataFactory();
    }

    @Test
    public void loadRoads() throws IOException, TrestleClassException, MissingOntologyEntity {
        File shpFile = new File("/Users/nrobison/Movies/groads-v1-africa-shp/gROADS-v1-africa.shp");

        Map<String, Object> map = new HashMap();
        map.put("url", shpFile.toURI().toURL());

        final DataStore dataStore = DataStoreFinder.getDataStore(map);
        final String typeName = dataStore.getTypeNames()[0];

        final SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);

        final SimpleFeatureCollection collection = featureSource.getFeatures();
            final SimpleFeatureIterator features = collection.features();
            while (features.hasNext()) {
                final SimpleFeature next = features.next();
                next.getID();
                next.getDefaultGeometryProperty().getValue();
                final gROADS road = new gROADS(next.getID(),
                        MultiLineString.class.cast(next.getDefaultGeometry()),
                        next.getAttribute("SOURCEID").toString(),
                        Double.parseDouble(next.getAttribute("EXS").toString()),
                        next.getAttribute("NOTES").toString(),
                        next.getAttribute("ROADID").toString(),
                        next.getAttribute("ONME").toString(),
                        Double.parseDouble(next.getAttribute("LENGTH_KM").toString()),
                        0.0,
//                        Double.parseDouble(next.getAttribute("SHAPE_LENGTH").toString()),
                        ZonedDateTime.of(1980, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC));

                reasoner.writeTrestleObject(road);
            }
    }

    @AfterAll
    public static void shutdown() {
        reasoner.shutdown(false);
    }


    @DatasetClass(name = "Africa-Roads")
    public static class gROADS {
        private final String ObjectID;
        private final Geometry geom;
        private final String SourceID;
        private final Double Exs;
        private final String Notes;
        private final String RoadID;
        private final String ONme;
        private final Double Length_Km;
        private final Double Shape_Length;
        private final ZonedDateTime defaultTime;

        @TrestleCreator
        public gROADS(String objectID, Geometry geom, String sourceID, Double exs, String notes, String roadID, String ONme, Double length_Km, Double shape_Length, ZonedDateTime defaultTime) {
            ObjectID = objectID;
            this.geom = geom;
            SourceID = sourceID;
            Exs = exs;
            Notes = notes;
            RoadID = roadID;
            this.ONme = ONme;
            Length_Km = length_Km;
            Shape_Length = shape_Length;
            this.defaultTime = defaultTime;
        }

        @IndividualIdentifier
        public String getObjectID() {
            return ObjectID;
        }

        public Geometry getGeom() {
            return geom;
        }

        public String getSourceID() {
            return SourceID;
        }

        public Double getExs() {
            return Exs;
        }

        public String getNotes() {
            return Notes;
        }

        public String getRoadID() {
            return RoadID;
        }

        public String getONme() {
            return ONme;
        }

        public Double getLength_Km() {
            return Length_Km;
        }

        public Double getShape_Length() {
            return Shape_Length;
        }

        @DefaultTemporal(type = TemporalType.INTERVAL, duration = 15, unit = ChronoUnit.YEARS)
        public ZonedDateTime getDefaultTime() {
            return this.defaultTime;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            gROADS gROADS = (gROADS) o;

            if (!getObjectID().equals(gROADS.getObjectID())) return false;
            if (!getGeom().equals(gROADS.getGeom())) return false;
            if (!getSourceID().equals(gROADS.getSourceID())) return false;
            if (!getExs().equals(gROADS.getExs())) return false;
            if (getNotes() != null ? !getNotes().equals(gROADS.getNotes()) : gROADS.getNotes() != null) return false;
            if (!getRoadID().equals(gROADS.getRoadID())) return false;
            if (getONme() != null ? !getONme().equals(gROADS.getONme()) : gROADS.getONme() != null) return false;
            if (!getLength_Km().equals(gROADS.getLength_Km())) return false;
            return getShape_Length().equals(gROADS.getShape_Length());

        }

        @Override
        public int hashCode() {
            int result = getObjectID().hashCode();
            result = 31 * result + getGeom().hashCode();
            result = 31 * result + getSourceID().hashCode();
            result = 31 * result + getExs().hashCode();
            result = 31 * result + (getNotes() != null ? getNotes().hashCode() : 0);
            result = 31 * result + getRoadID().hashCode();
            result = 31 * result + (getONme() != null ? getONme().hashCode() : 0);
            result = 31 * result + getLength_Km().hashCode();
            result = 31 * result + getShape_Length().hashCode();
            return result;
        }
    }
}
