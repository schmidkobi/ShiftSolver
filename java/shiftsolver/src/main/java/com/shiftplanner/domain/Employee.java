package com.shiftplanner.domain;

public class Employee {
    private String name;
    private MinMaxHours minMaxHours;
    private int maxConsecutiveShifts;

    public Employee() {}


    public Employee(String name) {
        this.name = name;
        this.maxConsecutiveShifts = 1;
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
}
