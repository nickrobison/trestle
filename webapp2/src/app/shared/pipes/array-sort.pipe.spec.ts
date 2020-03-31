import { ArraySortPipePipe } from './array-sort.pipe';

describe('SortedArrayPipe', () => {
  it('create an instance', () => {
    const pipe = new ArraySortPipePipe();
    expect(pipe).toBeTruthy();
  });
});
