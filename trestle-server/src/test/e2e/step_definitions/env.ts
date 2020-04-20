import { browser } from "protractor";

const {
    setDefaultTimeout, // jshint ignore:line
    Before, // jshint ignore:line
} = require('cucumber');

let firstLoad = true;

console.log("Setting env");
setDefaultTimeout(50 * 1000);

Before(() => {
    if (firstLoad) {
        console.log("First load");
        firstLoad = false;
        return browser.get(browser.baseUrl)
            .then(() => {
                console.log("Waiting for load");
                return browser.sleep(2000);
            });
    }
    return;
});
