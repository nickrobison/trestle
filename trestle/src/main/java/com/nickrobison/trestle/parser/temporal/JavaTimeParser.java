package com.nickrobison.trestle.parser.temporal;

import org.semanticweb.owlapi.model.OWLLiteral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Optional;

/**
 * Created by nrobison on 9/13/16.
 */
public class JavaTimeParser {

    private static final Logger logger = LoggerFactory.getLogger(JavaTimeParser.class);

    /**
     * Parse OWL xsd:dateTime to Java Temporal
     * @param destinationTypeName - Java type to parse to
     * @param literal - OWLLiteral of type xsd:dateTime
     * @return - Optional Java Temporal
     */
    public static Optional<Temporal> parseDateTimeToJavaTemporal(String destinationTypeName, OWLLiteral literal) {
        switch (destinationTypeName) {
            case "java.time.LocalDateTime": {
                return Optional.of(LocalDateTime.parse(literal.getLiteral(), DateTimeFormatter.ISO_DATE_TIME));
            }
            case "java.time.LocalDate": {
                return Optional.of(LocalDateTime.parse(literal.getLiteral(), DateTimeFormatter.ISO_DATE_TIME).toLocalDate());
            }
            case "java.time.OffsetDateTime": {
                return Optional.of(OffsetDateTime.parse(literal.getLiteral(), DateTimeFormatter.ISO_DATE_TIME));
            }
            default: {
                logger.error("Unsupported parsing of temporal {} to {}", literal.getDatatype(), destinationTypeName);
                return Optional.empty();
            }
        }
    }

    /**
     * Parse OWL xsd:Date to Java Temporal
     * SHOULD BE DEPRECATED
     * @param destinationTypeName - Java Type to parse to
     * @param literal - OWLLiteral of type xsd:Date
     * @return - Optional Java Temporal
     */
    public static Optional<Temporal> parseDateToJavaTemporal(String destinationTypeName, OWLLiteral literal) {
        switch (destinationTypeName) {
            case "java.time.LocalDateTime": {
                return Optional.of(LocalDateTime.parse(literal.getLiteral(), DateTimeFormatter.ISO_DATE));
            }
            case "java.time.LocalDate": {
                return Optional.of(LocalDate.parse(literal.getLiteral(), DateTimeFormatter.ISO_DATE));
            }
            case "java.time.OffsetDateTime": {
                return Optional.of(OffsetDateTime.parse(literal.getLiteral(), DateTimeFormatter.ISO_DATE));
            }
            default: {
                logger.error("Unsupported parsing of temporal {} to {}", literal.getDatatype(), destinationTypeName);
                return Optional.empty();
            }
        }
    }

}
