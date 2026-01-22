package com.shiftplanner.domain;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import com.shiftplanner.excelIO.ExcelHandler;
import com.shiftplanner.solver.ShiftPlannerConstraintProvider;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//todo: is it possible to stop when score threshold is met?

public class ShiftPlannerApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShiftPlannerApp.class);

    public static void main(String[] args) {
        String filePath = "okt25.xlsx";
        ExcelHandler handler = new ExcelHandler(filePath);
        Shiftplan plan = handler.ShiftPlanFromExcelFile();
        SolverConfig solverConfig = handler.getSolverConfig();

        SolverFactory<Shiftplan> solverFactory = SolverFactory.create(solverConfig);
        Solver<Shiftplan> solver = solverFactory.buildSolver();

        Shiftplan solution = solver.solve(plan);

        handler.writeSolvedShiftsToCopy(solution);

        printShiftPlan(solution);
    }

    public static boolean solveShiftPlanFile(String filePath) {
        ExcelHandler handler = new ExcelHandler(filePath);
        Shiftplan plan = handler.ShiftPlanFromExcelFile();
        SolverConfig solverConfig = handler.getSolverConfig();
        SolverFactory<Shiftplan> solverFactory = SolverFactory.create(solverConfig);
        Solver<Shiftplan> solver = solverFactory.buildSolver();

        Shiftplan solution = solver.solve(plan);

        handler.writeSolvedShiftsToCopy(solution);

        return true;
    }

    public static Shiftplan generateData(){
        Shiftplan plan = new Shiftplan();

        int nShifts = 60;
        List<TimeSlot> timeSlots = new ArrayList<>(nShifts);
        List<Shift> shifts = new ArrayList<Shift>(nShifts);
        List<ShiftAssignment> shiftAssignments = new ArrayList<ShiftAssignment>(nShifts);

        List<Employee> employees = new ArrayList<Employee>(3);

        for (int i=0; i<nShifts; i++){
            TimeSlot ts = new TimeSlot(i,Duration.ofHours(8),"TEST");
            Shift shift = new Shift(ts);
            ShiftAssignment shiftAssignment = new ShiftAssignment();
            shiftAssignment.setShift(shift);
            //shiftAssignment.setEmployee(employees.get(0));
            timeSlots.add(ts);
            shifts.add(shift);
            shiftAssignments.add(shiftAssignment);
        }

        Employee max =new Employee("Max");
        max.setMinMaxHours(new MinMaxHours(max,8, 240));
        employees.add(max);

        Employee lisa =new Employee("Lisa");
        lisa.setMaxConsecutiveShifts(2);
        lisa.setMinMaxHours(new MinMaxHours(lisa,20, 240));
        employees.add(lisa);

        Employee john =new Employee("John");
        john.setMinMaxHours(new MinMaxHours(john,8, 80));
        employees.add(john);


        plan.setShiftList(shifts);
        plan.setShiftAssignmentList(shiftAssignments);
        plan.setEmployeeList(employees);

        List<AvailableShift>availableShifts = new ArrayList<>(nShifts);
        for (int i=0; i<nShifts; i++){
            availableShifts.add(new AvailableShift(timeSlots.get(i),max));
            if(true){
                availableShifts.add(new AvailableShift(timeSlots.get(i),lisa));
            }
            if(i==2 || i==3 || i==8 || i==10 || i==15 || i==20){
                availableShifts.add(new AvailableShift(timeSlots.get(i),john));
            }
        }
        plan.setAvailableShiftList(availableShifts);
        return plan;
    }

    public static void printShiftPlan(Shiftplan plan){
        LOGGER.info("");
        List<ShiftAssignment> shiftAssignments = plan.getShiftAssignmentList();
        for (ShiftAssignment shiftAssignment : shiftAssignments){
            if(shiftAssignment.getEmployee()==null)
                continue;
            LOGGER.info("|"+ shiftAssignment.getShift().getTimeSlot().getId()+"("+shiftAssignment.getShift().getTimeSlot().getType()+") | "+ shiftAssignment.getEmployee().getName()+"|");
        }

        LOGGER.info("");
        for(Employee employee : plan.getEmployeeList()){
            if(employee == null){
                continue;
            }
            float hours=shiftAssignments.stream()
                    .filter(s->s.getEmployee()!=null)
                    .filter(s->s.getEmployee().equals(employee))
                    .map(s->s.getShift().getTimeSlot().getDuration().toMinutes()).reduce(0L,Long::sum)/ 60f;
            LOGGER.info("|"+ employee.getName()+"| ("
                    + employee.getMinMaxHours().getMin().toHours()+ "/"
                    + employee.getMinMaxHours().getMax().toHours()+ ") | "
                    + hours+"|" );
        }

    }

}
