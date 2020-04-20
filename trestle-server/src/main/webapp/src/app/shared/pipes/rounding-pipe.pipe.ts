import {Pipe, PipeTransform} from "@angular/core";

@Pipe({name: "rounding"})
export class RoundingPipe implements PipeTransform {

  /**
   * Rounds a number to the specified number of decimal places.
   * Apparently the most accurate method
   * per {@link https://stackoverflow.com/a/43532829/773566 this} link
   * @param {number} value
   * @param {number} digits
   * @returns {number}
   */
  public transform(value: number, digits: number): number {
    let negative = false;
    if (digits === undefined) {
      digits = 0;
    }
    if (value < 0) {
      negative = true;
      value = value * -1;
    }
    const multiplier = Math.pow(10, digits);
    value = parseFloat((value * multiplier).toFixed(11));
    value = Number.parseFloat((Math.round(value) / multiplier).toFixed(2));
    if (negative) {
      value = Number.parseFloat((value * -1).toFixed(2));
    }
    return value;
  }
}
