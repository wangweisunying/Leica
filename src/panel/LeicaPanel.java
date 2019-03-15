/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package panel;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Wei Wang
 */
public abstract class LeicaPanel {
    protected String testName , tableName , pillarId;
    protected Map<Integer , String> index2TestCodeMap;
    protected Map<String , double[]> testCode2IGGEquationMap;
    protected Map<String , double[]> testCode2IGAEquationMap; 
    
    public LeicaPanel(String pillarId){
        index2TestCodeMap = new LinkedHashMap();
        testCode2IGGEquationMap = new LinkedHashMap();
        testCode2IGAEquationMap = new LinkedHashMap();
        this.pillarId = pillarId;
    }
    
    public abstract Map<String , Map<String , Integer>> getRawMap(String chanel) throws Exception;  // <Location , <testCode , rawsignal>> 
    
    public abstract Map<String , Map<String , Float>> getUnitMap(String chanel , Map<String, Map<String, Integer>> rawMap);   // <Location , <testCode , unitresult>> 
    
    public abstract Map<String , String> getLoc2JunMap() throws Exception; // <Location , Julienbarcode>
    
    public abstract Map<String , Map<String , Float>> getDupUnitMap(String chanel ,Map<String , String> dupMap ,Collection<String> julienCollection) throws Exception;   // <newJun , <testCode , unitresult>> 
    
    public abstract Map<String , String> getNew2OldMap() throws Exception;
    
}

