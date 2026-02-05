package com.shiftplanner.excelIO;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

import java.time.LocalTime;

public final class ExcelHelpers {

    public static boolean isEmpty(Row row){
        for(Cell cell : row){
            if(!isEmpty(cell)){
                return false;
            }
        }
        return true;
    }
    public static boolean isEmpty(Cell cell){
        return cell == null || cell.getCellType() == CellType.BLANK;
    }

    public static boolean stringCellEquals(Cell cell, String string){
        if(cell.getCellType() == CellType.STRING){
            return string.equals(cell.getStringCellValue());
        }
        else{
            return false;
        }
    }

    public static LocalTime getTime(Cell cell){
        if(cell.getCellType() == CellType.NUMERIC){
            double numeric = cell.getNumericCellValue();
            return LocalTime.ofNanoOfDay((long)((numeric - Math.floor(numeric)) * 24 * 3600 * 1_000_000_000L));
        }
        else{
            return null;
        }
    }

}
