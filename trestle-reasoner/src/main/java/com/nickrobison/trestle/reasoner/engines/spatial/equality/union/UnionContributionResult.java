package com.nickrobison.trestle.reasoner.engines.spatial.equality.union;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class UnionContributionResult<T> implements Serializable {
    public static final long serialVersionUID = 42L;

    private final T object;
    private final double area;
    private final Set<UnionContributionPart<T>> contributingParts;


    public UnionContributionResult(T unionObject, double area) {
        this.object = unionObject;
        this.area = area;
        this.contributingParts = new HashSet<>();
    }

    public T getObject() {
        return object;
    }

    public double getArea() {
        return area;
    }

    /**
     * Get all the contributing objects and their associated percentages
     *
     * @return - {@link Set} {@link UnionContributionPart}
     */
    public Set<UnionContributionPart<T>> getContributingParts() {
        return contributingParts;
    }

    /**
     * Add object contributing to whole
     *
     * @param contributingObject     - {@link T} contributing object
     * @param contributionPercentage - {@link Double} contribution percentage
     */
    public void addContribution(T contributingObject, double contributionPercentage) {
        this.contributingParts.add(new UnionContributionPart<>(contributingObject, contributionPercentage));
    }

    /**
     * Add a {@link Collection} of {@link UnionContributionPart}
     *
     * @param parts - {@link Collection} of {@link UnionContributionPart}
     */
    public void addAllContributions(Collection<UnionContributionPart<T>> parts) {
        this.contributingParts.addAll(parts);
    }

    @Override
    public String toString() {
        return "UnionContributionResult{" +
                "object=" + object +
                ", area=" + area +
                ", contributingParts=" + contributingParts +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UnionContributionResult<?> that = (UnionContributionResult<?>) o;

        if (Double.compare(that.area, area) != 0) return false;
        if (!object.equals(that.object)) return false;
        return contributingParts.equals(that.contributingParts);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = object.hashCode();
        temp = Double.doubleToLongBits(area);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + contributingParts.hashCode();
        return result;
    }

    static class UnionContributionPart<T> implements Serializable {
        public static final long serialVersionUID = 42L;

        private final T object;
        private final double contribution;

        UnionContributionPart(T object, double contribution) {
            this.object = object;
            this.contribution = contribution;
        }

        public T getObject() {
            return object;
        }

        public double getContribution() {
            return contribution;
        }

        @Override
        public String toString() {
            return "UnionContributionPart{" +
                    "object=" + object +
                    ", contribution=" + contribution +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UnionContributionPart<?> that = (UnionContributionPart<?>) o;

            if (Double.compare(that.contribution, contribution) != 0) return false;
            return object.equals(that.object);
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            result = object.hashCode();
            temp = Double.doubleToLongBits(contribution);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }
    }
}
