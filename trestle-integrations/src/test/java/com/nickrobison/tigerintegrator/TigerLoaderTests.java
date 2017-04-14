package com.nickrobison.tigerintegrator;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Created by detwiler on 4/13/17.
 */
public class TigerLoaderTests {

    @Disabled("This test requires a prebuilt Postgres DB") @Test
    public void TestLoad()
    {
        TigerLoader loader = new TigerLoader();
        loader.run();
    }
}
