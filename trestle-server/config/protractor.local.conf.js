/**
 * Created by nrobison on 5/31/17.
 */
const helper = require("./helpers");
// @ts-check
// Protractor configuration file, see link for more information
// https://github.com/angular/protractor/blob/master/lib/config.ts

/**
 * @type { import("protractor").Config }
 */
exports.config = {
  allScriptsTimeout: 11000,
  capabilities: {
    browserName: 'chrome'
  },
  directConnect: true,
  baseUrl: 'http://localhost:4200/workspace/',

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

