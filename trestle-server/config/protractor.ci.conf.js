/**
 * Created by nickrobison on 2/28/18.
 */
const helper = require("./helpers");
exports.config = {
    baseUrl: "http://trestle:8080/workspace/",
    seleniumAddress: "http://localhost:4444/wd/hub",
    capabilities: {
        "browserName": "chrome",
            // marionette: true,
            shardTestFiles: true,
            maxInstances: 1
//        "browserName": "firefox",
//        shardTestFiles: true,
//        maxInstances: 1,
//        "moz:firefoxOptions": {
//            'args': ["--headless"]
//        }
    },
    useAllAngular2AppRoots: true,
    allScriptsTimeout: 110000,
    noGlobals: true,
    SELENIUM_PROMISE_MANAGER: false,
    framework: 'custom',
    frameworkPath: require.resolve('protractor-cucumber-framework'),
    specs: [
        helper.root('src/test/e2e/**/*.feature')
    ],
    cucumberOpts: {
        require: [
            helper.root('src/test/e2e/**/*.steps.ts'),
            helper.root('src/test/e2e/step_definitions/env.ts')
        ]
    },
    onPrepare() {
        require('ts-node').register({
            project: helper.root("src/test/e2e/tsconfig.json")
        });
    }
};
