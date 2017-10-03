package com.nickrobison.trestle.reasoner.parser;

import com.nickrobison.trestle.reasoner.annotations.DatasetClass;
import com.nickrobison.trestle.reasoner.annotations.IndividualIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DatasetClass(name = "ProviderTest")
public class ProviderTest {
    private static final Logger logger = LoggerFactory.getLogger(ProviderTest.class);


    public static void main(String[] args) {
        logger.debug("Logging, on!");
        final IClassParser parser = ClojureParserProvider.getParser();
        logger.info(parser.getDefaultLanguageCode());

        logger.info("{}", parser.getObjectClass(new ParserTestClass()));

        logger.info("{}", parser.getIndividual(new ParserTestClass()));
    }


    public static class ParserTestClass {

        public ParserTestClass() {}

        @IndividualIdentifier
        public String getMethodID() {
            return "Have_stuff";
        }
    }
}
