package com.nickrobison.trestle.tigerintegrator;

import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Created by detwiler on 4/13/17.
 */
@Tag("load")
@Disabled
public class TigerLoaderTests {

    /*@Disabled("This test requires a prebuilt Postgres DB")*/ @Test
    public void TestLoad()  {
        try {
            TigerLoader loader = new TigerLoader();
//            loader.loadObjects();
            loader.computeRelations();
//            if(!loader.verifyObjects())
//                fail("Loaded Trestle objects not equivalent to retrieved objects.");
        } catch (SQLException | TrestleClassException | MissingOntologyEntity e) {
            fail(e);
        }
    }
}
