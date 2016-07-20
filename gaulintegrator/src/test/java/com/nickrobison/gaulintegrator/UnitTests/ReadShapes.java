package com.nickrobison.gaulintegrator.UnitTests;

import com.esri.shp.ShpHeader;
import com.esri.shp.ShpReader;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by nrobison on 4/29/16.
 */
public class ReadShapes {
    private static final Logger logger = Logger.getLogger(ReadShapes.class);
    private final InputStream is = this.getClass().getResourceAsStream("/shapefiles/combined.shp");
    ;

    @Before
    public void setup() {
        assertNotNull(is);
    }

    @Test
    public void readShapefile() throws IOException {
        final ShpReader shpReader = new ShpReader(new DataInputStream(is));
        final ShpHeader shpHeader = shpReader.getHeader();
        assertTrue(shpReader.hasMore());
        int records = 0;
        while (shpReader.hasMore()) {
            shpReader.readPolygon();
            records++;
        }
        assertEquals(3000, records);
    }

    @After
    public void tearDown() throws IOException {
        is.close();
    }
}
