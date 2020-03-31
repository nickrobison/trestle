import { Pipe, PipeTransform } from "@angular/core";

@Pipe({name: "ArraySortPipe"})
export class ArraySortPipePipe implements PipeTransform {

    /**
     * Sorts a given input array either based on the element value, or an optional field
     *
     * @param {any[]} values - Array of input values to sort
     * @param {string} field - Optional field to use in sort function
     * @returns {any[]} - Sorted array
     */
    public transform(values: any[], field?: string): any[] {
        return values.sort((a, b) => this.sortFn(a, b, field));
    }


    private sortFn(a: any, b: any, field?: string): number {
        if (field) {

            if (a[field] < b[field]) {
                return -1;
            } else if (a[field] > b[field]) {
                return 1;
            }
            return 0;
        } else {
            if (a < b) {
                return -1;
            } else if (a > b) {
                return 1;
            }
            return 0;
        }
    }
}
