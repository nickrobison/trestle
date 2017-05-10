package com.nickrobison.tigerintegrator;

import com.nickrobison.trestle.reasoner.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static com.ibm.icu.impl.Assert.fail;

/**
 * Created by detwiler on 4/13/17.
 */
public class TigerLoaderTests {

    /*@Disabled("This test requires a prebuilt Postgres DB")*/ @Test
    public void TestLoad()  {
        try {
            TigerLoader loader = new TigerLoader();
            loader.loadObjects();
            if(!loader.verifyObjects())
                fail("Loaded Trestle objects not equivalent to retrieved objects.");
        } catch (SQLException e) {
            fail(e);
        } catch (TrestleClassException e) {
            fail(e);
        } catch (MissingOntologyEntity e) {
            fail(e);
        }
    }
}
