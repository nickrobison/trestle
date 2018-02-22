/**
 * Created by nrobison on 5/31/17.
 */
const helper = require("./helpers");
exports.config = {
    baseUrl: "http://localhost:8080/workspace/",
    directConnect: true,
    capabilities: {
        "browserName": "chrome",
        shardTestFiles: true,
        maxInstances: 2
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
            helper.root('config/env.js')
        ]
        // tags: [
        //     '@Permissions'
        // ]
    },
    onPrepare() {
        require('ts-node').register({
            project: helper.root("src/test/tsconfig.json")
        });
    }
};
