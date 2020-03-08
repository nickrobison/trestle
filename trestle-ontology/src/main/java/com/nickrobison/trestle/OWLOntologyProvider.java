package com.nickrobison.trestle;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import org.apache.commons.io.FileUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class OWLOntologyProvider implements Provider<OWLOntology> {

    private static final Logger logger = LoggerFactory.getLogger(OWLOntologyProvider.class);
    public static final String ONTOLOGY_RESOURCE_NAME = "ontology/trestle.owl";
    public static final String DEFAULT_NAME = "trestle";
    private static final String IMPORT_PREFIX = "ontology.imports";

    private final Config config;

    @Inject
    OWLOntologyProvider(Config config) {
        this.config = config;
    }

    @Override
    public OWLOntology get() {
        final OWLOntologyManager owlOntologyManager = OWLManager.createOWLOntologyManager();
        OWLOntology owlOntology = null;

        // load ontology mapper (favors local files if present)
        Set<OWLOntologyIRIMapper> mappers = new HashSet<>();
        OWLOntologyIRIMapper mapper = getImportsMapper();
        mappers.add(mapper);
        owlOntologyManager.setIRIMappers(mappers);

        OWLOntologyLoaderConfiguration loaderConfig = new OWLOntologyLoaderConfiguration();
        loaderConfig.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.THROW_EXCEPTION);
        final InputStream is = getIRI(Optional.empty());

        try {
//            if (iri.isPresent()) {
//            logger.debug("Loading ontology from: {}", iri);
//            owlOntology = owlOntologyManager.loadOntologyFromOntologyDocument((new IRIDocumentSource(iri)), loaderConfig);
//            logger.info("Loaded version {} of ontology {}",
//                    owlOntology.getOntologyID().getVersionIRI().orElse(IRI.create("0.0")).getShortForm(),
//                    owlOntology.getOntologyID().getOntologyIRI().orElse(IRI.create("trestle")).getShortForm().replace(".owl", ""));
//            } else if (is.isPresent()){
                logger.debug("Loading ontology from input stream");
                owlOntology = owlOntologyManager.loadOntologyFromOntologyDocument((new StreamDocumentSource(is)),loaderConfig);
                logger.info("Loaded version {} of ontology {}",
                        owlOntology.getOntologyID().getVersionIRI().orElse(IRI.create("0.0")).getShortForm(),
                        owlOntology.getOntologyID().getOntologyIRI().orElse(IRI.create("trestle")).getShortForm().replace(".owl", ""));
//            }
//            else {
//                owlOntology = owlOntologyManager.createOntology();
//            }
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }

        return owlOntology;
    }

    private OWLOntologyIRIMapper getImportsMapper() {
        OWLOntologyIRIMapper iriMapper = new OWLOntologyIRIMapper() {
            private Config importsConfig = config.getConfig(IMPORT_PREFIX);
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
                            String externalFileName = folder + "/imports/" + fileName;
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

    private InputStream getIRI(Optional<IRI> oIRI) {
//                If we have a manually specified ontology, use that.
        final URL ontologyResource;
        final InputStream ontologyIS;
        if (oIRI.isPresent()) {
            final IRI ontologyIRI = oIRI.get();
            try {
                ontologyResource = ontologyIRI.toURI().toURL();
                ontologyIS = Files.newInputStream(new File(ontologyIRI.toURI()).toPath());
            } catch (MalformedURLException e) {
                logger.error("Unable to parse IRI: {} to URI", ontologyIRI, e);
                throw new IllegalArgumentException(String.format("Unable to parse IRI %s to URI", ontologyIRI), e);
            } catch (IOException e) {
                logger.error("Cannot find ontology file {}", ontologyIRI, e);
                throw new MissingResourceException("File not found", this.getClass().getName(), ontologyIRI.getIRIString());
            }
        } else {
//            Load with the class loader
            ontologyResource = OWLOntologyProvider.class.getClassLoader().getResource(ONTOLOGY_RESOURCE_NAME);
            ontologyIS = OWLOntologyProvider.class.getClassLoader().getResourceAsStream(ONTOLOGY_RESOURCE_NAME);
        }

        if (ontologyIS == null) {
            logger.error("Cannot load trestle ontology from resources");
            throw new MissingResourceException("Cannot load ontology file", this.getClass().getName(), ONTOLOGY_RESOURCE_NAME);
        }
        try {
            final int available = ontologyIS.available();
            if (available == 0) {
                throw new MissingResourceException("Ontology InputStream does not seem to be available", this.getClass().getName(), ontologyIS.toString());
            }
        } catch (IOException e) {
            throw new MissingResourceException("Ontology InputStream does not seem to be available", this.getClass().getName(), ontologyIS.toString());
        }
        logger.info("Loading ontology from {}", ontologyResource == null ? "Null resource" : ontologyResource);

        //        Setup the ontology builder
//        logger.info("Connecting to ontology {} at {}", builder.ontologyName.orElse(DEFAULT_NAME), builder.connectionString.orElse("localhost"));
        logger.debug("IS: {}", ontologyIS);
        logger.debug("Resource: {}", ontologyResource == null ? "Null resource" : ontologyResource);

        return ontologyIS;
    }
}
