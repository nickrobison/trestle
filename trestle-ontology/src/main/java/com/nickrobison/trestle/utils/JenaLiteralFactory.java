package com.nickrobison.trestle.utils;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Optional;

import static com.nickrobison.trestle.utils.SharedLiteralUtils.parseNumericDatatype;

/**
 * Created by nrobison on 12/4/16.
 */
@NotThreadSafe
public class JenaLiteralFactory {

    private static final Logger logger = LoggerFactory.getLogger(JenaLiteralFactory.class);
    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();
    private final Model model;
    private final TypeMapper typeMapper;

    public JenaLiteralFactory(Model model) {
        this.model = model;
        typeMapper = TypeMapper.getInstance();
    }


    /**
     * Create Jena Literal from OWL Literal
     * If the OWLLiteral is of type RDF:PlainLiteral, it extracts the language string and returns the new literal
     *
     * @param owlLiteral - OWLLiteral to parse
     * @return - Jena RDF Literal
     */
    public Literal createLiteral(OWLLiteral owlLiteral) {
        if (owlLiteral.hasLang()) {
            logger.trace("Creating typed literal {} with language {}", owlLiteral.getLiteral(), owlLiteral.getLang());
            return model.createLiteral(owlLiteral.getLiteral(), owlLiteral.getLang());
        } else {
            final RDFDatatype rdfType = typeMapper.getSafeTypeByName(owlLiteral.getDatatype().toStringID());
            logger.trace("Creating typed literal {} with datatype {}", owlLiteral.getDatatype(), rdfType);
            return model.createTypedLiteral(owlLiteral.getLiteral(), rdfType);
        }
    }

    /**
     * Create OWLLiteral from Jena Literal
     *
     * @param literal - Jena Literal to parse
     * @return - Optional of OWLLiteral
     */
    public Optional<OWLLiteral> createOWLLiteral(Literal literal) {
        OWLDatatype owlDatatype;
        if (literal.getDatatypeURI() == null) {
            logger.error("Literal has an emptyURI");
            owlDatatype = df.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
        } else if (literal.getDatatypeURI().equals(OWL2Datatype.XSD_DECIMAL.getIRI().toString())) {
//                Work around Oracle bug by trying to parse an Int and see if it works
            final String numericString = literal.getLexicalForm();
            owlDatatype = parseNumericDatatype(numericString);

////            If it has a period in the string, it's a decimal
//            if (numericString.contains(".")) {
//                owlDatatype = df.getOWLDatatype(OWL2Datatype.XSD_DECIMAL.getIRI());
//            } else {
//                long l = Long.parseLong(numericString);
//                l = l >> (Integer.SIZE);
//                if (l == 0 | l == -1) {
//                    logger.trace("Decimal seems to be an Int");
//                    owlDatatype = df.getOWLDatatype(OWL2Datatype.XSD_INTEGER.getIRI());
//                } else {
//                    logger.trace("Decimal seems to be a Long");
//                    owlDatatype = df.getOWLDatatype(OWL2Datatype.XSD_LONG.getIRI());
//                }
//            }
        } else if (literal.getDatatype() == RDF.dtLangString) {
            return Optional.of(df.getOWLLiteral(literal.getLexicalForm(), literal.getLanguage()));
        } else {
            owlDatatype = df.getOWLDatatype(IRI.create(literal.getDatatypeURI()));
        }

        if (owlDatatype.getIRI().toString().equals("nothing")) {
            logger.error("Datatype {} doesn't exist", literal.getDatatypeURI());
            return Optional.empty();
        }
        return Optional.of(df.getOWLLiteral(literal.getLexicalForm(), owlDatatype));
    }
}
