package com.nickrobison.trestle.ontology.utils;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Created by nrobison on 1/11/17.
 */
public class RDF4JLiteralFactory {

    private static final Logger logger = LoggerFactory.getLogger(RDF4JLiteralFactory.class);
    private static final SimpleValueFactory vf = SimpleValueFactory.getInstance();
    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();


    public static Literal createLiteral(OWLLiteral owlLiteral) {
        if (owlLiteral.hasLang()) {
            logger.trace("Creating typed literal {} with language {}", owlLiteral.getLiteral(), owlLiteral.getLang());
            return vf.createLiteral(owlLiteral.getLiteral(), owlLiteral.getLang());
        } else {
            final IRI dataTypeIRI = vf.createIRI(owlLiteral.getDatatype().toStringID());
            logger.trace("Creating typed literal {} with datatype {}", owlLiteral.getDatatype(), dataTypeIRI);
            return vf.createLiteral(owlLiteral.getLiteral(), dataTypeIRI);
        }
    }

    public static Optional<OWLLiteral> createOWLLiteral(Literal literal) {
        OWLDatatype owlDatatype;
        if (literal.getDatatype() == null) {
            logger.error("Literal has an emptyURI");
            owlDatatype = df.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
        } else if (literal.getDatatype().toString().equals(OWL2Datatype.XSD_DECIMAL.getIRI().toString())) {
            final String numericString = literal.stringValue();
            owlDatatype = SharedLiteralUtils.parseNumericDatatype(numericString);
        } else if (literal.getLanguage().isPresent()) {
            return Optional.of(df.getOWLLiteral(literal.stringValue(), literal.getLanguage().get()));
        } else {
            owlDatatype = df.getOWLDatatype(org.semanticweb.owlapi.model.IRI.create(literal.getDatatype().toString()));
        }

        if (owlDatatype.getIRI().toString().equals("nothing")) {
            logger.error("Datatype {} doesn't exist", literal.getDatatype());
            return Optional.empty();
        }
        return Optional.of(df.getOWLLiteral(literal.stringValue(), owlDatatype));
    }
}
