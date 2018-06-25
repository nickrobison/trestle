package com.nickrobison.trestle.tigerintegrator;

import com.google.common.collect.ImmutableMap;
import com.nickrobison.trestle.datasets.TigerCountyObject;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.TrestleBuilder;
import com.nickrobison.trestle.reasoner.TrestleReasoner;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.semanticweb.owlapi.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by detwiler on 2/16/17.
 */
public class TigerLoader {
    private static final Logger logger = LoggerFactory.getLogger(TigerLoader.class);
    private static int firstYear = 2011;
    private static int lastYear = 2015;
    // supporting data structures
    /*
        Regions:
        1 = Northeast
        2 = Midwest
        3 = South
        4 = West
     */
    public static final Map<Integer,String> regionMap = ImmutableMap.of(1, "Northeast", 2, "Midwest",
            3, "South", 4, "West");

    /*
        Divisions:
        1 = New England
        2 = Middle Atlantic
        3 = East North Central
        4 = West North Central
        5 = South Atlantic
        6 = East South Central
        7 = West South Central
        8 = Mountain
        9 = Pacific
     */
    public static final Map<Integer,String> divisionMap = ImmutableMap.<Integer, String>builder()
            .put(1, "New England")
            .put(2, "Middle Atlantic")
            .put(3, "East North Central")
            .put(4, "West North Central")
            .put(5, "South Atlantic")
            .put(6, "East South Central")
            .put(7, "West South Central")
            .put(8, "Mountain")
            .put(9, "Pacific")
            .build();

    private Config config;
    private String connectStr;
    private String username;
    private String password;
    private String reponame;
    private String ontLocation;
    private String ontPrefix;
    private List<TigerCountyObject> tigerObjs;

    TigerLoader() throws SQLException
    {

//        config = ConfigFactory.load();
        config = ConfigFactory.parseResources("tiger-loader.conf");
        connectStr = config.getString("trestle.graphdb.connection_string");
        username = config.getString("trestle.graphdb.username");
        password = config.getString("trestle.graphdb.password");
        reponame = config.getString("trestle.graphdb.repo_name");
        ontLocation = config.getString("trestle.ontology.location");
        ontPrefix = config.getString("trestle.ontology.prefix");
        tigerObjs = buildObjects();
    }

