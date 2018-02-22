import { browser } from "protractor";

const {
    setDefaultTimeout, // jshint ignore:line
    Before, // jshint ignore:line
    BeforeAll, // jshint ignore:line
    After, // jshint ignore:line
    AfterStep // jshint ignore:line
} = require('cucumber');

console.log("Setting env");
setDefaultTimeout(60 * 1000);

Before(() => {
    console.log("Before");
    return browser.sleep(10000);
});
