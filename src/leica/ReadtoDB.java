/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leica;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import model.DataBaseCon;
import model.ExcelOperation;
import model.V7DataBaseCon;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import panel.ALPS;
import panel.FoodSen84;
import panel.FoodSen96;

/**
 *
 * @author Wei Wang
 */
public class ReadtoDB {
    public static void main(String args[]) throws IOException, SQLException{
        String pillarId = "FOOG80120010000167";
        String path = "C:\\Users\\Wei Wang\\Desktop\\new food sensitivity\\output\\";
        
        
        FoodSen96.insertDB(pillarId , path + pillarId + ".xlsx");
//        FoodSen84.insertDB(pillarId , path + pillarId + ".xlsx");
//        ALPS.insertDB(pillarId , path + pillarId + ".xlsx");
    }
    
    
    
}
