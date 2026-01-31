package com.sap.sse.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.scalablevalue.KadaneExtremeSubarraysFinder;
import com.sap.sse.common.scalablevalue.ScalableDouble;

public class KadaneExtremeSubarraysFinderTest {
    private static final double EPSILON = 0.00000001;
    private KadaneExtremeSubarraysFinder<Double, Double, ScalableDouble> finder;
    
    @BeforeEach
    public void setUp() {
        finder = new KadaneExtremeSubarraysFinder<>();
    }
    
    @Test
    public void testSimplePositiveSequence() {
        finder.add(new ScalableDouble(1));
        finder.add(new ScalableDouble(2));
        finder.add(new ScalableDouble(3));
        assertEquals(6.0, finder.getMaxSum(), EPSILON);
        assertEquals(0, finder.getStartIndexInclusiveOfMaxSumSequence());
        assertEquals(3, finder.getEndIndexExclusiveOfMaxSumSequence());
    }
}
