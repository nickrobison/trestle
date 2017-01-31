package com.nickrobison.trestle.types.temporal;

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
@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unchecked", "return.type.incompatible", "Duplicates"})
public class IntervalTemporal<T extends Temporal> extends TemporalObject {

    private static final TemporalType TYPE = TemporalType.INTERVAL;
    private final TemporalScope scope;
    private final T fromTime;
    private final Optional<T> toTime;
    private final boolean isDefault;
    private final Optional<String> startName;
    private final Optional<String> endName;
    private final Class<T> temporalType;
    private final ZoneId startTimeZone;
    private final ZoneId endTimeZone;

    private IntervalTemporal(Builder<T> builder) {
        super(builder.temporalID.orElse(UUID.randomUUID().toString()), builder.relations);
        this.scope = builder.scope;
        this.fromTime = builder.fromTime;
        this.toTime = builder.toTime;
        this.isDefault = builder.isDefault;
        this.startName = builder.startName;
        this.endName = builder.endName;
        this.temporalType = (Class<T>) builder.fromTime.getClass();
        this.startTimeZone = builder.fromTimeZone.orElse(ZoneOffset.UTC);
        this.endTimeZone = builder.toTimeZone.orElse(builder.fromTimeZone.orElse(ZoneOffset.UTC));
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
        return (this.scope == TemporalScope.VALID);
    }

    @Override
    public boolean isExists() {
        return (this.scope == TemporalScope.EXISTS);
    }

    @Override
    public boolean isDatabase() {
        return this.scope == TemporalScope.DATABASE;
    }

    @Override
    public Class<? extends Temporal> getBaseTemporalType() {
        return this.temporalType;
    }

    @Override
    public TemporalObject castTo(TemporalScope castTemporal) {
        if (castTemporal == TemporalScope.VALID) {
            if (isContinuing()) {
                return TemporalObjectBuilder.valid().from(this.fromTime).withRelations(this.getTemporalRelations().toArray(new OWLNamedIndividual[this.getTemporalRelations().size()]));
            }
            return TemporalObjectBuilder.valid().from(this.fromTime).to(this.toTime.get()).withRelations(this.getTemporalRelations().toArray(new OWLNamedIndividual[this.getTemporalRelations().size()]));
        } else {
            if (isContinuing()) {
                return TemporalObjectBuilder.exists().from(this.fromTime).withRelations(this.getTemporalRelations().toArray(new OWLNamedIndividual[this.getTemporalRelations().size()]));
            }
            return TemporalObjectBuilder.exists().from(this.fromTime).to(this.toTime.get()).withRelations(this.getTemporalRelations().toArray(new OWLNamedIndividual[this.getTemporalRelations().size()]));
        }
    }

    @Override
    public boolean isPoint() {
        return false;
    }

    @Override
    public boolean isInterval() {
        return true;
    }

    public boolean isContinuing() {
        return !toTime.isPresent();
    }

    public boolean isDefault() {
        return this.isDefault;
    }

    public T getFromTime() {
        return this.fromTime;
    }

    public Optional<T> getToTime() {
        return this.toTime;
    }

    public String getStartName() {
        return this.startName.orElse("intervalStart");
    }

    public String getEndName() {
        return this.endName.orElse("intervalEnd");
    }

    public ZoneId getStartTimeZone() {
        return this.startTimeZone;
    }

    public ZoneId getEndTimeZone() {
        return this.endTimeZone;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static class Builder<T extends Temporal> {

        private TemporalScope scope;
        private T fromTime;
        private Optional<T> toTime = Optional.empty();
        private Optional<Set<OWLNamedIndividual>> relations = Optional.empty();
        private Optional<String> startName = Optional.empty();
        private Optional<String> endName = Optional.empty();
        private Optional<String> temporalID = Optional.empty();
        private Optional<ZoneId> fromTimeZone = Optional.empty();
        private Optional<ZoneId> toTimeZone = Optional.empty();
        private boolean isDefault = false;

        Builder(TemporalScope scope, T from) {
            this.scope = scope;
            this.fromTime = from;
        }

        /**
         * Set this temporal as a default, meaning it has no related to parameter
         *
         * @param isDefault - boolean is default?
         * @return - Builder
         */
        public Builder isDefault(boolean isDefault) {
            this.isDefault = isDefault;
            return this;
        }

        /**
         * Set the to temporal
         *
         * @param to - Temporal of type T to use for to temporal
         * @return - Builder
         */
        public Builder to(T to) {
            this.toTime = Optional.of(to);
            return this;
        }

        /**
         * Set the parameter names for the from/to temporals
         *
         * @param startName - String for start name
         * @param endName   - Nullable string for endName
         * @return - Builder
         */
        public Builder withParameterNames(String startName, @Nullable String endName) {
            this.startName = Optional.of(startName);
            if (endName != null) {
                this.endName = Optional.of(endName);
            }
            return this;
        }

        /**
         * Manually set temporalID
         * @param temporalID - String of TemporalID
         * @return - Builder
         */
        @Deprecated
        public Builder withID(String temporalID) {
            this.temporalID = Optional.of(temporalID);
            return this;
        }

        /**
         * Set the time zone for the from temporal
         *
         * @param zoneId - String to parse into zoneID
         * @return - Builder
         */
        public Builder withFromTimeZone(String zoneId) {
            if (!zoneId.equals("")) {
                this.fromTimeZone = Optional.of(ZoneId.of(zoneId));
            }
            return this;
        }

        /**
         * Set the time zone for the from temporal
         *
         * @param zoneId - ZoneID to use
         * @return - Builder
         */
        public Builder withFromTimeZone(ZoneId zoneId) {
            this.fromTimeZone = Optional.of(zoneId);
            return this;
        }

        /**
         * Set the time zone for the to temporal
         *
         * @param zoneId - String to parse into ZoneID
         * @return - Builder
         */
        public Builder withToTimeZone(String zoneId) {
            if (!zoneId.equals("")) {
                this.toTimeZone = Optional.of(ZoneId.of(zoneId));
            }
            return this;
        }

        /**
         * Set the time zone for the to temporal
         *
         * @param zoneId - ZoneID to use
         * @return - Builder
         */
        public Builder withToTimeZone(ZoneId zoneId) {
            this.toTimeZone = Optional.of(zoneId);
            return this;
        }

        /**
         * Set the Individuals this temporal relates to
         *
         * @param relations - OWLNamedIndividuals associated with this temporal
         * @return - Builder
         */
        @Deprecated
        public IntervalTemporal withRelations(OWLNamedIndividual... relations) {
            this.relations = Optional.of(new HashSet<>(Arrays.asList(relations)));
            return new IntervalTemporal<>(this);
        }

        public IntervalTemporal build() {
            return new IntervalTemporal(this);
        }


    }
}
