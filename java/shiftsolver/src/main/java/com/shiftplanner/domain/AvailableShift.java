package com.shiftplanner.domain;

public class AvailableShift {
    private TimeSlot timeSlot;
    private Employee employee;


    public AvailableShift(TimeSlot timeSlot, Employee employee) {
        this.timeSlot = timeSlot;
        this.employee = employee;
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }
    public void setTimeSlot(TimeSlot timeSlot) {
        this.timeSlot = timeSlot;
    }

    public Employee getEmployee() {
        return employee;
    }
    public void setEmployee(Employee employee) {
        this.employee = employee;
    }
}
