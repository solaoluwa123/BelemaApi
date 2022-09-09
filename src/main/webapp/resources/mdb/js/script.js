/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */



localStorage['theme'];

$(document).ready(function(){
  $(document).bind("contextmenu",function(e){
  return false;
  });
});


$(window).on('keydown',function(event)
    {
    if(event.ctrlKey && event.shiftKey && event.keyCode==73)
    {
        return false;  //Prevent from ctrl+shift+i
    }
    else if(event.ctrlKey && event.keyCode==73)
    {
        return false;  //Prevent from ctrl+shift+i
    }
});

$(document).ready(function () {
    var url = window.location.href;
    url = url.split("/");
    var path = url[url.length - 1];
//    $(".side_links #"+path+" a").addClass("active");

    // SideNav Button Initialization
    $(".button-collapse").sideNav();
    // SideNav Scrollbar Initialization
//    var sideNavScrollbar = document.querySelector('.custom-scrollbar');
//    var ps = new PerfectScrollbar(sideNavScrollbar);
    $('.mdb-select').materialSelect();

    var date = new Date();
    $("#span_year").html(date.getFullYear());

});


// Tooltips Initialization
$(function () {
      $('[data-toggle="tooltip"]').tooltip();
});
