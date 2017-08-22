package com.nickrobison.trestle.types.temporal;

import com.nickrobison.trestle.common.TemporalUtils;
import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;
import java.util.*;

/**
 * Created by nrobison on 6/30/16.
 */
// I can suppress both of these warnings because I know for sure they are correct
@SuppressWarnings({"unchecked", "return.type.incompatible", "Duplicates"})
public class PointTemporal<T extends Temporal> extends TemporalObject {

    private static final TemporalType TYPE = TemporalType.POINT;
    private final TemporalScope scope;
    private final T atTime;
    private final @Nullable String parameterName;
    private final Class<T> temporalType;
    private ZoneId timeZone;

    private PointTemporal(Builder<T> builder) {
        super(builder.temporalID.orElse(UUID.randomUUID().toString()), builder.relations);
        this.scope = builder.scope;
        this.atTime = builder.atTime;
        this.parameterName = builder.parameterName.orElse(null);
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
    public boolean isDatabase() {
        return scope == TemporalScope.DATABASE;
    }

    @Override
    public boolean isContinuing() {
        return false;
    }

    @Override
    public Temporal getIdTemporal() {
        return this.atTime;
    }

    @Override
    public Class<? extends Temporal> getBaseTemporalType() {
        return this.temporalType;
    }

    @Override
    public TemporalObject castTo(TemporalScope castScope) {
        if (castScope == TemporalScope.VALID) {
            return TemporalObjectBuilder.valid().at(this.atTime).build(); //.withRelations(this.getTemporalRelations().toArray(new OWLNamedIndividual[this.getTemporalRelations().size()]));
        }
        return TemporalObjectBuilder.exists().at(this.atTime).build(); //.withRelations(this.getTemporalRelations().toArray(new OWLNamedIndividual[this.getTemporalRelations().size()]));
    }

    @Override
    public int compareTo(Temporal comparingTemporal) {
        return TemporalUtils.compareTemporals(this.atTime, comparingTemporal);
    }

    @Override
    public boolean during(TemporalObject comparingObject) {
        if (comparingObject.isPoint()) {
            final int pointCompare = TemporalUtils.compareTemporals(this.atTime, comparingObject.asPoint().atTime);
            return pointCompare == 0;
        }
//        If we're comparing against an interval
        final int fromCompare = TemporalUtils.compareTemporals(this.atTime, comparingObject.asInterval().getFromTime());
        if (fromCompare == -1) {
            return false;
        }
        if (!comparingObject.asInterval().isContinuing()) {
            @SuppressWarnings({"ConstantConditions", "squid:S3655"}) final int toCompare = TemporalUtils.compareTemporals(this.atTime, (Temporal) comparingObject.asInterval().getToTime().get());
            if (toCompare != -1) {
                return false;
            }
        }
        return true;
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
        if (parameterName == null) {
            return "pointTime";
        }
        return this.parameterName;
    }

    /**
     * Returns the explicit timezone of the temporal object
     *
     * @return - ZoneID
     */
    public ZoneId getTimeZone() {
        return this.timeZone;
    }

    @Override
    public String toString() {
        return String.format("%s@%s type:%s", this.scope, this.atTime, this.temporalType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PointTemporal<?> that = (PointTemporal<?>) o;

        if (scope != that.scope) return false;
        if (!atTime.equals(that.atTime)) return false;
        if (parameterName != null ? !parameterName.equals(that.parameterName) : that.parameterName != null)
            return false;
        if (!temporalType.equals(that.temporalType)) return false;
        return timeZone.equals(that.timeZone);
    }

    @Override
    public int hashCode() {
        int result = scope.hashCode();
        result = 31 * result + atTime.hashCode();
        result = 31 * result + (parameterName != null ? parameterName.hashCode() : 0);
        result = 31 * result + temporalType.hashCode();
        result = 31 * result + timeZone.hashCode();
        return result;
    }

    public static class Builder<T extends Temporal> {

        private TemporalScope scope;
        private T atTime;
        private Optional<Set<OWLNamedIndividual>> relations = Optional.empty();
        private Optional<String> parameterName = Optional.empty();
        private Optional<String> temporalID = Optional.empty();
        private Optional<ZoneId> explicitTimeZone = Optional.empty();

        Builder(TemporalScope scope, T at) {
            this.scope = scope;
            this.atTime = at;
        }

        /**
         * Manually set point parameter name
         *
         * @param name - String to use for parameter name
         * @return - Builder
         */
        public Builder withParameterName(String name) {
            this.parameterName = Optional.of(name);
            return this;
        }

        /**
         * Manually set temporalID
         *
         * @param temporalID - String of TemporalID
         * @return - Builder
         */
        public Builder withID(String temporalID) {
            this.temporalID = Optional.of(temporalID);
            return this;
        }

        /**
         * Set the point time zone
         *
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
         *
         * @param zoneId - ZoneID to use
         * @return - Builder
         */
        public Builder withTimeZone(ZoneId zoneId) {
            this.explicitTimeZone = Optional.of(zoneId);
            return this;
        }

        public PointTemporal build() {
            return new PointTemporal<>(this);
        }

    }
}
