package com.nickrobison.trestle.reasoner.parser.temporal;

import org.semanticweb.owlapi.model.OWLLiteral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Optional;

/**
 * Created by nrobison on 9/13/16.
 */
public class JavaTimeParser {

    private static final Logger logger = LoggerFactory.getLogger(JavaTimeParser.class);

    private JavaTimeParser() {
//        Empty constructor
    }

    /**
     * Parse {@link org.semanticweb.owlapi.vocab.OWL2Datatype#XSD_DATE_TIME} to Java Temporal
     * @param destinationTypeName - Java type to parse to
     * @param literal - {@link OWLLiteral} of type {@link org.semanticweb.owlapi.vocab.OWL2Datatype#XSD_DATE_TIME}
     * @param zoneId - Time zone to adjust {@link java.time.LocalDate} and {@link java.time.LocalDateTime} to
     * @return - Optional Java Temporal
     */
    public static Optional<Temporal> parseDateTimeToJavaTemporal(String destinationTypeName, OWLLiteral literal, ZoneId zoneId) {
        switch (destinationTypeName) {
            case "java.time.LocalDateTime": {
                return Optional.of(OffsetDateTime.parse(literal.getLiteral(), DateTimeFormatter.ISO_OFFSET_DATE_TIME).atZoneSameInstant(zoneId).toLocalDateTime());
            }
            case "java.time.LocalDate": {
                return Optional.of(OffsetDateTime.parse(literal.getLiteral(), DateTimeFormatter.ISO_OFFSET_DATE_TIME).atZoneSameInstant(zoneId).toLocalDateTime().toLocalDate());
            }
            case "java.time.OffsetDateTime": {
                return Optional.of(OffsetDateTime.parse(literal.getLiteral(), DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            }
            default: {
                logger.error("Unsupported parsing of temporal {} to {}", literal.getDatatype(), destinationTypeName);
                return Optional.empty();
            }
        }
    }
}
