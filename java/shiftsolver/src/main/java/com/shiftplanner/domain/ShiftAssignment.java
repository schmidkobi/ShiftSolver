package com.shiftplanner.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

import java.time.Duration;

@PlanningEntity
public class ShiftAssignment {
    private Shift shift;

    @PlanningVariable
    private Employee employee;

    public ShiftAssignment(){}

    public ShiftAssignment(Shift shift, Employee employee) {
        this.shift = shift;
        this.employee = employee;
    }

    public Shift getShift() {
        return shift;
    }
    public Employee getEmployee() {
        return employee;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
    }
    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public TimeSlot getShiftTimeSlot() {
        return shift.getTimeSlot();
    }

    public Duration getShiftDuration() {
        return shift.getTimeSlot().getDuration();
    }
}
