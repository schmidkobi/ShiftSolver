package com.shiftplanner.app;


import com.shiftplanner.domain.TimeSlotTypePattern;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TimeSlotTypePatternTest {

    @Test
    void testContains(){
        TimeSlotTypePattern aa = new TimeSlotTypePattern("A", "A");
        TimeSlotTypePattern ab = new TimeSlotTypePattern("A", "B");
        TimeSlotTypePattern ac = new TimeSlotTypePattern("A", "C");
        TimeSlotTypePattern aaa = new TimeSlotTypePattern("A", "A", "A");
        TimeSlotTypePattern aab = new TimeSlotTypePattern("A", "A", "B");
        TimeSlotTypePattern aac = new TimeSlotTypePattern("A", "A", "C");

        List<TimeSlotTypePattern> patterns = Arrays.asList(aa, ab);
        assertTrue(patterns.contains(aa));
        assertTrue(patterns.contains(ab));
        assertFalse(patterns.contains(ac));
        assertFalse(patterns.contains(aaa));
        assertFalse(patterns.contains(aab));

        List<TimeSlotTypePattern> patterns2 = Arrays.asList(aaa, aab);
        assertFalse(patterns2.contains(aa));
        assertTrue(patterns2.contains(aab));
        assertFalse(patterns2.contains(aac));

    }
}
