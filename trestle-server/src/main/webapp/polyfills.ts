/**
 * Created by nrobison on 1/17/17.
 */
// These polyfills are required on all browsers
import "core-js/es6/reflect";
import "core-js/es7/reflect";
import "zone.js/dist/zone";
import checkBrowser from "check-browser";

const evergreenBrowser = checkBrowser({
    chrome: 49,
    firefox: 52,
    edge: 14,
    safari: 10
});

if (!evergreenBrowser) {
    console.debug("Loading extra polyfills");
    System.import("./polyfills.target");
}

if (process.env.ENV === "production") {
    // Production
} else {
    // Development and test
    Error["stackTraceLimit"] = Infinity;
    System.import("zone.js/dist/long-stack-trace-zone");
    console.debug("Loading long stack trace for development");
}