package com.nickrobison.trestle.querybuilder;

import org.apache.jena.query.ParameterizedSparqlString;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.nickrobison.trestle.common.StaticIRI.PREFIX;

/**
 * Created by nrobison on 8/11/16.
 */
public class QueryBuilder {
    private final String prefixes;
    private final DefaultPrefixManager pm;
    private final String baseURI;
    private final Map<String, String> trimmedPrefixMap;

    public QueryBuilder(DefaultPrefixManager pm) {
        trimmedPrefixMap = new HashMap<>();
        StringBuilder builder = new StringBuilder();
        final Set<Map.Entry<String, String>> entries = pm.getPrefixName2PrefixMap().entrySet();
//        From the given prefix manager, extract the prefixes and build the prefix String
        for (Map.Entry<String, String> entry : entries) {
            if (entry.getKey().equals(":")) {
//                continue;
                trimmedPrefixMap.put("", entry.getValue());
            }
            trimmedPrefixMap.put(entry.getKey().replace(":", ""), entry.getValue());
            builder.append(String.format("PREFIX %s : <%s>\n", entry.getKey().replace(":", ""), entry.getValue()));
        }
        this.prefixes = builder.toString();
        final String defaultPrefix = pm.getDefaultPrefix();
//        Because the madness of nulls
        if (defaultPrefix != null) {
            final String prefix = pm.getPrefix(defaultPrefix);
            if (prefix != null) {
                baseURI = prefix;
            } else {
                baseURI = PREFIX;
            }
        } else {
            this.baseURI = PREFIX;
        }
        this.pm = pm;
    }

    public String buildRelationQuery(OWLNamedIndividual individual, @Nullable OWLClass datasetClass, double relationshipStrength) {
        final ParameterizedSparqlString ps = new ParameterizedSparqlString();
        ps.setBaseUri(baseURI);
        ps.setNsPrefixes(this.trimmedPrefixMap);
        ps.setCommandText("SELECT ?f" +
                " WHERE" +
                " { ?m rdf:type ?t ." +
                "?m :has_relation ?r ." +
                "?r rdf:type :Concept_Relation ." +
                "?r :Relation_Strength ?s ." +
                "?r :has_relation ?f ." +
                "?f rdf:type ?t " +
                "FILTER(?m = ?i && ?s >= ?st)}");
//        Replace the variables
        ps.setIri("i", getFullIRIString(individual));
        if (datasetClass != null ) {
            ps.setIri("t", getFullIRIString(datasetClass));
        } else {
            ps.setIri("t", getFullIRI(IRI.create("trestle:", ":Dataset")).toString());
        }
        ps.setLiteral("st", relationshipStrength);

//        return ps.toString().replace("<", "").replace(">", "");
        return ps.toString();
    }

    private IRI getFullIRI(IRI iri) {
        //        Check to see if it's already been expanded
        if (pm.getPrefix(iri.getScheme() + ":") == null) {
            return iri;
        } else {
            return pm.getIRI(iri.toString());
        }
    }

    private String getFullIRIString (OWLNamedObject object) {
        return getFullIRI(object.getIRI()).toString();
    }
}
