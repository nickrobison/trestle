/**
 * Created by nrobison on 5/31/17.
 */
import {browser, by, element} from "protractor";
describe("Protractor Demo App", () => {
    it("should have title", () => {
        browser.get("http://juliemr.github.io/protractor-demo/");
        let result = browser.getTitle();
        let expected = "Super Calculator";
        expect(result).toEqual(expected);
    });

    it("should add one and two", () => {
        browser.get("http://juliemr.github.io/protractor-demo/");
        element(by.model("first")).sendKeys(1);
        element(by.model("second")).sendKeys(2);

        element(by.id("gobutton")).click();

        expect(element(by.binding("latest"))
            .getText())
            .toEqual("3");
    });
});
