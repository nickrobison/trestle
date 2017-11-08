package com.nickrobison.gaulintegrator;

import com.nickrobison.trestle.common.exceptions.TrestleMissingIndividualException;
import com.nickrobison.trestle.datasets.GAULObject;
import com.nickrobison.trestle.reasoner.TrestleBuilder;
import com.nickrobison.trestle.reasoner.TrestleReasoner;
import com.nickrobison.trestle.types.TrestleIndividual;
import com.nickrobison.trestle.types.TrestleRelation;
import org.apache.hadoop.conf.Configuration;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.semanticweb.owlapi.model.IRI;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@SuppressWarnings({"squid:S106"}) // We can ignore this, using the system printer is fine.
public class IntegrationEvaluator {

    private final TrestleReasoner reasoner;
    private final String filePath;


    IntegrationEvaluator(String filePath) {
        this.reasoner = setupReasoner();
        this.filePath = filePath;
    }


    public void evaluateAlgorithm() throws IOException {
//        Read in the input file
        final FileInputStream inputStream = new FileInputStream(new File(this.filePath));
        final BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        Queue<AlgorithmResult> results = new ArrayDeque<>();
        try {
            String line;
            while ((line = br.readLine()) != null) {
//                Split the columns (\t)
                final String[] splitLine = line.split("\t");

//                Split the 2nd column into its pieces
                final String[] resultSplit = splitLine[1].split(":");

//                Split the adm2 code/name
                final String[] adm2Split = resultSplit[2].split("-");

//                Create the new record
                try {
                    final AlgorithmResult result = new AlgorithmResult(
                            Integer.parseInt(resultSplit[0]),
                            resultSplit[1],
                            Integer.parseInt(adm2Split[0]),
                            adm2Split[1],
                            LocalDate.parse(resultSplit[3]),
                            LocalDate.parse(resultSplit[4]));
                    results.add(result);
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println(splitLine[1]);
                }

            }
        } finally {
            inputStream.close();
            br.close();
        }

//        Now, process the results

        Set<AlgorithmResult> orphanedResults = new HashSet<>();
        Set<TrestleIndividual> orphanedIndividuals = new HashSet<>();

        for (AlgorithmResult result : results) {
            try {
                final TrestleIndividual trestleIndividual = this.reasoner.getTrestleIndividual(result.getID());
                final Optional<String> anyRelation = trestleIndividual.getRelations()
                        .stream()
                        .map(TrestleRelation::getType)
//                    Split Filter
                        .filter(relation -> (relation.contains("SPLIT") || relation.contains("MERGE")))
                        .findAny();
                if (!anyRelation.isPresent()) {
                    orphanedResults.add(result);
                    orphanedIndividuals.add(trestleIndividual);
                }
            } catch (TrestleMissingIndividualException e) {
//                Just don't fail
            }
        }

        System.out.println(String.format("========== (%s) Orphaned ADM2 Entities=======", orphanedResults.size()));
        orphanedResults
                .stream()
                .sorted(Comparator.comparing(AlgorithmResult::getAdm0Name))
                .forEach(result -> System.out.println(result.getID()));
    }


    private TrestleReasoner setupReasoner(@UnderInitialization IntegrationEvaluator this) {
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

    public void shutdown() {
        this.reasoner.shutdown(false);
    }


    public static void main(String[] args) throws IOException {
        final IntegrationEvaluator evaluator = new IntegrationEvaluator(args[0]);
        try {
            evaluator.evaluateAlgorithm();
        } finally {
            evaluator.shutdown();
        }
    }

    private static class AlgorithmResult {

        private final int adm0Code;
        private final String adm0Name;
        private final int adm2Code;
        private final String adm2Name;
        private final LocalDate start;
        private final LocalDate end;

        public AlgorithmResult(int adm0Code, String adm0Name, int adm2Code, String adm2Name, LocalDate start, LocalDate end) {
            this.adm0Code = adm0Code;
            this.adm0Name = adm0Name;
            this.adm2Code = adm2Code;
            this.adm2Name = adm2Name;
            this.start = start;
            this.end = end;
        }

        public int getAdm0Code() {
            return adm0Code;
        }

        public String getAdm0Name() {
            return adm0Name;
        }

        public int getAdm2Code() {
            return adm2Code;
        }

        public String getAdm2Name() {
            return adm2Name;
        }

        public LocalDate getStart() {
            return start;
        }

        public LocalDate getEnd() {
            return end;
        }

        public String getID() {
            return String.format("%s:%s:%s:%s", getAdm2Code(), getAdm2Name().replace(" ", "-"), getStart().getYear(), getEnd().getYear());
        }
    }
}
