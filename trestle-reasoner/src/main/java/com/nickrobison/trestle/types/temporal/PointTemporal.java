package com.nickrobison.trestle.types.temporal;

import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;
import java.util.*;

/**
 * Created by nrobison on 6/30/16.
 */
// I can suppress both of these warnings because I know for sure they are correct
@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unchecked", "return.type.incompatible", "Duplicates"})
public class PointTemporal<T extends Temporal> extends TemporalObject {

    private static final TemporalType TYPE = TemporalType.POINT;
    private final TemporalScope scope;
    private final T atTime;
    private final Optional<String> parameterName;
    private final Class<T> temporalType;
    private ZoneId timeZone;

    private PointTemporal(Builder<T> builder) {
        super(UUID.randomUUID().toString(), builder.relations);
        this.scope = builder.scope;
        this.atTime = builder.atTime;
        this.parameterName = builder.parameterName;
        this.temporalType = (Class<T>) builder.atTime.getClass();
        this.timeZone = builder.explicitTimeZone.orElse(ZoneOffset.UTC);
    }
    @Override
    public TemporalType getType() {
        return TYPE;
    }

    @Override
    public TemporalScope getScope() {
        return this.scope;
    }

    @Override
    public boolean isValid() {
        return scope == TemporalScope.VALID;
    }

    @Override
    public boolean isExists() {
        return scope == TemporalScope.EXISTS;
    }

    @Override
    public Class<? extends Temporal> getBaseTemporalType() {
        return this.temporalType;
    }

    @Override
    public boolean isPoint() {
        return true;
    }

    @Override
    public boolean isInterval() {
        return false;
    }

    public T getPointTime() {
        return this.atTime;
    }

    public String getParameterName() {
        return this.parameterName.orElse("pointTime");
    }

    /**
     * Returns the explicit timezone of the temporal object
     * @return - ZoneID
     */
    public ZoneId getTimeZone() {
        return this.timeZone;
    }

    public static class Builder<T extends Temporal> {

        private TemporalScope scope;
        private T atTime;
        private Optional<Set<OWLNamedIndividual>> relations = Optional.empty();
        private Optional<String> parameterName = Optional.empty();
        private Optional<ZoneId> explicitTimeZone = Optional.empty();

        Builder(TemporalScope scope, T at) {
            this.scope = scope;
            this.atTime = at;
        }

        /**
         * Manually set point parameter name
         * @param name - String to use for parameter name
         * @return - Builder
         */
        public Builder withParameterName(String name) {
            this.parameterName = Optional.of(name);
            return this;
        }

        /**
         * Set the point time zone
         * @param zoneID - String to parse into timezone
         * @return - Builder
         */
        public Builder withTimeZone(String zoneID) {
            if (!zoneID.equals("")) {
                this.explicitTimeZone = Optional.of(ZoneId.of(zoneID));
            }
            return this;
        }

        /**
         * Set the point time zone
         * @param zoneId - ZoneID to use
         * @return - Builder
         */
        public Builder withTimeZone(ZoneId zoneId) {
            this.explicitTimeZone = Optional.of(zoneId);
            return this;
        }

        /**
         * Set the Individuals this temporal relates to
         * @param relations - OWLNamedIndividuals associated with this temporal
         * @return - Builder
         */
        public PointTemporal withRelations(OWLNamedIndividual... relations) {
            this.relations = Optional.of(new HashSet<>(Arrays.asList(relations)));
            return new PointTemporal<>(this);
        }

    }
}
