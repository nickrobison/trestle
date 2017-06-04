/**
 * Created by nrobison on 5/31/17.
 */
require("ts-node/register");
const helper = require("./helpers");
exports.config = {
    baseUrl: "http://localhost:8080/workspace/",
    directConnect: true,
    capabilities: {
        "browserName": "chrome"
    },
    noGlobals: true,
    SELENIUM_PROMISE_MANAGER: false,
    framework: 'custom',
    frameworkPath: require.resolve('protractor-cucumber-framework'),
    // specs: ["src/test/e2e/**/*.feature"],
    specs: [
        helper.root('src/test/e2e/**/*.feature')
    ],
    cucumberOpts: {
        require: [
            helper.root('src/test/e2e/**/*.steps.ts')
        ],
        format: 'pretty'
    }
};
