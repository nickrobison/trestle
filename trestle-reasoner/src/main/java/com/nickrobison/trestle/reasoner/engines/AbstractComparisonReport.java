package com.nickrobison.trestle.reasoner.engines;

import com.nickrobison.trestle.types.relations.ObjectRelation;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class AbstractComparisonReport implements Serializable {
    public static final long serialVersionUID = 42L;

    private final String objectAID;
    private final String objectBID;
    //    I don't really like this, but it's a work around to the Serialization warning
    protected final HashSet<ObjectRelation> relations;

    public AbstractComparisonReport(OWLNamedIndividual objectA, OWLNamedIndividual objectB) {
        this.objectAID = objectA.toStringID();
        this.objectBID = objectB.toStringID();
        this.relations = new HashSet<>();
    }

    public String getObjectAID() {
        return objectAID;
    }

    public String getObjectBID() {
        return objectBID;
    }

    /**
     * Add the specified {@link ObjectRelation} to the relation set
     *
     * @param relation - {@link ObjectRelation} to add
     */
    public void addRelation(ObjectRelation relation) {
        this.relations.add(relation);
    }

    /**
     * Get all asserted relations between Object A and Object B
     *
     * @return - {@link Set} {@link ObjectRelation}
     */
    public Set<ObjectRelation> getRelations() {
        return this.relations;
    }
}
