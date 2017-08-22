package com.nickrobison.trestle.types.temporal;

import com.nickrobison.trestle.common.TemporalUtils;
import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalUnit;
import java.util.*;

/**
 * Created by nrobison on 6/30/16.
 */
// I can suppress both of these warnings because I know for sure they are correct
@SuppressWarnings({"unchecked", "return.type.incompatible", "Duplicates"})
public class IntervalTemporal<T extends Temporal> extends TemporalObject {

    private static final TemporalType TYPE = TemporalType.INTERVAL;
    private final TemporalScope scope;
    private final T fromTime;
    private final @Nullable T toTime;
    private final boolean isDefault;
    private final @Nullable String startName;
    private final @Nullable String endName;
    private final Class<T> temporalType;
    private final ZoneId startTimeZone;
    private final ZoneId endTimeZone;

    private IntervalTemporal(Builder<T> builder) {
        super(builder.temporalID.orElse(UUID.randomUUID().toString()), builder.relations);
        this.scope = builder.scope;
        this.fromTime = builder.fromTime;
        this.toTime = builder.toTime.orElse(null);
        this.isDefault = builder.isDefault;
        this.startName = builder.startName.orElse(null);
        this.endName = builder.endName.orElse(null);
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
    public Temporal getIdTemporal() {
        return this.fromTime;
    }

    @Override
    public Class<? extends Temporal> getBaseTemporalType() {
        return this.temporalType;
    }

    @Override
    public TemporalObject castTo(TemporalScope castTemporal) {
        if (castTemporal == TemporalScope.VALID) {
            if (isContinuing()) {
                return TemporalObjectBuilder.valid().from(this.fromTime).build();
            }
            return TemporalObjectBuilder.valid().from(this.fromTime).to(this.toTime).build();
        } else {
            if (isContinuing()) {
                return TemporalObjectBuilder.exists().from(this.fromTime).build();
            }
            return TemporalObjectBuilder.exists().from(this.fromTime).to(this.toTime).build();
        }
    }

    @Override
    public int compareTo(Temporal comparingTemporal) {
        final int compareFrom = TemporalUtils.compareTemporals(this.fromTime, comparingTemporal);
//        If the start temporal is after the comparingTemporal, then the interval object occurs after the comparingTemporal
        if (compareFrom == 1) {
            return 1;
        }

//        Do we have an ending temporal?
        if (this.toTime != null) {
            final int compareTo = TemporalUtils.compareTemporals(this.toTime, comparingTemporal);
//            If the ending temporal is less than or equal to the comparingTemporal, then the interval object occurs before the comparingTemporal
            if (compareTo == 0 || compareTo == -1) {
                return -1;
//            If the ending temporal comes after the comparingTemporal, hen the temporal is during the interval object
            } else {
                return 0;
            }
        }

//        If it's a continuing interval and the start is before the comparingTemporal, then the temporal is during the interval object
        return 0;
    }

    @Override
    @SuppressWarnings({"squid:S3655", "ConstantConditions"})
    public boolean during(TemporalObject comparingObject) {
//        If the given object is a point, we can't be during it.
        if (comparingObject.isPoint()) {
            return false;
        }

//        If we're comparing an interval object, are we fully with in the given object?
//        Do we start before the given object?
        final int fromCompare = TemporalUtils.compareTemporals(this.fromTime, comparingObject.asInterval().fromTime);
        if (fromCompare == -1) {
            return false;
        }

//        If they're continuing, we're fully within them
        if (comparingObject.isContinuing()) {
            return true;
        }

//        If we're continuing and they're not, we're not within them
        return !this.asInterval().isContinuing() && TemporalUtils.compareTemporals(this.toTime, (Temporal) comparingObject.asInterval().getToTime().get()) == -1;
    }

    @Override
    public boolean isPoint() {
        return false;
    }

    @Override
    public boolean isInterval() {
        return true;
    }

    @EnsuresNonNullIf(expression = "this.toTime", result = false)
    @Override
    public boolean isContinuing() {
        return toTime == null;
    }

    public boolean isDefault() {
        return this.isDefault;
    }

    public T getFromTime() {
        return this.fromTime;
    }

    public Optional<T> getToTime() {
        return Optional.ofNullable(this.toTime);
    }

    /**
     * Temporal intervals are exclusive of the toTime, this method returns the latest inclusive value of the interval
     * Executes {@link TemporalQueries#precision()} to find the smallest supported value and subtracts the given amount
     * If the interval is continuing, returns an empty optional
     * If the precision is finer than {@link ChronoUnit#MICROS}, we return {@link ChronoUnit#MICROS}
     *
     * @param amount - amount to add/subtract from the ending temporal
     * @return - Optional temporal of type {@link T}
     */
    public Optional<T> getAdjustedToTime(int amount) {
        if (isContinuing()) return Optional.empty();
        @SuppressWarnings({"ConstantConditions", "squid:S3655"}) final T end = this.getToTime().get();
        final TemporalUnit query = end.query(TemporalQueries.precision());
//        We can't do precisions finer than Microseconds
        if (((ChronoUnit) query).compareTo(ChronoUnit.MICROS) < 0) {
            return Optional.of((T) end.plus(amount, ChronoUnit.MICROS));
        }
        return Optional.of((T) end.plus(amount, query));
    }

    public String getStartName() {
        if (this.startName == null) {
            return "intervalStart";
        }
        return this.startName;
    }

    public String getEndName() {
        if (this.endName == null) {
            return "intervalEnd";
        }
        return this.endName;
    }

    public ZoneId getStartTimeZone() {
        return this.startTimeZone;
    }

    public ZoneId getEndTimeZone() {
        return this.endTimeZone;
    }

    @Override
    public String toString() {
        if (isContinuing()) {
            return String.format("%sFrom:%s type:%s", this.scope, this.fromTime, this.temporalType);
        }
        return String.format("%sFrom:%s To:%s type:%s", this.scope, this.fromTime, this.toTime, this.temporalType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IntervalTemporal<?> that = (IntervalTemporal<?>) o;

        if (isDefault != that.isDefault) return false;
        if (scope != that.scope) return false;
        if (!fromTime.equals(that.fromTime)) return false;
        if (toTime != null ? !toTime.equals(that.toTime) : that.toTime != null) return false;
        if (startName != null ? !startName.equals(that.startName) : that.startName != null) return false;
        if (endName != null ? !endName.equals(that.endName) : that.endName != null) return false;
        if (!temporalType.equals(that.temporalType)) return false;
        if (!startTimeZone.equals(that.startTimeZone)) return false;
        return endTimeZone.equals(that.endTimeZone);
    }

    @Override
    public int hashCode() {
        int result = scope.hashCode();
        result = 31 * result + fromTime.hashCode();
        result = 31 * result + (toTime != null ? toTime.hashCode() : 0);
        result = 31 * result + (isDefault ? 1 : 0);
        result = 31 * result + (startName != null ? startName.hashCode() : 0);
        result = 31 * result + (endName != null ? endName.hashCode() : 0);
        result = 31 * result + temporalType.hashCode();
        result = 31 * result + startTimeZone.hashCode();
        result = 31 * result + endTimeZone.hashCode();
        return result;
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
        public Builder to(@Nullable T to) {
            this.toTime = Optional.ofNullable(to);
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
         *
         * @param temporalID - String of TemporalID
         * @return - Builder
         */
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

        public IntervalTemporal build() {
            return new IntervalTemporal(this);
        }
    }
}
