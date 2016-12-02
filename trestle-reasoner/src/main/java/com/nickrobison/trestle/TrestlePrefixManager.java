package com.nickrobison.trestle;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import static com.nickrobison.trestle.common.StaticIRI.GEOSPARQLPREFIX;
import static com.nickrobison.trestle.common.StaticIRI.TRESTLE_PREFIX;

/**
 * Created by nrobison on 12/2/16.
 */
public class TrestlePrefixManager {

    private final DefaultPrefixManager pm;

    /**
     * Create new Trestle prefix manager, without a default prefix
     */
    public TrestlePrefixManager() {
        pm = new DefaultPrefixManager();
        setupDefaultPrefixes();
    }

    /**
     * Create a new Trestle prefix manager, with the specified default prefix
     * @param defaultIRI - String of default iri
     */
    public TrestlePrefixManager(String defaultIRI) {
        pm = new DefaultPrefixManager();
        setupDefaultPrefixes();
        setDefaultPrefix(defaultIRI);
    }

    private void setupDefaultPrefixes() {
        pm.setPrefix("rdf:", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        pm.setPrefix("rdfs:", "http://www.w3.org/2000/01/rdf-schema#");
        pm.setPrefix("owl:", "http://www.w3.org/2002/07/owl#");
//        Jena doesn't use the normal geosparql prefix, so we need to define a separate spatial class
        pm.setPrefix("spatial:", "http://www.jena.apache.org/spatial#");
        pm.setPrefix("geosparql:", GEOSPARQLPREFIX);
        pm.setPrefix("trestle:", TRESTLE_PREFIX);
//        Need the ogc and ogcf prefixes for the oracle spatial
        pm.setPrefix("ogcf:", "http://www.opengis.net/def/function/geosparql/");
        pm.setPrefix("ogc:", "http://www.opengis.net/ont/geosparql#");
        pm.setPrefix("orageo:", "http://xmlns.oracle.com/rdf/geo/");
    }

    /**
     * Set the default prefix of the prefix manager
     * @param defaultIRI - String of default IRI
     */
    public void setDefaultPrefix(String defaultIRI) {
        pm.setDefaultPrefix(defaultIRI);
    }

    /**
     * Add a prefix to the manager
     * @param prefix - String of prefix
     * @param iri - String of iri to expand from prefix
     */
    public void addPrefix(String prefix, String iri) {
        pm.setPrefix(prefix, iri);
    }

    /**
     * Add an IRI as a prefix to the prefix manager
     * @param iri - IRI to parse and extract the namespace and remainder as the prefix/iri pair
     */
    public void addPrefix(IRI iri) {
        if (iri.getRemainder().isPresent()) {
            pm.setPrefix(iri.getNamespace(), iri.getRemainder().get());
        }
    }

    /**
     * Return the DefaultPrefixManager
     * @return - DefaultPrefixManager
     */
    public DefaultPrefixManager getDefaultPrefixManager() {
        return pm;
    }
}
