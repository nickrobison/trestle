// base config from jest-present-angular
const jestPreset = require('jest-preset-angular/jest-preset');
const { globals } = jestPreset;
const tsjest = globals['ts-jest'];
const tsjestOverrides = {
  ...tsjest
};
const globalOverrides = {
  ...globals,
  'ts-jest': { ...tsjestOverrides }
};
// make sure to add in the required preset and
// and setup file entries
module.exports = {
  ...jestPreset,
  globals: { ...globalOverrides },
  preset: 'jest-preset-angular',
  setupFiles: [
    '<rootDir>/jest/jest.stubs.ts'
  ]
  // setupFilesAfterEnv: ['<rootDir>/src/setupJest.ts']
};
