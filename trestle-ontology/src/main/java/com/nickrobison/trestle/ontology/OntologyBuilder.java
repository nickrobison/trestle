package com.nickrobison.trestle.ontology;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import org.apache.commons.io.FileUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.nickrobison.trestle.common.StaticIRI.GEOSPARQLPREFIX;
import static com.nickrobison.trestle.common.StaticIRI.TRESTLE_PREFIX;

/**
 * Created by nrobison on 6/21/16.
 */
// FIXME(nrobison): Work to remove this, I feel like my optionals should fix the nullness, right?
@SuppressWarnings({"nullness", "OptionalUsedAsFieldOrParameterType"})
public class OntologyBuilder {
    private static final Logger logger = LoggerFactory.getLogger(OntologyBuilder.class);
    public static final String LOCAL_ONTOLOGY = "local_ontology";
    //private static Config config = ConfigFactory.load(ConfigFactory.parseResources("test.configuration.conf"));
    private static Config config = ConfigFactory.load(ConfigFactory.parseResources("reference.conf"));

    private Optional<IRI> iri = Optional.empty();
    private Optional<InputStream> is = Optional.empty();
    private Optional<String> connectionString = Optional.empty();
    private Optional<String> username = Optional.empty();
    private Optional<String> password = Optional.empty();
    private Optional<String> ontologyName = Optional.empty();
    private Optional<DefaultPrefixManager> pm = Optional.empty();

    public OntologyBuilder() {
//        Not needed
    }

    /**
     * Loads an initial base ontology from the given IRI
     * @param iri - IRI of the ontology to load
     * @return OntologyBuilder
     */
    public OntologyBuilder fromIRI(IRI iri) {
        this.iri = Optional.of(iri);
        return this;
    }

    /**
     * Loads an initial base ontology from a given InputStream
     * @param is - InputStream of the ontology to load
     * @return OntologyBuilder
     */
    public OntologyBuilder fromInputStream(InputStream is) {
        this.is = Optional.of(is);
        return this;
    }

    /**
     * Connects to ontology database, if this isn't set, the Builder returns a LocalOntology, otherwise it returns the correct database ontology
     * @param connectionString - Connection string of database to load
     * @param username - Username to connect with
     * @param password - User password
     * @return - OntologyBuilder
     */
    public OntologyBuilder withDBConnection(String connectionString, String username, String password) {
        this.connectionString = Optional.of(connectionString);
        this.username = Optional.of(username);
        this.password = Optional.of(password);
        return this;
    }

    /**
     * Sets the name of the ontology model, if null, parses the IRI to get the base name
     * @param ontologyName - Name of the model
     * @return - OntologyBuilder
     */
    public OntologyBuilder name(String ontologyName) {
        this.ontologyName = Optional.of(ontologyName);
        return this;
    }

    /**
     * Sets a custom prefix manager, otherwise a default one is generated
     * @param pm - DefaultPrefixManger, custom prefix manager
     * @return - OntologyBuilder
     */
    public OntologyBuilder withPrefixManager(DefaultPrefixManager pm) {
        this.pm = Optional.of(pm);
        return this;
    }

    /**
     * Builds and returns the correct ontology (either local or database backed)
     * @return - ITrestleOntology for the correct underlying ontology configuration
     * @throws OWLOntologyCreationException - Throws if it can't create the ontology
     */
    public ITrestleOntology build() throws OWLOntologyCreationException {
        final OWLOntologyManager owlOntologyManager = OWLManager.createOWLOntologyManager();
        OWLOntology owlOntology = null;

        // load ontology mapper (favors local files if present)
        Set<OWLOntologyIRIMapper> mappers = new HashSet<>();
        OWLOntologyIRIMapper mapper = getImportsMapper();
        mappers.add(mapper);
        owlOntologyManager.setIRIMappers(mappers);

        OWLOntologyLoaderConfiguration loaderConfig = new OWLOntologyLoaderConfiguration();
        loaderConfig.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.THROW_EXCEPTION);

