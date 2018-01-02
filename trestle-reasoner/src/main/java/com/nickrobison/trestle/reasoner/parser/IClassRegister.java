package com.nickrobison.trestle.reasoner.parser;

import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;

public interface IClassRegister {


    void registerClass(Class<?> clazz) throws TrestleClassException;

    void deregisterClass(Class<?> clazz);

    Object getRegisteredClass(Class<?> clazz) throws TrestleClassException;

    boolean isRegistered(Class<?> clazz) throws TrestleClassException;

    boolean isCacheable(Class<?> clazz) throws TrestleClassException;
}
