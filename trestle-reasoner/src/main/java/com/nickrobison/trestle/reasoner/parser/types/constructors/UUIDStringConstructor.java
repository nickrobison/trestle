package com.nickrobison.trestle.reasoner.parser.types.constructors;

import com.nickrobison.trestle.reasoner.parser.TypeConstructor;

import java.util.UUID;

import static com.nickrobison.trestle.common.StaticIRI.UUIDDatatypeIRI;

public class UUIDStringConstructor implements TypeConstructor<UUID, String> {

    public UUIDStringConstructor() {
//        Not used
    }


    @Override
    public UUID constructType(String owlRepresentation) {
        return UUID.fromString(owlRepresentation);
    }

    @Override
    public String deconstructType(UUID inputType) {
        return inputType.toString();
    }

    @Override
    public Class<UUID> getJavaType() {
        return UUID.class;
    }

    @Override
    public String getOWLDatatype() {
        return UUIDDatatypeIRI.toString();
    }

    @Override
    public String getConstructorName() {
        return "UUID String Constructor";
    }
}
