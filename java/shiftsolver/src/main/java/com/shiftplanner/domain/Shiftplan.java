package com.shiftplanner.domain;

import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

import java.util.List;

@PlanningSolution
public class Shiftplan {
    @ProblemFactCollectionProperty
    private List<AvailableShift> availableShiftList;
    @ValueRangeProvider
    @ProblemFactCollectionProperty
    private List<Employee> employeeList;
    @ProblemFactCollectionProperty
    private List<Shift> shiftList;
    @ProblemFactCollectionProperty
    private List<TimeSlotTypePattern> unwantedTimeSlotTypePatternList;

    @PlanningEntityCollectionProperty
    private List<ShiftAssignment> shiftAssignmentList;

    @PlanningScore
    HardSoftScore score;

    public Shiftplan(){}

    public List<AvailableShift> getAvailableShiftList() {
        return availableShiftList;
    }

    public void setAvailableShiftList(List<AvailableShift> availableShiftList) {
        this.availableShiftList = availableShiftList;
    }

    public List<Employee> getEmployeeList() {
        return employeeList;
    }
    public void setEmployeeList(List<Employee> employeeList) {
        this.employeeList = employeeList;
    }

    public List<Shift> getShiftList() {
        return shiftList;
    }
    public void setShiftList(List<Shift> shiftList) {
        this.shiftList = shiftList;
    }

    public List<TimeSlotTypePattern> getUnwantedShiftCombinationList() {
        return unwantedTimeSlotTypePatternList;
    }
    public void setUnwantedShiftCombinationList(List<TimeSlotTypePattern> unwantedTimeSlotTypePatternList) {
        this.unwantedTimeSlotTypePatternList = unwantedTimeSlotTypePatternList;
    }

    public List<ShiftAssignment> getShiftAssignmentList() {
        return shiftAssignmentList;
    }
    public void setShiftAssignmentList(List<ShiftAssignment> shiftAssignmentList) {
        this.shiftAssignmentList = shiftAssignmentList;
    }
}
