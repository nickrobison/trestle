declare module "sorted-array" {

  export type SortFunction<T> = (a: T, b: T) => number;

  export type CompareFunction<T> = (a: T) => number;

  export class SortedArray<T> {
    public array: T[];
    constructor(array: T[], compare?: SortFunction<T>);

    public insert(element: T): this;
    public search(element: T): number;
    public remove(element: T): this;

    public static comparing<C>(sorter: CompareFunction<C>, array: C[]): SortedArray<C>;
  }

  export default SortedArray;
}
