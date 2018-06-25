import { debounceTime, distinctUntilChanged } from "rxjs/operators";
import { Observable } from "rxjs/Observable";

/**
 * Combines both {@link debounceTime} and {@link distinctUntilChanged}
 * @param {number} ms - debounce time (in milliseconds)
 * @returns {<T>(source: Observable<T>) => Observable<any>}
 */
export const distinctTime = (ms: number) => <T>(source: Observable<T>) => {
    return source
        .pipe(
            debounceTime(ms),
            distinctUntilChanged()
        );
};