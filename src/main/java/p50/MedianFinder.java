package p50;

/*
This interface allows calculating the median of the last N elements in data stream.
 */
public interface MedianFinder {

    /*
    @param num. The new element in the data stream
     */
    void add(int num);

    /*
    @return the median of the last N numbers
     */
    int getMedian();
}
