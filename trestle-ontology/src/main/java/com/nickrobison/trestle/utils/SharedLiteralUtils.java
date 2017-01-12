package com.nickrobison.trestle.utils;

import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by nrobison on 1/11/17.
 */
public class SharedLiteralUtils {

    private static final Logger logger = LoggerFactory.getLogger(SharedLiteralUtils.class);

    /**
     * Parse a Literal string, representing a numeric value, to determine its correct datatype
     * This works around some 'optimizations' in various triple-stores that wil change datatypes to ints or decimals
     *
     * @param numericString - String value of numeric literal
     * @return - OWLDataype deduced from string
     */
    static OWLDatatype parseNumericDatatype(String numericString) {
//            If it has a period in the string, it's a decimal
        if (numericString.contains(".")) {
            owlDatatype = df.getOWLDatatype(OWL2Datatype.XSD_DECIMAL.getIRI());
        } else {
            long l = Long.parseLong(numericString);
            l = l >> (Integer.SIZE);
            if (l == 0 | l == -1) {
                logger.trace("Decimal seems to be an Int");
                owlDatatype = df.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
            } else {
                logger.trace("Decimal seems to be a Long");
                owlDatatype = df.getOWLDatatype(OWL2Datatype.XSD_LONG.getIRI());
            }
        }
    }
}
