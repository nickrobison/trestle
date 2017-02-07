package com.nickrobison.trestle.iri;

import com.nickrobison.trestle.common.IRIUtils;
import com.nickrobison.trestle.iri.exceptions.IRIParseException;
import com.nickrobison.trestle.iri.exceptions.IRIVersionException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.IRI;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by nrobison on 1/23/17.
 */
public class IRIBuilder {

    private static final Pattern versionPattern = Pattern.compile("^[^:]*");

    public static TrestleIRI encodeIRI(IRIVersion version, String prefix, String objectID, @Nullable String objectFact, @Nullable OffsetDateTime objectTemporal, @Nullable OffsetDateTime databaseTemporal) {
        switch (version) {
            case V1:
                return V1IRIBuilder.encodeIRI(prefix, objectID, objectFact, objectTemporal, databaseTemporal);
            default:
                throw new IRIVersionException(version);
        }
    }

    /**
     * Extract ObjectID from IRI
     * @param encodedIRI - IRI to decode
     * @return - String of ObjectID
     */
    public static String getObjectID(IRI encodedIRI) {
        final String individualString = IRIUtils.extractTrestleIndividualName(encodedIRI);
        if (individualString.equals("")) {
            throw new IRIParseException(encodedIRI);
        }
        final IRIVersion version = getIRIVersion(individualString);
        switch (version) {
            case V1:
                return V1IRIBuilder.getObjectID(individualString);
            default:
                throw new IRIVersionException(version);
        }
    }

    /**
     * Extract Fact name from IRI
     * If not fact is specified, returns an empty Optional
     * @param encodedIRI - IRI to decode
     * @return - Optional String of Fact name
     */
    public static Optional<String> getObjectFact(IRI encodedIRI) {
        final String individualString = IRIUtils.extractTrestleIndividualName(encodedIRI);
        if (individualString.equals("")) {
            throw new IRIParseException(encodedIRI);
        }
        final IRIVersion version = getIRIVersion(individualString);
        switch (version) {
            case V1:
                return V1IRIBuilder.getObjectFact(individualString);
            default:
                throw new IRIVersionException(version);
        }
    }

    /**
     * Extract object temporal from IRI
     * If an object fact is specified, the temporal refers to the valid point of that temporal
     * Returned temporal is specified at UTC
     * @param encodedIRI - IRI to decode
     * @return - Optional OffsetDateTime of object/fact temporal
     */
    public static Optional<OffsetDateTime> getObjectTemporal(IRI encodedIRI) {
        final String individualString = IRIUtils.extractTrestleIndividualName(encodedIRI);
        if (individualString.equals("")) {
            throw new IRIParseException(encodedIRI);
        }
        final IRIVersion version = getIRIVersion(individualString);
        switch (version) {
            case V1:
                return V1IRIBuilder.getObjectTemporal(individualString);
            default:
                throw new IRIVersionException(version);
        }
    }

    /**
     * Extract database temporal from IRI
     * If an object fact is specified, the temporal refers to the database point of that temporal
     * Returned temporal is specified at UTC
     * @param encodedIRI - IRI to decode
     * @return - Optional OffsetDateTime of object/fact database temporal
     */
    public static Optional<OffsetDateTime> getDatabaseTemporal(IRI encodedIRI) {
        final String individualString = IRIUtils.extractTrestleIndividualName(encodedIRI);
        if (individualString.equals("")) {
            throw new IRIParseException(encodedIRI);
        }
        final IRIVersion version = getIRIVersion(individualString);
        switch (version) {
            case V1:
                return V1IRIBuilder.getDatabaseTemporal(individualString);
            default:
                throw new IRIVersionException(version);
        }
    }

    private static IRIVersion getIRIVersion(String iriString) {
        final Matcher matcher = versionPattern.matcher(iriString);
        if (matcher.find()) {
            return IRIVersion.matchVersion(matcher.group());
        } else {
            throw new IRIParseException(iriString);
        }
    }


}
