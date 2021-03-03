## Coding Question

Write an interface for a data structure that can provide the moving average of the last N elements added, add elements to the structure and get access to the elements. Provide an efficient implementation of the interface for the data structure.

### Minimum Requirements

1. Provide a separate interface (IE `interface`/`trait`) with documentation for the data structure
2. Provide an implementation for the interface
3. Provide any additional explanation about the interface and implementation in a README file.

### Algorithm Design

There are multiple algorithms to calculate the median of the last elements in a data stream. For example, two heap, bucket, and sampling.

In the HeapMedianFinder class, I implemented the two heap algorithm. In the two heap algorithm, we keep two heaps. The "maxHeap" keeps the elements that's smaller than the median. The "minHeap" keeps the elements that's smaller than the median. In the "maxHeap", the first element is always the largest. Vise versa.

When we add an element. First we compare. If the new element is smaller than the median, we add the new element to the "maxHeap". Otherwise we add to the "minHeap". Then if the size difference between the two heaps is larger than one, we rebalance to two heaps by moving one element across.

Aside from the heaps, I use the "list" as the first-in first-out (FIFO) queue to keep track of the latest N elements. So that when I add a new element to the heaps, I know the value of the N+1 elemnt to remove from the heaps. Since writing to and deleting from heaps are of O(N) time complexity, the overall time complexity to add an new element to the MedianFinder is O(N).

When we read the median, we know that since the size of the two heaps are balanced, the median is always between the two tops of the two heaps. In this way, the time complexity to read the median is O(1).

### Example

We can take the following example for validation.

```java
        HeapMedianFinder medianFinder = new HeapMedianFinder(2);

        medianFinder.add(1);
        Assertions.assertEquals(1, medianFinder.getMedian()); // the list is [1]
        medianFinder.add(1);
        Assertions.assertEquals(1, medianFinder.getMedian()); // the list is [1, 1]
        medianFinder.add(4);
        Assertions.assertEquals(1, medianFinder.getMedian()); // the list is [1, 4]
        medianFinder.add(4);
        Assertions.assertEquals(4, medianFinder.getMedian()); // the list is [4, 4]
```
