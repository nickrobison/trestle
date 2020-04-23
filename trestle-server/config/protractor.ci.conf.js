/**
 * Created by nickrobison on 2/28/18.
 */

const browserstack = require('browserstack-local');
const helper = require("./helpers");
exports.config = {
  baseUrl: "http://trestle:8080/workspace/",
  seleniumAddress: "https://hub-cloud.browserstack.com/wd/hub",
  commonCapabilities: {
    'browserstack.user': process.env.BROWSERSTACK_USERNAME,
    'browserstack.key': process.env.BROWSERSTACK_ACCESS_KEY,
    'build': 'UI build',
    'project': 'Trestle'
  },
  multiCapabilities: [{
    'browserName': 'Chrome',
    'name': 'Trestle UI [Chrome] Test'
  }, {
    'browserName': 'Firefox',
    'name': 'Trestle UI [Firefox] Test',
    'os': 'OS X',
    'os_version': 'Catalina'
  }, {
    'browserName': 'Edge',
    'name': 'Trestle UI [Edge] Test'
  }],
  allScriptsTimeout: 110000,
  noGlobals: true,
  // SELENIUM_PROMISE_MANAGER: false,
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
  },

  // Code to start browserstack local before start of test
  beforeLaunch: function () {
    console.log("Connecting local");
    return new Promise(function (resolve, reject) {
      exports.bs_local = new browserstack.Local();
      exports.bs_local.start({'key': exports.config.commonCapabilities['browserstack.key']}, function (error) {
        if (error) return reject(error);
        console.log('Connected. Now testing...');

        resolve();
      });
    });
  },

  // Code to stop browserstack local after end of test
  afterLaunch: function () {
    return new Promise(function (resolve, reject) {
      exports.bs_local.stop(resolve);
    });
  }
};

exports.config.multiCapabilities.forEach(function (caps) {
  for (var i in exports.config.commonCapabilities) caps[i] = caps[i] || exports.config.commonCapabilities[i];
});
