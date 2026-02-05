package com.shiftplanner.app;


import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;
import com.shiftplanner.domain.*;
import com.shiftplanner.solver.ShiftPlannerConstraintProvider;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConstraintTest {
    private ConstraintVerifier<ShiftPlannerConstraintProvider, Shiftplan> constraintVerifier =
            ConstraintVerifier.build(new ShiftPlannerConstraintProvider(), Shiftplan.class, ShiftAssignment.class);
    @Test
    public void test(){}
    /*    Employee mike = new Employee("Mike");
        List<TimeSlot> timeSlotList = Arrays.asList(
                new TimeSlot(0, Duration.ofHours(8),"A", false),
                new TimeSlot(1, Duration.ofHours(8),"B",false),
                new TimeSlot(3, Duration.ofHours(8),"A", false),
                new TimeSlot(5, Duration.ofHours(8),"A", false),
                new TimeSlot(6, Duration.ofHours(8),"C", false)
        );
        List<Shift> shiftList = new ArrayList<>();
        List<ShiftAssignment>  shiftAssignmentList = new ArrayList<>();
        List<AvailableShift>  availableShiftList = new ArrayList<>();
        for (TimeSlot timeSlot : timeSlotList){
            Shift shift = new Shift(timeSlot);
            shiftList.add(shift);
            shiftAssignmentList.add(new ShiftAssignment(shift,mike));
            availableShiftList.add(new AvailableShift(timeSlot,mike));
        }
        List<Employee> employeeList = new ArrayList<>();
        employeeList.add(mike);
        List<TimeSlotTypePattern>unwantedPatterns = new ArrayList<>();
        unwantedPatterns.add(new TimeSlotTypePattern("A", "B"));
        unwantedPatterns.add(new TimeSlotTypePattern("A", "C"));
        Shiftplan plan = new Shiftplan();
        plan.setShiftList(shiftList);
        plan.setEmployeeList(employeeList);
        plan.setShiftAssignmentList(shiftAssignmentList);
        plan.setUnwantedShiftCombinationList(unwantedPatterns);
        plan.setAvailableShiftList(availableShiftList);
        System.out.println(constraintVerifier.verifyThat(ShiftPlannerConstraintProvider::unwantedPatterns).givenSolution(plan).toString());
        constraintVerifier.verifyThat(ShiftPlannerConstraintProvider::unwantedPatterns).givenSolution(plan).rewards();

    }*/
}
