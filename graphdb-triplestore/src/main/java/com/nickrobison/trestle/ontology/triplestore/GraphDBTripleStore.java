package com.nickrobison.trestle.ontology.triplestore;

import com.ontotext.jena.SesameDataset;
import com.ontotext.trree.OwlimSchemaRepository;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nrobison on 1/10/17.
 */
public class GraphDBTripleStore {
    private static SailRepositoryConnection connection;
    private static SesameDataset dataset;

    public static Model initializeGraphDBModel() {
        OwlimSchemaRepository schema = new OwlimSchemaRepository();
        schema.setDataDir(new File("./target/data"));

        Map<String, String> modelParameters = new HashMap<>();
        modelParameters.put("storage-folder", "./");
        modelParameters.put("repository-type", "file-repository");
        modelParameters.put("ruleset", "owl2-rl");

        schema.setParameters(modelParameters);

        final SailRepository repository = new SailRepository(schema);
        repository.initialize();
        connection = repository.getConnection();
        dataset = new SesameDataset(connection);
        return ModelFactory.createModelForGraph(dataset.getDefaultGraph());
    }

    public static SailRepositoryConnection getConnection() {
        return connection;
    }

    public static SesameDataset getDataset() {
        return dataset;
    }
}
