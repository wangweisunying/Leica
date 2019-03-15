/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package panel;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import model.DataBaseCon;
import model.ExcelOperation;
import model.LXDataBaseCon;
import model.V7DataBaseCon;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/**
 *
 * @author Wei Wang
 */
public class ALPS extends LeicaPanel {

    public ALPS(String pillarId) throws IOException {
        super(pillarId);
        testName = "ALPS";
        index2TestCodeMap.put(0, "ALPS");
        initEquation();

    }

    private void initEquation() throws IOException {
        Workbook wb = ExcelOperation.getReadConnection("Y:\\ALPS.xlsx", ExcelOperation.ExcelType.XLSX);
        Sheet sheetG = wb.getSheet("IGG");
        Sheet sheetA = wb.getSheet("IGA");

        int rowCt = 1;
        while (sheetG.getRow(rowCt) != null) {
            Row row = sheetG.getRow(rowCt++);
            int index = (int) row.getCell(0).getNumericCellValue();
            testCode2IGGEquationMap.put(index2TestCodeMap.get(index), new double[]{row.getCell(1).getNumericCellValue(), row.getCell(2).getNumericCellValue()});
        }

        rowCt = 1;
        while (sheetA.getRow(rowCt) != null) {
            Row row = sheetA.getRow(rowCt++);
            int index = (int) row.getCell(0).getNumericCellValue();
            testCode2IGAEquationMap.put(index2TestCodeMap.get(index), new double[]{row.getCell(1).getNumericCellValue(), row.getCell(2).getNumericCellValue()});
        }

    }

    // <Location , <testCode , unitresult>> 
    @Override
    public Map<String, Map<String, Float>> getUnitMap(String chanel, Map<String, Map<String, Integer>> rawMap) {
        Map<String, double[]> testCode2EquationMap = chanel.equals("GREEN") ? testCode2IGGEquationMap : testCode2IGAEquationMap;
        
        int sufixLen = chanel.equals("GREEN") ? 12 : 8;
        Map<String, Map<String, Float>> map = new LinkedHashMap();
        Random rand = new Random();
        for (String location : rawMap.keySet()) {
            for (String testCode : rawMap.get(location).keySet()) {
                String tmpTest = testCode.substring(0, testCode.length() - sufixLen);
                double[] equation = testCode2EquationMap.get(tmpTest);
                float unit = (float) (rawMap.get(location).get(testCode) * equation[0] + equation[1]);
                unit = unit <= 0 ? (rand.nextInt(8) + 1) : unit;
                unit = unit > 30 ? 20 + rand.nextInt(10) : unit;
                map.computeIfAbsent(location, x -> new LinkedHashMap()).put(testCode, unit);
            }
        }
        return map;
    }

    //<Location , <testCode , rawsignal>> 
    @Override
    public Map<String, Map<String, Integer>> getRawMap(String chanel) throws Exception {
        Map<String, Map<String, Integer>> map = new LinkedHashMap();
        DataBaseCon db = new V7DataBaseCon();

        String surfix = chanel.toUpperCase().equals("GREEN") ? "IGG_IGM_unit" : "IGA_unit";
        String sql = "SELECT concat(row ,col) , `index` , `signal` FROM `vibrant_test_raw_data`.`alps` where  pillar_plate_id = '" + pillarId + "' and  channel = '" + chanel + "' and `index` not in (0 , 9 , 90 , 99)  order by  col , `row` , `index`;";
        System.out.println(sql);
        ResultSet rs = db.read(sql);
        while (rs.next()) {
            map.computeIfAbsent(rs.getString(1), x -> new LinkedHashMap()).put(index2TestCodeMap.get(rs.getInt(2)) + surfix, rs.getInt(3));
        }
        if(map.isEmpty()) throw new Exception("pillar plate is not stitched ! stith it first !");
        
        db.close();
        return map;
    }

    @Override
    public Map<String, String> getLoc2JunMap() throws Exception {
        DataBaseCon db = new V7DataBaseCon();
        String sql = "select well_plate_id from vibrant_test_tracking.pillar_plate_info where `status` = 'finish_stitching' and  pillar_plate_id = '" + pillarId + "';";
        ResultSet rs = db.read(sql);
        rs.last();
        if (rs.getRow() != 1) {
            throw new Exception("the wellPlateId linked to this pillar Plate id : " + pillarId + " is not unique , please check related database!!!");
        }
        String wellId = "";
        rs.beforeFirst();
        while (rs.next()) {
            wellId = rs.getString(1);
        }

        Map<String, String> map = new HashMap();
        String sql2 = "select concat(well_row , well_col) , julien_barcode from vibrant_test_tracking.well_info where well_plate_id ='" + wellId + "';";
        ResultSet rs2 = db.read(sql2);
        while (rs2.next()) {
            map.put(rs2.getString(1), rs2.getString(2));
        }
        db.close();
        return map;
    }

