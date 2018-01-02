package com.nickrobison.trestle.reasoner.parser;

import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.reasoner.exceptions.UnregisteredClassException;
import org.semanticweb.owlapi.model.OWLClass;

import java.util.Set;

public interface IClassRegister {


    void registerClass(OWLClass owlClass, Class<?> clazz) throws TrestleClassException;

    void deregisterClass(Class<?> clazz);

    Object getRegisteredClass(Class<?> clazz) throws UnregisteredClassException;

    Set<OWLClass> getRegisteredOWLClasses();

    Class<?> lookupClass(OWLClass owlClass) throws UnregisteredClassException;

    boolean isRegistered(Class<?> clazz);

    boolean isCacheable(Class<?> clazz) throws UnregisteredClassException;
}
