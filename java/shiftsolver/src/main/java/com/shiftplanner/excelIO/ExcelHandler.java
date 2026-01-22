package com.shiftplanner.excelIO;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import com.shiftplanner.domain.*;
import com.shiftplanner.solver.ShiftPlannerConstraintProvider;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.shiftplanner.excelIO.ExcelHelpers.isEmpty;
import static com.shiftplanner.excelIO.ExcelHelpers.stringCellEquals;


public class ExcelHandler {
    private String originalFilePath;
    //Keys
    private List<String> headers;
    private String employeeSettingsKey;
    private String minHoursKey;
    private String maxHoursKey;
    private String consecutiveShiftsKey;
    private String specialUnwantedPatternKey;


    //Important Indices
    private int nEmployees;
    private int shiftStartColumnNumber;
    private int shiftEndColumnNumber;
    private int shiftEndRowNumber;

    private XSSFWorkbook workbook;
    private Map<String,Duration> shiftDurations;
    private List<TimeSlotTypePattern>unwantedPatterns;
    private List<TimeSlotTypePattern>specialUnwantedPatterns;
    private Map<String, HardSoftScore> constraintWeightOverridesMap;
    private SolverConfig solverConfig;

    public ExcelHandler(String filepath){
        this.originalFilePath = filepath;
        this.headers = new ArrayList<>();
        this.shiftDurations = new HashMap<>();
        this.unwantedPatterns = new ArrayList<>();
        this.specialUnwantedPatterns = new ArrayList<>();
        this.constraintWeightOverridesMap = new HashMap<>();
        this.loadWorkbook();
        this.loadSettings();
    }

    public static void main(String[] args) {
        String filePath = "testSmall.xlsx"; // Update with your file path
        ExcelHandler handler = new ExcelHandler(filePath);
        Shiftplan plan = handler.ShiftPlanFromExcelFile();
        plan.getShiftList().stream().map(Shift::getTimeSlot)
                .filter(t -> t.getDuration()==null)
                .map(TimeSlot::getId)
                .forEach(System.out::println);
    }

