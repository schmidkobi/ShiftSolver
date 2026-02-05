package com.shiftplanner.domain;

public class Employee {
    private String name;
    private MinMaxHours minMaxHours;
    private int maxConsecutiveShifts;
    private boolean avoidSpecialPatterns;
    private boolean avoidDoubleWeekendShifts;
    public Employee() {}


    public Employee(String name) {
        this.name = name;
        this.maxConsecutiveShifts = 1;
        this.avoidSpecialPatterns = false;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public MinMaxHours getMinMaxHours() {
        return minMaxHours;
    }
    public void setMinMaxHours(MinMaxHours minMaxHours) {
        this.minMaxHours = minMaxHours;
    }

    public int getMaxConsecutiveShifts() {
        return maxConsecutiveShifts;
    }

    public void setMaxConsecutiveShifts(int maxConsecutiveShifts) {
        this.maxConsecutiveShifts = maxConsecutiveShifts;
    }

    public boolean isAvoidSpecialPatterns() {
        return avoidSpecialPatterns;
    }
    public void setAvoidSpecialPatterns(boolean avoidSpecialPatterns) {
        this.avoidSpecialPatterns = avoidSpecialPatterns;
    }

    public boolean isAvoidDoubleWeekendShifts() {
        return avoidDoubleWeekendShifts;
    }
    public void setAvoidDoubleWeekendShifts(boolean avoidDoubleWeekendShifts) {
        this.avoidDoubleWeekendShifts = avoidDoubleWeekendShifts;
    }
}
