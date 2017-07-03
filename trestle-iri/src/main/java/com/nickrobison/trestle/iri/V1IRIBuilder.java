package com.nickrobison.trestle.iri;

import com.nickrobison.trestle.common.IRIUtils;
import com.nickrobison.trestle.iri.exceptions.IRIParseException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.IRI;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.nickrobison.trestle.common.IRIUtils.extractTrestleIndividualName;

/**
 * Created by nrobison on 1/23/17.
 */
class V1IRIBuilder {

    private static final Pattern objectIDPattern = Pattern.compile("(?<=:).*?(?=:)");

    /**
     * Encode IRI using {@link IRIVersion#V1} specification
     *
     * @param prefix           - IRI Prefix
     * @param objectID         - ObjectID
     * @param objectFact       - Optional Fact to identify
     * @param objectTemporal   - Optional temporal to identify object state
     * @param databaseTemporal - Optional temporal to identify object database state
     * @return - {@link TrestleIRIV1}
     */
    static TrestleIRIV1 encodeIRI(String prefix, String objectID, @Nullable String objectFact, @Nullable OffsetDateTime objectTemporal, @Nullable OffsetDateTime databaseTemporal) {
        return new TrestleIRIV1(IRIVersion.V1, prefix, objectID, objectFact, objectTemporal, databaseTemporal);
    }

    /**
     * Parse a given {@link IRI} and return the {@link IRIVersion#V1} - {@link TrestleIRI}
     * @param encodedIRI - {@link IRI} to parse
     * @return - {@link TrestleIRIV1}
     */
    static TrestleIRIV1 extractFromIRI(IRI encodedIRI) {
        final String iriString = extractTrestleIndividualName(encodedIRI);
        final String prefix = IRIUtils.extractPrefix(encodedIRI);
        final String objectID = getObjectID(iriString);
        final Optional<String> objectFact = getObjectFact(iriString);
        final Optional<OffsetDateTime> objectTemporal = getObjectTemporal(iriString);
        final Optional<OffsetDateTime> databaseTemporal = getDatabaseTemporal(iriString);
        return new TrestleIRIV1(IRIVersion.V1, prefix, objectID, objectFact.orElse(""), objectTemporal.orElse(null), databaseTemporal.orElse(null));
    }

    static String getObjectID(String iriString) {
        final Matcher matcher = objectIDPattern.matcher(iriString);
        if (matcher.find()) {
//            This will also match on the object fact, so we need to parse that out
            final String IDFactGroup = matcher.group();
            final String id = IDFactGroup.split("@")[0];
            if (id.equals("")) {
                throw new IRIParseException(IDFactGroup);
            }
            return id;
        }
        throw new IRIParseException(iriString);
    }

    static Optional<String> getObjectFact(String iriString) {
        final Matcher matcher = objectIDPattern.matcher(iriString);
        if (matcher.find()) {
//            This will also match on the objectID, so we need to parse that out
            final String IDFactGroup = matcher.group();
            final String[] fact = IDFactGroup.split("@");
            if (fact.length > 1) {
                return Optional.of(fact[1]);
            }
            return Optional.empty();
        }
        throw new IRIParseException(iriString);
    }

    static Optional<OffsetDateTime> getObjectTemporal(String iriString) {

        final String[] splitGroups = iriString.split(":");
        if (splitGroups.length >= 3) {
            final long temporalLong = Long.parseLong(splitGroups[2]);
            return Optional.of(OffsetDateTime.ofInstant(Instant.ofEpochMilli(temporalLong), ZoneOffset.UTC));
        } else {
            return Optional.empty();
        }
    }

    static Optional<OffsetDateTime> getDatabaseTemporal(String iriString) {
        final Optional<OffsetDateTime> objectTemporal = getObjectTemporal(iriString);
        if (objectTemporal.isPresent()) {
            final int i = iriString.lastIndexOf(":");
            final long temporalLong = Long.parseLong(iriString.substring(i + 1, iriString.length()));
            return Optional.of(OffsetDateTime.ofInstant(Instant.ofEpochMilli(temporalLong), ZoneOffset.UTC));
        }
        return Optional.empty();
    }
}
