package com.shiftplanner.domain;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.ScoreExplanation;
import ai.timefold.solver.core.api.score.analysis.ConstraintAnalysis;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.solver.SolutionManager;
import com.shiftplanner.excelIO.ExcelHandler;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

//todo: is it possible to stop when score threshold is met?

public class ShiftPlannerApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShiftPlannerApp.class);
    private static volatile SolverFactory<Shiftplan> currentSolverFactory;
    private static volatile Solver<Shiftplan> currentSolver;
    private static volatile Shiftplan currentSolution;

    public static void main(String[] args) {
        String filePath = "template.xlsx";
        ExcelHandler handler = new ExcelHandler(filePath);
        Shiftplan plan = handler.ShiftPlanFromExcelFile();
        SolverConfig solverConfig = handler.getSolverConfig();

        currentSolverFactory = SolverFactory.create(solverConfig);
        currentSolver = currentSolverFactory.buildSolver();

        currentSolution = currentSolver.solve(plan);
        handler.writeSolvedShiftsToCopy(currentSolution);
        printShiftPlan(currentSolution);
        System.out.println(getCurrentScoreExplanation());
    }

    public static boolean solveShiftPlanFile(String filePath,  Consumer<String> bestScoreConsumer) {
        try{
            ExcelHandler handler = new ExcelHandler(filePath);
            Shiftplan plan = handler.ShiftPlanFromExcelFile();
            SolverConfig solverConfig = handler.getSolverConfig();
            currentSolverFactory = SolverFactory.create(solverConfig);
            Solver<Shiftplan> solver = currentSolverFactory.buildSolver();
            solver.addEventListener(event -> {
                Score score = event.getNewBestScore();
                bestScoreConsumer.accept(score.toString());
            });
            currentSolver = solver;
            currentSolution = solver.solve(plan);
            handler.writeSolvedShiftsToCopy(currentSolution);
        return currentSolution != null;
    } finally {
       currentSolver = null;
    }
    }

    public static void requestTerminateEarly() {
        Solver<Shiftplan> s = currentSolver;
        if (s != null) s.terminateEarly();
    }

    public static void printShiftPlan(Shiftplan plan){
        LOGGER.info("");
        List<ShiftAssignment> shiftAssignments = plan.getShiftAssignmentList();
        for (ShiftAssignment shiftAssignment : shiftAssignments){
            if(shiftAssignment.getEmployee()==null)
                continue;
            int id = shiftAssignment.getShift().getTimeSlot().getId();
            String type = shiftAssignment.getShift().getTimeSlot().getType();
            String name = shiftAssignment.getEmployee().getName();
            String weekend = shiftAssignment.getShiftTimeSlot().isWeekend() ? "WE" : "";
            LOGGER.info("|"+ id +"("+ type +")"+ weekend +" | "+ name +"|");
        }

        LOGGER.info("");
        for(Employee employee : plan.getEmployeeList()){
            if(employee == null){
                continue;
            }
            float hours=shiftAssignments.stream()
                    .filter(s->s.getEmployee()!=null)
                    .filter(s->s.getEmployee().equals(employee))
                    .map(s->s.getShift().getTimeSlot().getPaidHours().toMinutes()).reduce(0L,Long::sum)/ 60f;
            LOGGER.info("|"+ employee.getName()+"| ("
                    + employee.getMinMaxHours().getMin().toHours()+ "/"
                    + employee.getMinMaxHours().getMax().toHours()+ ") | "
                    + hours+"|" );
        }

    }

    public static String getCurrentScoreExplanation(){
        if(currentSolverFactory == null) throw new RuntimeException("Could not get score explanation. No solver yet");
        if(currentSolution == null)throw new RuntimeException("Could not get score explanation. No solution yet. Solve a problem first");
        SolutionManager<Shiftplan, ?> solutionManager = SolutionManager.create(currentSolverFactory);
        ScoreAnalysis<?> scoreAnalysis = solutionManager.analyze(currentSolution);
        //ScoreExplanation<Shiftplan,?> scoreExplanation = solutionManager.explain(currentSolution);
        return getExplantionString(scoreAnalysis);
    }

    private static String getExplantionString(ScoreAnalysis<?> scoreAnalysis) {
        var constraintMap = scoreAnalysis.constraintMap();
        StringBuilder sb = new StringBuilder();
        constraintMap.forEach((constraintRef, constraintAnalysis) -> {
            int matchCount = constraintAnalysis.matchCount();
            String[] parts = constraintRef.constraintId().split("/");
            String name = parts[parts.length - 1];
            if (matchCount != 0) {
                sb.append("- ").append(matchCount)
                        .append("x ").append(name)
                        .append(" | Score:").append(constraintAnalysis.score())
                        .append(System.lineSeparator());
            }
        });
        return sb.toString();
    }


}
