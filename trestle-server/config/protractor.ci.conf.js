/**
 * Created by nickrobison on 2/28/18.
 */

const browserstack = require('browserstack-local');
const helper = require("./helpers");
const fetch = require('node-fetch');

function sleep(ms) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

exports.config = {
  baseUrl: "http://localhost:8080/workspace/",
  healthUrl: "http://localhost:8087/healthcheck",
  seleniumAddress: "https://hub-cloud.browserstack.com/wd/hub",
  commonCapabilities: {
    'browserstack.user': process.env.BROWSERSTACK_USERNAME,
    'browserstack.key': process.env.BROWSERSTACK_ACCESS_KEY,
    'browserstack.local': true,
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
    const bsPromise = new Promise(function (resolve, reject) {
      exports.bs_local = new browserstack.Local();
      helper.getBranch().then(branch => {
        const branchName = process.env.BUILD_SOURCEBRANCHNAME || branch;
        console.debug("Setting build name: ", branchName);
        exports.config.multiCapabilities.forEach(caps => {
          caps.build = branchName;
        });
        exports.bs_local.start({'key': exports.config.commonCapabilities['browserstack.key']}, function (error) {
          if (error) return reject(error);
          console.log('Connected. Now testing...');

          resolve();
        });
      }, error => {
        return reject(error);
      })

    });

    // Ensure Trestle is working
    const trestlePromise = (async () => {
      console.log("Waiting for Trestle to start");
      let retryCount = 10;
      let ok = false;
      while (!ok && retryCount > 0) {
        try {
          const response = await fetch(exports.config.healthUrl);
          ok = response.ok;
        } catch (e) {
          console.log("Cannot connect to trestle, retrying", e);
        }
        // Sleep for 10 seconds
        await sleep(10000);
        retryCount--;
      }
      if (retryCount === 0) {
        throw Error("Cannot connect to Trestle after 10 tries");
      }

      console.log("Trestle running");
    });
    return Promise.all([bsPromise, trestlePromise()]);
  },

  // Code to stop browserstack local after end of test
  afterLaunch: function () {
    return new Promise(function (resolve) {
      exports.bs_local.stop(resolve);
    });
  }
};

exports.config.multiCapabilities.forEach(function (caps) {
  for (var i in exports.config.commonCapabilities) caps[i] = caps[i] || exports.config.commonCapabilities[i];
});
