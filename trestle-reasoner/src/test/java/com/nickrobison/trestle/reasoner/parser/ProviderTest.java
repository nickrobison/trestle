package com.nickrobison.trestle.reasoner.parser;

public class ProviderTest {


    public static void main(String[] args) {
        final IClassParser parser = ClojureParserProvider.getParser();
        parser.sayHello("nick");

        System.out.println(parser.addSomething(2, 3));
    }
}
