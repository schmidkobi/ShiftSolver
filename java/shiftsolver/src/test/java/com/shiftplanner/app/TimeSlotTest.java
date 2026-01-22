package com.shiftplanner.app;


import com.shiftplanner.domain.TimeSlot;
import com.shiftplanner.domain.TimeSlotTypePattern;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test for simple App.
 */
public class TimeSlotTest {

    /**
     * Rigorous Test :-)
     */
    @Test
    public void testConsectutiveSlots() {
        List<TimeSlot> timeSlotList = new ArrayList<>();
        timeSlotList.add(new TimeSlot(0,Duration.ofHours(8),"A")); //1
        timeSlotList.add(new TimeSlot(11,Duration.ofHours(8),"A"));
        timeSlotList.add(new TimeSlot(12,Duration.ofHours(8),"A"));
        timeSlotList.add(new TimeSlot(3,Duration.ofHours(8),"A"));
        timeSlotList.add(new TimeSlot(5,Duration.ofHours(8),"A")); //3
        timeSlotList.add(new TimeSlot(6,Duration.ofHours(8),"A"));
        timeSlotList.add(new TimeSlot(2,Duration.ofHours(8),"A")); //2
        timeSlotList.add(new TimeSlot(9,Duration.ofHours(8),"A"));//4
        timeSlotList.add(new TimeSlot(10,Duration.ofHours(8),"A"));
        timeSlotList.add(new TimeSlot(7,Duration.ofHours(8),"A"));

        assertEquals( 6,TimeSlot.consecutiveSlots(timeSlotList,1,new ArrayList<>()));
        assertEquals( 3,TimeSlot.consecutiveSlots(timeSlotList,2,new ArrayList<>()));
        assertEquals( 1,TimeSlot.consecutiveSlots(timeSlotList,3,new ArrayList<>()));
        assertEquals( 0,TimeSlot.consecutiveSlots(timeSlotList,4,new ArrayList<>()));
    }

}