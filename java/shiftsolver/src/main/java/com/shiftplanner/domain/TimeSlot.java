package com.shiftplanner.domain;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class TimeSlot implements Comparable<TimeSlot> {
    private int id;
    private Duration duration;
    private String type;
    private boolean isWeekend;

    public TimeSlot() {}

    public TimeSlot(int id, Duration duration, String type, boolean isWeekend) {
        this.id = id;
        this.duration = duration;
        this.type = type;
        this.isWeekend = isWeekend;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
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
    public void setWeekend(boolean weekend) {
        isWeekend = weekend;
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
            test.add(timeSlots.get(i).getId());
            if ( (i == 0 || timeSlots.get(i).getId() == timeSlots.get(i-1).getId() + 1)){
                consecutive.add(timeSlots.get(i));
            }
            else {
                if (consecutive.size() >  threshold){
                    result+=consecutive.size()-threshold;
                    List<String> pattern = new ArrayList<>();
                    for(TimeSlot t:consecutive){
                        pattern.add(t.getType());
                    }
                    if(unwantedPatterns.contains(new TimeSlotTypePattern(pattern))){
                        result+=1;
                    }
                }
                consecutive.clear();
                consecutive.add(timeSlots.get(i));
            }
            if(i == timeSlots.size()-1){
                if (consecutive.size() >  threshold){
                    result+=consecutive.size()-threshold;
                    List<String> pattern = new ArrayList<>();
                    for(TimeSlot t:consecutive){
                        pattern.add(t.getType());
                    }
                    if(unwantedPatterns.contains(new TimeSlotTypePattern(pattern))){
                        result+=1;
                    }
                }
            }
        }
        //System.out.println("inputarr size " + timeSlots.size()+"Result:"+ result + "IDs"+ test);
        return result;
    }
}