        try {
            if (this.iri.isPresent()) {
                logger.debug("Loading ontology from: {}", this.iri.get());
                owlOntology = owlOntologyManager.loadOntologyFromOntologyDocument((new IRIDocumentSource(this.iri.get())),loaderConfig);
                logger.info("Loaded version {} of ontology {}",
                        owlOntology.getOntologyID().getVersionIRI().orElse(IRI.create("0.0")).getShortForm(),
                        owlOntology.getOntologyID().getOntologyIRI().orElse(IRI.create("trestle")).getShortForm().replace(".owl", ""));
            } else if (this.is.isPresent()){
                logger.debug("Loading ontology from input stream");
                owlOntology = owlOntologyManager.loadOntologyFromOntologyDocument((new StreamDocumentSource(this.is.get())),loaderConfig);
                logger.info("Loaded version {} of ontology {}",
                        owlOntology.getOntologyID().getVersionIRI().orElse(IRI.create("0.0")).getShortForm(),
                        owlOntology.getOntologyID().getOntologyIRI().orElse(IRI.create("trestle")).getShortForm().replace(".owl", ""));
            }
            else {
                owlOntology = owlOntologyManager.createOntology();
            }
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }

//            If there's a connection string, then we need to return a database Ontology
        if (connectionString.isPresent() && connectionString.get().contains("oracle")&&owlOntology!=null) {
            logger.info("Connecting to Oracle database {} at: {}", this.ontologyName.orElse(""), connectionString.get());
            return new OracleOntology(
                    this.ontologyName.orElse(extractNamefromIRI(this.iri.orElse(IRI.create(LOCAL_ONTOLOGY)))),
                    owlOntology,
                    pm.orElse(createDefaultPrefixManager()),
//                    classify(owlOntology, new ConsoleProgressMonitor()),
                    connectionString.get(),
                    username.orElse(""),
                    password.orElse("")
            );
        } else if (connectionString.isPresent() && connectionString.get().contains("http")) {
            logger.info("Connecting to remote GraphDB instance {} at: {}", this.ontologyName.orElse(""), this.connectionString.get());
            return new GraphDBOntology(
                    this.ontologyName.orElse(extractNamefromIRI(this.iri.orElse(IRI.create(LOCAL_ONTOLOGY)))),
                    connectionString.get(),username.orElse(""), password.orElse(""), owlOntology,
                    pm.orElse(createDefaultPrefixManager())
            );
        } else {
            logger.info("Connect to embedded GraphDB instance {}", this.ontologyName.orElse(""));
            return new GraphDBOntology(
                    this.ontologyName.orElse(extractNamefromIRI(this.iri.orElse(IRI.create(LOCAL_ONTOLOGY)))),
                    null, "", "", owlOntology,
                    pm.orElse(createDefaultPrefixManager())
            );
        }
    }

    private OWLOntologyIRIMapper getImportsMapper() {
        OWLOntologyIRIMapper iriMapper = new OWLOntologyIRIMapper() {
            private Config importsConfig = config.getConfig("trestle.ontology.imports");
            private String importsDirPath = importsConfig.getString("importsDirectory");
            private Map<IRI, String> fileMap;
            {

                fileMap = new HashMap<IRI, String>();
                for (ConfigObject mappingObj : importsConfig.getObjectList("importsIRIMappings")) {
                    Config mappingConfig = mappingObj.toConfig();
                    if (mappingObj.containsKey("iri") && mappingObj.containsKey("file")) {
                        String iriString = mappingConfig.getString("iri");
                        IRI iri = IRI.create(iriString);
                        String fileString = mappingConfig.getString("file");
                        fileMap.put(iri, fileString);
                    }
                }
                logger.debug("mapping = {}", fileMap);
            }

            @Override
            public IRI getDocumentIRI(IRI iri) {

                // construct IRI if in mapper
                IRI documentIRI = null;
                String fileName = fileMap.get(iri);

                URL fileURL = this.getClass().getClassLoader().getResource(importsDirPath + fileName);
                if (fileURL != null) {
                    if (fileURL.getProtocol().equals("jar")) {
                        // externalize file from jar for owlapi
                        try {
                            final JarURLConnection connection =
                                    (JarURLConnection) fileURL.openConnection();
                            final URL url = connection.getJarFileURL();
                            Path p = Paths.get(url.toURI());
                            Path folder = p.getParent();
                            String externalFileName = folder + "/ontology/imports/" + fileName;
                            File externalFile = new File(externalFileName);

                            FileUtils.copyURLToFile(fileURL, externalFile);
                            documentIRI = IRI.create(externalFile);
                            logger.info("creating external ontology file: {}", documentIRI);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }
                    } else {
                        File importOntFile = null;
                        try {
                            importOntFile = new File(fileURL.toURI());
                            if (importOntFile.exists() && importOntFile.isFile() && importOntFile.canRead()) {
                                documentIRI = IRI.create(importOntFile);
                            }
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                }

                return documentIRI;
            }
        };

        return iriMapper;
    }

    private static String extractNamefromIRI(IRI iri) {
        return iri.getShortForm();
    }

    private DefaultPrefixManager createDefaultPrefixManager() {
        DefaultPrefixManager pm = new DefaultPrefixManager();
        pm.setDefaultPrefix(TRESTLE_PREFIX);
        pm.setPrefix("rdf:", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        pm.setPrefix("rdfs:", "http://www.w3.org/2000/01/rdf-schema#");
        pm.setPrefix("owl:", "http://www.w3.org/2002/07/owl#");
//        Jena doesn't use the normal geosparql prefix, so we need to define a separate spatial class
        pm.setPrefix("spatial:", "http://www.jena.apache.org/spatial#");
        pm.setPrefix("geosparql:", GEOSPARQLPREFIX);
        pm.setPrefix("trestle:", TRESTLE_PREFIX);
//        Need the ogc and ogcf prefixes for the oracle spatial
        pm.setPrefix("ogcf:", "http://www.opengis.net/def/function/geosparql/");
        pm.setPrefix("ogc:", "http://www.opengis.net/ont/geosparql#");
        pm.setPrefix("orageo:", "http://xmlns.oracle.com/rdf/geo/");
        return pm;
    }
}
