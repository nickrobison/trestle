package com.nickrobison.trestle.common;

import org.apache.jena.rdf.model.Literal;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Created by nrobison on 10/23/16.
 */
public class JenaUtils {

    private static final Logger logger = LoggerFactory.getLogger(JenaUtils.class);

    public static Optional<OWLLiteral> parseLiteral(OWLDataFactory df, Literal literal) {
        OWLDatatype owlDatatype;
        if (literal.getDatatypeURI() == null) {
            logger.error("Literal has an emptyURI");
            owlDatatype = df.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
        } else if (literal.getDatatypeURI().equals(OWL2Datatype.XSD_DECIMAL.getIRI().toString())) {
//                Work around Oracle bug by trying to parse an Int and see if it works

            final String numericString = literal.getLexicalForm();
//            If it has a period in the string, it's a decimal
            if (numericString.contains(".")) {
                owlDatatype = df.getOWLDatatype(OWL2Datatype.XSD_DECIMAL.getIRI());
            } else {
                long l = Long.parseLong(numericString);
                l = l >> (Integer.SIZE);
                if (l == 0 | l == -1) {
                    logger.debug("Decimal seems to be an Int");
                    owlDatatype = df.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
                } else {
                    logger.debug("Decimal seems to be a Long");
                    owlDatatype = df.getOWLDatatype(OWL2Datatype.XSD_LONG.getIRI());
                }
            }
        } else {
            owlDatatype = df.getOWLDatatype(IRI.create(literal.getDatatypeURI()));
        }

        if (owlDatatype.getIRI().toString().equals("nothing")) {
            logger.error("Datatype {} doesn't exist", literal.getDatatypeURI());
//                return Optional.empty();
            return Optional.empty();
        }
        return Optional.of(df.getOWLLiteral(literal.getLexicalForm(), owlDatatype));
    }
}
