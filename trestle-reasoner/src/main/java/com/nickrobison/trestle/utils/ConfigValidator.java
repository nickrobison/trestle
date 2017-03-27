package com.nickrobison.trestle.utils;

import com.nickrobison.trestle.common.LanguageUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by nrobison on 12/8/16.
 */
public class ConfigValidator {

    private static final Logger logger = LoggerFactory.getLogger(ConfigValidator.class);

    /**
     * Run the various validators to ensure that the specified config file is valid
     * Throws a RuntimeException if it encounters a missing config variable in invalid value
     * @param config - Config file to validate
     */
    public static void ValidateConfig(Config config) {
        logConfigSettings(config);
        validateDefaultLanguage(config.getString("defaultLanguage"));
    }

//    TODO(nrobison): Pretty-print config
    private static void logConfigSettings(Config config) {
        logger.debug("Running with config settings: {}", config.toString());
    }

    /**
     * Ensure the default language code is valid and won't get rejected by the SPARQL query parser
     * @param languageCode - default language code
     */
    private static void validateDefaultLanguage(String languageCode) {
        if (!LanguageUtils.checkLanguageCodeIsValid(languageCode)) {
            throw new RuntimeException(String.format("Invalid language code %s", languageCode));
        }
    }
}
