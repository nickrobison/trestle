package com.nickrobison.trestle.gaulintegrator;

import com.nickrobison.trestle.datasets.GAULObject;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.TrestleBuilder;
import com.nickrobison.trestle.reasoner.TrestleReasoner;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.types.TrestleObjectHeader;
import me.tongfei.progressbar.ProgressBar;
import org.apache.hadoop.conf.Configuration;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.semanticweb.owlapi.model.IRI;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by nickrobison on 8/1/18.
 */
@SuppressWarnings("Duplicates")
public class GAULAnalyzer {

    private final TrestleReasoner reasoner;
    private final String filePath;

    GAULAnalyzer(String filePath) {
        this.reasoner = setupReasoner();
        this.filePath = filePath;
    }

    private TrestleReasoner setupReasoner(@UnderInitialization GAULAnalyzer this) {
//        Read in the conf
        Configuration conf = new Configuration();

        final Properties userProperties = new Properties();
        final InputStream is = IntegrationRunner.class.getClassLoader().getResourceAsStream("sd.properties");
        try {
            userProperties.load(is);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        for (String name : userProperties.stringPropertyNames()) {
            conf.set(name, userProperties.getProperty(name));
        }


        return new TrestleBuilder()
                .withDBConnection(conf.get("reasoner.db.connection"),
                        conf.get("reasoner.db.username"),
                        conf.get("reasoner.db.password"))
                .withInputClasses(GAULObject.class)
                .withOntology(IRI.create(conf.get("reasoner.ontology.location")))
                .withPrefix(conf.get("reasoner.ontology.prefix"))
                .withName(conf.get("reasoner.ontology.name"))
                .withoutCaching()
                .withoutMetrics()
                .build();
    }

    private void evaluateSize() throws IOException, TrestleClassException, MissingOntologyEntity {
        final List<String> members = this.reasoner.getDatasetMembers(GAULObject.class);

        Map<String, Double> sizeDistribution = new HashMap<>();

        final ProgressBar pb = new ProgressBar("Calculating Size Distribution", members.size());
        pb.start();

        for (final String member : members) {
            final TrestleObjectHeader header = this.reasoner.readObjectHeader(GAULObject.class, member).orElseThrow(() -> new IllegalStateException("Cannot not have object"));
            final GAULObject gaulObject = this.reasoner.readTrestleObject(GAULObject.class, member, header.getExistsFrom(), null);
            final double area = gaulObject.getShapePolygon().calculateArea2D();
            sizeDistribution.put(member, area);
            pb.step();
        }
        pb.stop();
        System.out.println("Done with size calc");

        try (final FileWriter fileWriter = new FileWriter(this.filePath)) {
            for (Map.Entry<String, Double> entry : sizeDistribution.entrySet()) {
                fileWriter.write(String.format("%s,%f\n", entry.getKey(), entry.getValue()));
            }
        }
    }

    public void shutdown() {
        this.reasoner.shutdown(false);
    }

    public static void main(String[] args) throws IOException, TrestleClassException, MissingOntologyEntity {
        final String method = args[0];
        final GAULAnalyzer analyzer = new GAULAnalyzer(args[1]);
        switch (method) {
            case "size":
                analyzer.evaluateSize();
                break;
        }
    }
}
