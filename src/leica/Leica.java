/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leica;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import model.ExcelOperation;
import org.apache.poi.ss.usermodel.ComparisonOperator;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import panel.ALPS;
import panel.FoodSen84;
import panel.FoodSen96;
import panel.LeicaPanel;

/**
 *
 * @author Wei Wang
 */
public class Leica {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
       String pillarId = "FS8480010010000011";
       String path = "C:\\Users\\Wei Wang\\Desktop\\new food sensitivity\\output\\";



//       LeicaPanel test = new ALPS(pillarId);
//       LeicaPanel test = new FoodSen96(pillarId);
       LeicaPanel test = new FoodSen84(pillarId); 



       
       


       Map<String, String> loc2JunMap = test.getLoc2JunMap();
       Map<String , String> new2oldJunMap = test.getNew2OldMap();
       
       Map<String, Map<String, Integer>> rawGMap = test.getRawMap("GREEN");
       Map<String , Map<String , Float>> dupUnitGMap = test.getDupUnitMap("GREEN", new2oldJunMap, loc2JunMap.values());
       Map<String, Map<String, Float>> unitGMap = test.getUnitMap("GREEN", rawGMap);
       
       Map<String, Map<String, Integer>> rawAMap = test.getRawMap("RED");
       Map<String , Map<String , Float>> dupUnitAMap = test.getDupUnitMap("RED", new2oldJunMap, loc2JunMap.values());
       Map<String, Map<String, Float>> unitAMap = test.getUnitMap("RED", rawAMap);
       
       
       exportExcel(new ArrayList(Arrays.asList(rawGMap , rawAMap)) ,
                    new ArrayList(Arrays.asList(unitGMap , unitAMap)) ,
                    new ArrayList(Arrays.asList(dupUnitGMap , dupUnitAMap)),
                    loc2JunMap ,
                    new2oldJunMap,
                    path + pillarId
                    );
       
       

       
    }
     
    
    private static void exportExcel(List<Map<String, Map<String, Integer>>> rawMapList ,
                                List<Map<String, Map<String, Float>>> unitMapList , 
                                List<Map<String , Map<String , Float>>> dupUnitMapList,
                                Map<String, String> loc2JunMap ,
                                Map<String , String> new2oldJunMap ,
                                String path
                                ) throws IOException, Exception {
        
        Workbook wb = ExcelOperation.getWriteConnection(ExcelOperation.ExcelType.XLSX);        
        // rawData generation
        for(int i = 0 ; i < rawMapList.size() ; i++ ){
            String rawSheetName = i == 0 ? "IGGRaw" : "IGARAW";
            Sheet sheetRaw = wb.createSheet(rawSheetName);

            Map<String, Map<String, Integer>> rawMap = rawMapList.get(i);      
            int curRowCt = 0;
            int curColCt = 0;
            Row rowLoc = sheetRaw.createRow(curRowCt++);
            rowLoc.createCell(curColCt).setCellValue("Location");
            Row rowJun = sheetRaw.createRow(curRowCt++);
            rowJun.createCell(curColCt++).setCellValue("JulienBarcode");
            
            
            
            
            Set<String> testCodeSet = rawMap.get(rawMap.keySet().toArray()[0]).keySet();
            for(String testCode : testCodeSet){
                sheetRaw.createRow(curRowCt++).createCell(0).setCellValue(testCode);
            }
            
            
            for(String location : rawMap.keySet()){
                curRowCt = 2;
                
                rowLoc.createCell(curColCt).setCellValue(location);
                rowJun.createCell(curColCt).setCellValue(loc2JunMap.get(location));
                
                for(String testCode : rawMap.get(location).keySet()){
                    sheetRaw.getRow(curRowCt++).createCell(curColCt).setCellValue(rawMap.get(location).get(testCode));
                }
                ++curColCt;
            } 
        }
        
        
        //unit generation
        
        for(int i = 0 ; i < unitMapList.size() ; i++ ){

            String unitSheetName = i == 0 ? "IGG" : "IGA";
            Sheet sheetUnit = wb.createSheet(unitSheetName);
            
            Map<String, Map<String, Float>> unitMap =  unitMapList.get(i);
            Map<String , Map<String , Float>> dupUnitMap = dupUnitMapList.get(i);
            
            int curRowCt = 0;
            int curColCt = 0;
            
            
            Row rowCfRow = sheetUnit.createRow(curRowCt++);
            rowCfRow.createCell(curColCt).setCellValue("cf");
            Row rowLoc = sheetUnit.createRow(curRowCt++);
            rowLoc.createCell(curColCt).setCellValue("Location");
            Row rowJun = sheetUnit.createRow(curRowCt++);
            rowJun.createCell(curColCt++).setCellValue("JulienBarcode");
            
            Set<String> testCodeSet = unitMap.get(unitMap.keySet().toArray()[0]).keySet();
            for(String testCode : testCodeSet){
                sheetUnit.createRow(curRowCt++).createCell(0).setCellValue(testCode);
            }
            ++curColCt;
            
            //init row cf
            curRowCt = 2;
            while(sheetUnit.getRow(curRowCt)!= null) sheetUnit.getRow(curRowCt++).createCell(1).setCellValue(1);
    
            
            for(String location : unitMap.keySet()){
                curRowCt = 3;
                rowCfRow.createCell(curColCt).setCellValue(1);
                rowLoc.createCell(curColCt).setCellValue(location);
                String newJun = loc2JunMap.get(location);
                rowJun.createCell(curColCt).setCellValue(newJun);
                for(String testCode : unitMap.get(location).keySet()){
                    double unit = unitMap.get(location).get(testCode);
                    String cellLoc = ExcelOperation.transferIntgerToString(curColCt + 1);
                    sheetUnit.getRow(curRowCt++).createCell(curColCt).setCellFormula(  cellLoc + 1 + "*" + unit + "*B" +  + curRowCt );
                }
                ++curColCt;
                
                //insert dup data
                if(dupUnitMap.containsKey(newJun)){
                    curRowCt = 3;
                    rowLoc.createCell(curColCt).setCellValue("Duplicate");
                    String oldJun = new2oldJunMap.get(newJun);
                    rowJun.createCell(curColCt).setCellValue(oldJun);
                    
                    
                    while(sheetUnit.getRow(curRowCt) != null){
                        String testCode = sheetUnit.getRow(curRowCt).getCell(0).getStringCellValue();
                        if(dupUnitMap.get(newJun).containsKey(testCode)){
                            sheetUnit.getRow(curRowCt).createCell(curColCt).setCellValue(dupUnitMap.get(newJun).get(testCode));
                        }
                        ++curRowCt;
                    }
                    ++curColCt;
                }
                
            }
            
            //color the range
            String range = "C4:" + ExcelOperation.transferIntgerToString(curColCt) + curRowCt;
            
            ExcelOperation.setConditionalFormatting(sheetUnit, IndexedColors.GREEN, ComparisonOperator.LT, new String[]{"10"}, range);
            ExcelOperation.setConditionalFormatting(sheetUnit, IndexedColors.YELLOW, ComparisonOperator.BETWEEN, new String[]{"10" , "20"}, range);
            ExcelOperation.setConditionalFormatting(sheetUnit, IndexedColors.RED, ComparisonOperator.GT, new String[]{"20"}, range);
            
            
        }
        
        XSSFFormulaEvaluator.evaluateAllFormulaCells(wb);
        ExcelOperation.writeExcel(path + ".xlsx", wb);

    }
    
    

    
}
