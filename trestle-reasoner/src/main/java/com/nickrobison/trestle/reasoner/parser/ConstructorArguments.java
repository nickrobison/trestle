package com.nickrobison.trestle.reasoner.parser;

import org.checkerframework.checker.nullness.qual.KeyFor;

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

    public Set<@KeyFor("this.arguments") String> getNames() {
        return this.arguments.keySet();
    }

    public List<Class<?>> getTypes() {
        List<Class<?>> argumentTypes = new ArrayList<>();
        this.arguments.values()
                .stream()
                .map(Argument::getArgumentType)
                .sorted(Comparator.comparing(Class::getName))
                .forEach(argumentTypes::add);

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
            type.ifPresent(sortedTypes::add);
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
            value.ifPresent(sortedObjects::add);
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

        @Override
        public String toString() {
            return "Argument{" +
                    "argumentType=" + argumentType +
                    ", argumentObject=" + argumentObject +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "ConstructorArguments{" +
                "arguments=" + arguments +
                '}';
    }
}
