package com.shiftplanner.domain;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TimeSlot implements Comparable<TimeSlot> {
    private int id;
    private Duration paidHours;
    private String type;
    private LocalDateTime start;
    private LocalDateTime end;
    private boolean isWeekend;

    public TimeSlot() {}

    public TimeSlot(int id, Duration paidHours, String type, LocalDateTime start, LocalDateTime end) {
        this.id = id;
        this.paidHours = paidHours;
        this.type = type;
        this.start = start;
        this.end = end;
        setWeekend();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Duration getPaidHours() {
        return paidHours;
    }

    public void setPaidHours(Duration paidHours) {
        this.paidHours = paidHours;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public boolean isWeekend() {
        return isWeekend;
    }
    private void setWeekend() {
        this.isWeekend = start.getDayOfWeek() == DayOfWeek.SATURDAY || start.getDayOfWeek() == DayOfWeek.SUNDAY || end.getDayOfWeek() == DayOfWeek.SATURDAY;
    }
    public LocalDateTime getStart() {
        return start;
    }
    public void setStart(LocalDateTime start) {
        this.start = start;
        setWeekend();
    }
    public LocalDateTime getEnd() {
        return end;
    }
    public void setEnd(LocalDateTime end) {
        this.end = end;
        setWeekend();
    }


    @Override
    public int compareTo(TimeSlot o) {
        if(this.getId() == o.getId()) {
            return 0;
        }
        else return this.getId() < o.getId() ? -1 : 1;
    }
    //todo: refactor
    //returns how often at least threshold-times consecutive timeslot are in an array
    public static int consecutiveSlots(List<TimeSlot>timeSlots, int threshold, List<TimeSlotTypePattern> unwantedPatterns) {
        int result = 0;
        List<TimeSlot> consecutive = new ArrayList<>();
        List<Integer> test = new ArrayList<>();
        timeSlots.sort(TimeSlot::compareTo);
        for(int i = 0; i < timeSlots.size(); i++) {
            //test.add(timeSlots.get(i).getId());
            if ( (i == 0 || timeSlots.get(i).getId() == timeSlots.get(i-1).getId() + 1)){
                consecutive.add(timeSlots.get(i));
            }
            else {
                if (consecutive.size() >  threshold) {
                    result += consecutive.size() - threshold;
                }
                List<String> pattern = new ArrayList<>();
                for(TimeSlot t:consecutive){
                    pattern.add(t.getType());
                }
                //todo This only compares exactly the same not if it is some where
                if(unwantedPatterns.contains(new TimeSlotTypePattern(pattern))){
                    result+=1;
                }
                consecutive.clear();
                consecutive.add(timeSlots.get(i));
            }
            if(i == timeSlots.size()-1){
                if (consecutive.size() >  threshold) {
                    result += consecutive.size() - threshold;
                }
                List<String> pattern = new ArrayList<>();
                for(TimeSlot t:consecutive){
                    pattern.add(t.getType());
                }
                if(unwantedPatterns.contains(new TimeSlotTypePattern(pattern))){
                    result+=1;
                }
            }
        }
        //System.out.println("inputarr size " + timeSlots.size()+"Result:"+ result + "IDs"+ test);
        return result;
    }
}
