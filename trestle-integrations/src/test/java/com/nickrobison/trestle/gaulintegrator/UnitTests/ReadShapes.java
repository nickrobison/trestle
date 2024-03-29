package com.nickrobison.trestle.gaulintegrator.UnitTests;

import com.esri.shp.ShpHeader;
import com.esri.shp.ShpReader;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by nrobison on 4/29/16.
 */
@Tag("tag")
@Disabled
public class ReadShapes {
    private static final Logger logger = Logger.getLogger(ReadShapes.class);
    private final InputStream is = ReadShapes.class.getClassLoader().getResourceAsStream("shapefiles/combined_2000.shp");

    @Test
    public void readShapefile() throws IOException {
        assertNotNull(is);
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
        assertNotNull(is);
        is.close();
    }
}
