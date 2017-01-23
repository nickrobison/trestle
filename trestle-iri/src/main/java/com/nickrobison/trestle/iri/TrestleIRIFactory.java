package com.nickrobison.trestle.iri;

/**
 * Created by nrobison on 1/23/17.
 */
public class TrestleIRIFactory {

    public static ITrestleIRIBuilder getIRIBuilder(IRIVersion version) {
        switch (version) {
            case V1: {
                return new V1IRIBuilder();
            }
            default:
                throw new RuntimeException("Only V1 IRIs implemented");
        }

    }
}