    private void loadWorkbook() {
        try{
            FileInputStream file = new FileInputStream(new File(this.originalFilePath));
            this.workbook = new XSSFWorkbook(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void loadSettings() {
        Sheet sheet = this.workbook.getSheetAt(0);
        //Get Row Indices
        int shiftSettingsIndex = -1;
        int unwantedShiftPatternsIndex = -1;
        int specialUnwantedShiftPatternsIndex= -1;
        int solverConfigurationIndex = -1;
        int constraintWeightsIndex = -1;
        int labelsSettingsIndex = -1;

        for (Row row : sheet) {
            Cell cell = row.getCell(0);
            if(cell==null || cell.getCellType() != CellType.STRING) continue;
            switch (cell.getStringCellValue()){
                case "Shift Settings":
                    shiftSettingsIndex = row.getRowNum();
                    break;
                case "Unwanted Shift Patterns":
                    unwantedShiftPatternsIndex = row.getRowNum();
                    break;
                case "Special Unwanted Shift Patterns":
                    specialUnwantedShiftPatternsIndex = row.getRowNum();
                    break;
                case "Solver Configuration":
                    solverConfigurationIndex = row.getRowNum();
                    break;
                case "Constraint Weights":
                    constraintWeightsIndex = row.getRowNum();
                    break;
                case "Labels":
                    labelsSettingsIndex = row.getRowNum();
                    break;
                default:
                    continue;
            }
        }
        loadShiftSettings(shiftSettingsIndex,unwantedShiftPatternsIndex);
        loadUnwantedShiftPatterns(unwantedShiftPatternsIndex,specialUnwantedShiftPatternsIndex,this.unwantedPatterns);
        loadUnwantedShiftPatterns(specialUnwantedShiftPatternsIndex,solverConfigurationIndex, this.specialUnwantedPatterns);
        loadSolverConfiguration(solverConfigurationIndex,constraintWeightsIndex);
        loadConstraintWeights(constraintWeightsIndex,labelsSettingsIndex);
        loadLabels(labelsSettingsIndex);
    }

    private void loadShiftSettings(int startIndex, int endIndex) {
        Sheet sheet = this.workbook.getSheetAt(0);
        //check header
        Row shiftSettingsHeader = sheet.getRow(startIndex+1);
        if(!stringCellEquals(shiftSettingsHeader.getCell(0),"type")&& !stringCellEquals(shiftSettingsHeader.getCell(1),"hours")) {
            throw new RuntimeException("Shift settings header not as expected");
        }
        for(int i = startIndex+2;i<endIndex;i++){
            Row row = sheet.getRow(i);
            if(row.getCell(0).getCellType()==CellType.BLANK) break;
            if(row.getCell(0).getCellType()!=CellType.STRING && row.getCell(0).getCellType()!=CellType.BLANK) throw new RuntimeException("shift type needs to be a string");
            String shiftType = row.getCell(0).getStringCellValue();
            if(row.getCell(1).getCellType()!=CellType.NUMERIC) throw new RuntimeException("shift duration needs to be a number");
            Duration shitDuration = Duration.ofMinutes((long)row.getCell(1).getNumericCellValue()*60);
            this.shiftDurations.put(shiftType,shitDuration);
        }
    }

    private void loadUnwantedShiftPatterns(int startIndex, int endIndex, List<TimeSlotTypePattern> patternList) {
        Sheet sheet = this.workbook.getSheetAt(0);
        //check header
        Row shiftSettingsHeader = sheet.getRow(startIndex);
        for(int i = startIndex+1;i<endIndex;i++){
            Row row = sheet.getRow(i);
            List<String>types = new ArrayList<>();
            for(Cell cell : row){
                if(cell.getCellType()==CellType.BLANK) break;
                if(cell.getCellType()!=CellType.STRING) throw new RuntimeException("Shift types need to be string");
                types.add(cell.getStringCellValue());
            }
            patternList.add(new TimeSlotTypePattern(types));
        }
    }

    private void loadSolverConfiguration(int startIndex, int endIndex) {
        Sheet sheet = this.workbook.getSheetAt(0);
        int terminationSpentLimit = -1;
        for(int i = startIndex+1;i<endIndex;i++) {
            Row row = sheet.getRow(i);
            Cell keyCell = row.getCell(0);
            Cell valueCell = row.getCell(1);
            CellType keyType = keyCell == null ? CellType.BLANK : keyCell.getCellType();
            CellType valueType = valueCell == null ? CellType.BLANK : valueCell.getCellType();

            if (keyType == CellType.BLANK) break; // stop early if empty key
            if (keyType != CellType.STRING) {
                throw new RuntimeException("solver configuration key needs to be a string");
            }
            switch (keyCell.getStringCellValue()) {
                case "TerminationSpentLimit":
                    if (valueType != CellType.NUMERIC) {
                        throw new RuntimeException("TerminationSpentLimit value needs to be a number");
                    }
                    terminationSpentLimit = (int) valueCell.getNumericCellValue();
                    break;
                default:
                    throw new RuntimeException("solver configuration key " + keyCell.getStringCellValue() + " is unknown");
            }
        }
        this.solverConfig = new SolverConfig()
                .withSolutionClass(Shiftplan.class)
                .withEntityClasses(ShiftAssignment.class)
                .withConstraintProviderClass(ShiftPlannerConstraintProvider.class)
                .withTerminationSpentLimit(Duration.ofSeconds(terminationSpentLimit));
    }

    private void loadConstraintWeights(int startIndex, int endIndex) {
        Sheet sheet = this.workbook.getSheetAt(0);
        for(int i = startIndex+1;i<endIndex;i++){
            Row row = sheet.getRow(i);
            Cell keyCell = row.getCell(0);
            Cell hardCell = row.getCell(1);
            Cell softCell = row.getCell(2);

            CellType keyType = keyCell == null ? CellType.BLANK : keyCell.getCellType();
            CellType hardType = hardCell == null ? CellType.BLANK : hardCell.getCellType();
            CellType softType = softCell == null ? CellType.BLANK : softCell.getCellType();

            if (keyType == CellType.BLANK) break; // stop early if empty key

            if (keyType != CellType.STRING) {
                throw new RuntimeException("constraint weight key needs to be a string");
            }

            if (hardType == CellType.NUMERIC && softType == CellType.NUMERIC) {
                throw new RuntimeException("either hard or soft score allowed not both");
            }

            HardSoftScore weight;
            if (hardType == CellType.NUMERIC) {
                weight = HardSoftScore.ofHard((int) hardCell.getNumericCellValue());
            } else if (softType == CellType.NUMERIC) {
                weight = HardSoftScore.ofSoft((int) softCell.getNumericCellValue());
            } else {
                throw new RuntimeException("both weights are empty");
            }
            this.constraintWeightOverridesMap.put(row.getCell(0).getStringCellValue(), weight);
        }
    }

    private void loadLabels(int startIndex) {
        Sheet sheet = this.workbook.getSheetAt(0);
        for(int i = startIndex+1;i<=sheet.getLastRowNum();i++){
            Row row = sheet.getRow(i);
            if(row.getCell(0).getCellType()==CellType.BLANK) break; //Stop early if empty cell is encountered
            if(row.getCell(0).getCellType()!=CellType.STRING && row.getCell(0).getCellType()!=CellType.BLANK) throw new RuntimeException("label needs to be a string");
            switch (row.getCell(0).getStringCellValue()){
                case "header":
                    for(Cell cell : row){
                        if(cell.getColumnIndex()==0) continue;
                        if(cell.getCellType()==CellType.BLANK) break;
                        this.headers.add(cell.getStringCellValue());
                    }
                    break;
                case "employeeSettings":
                    this.employeeSettingsKey = row.getCell(1).getStringCellValue();
                    break;
                case "minHours":
                    this.minHoursKey = row.getCell(1).getStringCellValue();
                    break;
                case "maxHours":
                    this.maxHoursKey = row.getCell(1).getStringCellValue();
                    break;
                case "consecutiveShifts":
                    this.consecutiveShiftsKey = row.getCell(1).getStringCellValue();
                    break;
                case "specialUnwantedPattern":
                    this.specialUnwantedPatternKey = row.getCell(1).getStringCellValue();
                    break;
                default:
                    throw new RuntimeException("unknown key label");

            }
        }
    }

    public Shiftplan ShiftPlanFromExcelFile() {
        return this.loadShiftPlan(this.workbook);
    }

    public void writeSolvedShiftsToCopy(Shiftplan plan) {
        //get column num of employee
        Sheet sheet = this.workbook.getSheetAt(1);
        Row header = sheet.getRow(0);
        Map<String,Integer>employeeIndices = new HashMap<>();
        for(Employee employee : plan.getEmployeeList()) {
           for(Cell cell : header) {
               if(stringCellEquals(cell,employee.getName())){
                   employeeIndices.put(employee.getName(), cell.getColumnIndex());
               }
           }
        }
        for(ShiftAssignment shiftAssignment: plan.getShiftAssignmentList()){
            int rowIndex = shiftAssignment.getShift().getTimeSlot().getId();
            int cellIndex = employeeIndices.get(shiftAssignment.getEmployee().getName());
            for(int i = this.shiftStartColumnNumber; i < this.shiftEndColumnNumber; i++){
                Cell cell = sheet.getRow(rowIndex).getCell(i);
                if(i != cellIndex){
                    cell.setBlank();
                }
            }
        }
        String outputFileName = appendSuffix(this.originalFilePath,"Solved");
        try (FileOutputStream fileOut = new FileOutputStream(outputFileName)) {
                workbook.write(fileOut);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public Shiftplan loadShiftPlan(XSSFWorkbook workbook) {
        Sheet sheet = workbook.getSheetAt(1);

        //SETTINGS
        //Todo: rmv
        /*this.headers = Arrays.asList("Tag", "Dienst", "Belegung");
        this.employeeSettingsKey = "Einstellungen";
        this.minHoursKey = "minimal";
        this.maxHoursKey = "maximal";

        Map<String, Duration> durationMap = Map.of(
                "KT", Duration.ofMinutes(210), //3.5h
                "KN", Duration.ofMinutes(450), //7,5h
                "MT", Duration.ofMinutes(270), //4,5h
                "LT", Duration.ofHours(7),
                "LN", Duration.ofHours(10)
        );*/

        //Init Lists
        List<Employee> employees;
        List<Shift> shifts = new ArrayList<>();
        List<ShiftAssignment> shiftAssignments = new ArrayList<>();
        List<AvailableShift> availableShifts = new ArrayList<>();

        //Get Employees
        Row headerRow = sheet.getRow(0);
        employees = loadEmployees(headerRow, headers);

        //Find border indices
        this.nEmployees = employees.size();
        this.shiftStartColumnNumber = headers.size();
        this.shiftEndColumnNumber = shiftStartColumnNumber + nEmployees;

        int settingsRowNum = -1;
        for (Row row : sheet){
            Cell firstCell = row.getCell(0);
            if(firstCell.getCellType()==CellType.STRING){
                if (firstCell.getStringCellValue().equals(employeeSettingsKey)) {
                    settingsRowNum = row.getRowNum();
                    break;
                }
            }
        }
        if(settingsRowNum == -1){
            throw new RuntimeException("No settings row found");
        }
        this.shiftEndRowNumber = settingsRowNum - 1;
        //Get shifts and availability
        for(int i = 1; i < shiftEndRowNumber; i++){
            Row row = sheet.getRow(i);
            //Create TimeSlot,Shift and ShiftAssignment
            String shiftType = row.getCell(1).getStringCellValue();
            Duration duration = this.shiftDurations.get(shiftType);
            if(duration==null){
                throw new RuntimeException("Shift label \""+ shiftType +"\" in row " + (i+1) + " with not found. Typo?");
            }
            TimeSlot ts = new TimeSlot(row.getRowNum(), duration, shiftType);
            Shift shift = new Shift(ts);
            ShiftAssignment shiftAssignment = new ShiftAssignment();
            shiftAssignment.setShift(shift);
            shifts.add(shift);
            shiftAssignments.add(shiftAssignment);

            for(int j = shiftStartColumnNumber; j < shiftEndColumnNumber; j++){
                if(isEmpty(row.getCell(j))){
                    continue;
                }
                Employee e = employees.get(j-headers.size());
                AvailableShift availableShift = new AvailableShift(ts,e);
                availableShifts.add(availableShift);
            }
        }

        //read settings like min max hours
        int minHoursRowNum = settingsRowNum;
        int maxHoursRowNum = settingsRowNum + 1;
        int consecutiveShiftRowNum = settingsRowNum + 3;
        int specialUnwantedPattern = settingsRowNum + 4;

        //Check keys
        Row minRow = sheet.getRow(minHoursRowNum);
        if( !minRow.getCell(shiftStartColumnNumber-1).getStringCellValue().equals(this.minHoursKey)) throw new RuntimeException("Min hours key not found");
        Row maxRow = sheet.getRow(maxHoursRowNum);
        if( !maxRow.getCell(shiftStartColumnNumber-1).getStringCellValue().equals(this.maxHoursKey)) throw new RuntimeException("Max hours key not found");
        Row consecutiveShiftsRow= sheet.getRow(consecutiveShiftRowNum);
        if( !consecutiveShiftsRow.getCell(shiftStartColumnNumber-1).getStringCellValue().equals(this.consecutiveShiftsKey)) throw new RuntimeException("Consecutive shifts key not found");
        Row specialUnwantedPatternRow = sheet.getRow(specialUnwantedPattern);
        if( !specialUnwantedPatternRow.getCell(shiftStartColumnNumber-1).getStringCellValue().equals(this.specialUnwantedPatternKey)) throw new RuntimeException("Special unwanted pattern key not found");

        for(int i = shiftStartColumnNumber; i < shiftEndColumnNumber; i++){
            int employeeIndex = i-shiftStartColumnNumber;
            long minHours=0;
            long maxHours=0;
            Cell minCell = minRow.getCell(i);
            if(minCell.getCellType()==CellType.NUMERIC){
                minHours = (long) minCell.getNumericCellValue();
            }
            else{
                throw new RuntimeException("Minimal hours has wrong cell type");
            }

            Cell maxCell = maxRow.getCell(i);
            if(maxCell.getCellType()==CellType.NUMERIC){
                maxHours = (long) maxCell.getNumericCellValue();
            }
            else{
                throw new RuntimeException("Maximal hours has wrong cell type");
            }
            MinMaxHours minMaxHours = new MinMaxHours(employees.get(employeeIndex),minHours,maxHours);
            employees.get(employeeIndex).setMinMaxHours(minMaxHours);

            Cell consecutiveShiftCell = consecutiveShiftsRow.getCell(i);
            if(consecutiveShiftCell.getCellType()==CellType.BOOLEAN){
                int nConsecutiveShifts = consecutiveShiftCell.getBooleanCellValue() ? 2 : 1;
                employees.get(employeeIndex).setMaxConsecutiveShifts(nConsecutiveShifts);
            }else{
                throw new RuntimeException("Consecutive shift field has wrong cell type");
            }

            Cell specialUnwantedPatternCell = specialUnwantedPatternRow.getCell(i);
            if(specialUnwantedPatternCell.getCellType()==CellType.BOOLEAN){
                employees.get(employeeIndex).setAvoidSpecialPatterns(specialUnwantedPatternCell.getBooleanCellValue());
            }else{
                throw new RuntimeException("Special unwanted pattern field has wrong cell type");
            }
        }



        System.out.println(" ");
        Shiftplan plan = new Shiftplan();
        plan.setAvailableShiftList(availableShifts);
        plan.setShiftAssignmentList(shiftAssignments);
        plan.setEmployeeList(employees);
        plan.setShiftList(shifts);
        plan.setUnwantedShiftCombinationList(this.unwantedPatterns);
        plan.setSpecialUnwantedShiftCombinationList(this.specialUnwantedPatterns);
        plan.setConstraintWeightOverrides(ConstraintWeightOverrides.of(this.constraintWeightOverridesMap));
        return plan;
    }

    public static List<Employee> loadEmployees(Row row, List<String>expectedHeaders) {
        if(!verifyHeaderFormat(row, expectedHeaders)){
            throw new RuntimeException("Header format is incorrect");
        }
        List<Employee> employees = new ArrayList<>();
        for (Cell cell : row) {
            if (cell.getColumnIndex() < expectedHeaders.size()){
                continue;
            }
            if (isEmpty(cell)){
                break;
            }
            if (cell.getCellType() == CellType.STRING){
                employees.add(new Employee(cell.getStringCellValue()));
            }
            else{
                throw new RuntimeException("Unexpected cell type for Employees. After Employees an empty cell need to follow!");
            }
        }
        return employees;
    }

    public static boolean verifyHeaderFormat(Row row, List<String>expectedHeaders) {
        if(row==null || row.getPhysicalNumberOfCells() < expectedHeaders.size()){
            return false;
        }
        for (int i = 0; i < expectedHeaders.size(); i++) {
            String cellValue = row.getCell(i).getStringCellValue();
            if (!expectedHeaders.get(i).equals(cellValue)) {
                return false;
            }
        }
        return true;
    }



    private static String appendSuffix(String filepath, String suffix) {
        int dot = filepath.lastIndexOf('.');
        if (dot == -1) return filepath + suffix;            // no extension
        return filepath.substring(0, dot) + suffix + filepath.substring(dot);
    }

    public SolverConfig getSolverConfig() {
        return solverConfig;
    }
}
