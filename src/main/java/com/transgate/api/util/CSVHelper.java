/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Makintola
 */
public class CSVHelper {
    
    Validators validators = new Validators();
    public final String WriteFile(
            String fileName,
            String[] headers,
            List<String[]> data
    ) {
        FileWriter writer = null;
        try {
            String location = "";
            String csvFilePath = "C:\\Supersoft\\accepteddisputereports\\"+validators.RemoveSpecialCharacters(fileName)+".csv";
            File csvFile = new File(csvFilePath);
            if (!csvFile.exists()) {
                csvFile.createNewFile();
            }   writer = new FileWriter(csvFilePath);
            // Write the header
            for (int i = 0; i < headers.length; i++) {
                writer.append(headers[i]);
                if (i < headers.length - 1) {
                    writer.append(',');
                }
            }   writer.append('\n');
            // Write the data
            for (String[] row : data) {
                for (int i = 0; i < row.length; i++) {
                    writer.append(row[i]);
                    if (i < row.length - 1) {
                        writer.append(',');
                    }
                }
                writer.append('\n');
            }
            return csvFilePath;
        } catch (IOException ex) {
            Logger.getLogger(CSVHelper.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                writer.close();
            } catch (IOException ex) {
                Logger.getLogger(CSVHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return "";
    }
}
