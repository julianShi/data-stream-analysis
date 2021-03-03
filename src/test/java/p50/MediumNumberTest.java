package p50;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MediumNumberTest {
    @Test
    public void testInit() {
        HeapMedianFinder medianFinder = new HeapMedianFinder(2);

        medianFinder.add(1);
        Assertions.assertEquals(1, medianFinder.getMedian());
        medianFinder.add(1);
        Assertions.assertEquals(1, medianFinder.getMedian());
        medianFinder.add(4);
        Assertions.assertEquals(1, medianFinder.getMedian());
        medianFinder.add(4);
        Assertions.assertEquals(4, medianFinder.getMedian());
    }
}