package com.shiftplanner.solver;

import com.shiftplanner.domain.*;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.*;


import static com.shiftplanner.domain.TimeSlot.consecutiveSlots;
import static org.optaplanner.core.api.score.stream.ConstraintCollectors.*;
import static org.optaplanner.core.api.score.stream.Joiners.equal;

public class ShiftPlannerConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                NoAssignmentsConflict(constraintFactory),
                MinMaxWorkingHoursConflict(constraintFactory),
                EmployeeNotAvailable(constraintFactory),
                ConsecutiveShifts(constraintFactory),
        };
    }

    private Constraint NoAssignmentsConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Employee.class)
                .ifNotExists(ShiftAssignment.class, equal(employee -> employee, ShiftAssignment::getEmployee))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Unassigned Employee");
    }

    private Constraint MinMaxWorkingHoursConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(ShiftAssignment.class)
                .filter(s -> s.getEmployee() != null)
                .groupBy(ShiftAssignment::getEmployee, sumDuration(ShiftAssignment::getShiftDuration))
                .filter((employee,d) -> !employee.getMinMaxHours().between(d))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Too much or less working hours");
    }

    private Constraint EmployeeNotAvailable(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(ShiftAssignment.class)
                .filter(s -> s.getEmployee() != null)
                .ifNotExists(AvailableShift.class,
                        equal(ShiftAssignment::getShiftTimeSlot, AvailableShift::getTimeSlot),
                        equal(ShiftAssignment::getEmployee, AvailableShift::getEmployee))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Employee Not Available");
    }

    Constraint ConsecutiveShifts(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(ShiftAssignment.class)
                .filter(s -> s.getEmployee() != null)
                .join(TimeSlotTypePattern.class)
                .groupBy((sa,tstp)->sa.getEmployee(),
                        toList((sa,tstp)->sa.getShiftTimeSlot()),
                        toList((sa,tstp)->tstp))
                .penalize(HardSoftScore.ONE_SOFT, (e,l,p)->consecutiveSlots(l,e.getMaxConsecutiveShifts(),p))
                .asConstraint("Too many consecutive shifts");
    }
}