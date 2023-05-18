/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.interfaces;

import org.springframework.http.ResponseEntity;

/**
 *
 * @author roqeeb
 */
public interface CardChartInterface {


    public ResponseEntity GetSuccessTNXVolume(String startDate, String endDate);

    public ResponseEntity GetSuccessTNXVolume(String institutioncode, String midDate, String endDate);

    public ResponseEntity GetTop6ResponseCodesTNX(String startDate, String endDate, boolean isCurrent);

    public ResponseEntity GetTop6ResponseCodesTNX(String institutioncode, String startDate, String endDate, boolean isCurrent);

    public ResponseEntity GetTransactionsVolumeByChannels(String startDate, String endDate, boolean isCurrent);

    public ResponseEntity GetTransactionsVolumeByChannels(String institutioncode, String startDate, String endDate, boolean isCurrent);

}