    public void loadObjects()
    {
        TrestleReasoner reasoner = new TrestleBuilder()
                .withDBConnection(connectStr, username, password)
                .withName(reponame)
                .withOntology(IRI.create(ontLocation))
                .withPrefix(ontPrefix)
                .withInputClasses(TigerCountyObject.class)
//                .withoutCaching()
//                .initialize()
                .build();

        for(int count=0; count<tigerObjs.size(); count++)
        {
            if(count%1000==0)
                logger.info("Writing trestle object {}", +count);

            TigerCountyObject tigerObj = tigerObjs.get(count);
            try {
                final Instant start = Instant.now();
                reasoner.writeTrestleObject(tigerObj);
//                reasoner.writeTrestleObject(tigerObj,startTemporal,null);
                final Instant end = Instant.now();
                logger.info("Writing object {} took {} ms", count, Duration.between(start, end).toMillis());
            } catch (TrestleClassException e) {
                e.printStackTrace();
                System.exit(-1);
            } catch (MissingOntologyEntity e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }

        reasoner.getMetricsEngine().exportData(new File("./tiger.csv"));


        reasoner.shutdown();
    }

    private List<TigerCountyObject> buildObjects() throws SQLException
    {
        String connectStr = config.getString("data_db.connection_string");
        String queryStr = config.getString("data_db.query");
        if(connectStr==null||queryStr==null)
            return null; // should probably throw an exception here

        List<TigerCountyObject> objects = new ArrayList<>();

        Statement stmt = null;
        try {
            Connection conn = DriverManager.getConnection(connectStr);
            stmt = conn.createStatement();
            for(int year=firstYear; year<=lastYear; year++)
            {
                String shapetable = "shp"+year;
                queryStr = queryStr.replaceAll("<shapetable>",shapetable);

                ResultSet rs = stmt.executeQuery(queryStr);
                while(rs.next()) {
                    /*
                        // object attributes
                        private final String geom;
                        private final int geoid;
                        private final String region;
                        private final String division;
                        private final String state;
                        private final String county;
                        private final int pop_estimate;
                        private final int births;
                        private final int deaths;
                        private final int natural_increase;
                        private final int international_migration;
                        private final int domestic_migration;
                        private final float rate_birth;
                        private final float rate_death;
                        private final float rate_natural_increase;
                        private final LocalDate record_start_date;
                     */

                    String geom = rs.getString("geotext");
                    if(geom==null)
                        continue; // all entries must have spatial data

                    String geoid = rs.getString("geoid");

                    // convert region code to region name
                    int regionCode = rs.getInt("REGION");
                    String region = regionMap.get(regionCode);

                    // convert division code to division name
                    int divisionCode = rs.getInt("DIVISION");
                    String division = divisionMap.get(divisionCode);

                    String state = rs.getString("STNAME");

                    String county = rs.getString("CTYName");

                    // get population data
                    int pop_estimate = rs.getInt("POPESTIMATE"+year);
                    int births = rs.getInt("BIRTHS"+year);
                    int deaths = rs.getInt("DEATHS"+year);
                    int natural_increase = rs.getInt("NATURALINC"+year);
                    int international_migration = rs.getInt("INTERNATIONALMIG"+year);
                    int domestic_migration = rs.getInt("DOMESTICMIG"+year);
                    float rate_birth = rs.getFloat("RBIRTH"+year);
                    float rate_death = rs.getFloat("RDEATH"+year);
                    float rate_natural_increase = rs.getFloat("RNATURALINC"+year);
                    LocalDate record_start_date = LocalDate.of(year,7,1);
                    //LocalDate record_end_date = LocalDate.of(year+1,7,1);

                    // construct Trestle object
                    TigerCountyObject tcObj = new TigerCountyObject(geoid,geom,region,division,state,county,
                            pop_estimate,births,deaths,natural_increase,international_migration,domestic_migration,
                            rate_birth,rate_death,rate_natural_increase,record_start_date);
                    objects.add(tcObj);

                }
                rs.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if(stmt!=null)
                stmt.close();
        }

        return objects;
    }

    public boolean verifyObjects() throws TrestleClassException, MissingOntologyEntity {
        TrestleReasoner reasoner = new TrestleBuilder()
                .withDBConnection(connectStr, username, password)
                .withName(reponame)
                .withOntology(IRI.create(ontLocation))
                .withPrefix(ontPrefix)
                .withInputClasses(TigerCountyObject.class)
                .withoutCaching()
                .withoutMetrics()
                .build();

        boolean allEquivalent = true;
        for(int count=0; count<tigerObjs.size(); count++)
        {
            TigerCountyObject tigerObj = tigerObjs.get(count);
            String id = tigerObj.getGeoid();
            LocalDate startDate = tigerObj.getRecord_start_date().plusMonths(1);
            TigerCountyObject outObj = reasoner.readTrestleObject(TigerCountyObject.class, id, startDate, null);

            if(!tigerObj.equals(outObj))
            {
                logger.error("Error, Trestle input object and output object not equivalent; in:"+tigerObj.getGeoid()+", out:"+outObj.getGeoid());
                allEquivalent = false;
            }
        }

        reasoner.shutdown();

        return allEquivalent;
    }

    public static void main(String[] args)
    {
        System.out.println("start time: "+Instant.now());
        try {
            TigerLoader loader = new TigerLoader();
            loader.loadObjects();
            loader.verifyObjects();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (MissingOntologyEntity e) {
            e.printStackTrace();
        } catch (TrestleClassException e) {
            e.printStackTrace();
        }
        System.out.println("end time: "+Instant.now());
    }
}
