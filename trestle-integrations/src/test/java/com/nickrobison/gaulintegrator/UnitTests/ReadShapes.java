package com.nickrobison.gaulintegrator.UnitTests;

import com.esri.shp.ShpHeader;
import com.esri.shp.ShpReader;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by nrobison on 4/29/16.
 */
public class ReadShapes {
    private static final Logger logger = Logger.getLogger(ReadShapes.class);
    private final InputStream is = ReadShapes.class.getClassLoader().getResourceAsStream("shapefiles/combined_2000.shp");

    @BeforeEach
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
        assertEquals(1000, records);
    }

    @AfterEach
    public void tearDown() throws IOException {
        is.close();
    }
}
