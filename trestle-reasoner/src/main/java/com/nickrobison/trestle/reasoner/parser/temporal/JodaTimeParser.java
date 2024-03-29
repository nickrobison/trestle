package com.nickrobison.trestle.reasoner.parser.temporal;

import org.joda.time.LocalDateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.temporal.Temporal;
import java.util.Optional;

/**
 * Created by nrobison on 9/13/16.
 */
public class JodaTimeParser {

    private static final Logger logger = LoggerFactory.getLogger(JodaTimeParser.class);

    private JodaTimeParser() {
//        Empty constructory
    }

    /**
     * Parse {@link org.semanticweb.owlapi.vocab.OWL2Datatype#XSD_DATE_TIME} to Joda Temporal
     * @param destinationTypeName - Java type to parse to
     * @param literal - {@link OWLLiteral} of type {@link org.semanticweb.owlapi.vocab.OWL2Datatype#XSD_DATE_TIME}
     * @return - {@link Optional} Joda Time Temporal
     */
//    TODO(nrobison): Should support time zones
    public static Optional<Temporal> parseDateTimeToJodaTemporal(String destinationTypeName, OWLLiteral literal) {
        switch (destinationTypeName) {
            case "org.joda.time.LocalDateTime": {
                return Optional.of(java.time.LocalDateTime.parse(LocalDateTime.parse(literal.getLiteral(), ISODateTimeFormat.basicOrdinalDateTime()).toString()));
            }
            default: {
                logger.error("Unsupported parsing of temporal {} to {}", literal.getDatatype(), destinationTypeName);
                return Optional.empty();
            }
        }
    }
}
