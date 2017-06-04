"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * Created by nrobison on 5/31/17.
 */
var protractor_1 = require("protractor");
describe("Protractor Demo App", function () {
    it("should have title", function () {
        protractor_1.browser.get("http://juliemr.github.io/protractor-demo/");
        var result = protractor_1.browser.getTitle();
        var expected = "Super Calculator";
        expect(result).toEqual(expected);
    });
    it("should add one and two", function () {
        protractor_1.browser.get("http://juliemr.github.io/protractor-demo/");
        protractor_1.element(protractor_1.by.model("first")).sendKeys(1);
        protractor_1.element(protractor_1.by.model("second")).sendKeys(2);
        protractor_1.element(protractor_1.by.id("gobutton")).click();
        expect(protractor_1.element(protractor_1.by.binding("latest"))
            .getText())
            .toEqual("3");
    });
});
//# sourceMappingURL=spec.js.map