    @Override
    public Map<String, String> getNew2OldMap() throws Exception {
        Map<String, String> map = new HashMap();
        DataBaseCon db = new LXDataBaseCon();

        String sql = "SELECT \n" +
"    SUBSTRING_INDEX(GROUP_CONCAT(sd.julien_barcode\n" +
"                ORDER BY sd.julien_barcode DESC),\n" +
"            ',',\n" +
"            2) AS julien_barcode\n" +
"FROM\n" +
"    vibrant_america_information.`patient_details` pd\n" +
"        JOIN\n" +
"    vibrant_america_information.`sample_data` sd ON sd.`patient_id` = pd.`patient_id`\n" +
"        JOIN\n" +
"    vibrant_america_information.selected_test_list slt ON slt.sample_id = sd.sample_id\n" +
"WHERE\n" +
"    slt.Order_Wellness_Panel2 != 0\n" +
"        AND sd.julien_barcode > 1801010000\n" +
"GROUP BY pd.`patient_id`\n" +
"HAVING COUNT(*) > 1\n" +
"ORDER BY julien_barcode DESC;";
        ResultSet rs = db.read(sql);
        while (rs.next()) {
            String[] junArr = rs.getString(1).split(",");
            map.put(junArr[0], junArr[1]);
        }

        db.close();
        return map;
    }

    @Override// <newJun , <testCode , unitresult>>
    public Map<String, Map<String, Float>> getDupUnitMap(String chanel, Map<String, String> new2oldJunMap, Collection<String> julienCollection) throws Exception {
        Map<String, Map<String, Float>> map = new HashMap();

        List<String> list = new ArrayList();
        Map<String, String> old2NewMap = new HashMap();
        for (String newJun : new2oldJunMap.keySet()) {
            old2NewMap.put(new2oldJunMap.get(newJun), newJun);
        }
        for (String julien : julienCollection) {
            if (new2oldJunMap.containsKey(julien)) {
                list.add(new2oldJunMap.get(julien));
            }
        }
        if(list.isEmpty()) return map;
        String type = chanel.toUpperCase().equals("GREEN") ? "IGG_IGM_unit" : "IGA_unit";
        DataBaseCon db = new V7DataBaseCon();
        String sqlnew = "select julien_barcode , test_name , unit from tsp_test_unit_data.test_unit_data where pillar_plate_id like 'ALP%' and test_name like '%"+ type +"%' and julien_barcode in (" + getSampleSql(list).toString() + ");";
        
        ResultSet rsNew = db.read(sqlnew);
        while(rsNew.next()){
            map.computeIfAbsent(old2NewMap.get(rsNew.getString(1)), x -> new HashMap()).put(rsNew.getString(2), rsNew.getFloat(3));
        }

        db.close();
        return map;
    }

    private StringBuilder getSampleSql(List<String> sampleIdList) throws Exception {
        StringBuilder sbSampleId = new StringBuilder();
        if (sampleIdList == null || sampleIdList.size() == 0) {
            throw new Exception("sampleIdList can not be empty!");
        }
        for (String sampleId : sampleIdList) {
            sbSampleId.append("'").append(sampleId).append("',");
        }
        sbSampleId.setLength(sbSampleId.length() - 1);
        return sbSampleId;
    }
    
    
        public static void insertDB(String pillarId, String path) throws IOException, SQLException {
        Workbook wb = ExcelOperation.getReadConnection(path, ExcelOperation.ExcelType.XLSX);
        List<Sheet> list = new ArrayList(Arrays.asList(wb.getSheet("IGG"), wb.getSheet("IGA")));
        DataBaseCon db = new V7DataBaseCon();

        Random rand = new Random();
        for (int i = 0; i < list.size(); i++) {
            Sheet sheet = list.get(i);
            String type = i == 0 ? "IGG_IGM" : "IGA";
            int curRowCt = 3;
            int curColCt = 2;
            Row locationRow = sheet.getRow(1);
            Row julienRow = sheet.getRow(2);
            while (locationRow.getCell(curColCt) != null) {
                curRowCt = 3;
                
                String location = locationRow.getCell(curColCt).getStringCellValue();
                
                if ( julienRow.getCell(curColCt) == null || !Character.isDigit(julienRow.getCell(curColCt).getStringCellValue().charAt(0))||location.startsWith("Dup")) {
                    ++curColCt;
                    continue;
                }
                String julien = julienRow.getCell(curColCt).getStringCellValue();
                while (sheet.getRow(curRowCt) != null) {
                    String testCode = sheet.getRow(curRowCt).getCell(0).getStringCellValue();
                    double unit = sheet.getRow(curRowCt++).getCell(curColCt).getNumericCellValue();
                    String sql = "insert into tsp_test_unit_data.test_unit_data(test_name , julien_barcode , raw_unit , unit ,pillar_plate_id , row , col) values ('" + testCode + "' , '" + julien + "', '-1' ,'"+ unit + "' , '" + pillarId + "' , '" + location.substring(0, 1) + "' ," + location.substring(1) + " ) on duplicate key update unit = '"+ unit+"';";
                    System.out.println(sql);
                    db.write(sql);
                }
                ++curColCt;
            }

            db.write("insert into `tsp_test_qc_data`.`test_qc_data`(test_name,pillar_plate_id,cal_1,cal_2,cal_3,cal_4,pos_ctrl_1,neg_ctrl_1,time) values ('ALPS"+ type +"','" + pillarId + "','100','50','25','12.5','62.5','1.5',now());");
        }
        
        db.write("UPDATE `vibrant_test_tracking`.`pillar_plate_info` SET `status`='finish' WHERE `pillar_plate_id`='" + pillarId + "';");
        db.close();
    }


}
