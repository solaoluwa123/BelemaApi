/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.util;

/**
 *
 * @author Makintola
 */
public class ResponseCodeInterpreter {
    public String InterpreteCode(String code) {
        switch(code) {
            case "00":
                return "Completed successfully";
            case "01":
                return "Status unknown";
            case "03":
                return "Invalid Sender";
            case "05":
                return "Do not honor";
            case "06": 
                return "Dormant Account";
            case "07":
                return "Invalid Account";
            case "08":
                return "Account Name Mismatch";
            case "09":
                return "Request processing in progress";
            case "12":
                return "Invalid transaction";
            case "13":
                return "Invalid amount";
            case "14":
                return "Invalid Batch Number";
            case "15":
                return "Invalid Session or Record ID";
            case "16":
                return "Unknown Bank Code";
            case "17":
                return "Invalid Channel";
            case "18":
                return "Wrong Method Call";
            case "21":
                return "No action taken";
            case "25":
                return "Unable to locate record";
            case "26":
                return "Duplicate record";
            case "30":
                return "Format error";
            case "34":
                return "Suspected fraud";
            case "35":
                return "Contact sending bank";
            case "51":
                return "No sufficient funds";
            case "57":
                return "Transaction not permitted to sender";
            case "58":
                return "Transaction not permitted on channel";
            case "61":
                return "Transfer limit Exceeded";
            case "63":
                return "Security violation";
            case "65":
                return "Exceeds withdrawal frequency";
            case "68":
                return "Response received too late";
            case "69":
                return "Unsuccessful Account/Amount block";
            case "70":
                return "Unsuccessful Account/Amount unblock";
            case "71":
                return "Empty Mandate Reference Number";
            case "91":
                return "Beneficiary Bank not available";
            case "92":
                return "Routing error";
            case "94":
                return "Duplicate transaction";
            case "96":
                return "System malfunction";
            case "97":
                return "Timeout waiting for response from destination";
            case "98":
                return "Invalid Bank Code";
            default:
                return "Failed";
        }
    }
}