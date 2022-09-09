/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author Makintola
 */
@Controller
public class MainController {
    
     
    public MainController() {
         
    }
      
    @RequestMapping(value = {"/"}, method = RequestMethod.GET)
    public ModelAndView Landing() {
        ModelAndView mv;
        mv = new ModelAndView("index");  
         
        return mv;
    }
    
}

