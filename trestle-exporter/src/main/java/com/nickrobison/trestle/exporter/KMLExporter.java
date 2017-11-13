package com.nickrobison.trestle.exporter;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nickrobison.trestle.exporter.kml.*;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class KMLExporter implements ITrestleExporter {
    private static final Logger logger = LoggerFactory.getLogger(KMLExporter.class);

    private final XmlMapper mapper;
    private final String prefix;
    private final XMLInputFactory xmlInputFactory;

    public KMLExporter() {
        this.prefix = "Trestle";
        xmlInputFactory = XMLInputFactory.newFactory();
        this.mapper = new XmlMapper(xmlInputFactory);
    }

    @Override
    public DataType exporterType() {
        return DataType.KML;
    }

    @Override
    public File writePropertiesToByteBuffer(List<TSIndividual> individuals, String fileName) throws IOException {
        final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        final WKTReader wktReader = new WKTReader(geometryFactory);
        final KMLWriter kmlWriter = new KMLWriter();
        final String exportName;
        if (fileName != null) {
            exportName = String.format("%s_%s", this.prefix, fileName);
        } else {
            exportName = String.format("%s_Export_%s", this.prefix, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        final File file = new File(exportName);
        final FileOutputStream fos = new FileOutputStream(file);
        final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
//        Try to write the values
        try {
            final XMLStreamWriter sw = xmlOutputFactory.createXMLStreamWriter(fos);
            sw.writeStartDocument();
            sw.writeStartElement("kml");
            sw.writeNamespace("xmlns", "http://www.opengis.net/kml/2.2");

//            Write each of the Individuals as placemarks
            for (final TSIndividual individual : individuals) {
                final Geometry geometry = wktReader.read(individual.getGeom());
                final String kmlGeometry = kmlWriter.write(geometry);
                final KMLGeometry geom = this.parseKMLGeom(kmlGeometry, geometry);
//                Now, write the individual
                sw.writeStartElement("Placemark");
                mapper.writeValue(sw, geom);
                sw.writeEndElement();
            }

//            Be done
            sw.writeEndElement();
            sw.writeEndDocument();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            fos.close();
        }
        return file;
    }


    private KMLGeometry parseKMLGeom(String kmlValue, Geometry geom) throws IOException {
        if (geom instanceof MultiPolygon) {
            return this.mapper.readValue(kmlValue, KMLMultiGeometry.class);
        } else if (geom instanceof Polygon) {
            return this.mapper.readValue(kmlValue, KMLPolygon.class);
        } else if (geom instanceof LinearRing) {
            return this.mapper.readValue(kmlValue, KMLLinearRing.class);
        } else if (geom instanceof Point) {
            return this.mapper.readValue(kmlValue, KMLPoint.class);
        } else {
            throw new IllegalStateException("Nope");
        }
    }
}
