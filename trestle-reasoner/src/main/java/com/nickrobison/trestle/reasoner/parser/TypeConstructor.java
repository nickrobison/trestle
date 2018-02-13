package com.nickrobison.trestle.reasoner.parser;

public interface TypeConstructor<T, S> {

    T constructType(S owlRepresentation);

    S deconstructType(T inputType);

    Class<T> getJavaType();

    String getOWLDatatype();

    String getConstructorName();
}
