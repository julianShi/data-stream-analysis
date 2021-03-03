package p50;

import java.util.*;

/*
The HeapMedianFinder uses two heaps to save the queue that is smaller than the median, and the queue that is larger
than the median. The write time complexity is O(log(N)), the read time complexity is O(1).

We have to maintain a list and two queues in this data structure. So the space complexity is O(N). So a small N is
preferred. If N is too large. Other algorithms like the sampling algorithm is preferred.
 */
class HeapMedianFinder implements MedianFinder {

    private PriorityQueue<Integer> maxQueue;
    private PriorityQueue<Integer> minQueue;
    private List<Integer> list;
    private boolean isOdd;
    private int capacity;

    /*
    Define the "list" field to save the last N elements in the data stream.
     */
    public HeapMedianFinder(final int N) {
        maxQueue = new PriorityQueue<>(N, Collections.reverseOrder());
        minQueue = new PriorityQueue<>(N);
        list = new LinkedList<>();
        isOdd = false;
        capacity = N;
    }

    /*
    we decide if the new element is smaller than the median or larger than the median. If the new element is smaller
    than the median, we add the new element to the maxQueue. Otherwise, we add the new element to the minQueue. The
    complexity of writing is O(log(N)).

    We balance the two queues if the sizes between them are larger than one.
     */
    @Override
    synchronized public void add(final int num) {
        if(maxQueue.size()==0){
            maxQueue.add(num);
            isOdd =!isOdd;
            return;
        }
        if(!isOdd) {
            if(num<= maxQueue.peek()) {
                maxQueue.add(num);
            } else {
                minQueue.add(num);
                maxQueue.add(minQueue.poll());
            }
        } else {
            if(num< maxQueue.peek()) {
                maxQueue.add(num);
                minQueue.add(maxQueue.poll());
            } else {
                minQueue.add(num);
            }
        }
        isOdd =!isOdd;

        // Add the new element, and evict the N+1 element.
        list.add(num);
        if (list.size() > capacity) {
            delete(list.remove(0));
        }
    }

    private void delete(final int num) {
        if(maxQueue.size()==0){
            return;
        }

        // Assume that num exists in the maxQueue or in the minQueue
        if (num <= maxQueue.peek()) {
            maxQueue.remove(num);
            if (!isOdd) {
                maxQueue.add(minQueue.poll());
            }
        } else {
            minQueue.remove(num);
            if (isOdd) {
                minQueue.add(maxQueue.poll());
            }
        }
        isOdd =!isOdd;
    }

    /*
    Get the median of the last N elements. The read time complexity is O(1).
     */
    @Override
    public int getMedian() {
        return maxQueue.peek();
    }
}
