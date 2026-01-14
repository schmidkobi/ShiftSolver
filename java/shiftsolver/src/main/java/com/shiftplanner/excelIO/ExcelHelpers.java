package com.shiftplanner.excelIO;

import org.apache.poi.ss.usermodel.*;

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

}
