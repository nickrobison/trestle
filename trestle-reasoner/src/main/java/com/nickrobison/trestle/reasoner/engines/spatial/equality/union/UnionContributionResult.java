package com.nickrobison.trestle.reasoner.engines.spatial.equality.union;

import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class UnionContributionResult implements Serializable {
    public static final long serialVersionUID = 42L;

    private final String objectID;
    private final double area;
    private final Set<UnionContributionPart> contributingParts;


    public UnionContributionResult(OWLNamedIndividual unionObject, double area) {
        this.objectID = unionObject.toStringID();
        this.area = area;
        this.contributingParts = new HashSet<>();
    }

    public String getObjectID() {
        return objectID;
    }

    public double getArea() {
        return area;
    }

    /**
     * Get all the contributing objects and their associated percentages
     *
     * @return - {@link Set} {@link UnionContributionPart}
     */
    public Set<UnionContributionPart> getContributingParts() {
        return contributingParts;
    }

    /**
     * Add objectID contributing to whole
     *
     * @param contributingObjectID   - {@link OWLNamedIndividual} contributing objectID
     * @param contributionPercentage - {@link Double} contribution percentage
     */
    public void addContribution(OWLNamedIndividual contributingObjectID, double contributionPercentage) {
        this.contributingParts.add(new UnionContributionPart(contributingObjectID, contributionPercentage));
    }

    /**
     * Add a {@link Collection} of {@link UnionContributionPart}
     *
     * @param parts - {@link Collection} of {@link UnionContributionPart}
     */
    public void addAllContributions(Collection<UnionContributionPart> parts) {
        this.contributingParts.addAll(parts);
    }

    @Override
    public String toString() {
        return "UnionContributionResult{" +
                "objectID=" + objectID +
                ", area=" + area +
                ", contributingParts=" + contributingParts +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UnionContributionResult that = (UnionContributionResult) o;

        if (Double.compare(that.area, area) != 0) return false;
        if (!objectID.equals(that.objectID)) return false;
        return contributingParts.equals(that.contributingParts);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = objectID.hashCode();
        temp = Double.doubleToLongBits(area);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + contributingParts.hashCode();
        return result;
    }


    static class UnionContributionPart implements Serializable {
        public static final long serialVersionUID = 42L;

        private final String objectID;
        private final double contribution;

        UnionContributionPart(OWLNamedIndividual objectID, double contribution) {
            this.objectID = objectID.toStringID();
            this.contribution = contribution;
        }

        public String getObjectID() {
            return objectID;
        }

        public double getContribution() {
            return contribution;
        }

        @Override
        public String toString() {
            return "UnionContributionPart{" +
                    "objectID=" + objectID +
                    ", contribution=" + contribution +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UnionContributionPart that = (UnionContributionPart) o;

            if (Double.compare(that.contribution, contribution) != 0) return false;
            return objectID.equals(that.objectID);
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            result = objectID.hashCode();
            temp = Double.doubleToLongBits(contribution);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }
    }
}
