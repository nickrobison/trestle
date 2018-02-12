package com.nickrobison.trestle.types;

import com.nickrobison.trestle.types.relations.ObjectRelation;

import java.io.Serializable;

/**
 * Created by nrobison on 1/9/17.
 */

/**
 * Relation class that defines a subject/object relation and its type.
 */
public class TrestleRelation implements Serializable {
    private static final long serialVersionUID = 42L;

    private final String subject;
    private final String object;
    private final ObjectRelation type;

    /**
     * Default constructor for TrestleRelation
     * @param subject - String IRI of the relation subject
     * @param type - ObjectRelation type
     * @param object - String IRI of the relation object
     */
    public TrestleRelation(String subject, ObjectRelation type, String object) {
        this.subject = subject;
        this.type = type;
        this.object = object;
    }

    /**
     * @return - Subject IRI string
     */
    public String getSubject() {
        return subject;
    }

    /**
     * @return - Object IRI string
     */
    public String getObject() {
        return object;
    }

    /**
     * @return - Relation type string
     */
    public String getType() {
        return type.toString();
    }

    @Override
    public String toString() {
        return String.format("%s--%s--%s", getSubject(), getType(), getObject());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrestleRelation that = (TrestleRelation) o;

        if (!subject.equals(that.subject)) return false;
        if (!object.equals(that.object)) return false;
        return type == that.type;
    }

    @Override
    public int hashCode() {
        int result = subject.hashCode();
        result = 31 * result + object.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }
}
