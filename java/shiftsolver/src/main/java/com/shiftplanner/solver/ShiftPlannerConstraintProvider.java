package com.shiftplanner.solver;

import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import com.shiftplanner.domain.*;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;


import java.util.stream.Collectors;

import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.sumDuration;
import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.toList;
import static ai.timefold.solver.core.api.score.stream.Joiners.equal;

public class ShiftPlannerConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                noAssignmentsConflict(constraintFactory),
                minMaxWorkingHoursConflict(constraintFactory),
                employeeNotAvailable(constraintFactory),
                consecutiveShifts(constraintFactory),
                unwantedPatterns(constraintFactory),
                specialUnwantedPatterns(constraintFactory)
        };
    }

    private Constraint noAssignmentsConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Employee.class)
                .ifNotExists(ShiftAssignment.class, equal(employee -> employee, ShiftAssignment::getEmployee))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Unassigned Employee");
    }

    private Constraint minMaxWorkingHoursConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(ShiftAssignment.class)
                .filter(s -> s.getEmployee() != null)
                .groupBy(ShiftAssignment::getEmployee, sumDuration(ShiftAssignment::getShiftDuration))
                .filter((employee,d) -> !employee.getMinMaxHours().between(d))
                .penalize(HardSoftScore.ONE_HARD,(e,d)->(int)e.getMinMaxHours().getDifference(d).toHours())
                .asConstraint("Too much or less working hours");
    }

    private Constraint employeeNotAvailable(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(ShiftAssignment.class)
                .filter(s -> s.getEmployee() != null)
                .ifNotExists(AvailableShift.class,
                        equal(ShiftAssignment::getShiftTimeSlot, AvailableShift::getTimeSlot),
                        equal(ShiftAssignment::getEmployee, AvailableShift::getEmployee))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Employee Not Available");
    }

    /*Constraint ConsecutiveShiftsOLD(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(ShiftAssignment.class)
                .filter(s -> s.getEmployee() != null)
                .join(TimeSlotTypePattern.class)
                .groupBy((sa,tstp)->sa.getEmployee(),
                        toList((sa,tstp)->sa.getShiftTimeSlot()),
                        toList((sa,tstp)->tstp))
                .penalize(HardSoftScore.ONE_SOFT, (e,l,p)->consecutiveSlots(l,e.getMaxConsecutiveShifts(),p))
                .asConstraint("Too many consecutive shifts");
    }*/

    protected Constraint consecutiveShifts(ConstraintFactory constraintFactory){
        return constraintFactory.forEach(ShiftAssignment.class)
                .filter(s->s.getEmployee() != null)
                .groupBy(ShiftAssignment::getEmployee,
                        ConstraintCollectors.toConsecutiveSequences(s->s.getShiftTimeSlot().getId()))
                .flattenLast(SequenceChain::getConsecutiveSequences)
                .filter((e,sq)->sq.getCount()>e.getMaxConsecutiveShifts())
                .penalize(HardSoftScore.ONE_SOFT,(e,sq)->sq.getCount()-e.getMaxConsecutiveShifts())
                .asConstraint("Too many consecutive shifts");
    }

    public Constraint unwantedPatterns(ConstraintFactory constraintFactory){
        return constraintFactory.forEach(ShiftAssignment.class)
                .filter(s->s.getEmployee() != null)
                .groupBy(ShiftAssignment::getEmployee,
                        ConstraintCollectors.toConsecutiveSequences(s->s.getShiftTimeSlot().getId()))
                .flattenLast(SequenceChain::getConsecutiveSequences)
                .map((e,sq)->e,
                        (e,sq)->sq.getItems().stream().map(s->s.getShift().getTimeSlot().getType()).collect(Collectors.toList()))
                .join(TimeSlotTypePattern.class)
                .filter((e,sq,tsps)->tsps.equals(new TimeSlotTypePattern(sq)))
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Unwanted shift pattern");
    }
    public Constraint specialUnwantedPatterns(ConstraintFactory constraintFactory){
        return constraintFactory.forEach(ShiftAssignment.class)
                .filter(s->s.getEmployee() != null && s.getEmployee().isAvoidSpecialPatterns())
                .groupBy(ShiftAssignment::getEmployee,
                        ConstraintCollectors.toConsecutiveSequences(s->s.getShiftTimeSlot().getId()))
                .flattenLast(SequenceChain::getConsecutiveSequences)
                .map((e,sq)->e,
                        (e,sq)->sq.getItems().stream().map(s->s.getShift().getTimeSlot().getType()).collect(Collectors.toList()))
                .join(TimeSlotTypePattern.class)
                .filter((e,sq,tsps)->tsps.equals(new TimeSlotTypePattern(sq)))
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Special unwanted pattern");
    }
}
