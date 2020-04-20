import { ArraySortPipe } from './array-sort.pipe';

describe('SortedArrayPipe', () => {
  it('create an instance', () => {
    const pipe = new ArraySortPipe();
    expect(pipe).toBeTruthy();
  });
});
