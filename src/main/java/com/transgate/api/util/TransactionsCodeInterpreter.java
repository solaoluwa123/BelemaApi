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
public class TransactionsCodeInterpreter {
    public String GetResponse(String code) {
        String response;
        switch(code){
            case "00":
                response = "Approved or completed successfully";
                break;
            case "01":
                response = "Refer to card issuer";
                break;
              case "02":
                response = "Refer to card issuer, special condition";
                break;

            case "03":
                response = "Invalid merchant";
                break;
                case "04":
                response = "Pick-up card";
                break;
                case "05":
                response = "Do not honor";
                break;
            case "06":
                response = "Error";
                break;
                case "07":
                response = "Pick-up card, special condition";
                break;
            case "08":
                response = "Honor with identification";
                break;
            case "09":
                response = "Request in progress";
                break;
            case "10":
                response = " Approved, partial";
                break;
            case "11":
                response = "Approved, VIP";
                break;
            case "12":
                response = "Invalid transaction";
                break;
            case "13":
                response = "Invalid amount";
                break;
            case "14":
                response = " Invalid card number";
                break;
            case "15":
                response = "No such issuer";
                break;
            case "16":
                response = "Approved, update track 3";
                break;
            case "17":
                response = "Customer cancellation";
                break;
            case "18":
                response = "Customer dispute";
                break;
            case "19":
                response = "Re-enter transaction";
                break;
            case "20":
                response = "Invalid response";
                break;
            case "21":
                response = "TransNo action taken";
                break;
             case "22":
                response = "Suspected malfunction";
                break;
            case "23":
                response = "Unacceptable transaction fee";
                break;
            case "24":
                response = "File update not supported";
                break;
            case "25":
                response = "Unable to locate record";
                break;
            case "26":
                response = "Duplicate record";
                break;
            case "27":
                response = "File update edit error";
                break;
            case "28":
                response = "File update file locked";
                break;
                case "29":
                response = " File update failed";
                break;
            case "30":
                response = " Format error";
                break;
            case "31":
                response = "Bank not supported";
                break;
            case "32":
                response = "Completed partially";
                break;
            case "33":
                response = "Expired card, pick-up";
                break;
            case "34":
                response = " Suspected fraud, pick-up";
                break;
           case "35":
                response = "Contact acquirer, pick-up";
                break;
            case "36":
                response = "Restricted card, pick-up";
                break;
            case "37":
                response = "Call acquirer security, pick-up";
                break;
            case "38":
                response = "PIN tries exceeded, pick-up";
                break;
                case "39":
                response = " No credit account";
                break;
            case "40":
                response = " Function not supported";
                break;
            case "41":
                response = "Lost card";
                break;
            case "42":
                response = "No universal account";
                break;
            case "43":
                response = "Stolen card";
                break;
            case "44":
                response = " No investment account";
                break;
            case "51":
                response = "Not sufficient funds";
                break;
            case "52":
                response = "No check account";
                break;
                case "53":
                response = " No savings account";
                break;
            case "54":
                response = " Expired card";
                break;
            case "55":
                response = "Incorrect PIN";
                break;
            case "56":
                response = "No card record";
                break;
            case "57":
                response = "Transaction not permitted to cardholder";
                break;
            case "58":
                response = "Transaction not permitted on terminal";
                break;
            case "59":
                response = "Suspected fraud";
                break;
            case "60":
                response = "Contact acquirer";
                break;
            case "61":
                response = "Exceeds withdrawal limit";
                break;
            case "62":
                response = "Restricted card";
                break;
            case "63":
                response = "Security";
                break;
            case "64":
                response = "Original amount incorrect";
                break;
            case "65":
                response = "Exceeds withdrawal frequency";
                break;
            case "66":
                response = "Call acquirer security";
                break;
            case "67":
                response = "Hard capture";
                break;
            case "68":
                response = "Response received too late";
                break;

            case "75":
                response = "PIN tries exceeded";
                break;
            case "77":
                response = "Intervene, bank approval required";
                break;
            case "78":
                response = "Intervene, bank approval required for partial amount";
                break;

            case "90":
                response = "Cut-off in progress";
                break;
            case "91":
                response = "Issuer or switch inoperative";
                break;
            case "92":
                response = "Routing error";
                break;
            case "93":
                response = "Violation of law";
                break;
            case "94":
                response = "Duplicate transaction";
                break;
            case "95":
                response = "Reconcile error";
                break;
            case "96":
                response = "System malfunction";
                break;

            case "98":
                response = "Exceed cash limit";
                break;


            default: 
                response = "Error Occured";

        }
        return response;
    }
}
