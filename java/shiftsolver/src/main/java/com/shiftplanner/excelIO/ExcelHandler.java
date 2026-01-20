package com.shiftplanner.excelIO;

import com.shiftplanner.domain.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

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


    //Important Indices
    private int nEmployees;
    private int shiftStartColumnNumber;
    private int shiftEndColumnNumber;
    private int shiftEndRowNumber;

    private XSSFWorkbook workbook;
    private Map<String,Duration> shiftDurations;
    private List<TimeSlotTypePattern>unwantedPatterns;

    public ExcelHandler(String filepath){
        this.originalFilePath = filepath;
        this.headers = new ArrayList<>();
        this.shiftDurations = new HashMap<>();
        this.unwantedPatterns = new ArrayList<>();
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
        int labelsSettingsIndex = -1;
        int unwantedShiftPatternsIndex = -1;
        for (Row row : sheet) {
            Cell cell = row.getCell(0);
            if(cell==null || cell.getCellType() != CellType.STRING) continue;
            switch (cell.getStringCellValue()){
                case "Shift Settings":
                    shiftSettingsIndex = row.getRowNum();
                    break;
                case "Unwanted Shift Patterns":
                    unwantedShiftPatternsIndex = row.getRowNum();
                case "Labels":
                    labelsSettingsIndex = row.getRowNum();
                    break;
                default:
                    continue;

            }
        }
        loadShiftSettings(shiftSettingsIndex,unwantedShiftPatternsIndex);
        loadUnwantedShiftPatterns(unwantedShiftPatternsIndex,labelsSettingsIndex);
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

    private void loadUnwantedShiftPatterns(int startIndex, int endIndex) {
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
            this.unwantedPatterns.add(new TimeSlotTypePattern(types));
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
        int consecutiveShiftRowNum = settingsRowNum +3;
        if(!verifySettingsRowFormat(sheet,minHoursKey,maxHoursKey,minHoursRowNum,maxHoursRowNum,shiftStartColumnNumber)){
            throw new RuntimeException("Can not find min or max hours settings");
        }
        Row minRow = sheet.getRow(minHoursRowNum);
        Row maxRow = sheet.getRow(maxHoursRowNum);
        Row consecutiveShiftsRow= sheet.getRow(consecutiveShiftRowNum);
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
        }



        System.out.println(" ");
        Shiftplan plan = new Shiftplan();
        plan.setAvailableShiftList(availableShifts);
        plan.setShiftAssignmentList(shiftAssignments);
        plan.setEmployeeList(employees);
        plan.setShiftList(shifts);
        plan.setUnwantedShiftCombinationList(this.unwantedPatterns);

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

    public static boolean verifySettingsRowFormat(Sheet sheet, String minHoursKey, String maxHoursKey, int minimalRowNum, int maximalRowNum, int shiftStartColumnNumber ) {
        int keyColumnNumber = shiftStartColumnNumber - 1;
        if(!stringCellEquals(sheet.getRow(minimalRowNum).getCell(keyColumnNumber), minHoursKey)){
            return false;
        }
        if(!stringCellEquals(sheet.getRow(maximalRowNum).getCell(keyColumnNumber), maxHoursKey)){
            return false;
        }
        return true;
    }

    private static String appendSuffix(String filepath, String suffix) {
        int dot = filepath.lastIndexOf('.');
        if (dot == -1) return filepath + suffix;            // no extension
        return filepath.substring(0, dot) + suffix + filepath.substring(dot);
    }
}
