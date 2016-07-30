package com.nickrobison.trestle.parser;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by nrobison on 7/29/16.
 */
public class ConstructorArguments {

//    private final String argumentName;
//    private final Class<?> argumentType;
//    private final Object argumentObject;

    private final Map<String, Argument> arguments;

    public ConstructorArguments() {
        this.arguments = new HashMap<>();
    }

    public void addArgument(String name, Class<?> type, Object value) {
        this.arguments.put(name, new Argument(type, value));
    }

    public List<Class<?>> getTypes() {
        List<Class<?>> argumentTypes = new ArrayList<>();
        this.arguments.values()
                .forEach(value -> argumentTypes.add(value.getArgumentType()));

        return argumentTypes;
    }

    public List<Object> getValues() {
        return this.arguments
                .values()
                .stream()
                .map(Argument::getArgumentValue)
                .collect(Collectors.toList());
    }

    public Optional<Class<?>> getType(String argumentName) {
        final Argument argument = this.arguments.get(argumentName);
        if (argument == null) {
            return Optional.empty();
        }
        return Optional.of(argument.getArgumentType());
    }

    public Optional<Object> getValue(String argumentName) {
        final Argument argument = this.arguments.get(argumentName);
        if (argument == null) {
            return Optional.empty();
        }
        return Optional.of(argument.getArgumentValue());
    }

    public Class<?>[] getSortedTypes(String... argumentName) {
        return getSortedTypes(Arrays.asList(argumentName));
    }

    public Class<?>[] getSortedTypes(List<String> argumentNames) {
        List<Class<?>> sortedTypes = new ArrayList<>();
        argumentNames.forEach(arg -> {
            final Optional<Class<?>> type = getType(arg);
            if (type.isPresent()) {
                sortedTypes.add(type.get());
            }
        });
        return sortedTypes.toArray(new Class<?>[sortedTypes.size()]);
    }

    public Object[] getSortedValues(String... argumentName) {
        return getSortedValues(Arrays.asList(argumentName));
    }

    public Object[] getSortedValues(List<String> argumentNames) {
        List<Object> sortedObjects = new ArrayList<>();
        argumentNames.forEach(arg -> {
            final Optional<Object> value = getValue(arg);
            if (value.isPresent()) {
                sortedObjects.add(value.get());
            }
        });
        return sortedObjects.toArray(new Object[sortedObjects.size()]);
    }

    private class Argument {

        private final Class<?> argumentType;
        private final Object argumentObject;

        Argument(Class<?> argumentType, Object argumentObject) {
            this.argumentType = argumentType;
            this.argumentObject = argumentObject;
        }

        Class<?> getArgumentType() {
            return this.argumentType;
        }

        Object getArgumentValue() {
            return this.argumentObject;
        }
    }
}
