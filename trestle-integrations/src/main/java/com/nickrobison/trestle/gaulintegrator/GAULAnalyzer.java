package com.nickrobison.trestle.gaulintegrator;

import com.nickrobison.trestle.common.exceptions.TrestleMissingIndividualException;
import com.nickrobison.trestle.datasets.GAULObject;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.TrestleBuilder;
import com.nickrobison.trestle.reasoner.TrestleReasoner;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.types.TrestleIndividual;
import com.nickrobison.trestle.types.TrestleObjectHeader;
import com.nickrobison.trestle.types.TrestleRelation;
import me.tongfei.progressbar.ProgressBar;
import org.apache.hadoop.conf.Configuration;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.semanticweb.owlapi.model.IRI;
import si.uom.SI;
import tech.units.indriya.quantity.Quantities;

import javax.measure.MetricPrefix;
import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Area;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javax.measure.MetricPrefix.KILO;

/**
 * Created by nickrobison on 8/1/18.
 */
@SuppressWarnings("Duplicates")
public class GAULAnalyzer {

    private static final Pattern codeNameRegex = Pattern.compile("([0-9]+)-(.*)");

    private final TrestleReasoner reasoner;
    private final String filePath;

    private GAULAnalyzer(String filePath) {
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

    public void shutdown() {
        this.reasoner.shutdown(false);
    }

    private void evaluateSize() throws IOException, TrestleClassException, MissingOntologyEntity, ParseException {
        final List<String> members = this.reasoner.getDatasetMembers(GAULObject.class);

        Map<String, Double> sizeDistribution = new HashMap<>();

        try (ProgressBar pb = new ProgressBar("Calculating Size Distribution", members.size())) {
            for (final String member : members) {
                final TrestleObjectHeader header = this.reasoner.readObjectHeader(GAULObject.class, member).orElseThrow(() -> new IllegalStateException("Cannot not have object"));
                final GAULObject gaulObject = this.reasoner.readTrestleObject(GAULObject.class, member, header.getExistsFrom(), null);
    //            final double area = gaulObject.getShapePolygon().calculateArea2D();
                final String wktValue = gaulObject.getPolygonAsWKT();
                final Geometry read = new WKTReader().read(wktValue);
                final Quantity<Area> area = GAULAnalyzer.calculatePolygonArea(read);
                @SuppressWarnings("unchecked") final Unit<Area> sq_km = (Unit<Area>) KILO(SI.METRE).multiply(KILO(SI.METRE));
                sizeDistribution.put(member, area.to(sq_km).getValue().doubleValue());
                pb.step();
            }
        }
        System.out.println("Done with size calc");

        GAULAnalyzer.writeMapToFile(sizeDistribution, "%s,%f\n", this.filePath);
    }

    private void objectLifetimes() throws IOException {
        final List<String> members = this.reasoner.getDatasetMembers(GAULObject.class);

        Map<String, Integer> lifetimes = new HashMap<>();

        final ProgressBar pb = new ProgressBar("Calculating object lifetime length", members.size());
        pb.start();

        for (String member : members) {
            final TrestleObjectHeader header = this.reasoner.readObjectHeader(GAULObject.class, member).orElseThrow(() -> new IllegalStateException("Should have member header"));
            if (!header.continuing()) {
                final Integer yearsBetween = GAULAnalyzer.adjustedYearsBetween(header.getExistsFrom(), header.getExistsTo());
                lifetimes.put(member, yearsBetween);
                pb.step();
            }
        }
        pb.stop();
        GAULAnalyzer.writeMapToFile(lifetimes, "%s,%d\n", this.filePath);
    }

    private void evaluateAlgorithm() throws IOException {
        //        Read in the input file
        final FileInputStream inputStream = new FileInputStream(new File(this.filePath));
        final BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        Queue<GAULAnalyzer.AlgorithmResult> results = new ArrayDeque<>();
        try {
            String line;
            while ((line = br.readLine()) != null) {
//                Split the columns (\t)
                final String[] splitLine = line.split("\t");

//                Split the 2nd column into its pieces
                final String[] resultSplit = splitLine[1].split(":");

//                Split the adm2 code/name
                final Matcher matchedCodeName = codeNameRegex.matcher(resultSplit[2]);
                if (!matchedCodeName.matches()) {
                    throw new IllegalStateException("Cannot match with Regex!");
                }

//                Create the new record
                try {
                    final GAULAnalyzer.AlgorithmResult result = new GAULAnalyzer.AlgorithmResult(
                            Integer.parseInt(resultSplit[0]),
                            resultSplit[1],
                            Integer.parseInt(matchedCodeName.group(1)),
                            matchedCodeName.group(2),
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

//        Create a progress base
        final ProgressBar pb = new ProgressBar("Analyzing Individuals:", results.size());
        pb.start();

//        Now, process the results

        Set<GAULAnalyzer.AlgorithmResult> orphanedResults = new HashSet<>();
        Set<TrestleIndividual> orphanedIndividuals = new HashSet<>();

        for (GAULAnalyzer.AlgorithmResult result : results) {
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
                System.err.println(String.format("Could not find %s", e.getIndividual()));
//                Just don't fail
            }
            pb.step();
        }
        pb.stop();

        System.out.println(String.format("========== (%s) Orphaned ADM2 Entities=======", orphanedResults.size()));
        orphanedResults
                .stream()
                .sorted(Comparator.comparing(GAULAnalyzer.AlgorithmResult::getAdm0Code)
                        .thenComparing(GAULAnalyzer.AlgorithmResult::getAdm2Name)
                        .thenComparing(GAULAnalyzer.AlgorithmResult::getStart))
                .forEach(result -> System.out.println(result.getID()));
    }

    public static void main(String[] args) throws IOException, TrestleClassException, MissingOntologyEntity, ParseException {
        final String method = args[0];
        final GAULAnalyzer analyzer = new GAULAnalyzer(args[1]);
        try {
            switch (method) {
                case "size":
                    analyzer.evaluateSize();
                    break;
                case "evaluate":
                    analyzer.evaluateAlgorithm();
                    break;
                case "lifetimes":
                    analyzer.objectLifetimes();
                    break;
                default:
                    throw new IllegalStateException(String.format("Unsupported operation: %s", method));
            }
        } finally {
            analyzer.shutdown();
        }
        System.exit(0);
    }

    private static Quantity<Area> calculatePolygonArea(Geometry jtsGeom) {
        final Point centroid = jtsGeom.getCentroid();
        try {
            final String code = "AUTO:42001," + centroid.getX() + "," + centroid.getY();
            final CoordinateReferenceSystem crs = CRS.decode(code);

            final MathTransform transform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, crs);
            final Geometry transformed = JTS.transform(jtsGeom, transform);
//            return Measure.valueOf(transformed.getArea(), SI.SQUARE_METRE);
            return Quantities.getQuantity(transformed.getArea(), SI.SQUARE_METRE);
        } catch (FactoryException | TransformException e) {
            e.printStackTrace();
        }

        return Quantities.getQuantity(0.0, SI.SQUARE_METRE);
    }

    /**
     * Computes the years between the two {@link Temporal} values.
     * Normalizes the values to be within the range of [1990, 2014]
     *
     * @param start - start {@link Temporal}
     * @param end   - end {@link Temporal}
     * @return - {@link Integer}
     */
    private static Integer adjustedYearsBetween(Temporal start, Temporal end) {
        final int startYear = start.get(ChronoField.YEAR);
        final int endYear = end.get(ChronoField.YEAR);

        return Math.min(endYear, 2014) - Math.max(startYear, 1990);
    }

    /**
     * Write the {@link Map#entrySet()} to a file.
     * Format string should include newline character.
     *
     * @param map          - {@link Map} of values to write
     * @param formatString - {@link String} format string to use for building lines.
     * @param filePath     - {@link String} filepath to write to
     * @throws IOException
     */
    private static void writeMapToFile(Map<String, ?> map, String formatString, String filePath) throws IOException {
        try (final FileWriter fileWriter = new FileWriter(filePath)) {
            for (Map.Entry<String, ?> entry : map.entrySet()) {
                fileWriter.write(String.format(formatString, entry.getKey(), entry.getValue()));
            }
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
            return String.format("%s-%s-%s-%s", getAdm2Code(), getAdm2Name(), getStart().getYear(), getEnd().getYear());
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AlgorithmResult that = (AlgorithmResult) o;
            return adm0Code == that.adm0Code &&
                    adm2Code == that.adm2Code &&
                    Objects.equals(adm0Name, that.adm0Name) &&
                    Objects.equals(adm2Name, that.adm2Name) &&
                    Objects.equals(start, that.start) &&
                    Objects.equals(end, that.end);
        }

        @Override
        public int hashCode() {

            return Objects.hash(adm0Code, adm0Name, adm2Code, adm2Name, start, end);
        }
    }
}
