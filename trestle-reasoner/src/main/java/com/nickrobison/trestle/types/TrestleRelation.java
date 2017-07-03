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
}
