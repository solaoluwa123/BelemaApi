function validateEmail(email) {
    let re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    return re.test(String(email).toLowerCase());
}

function validatePassword(pw) {
    let re = /[A-Za-z]/;
    let num = /[0-9]/;			//alphabeths and numbers alone
    // /[A-Z]/ uppercase alone
    //	/[a-z]/ lowercase only
    // /[0-9]/ numbers only
    // /[^A-Za-z0-9]/ special characters only
    return re.test(pw) && num.test(pw) && pw.length > 7;
}

function validatePhone(phone) {
    let re = /[0-9]/;
    return re.test(phone) && phone.length > 8;
}

function getUrlParameter(sParam) {
    let sPageURL = window.location.search.substring(1),
    sURLVariables = sPageURL.split('&'),
    sParameterName,
    i;
    for (i = 0; i < sURLVariables.length; i++) {
        sParameterName = sURLVariables[i].split('=');
        if (sParameterName[0] === sParam) {
            return sParameterName[1] === undefined ? true : decodeURIComponent(sParameterName[1]);
        }
    }
};

function strToMin(str, separator) {
    let a = str.split(separator); // split it at the separator
    let mins = (+a[0]) * 60 + (+a[1]);
    return mins;
}

function getTime() {
    let date = new Date();
    let time = date.getHours() + "-" + date.getMinutes() + "-" + date.getSeconds();
    return time;
}

function padZero(n) {
    return (n < 10 ? "0" + n : n);
}

function showExamCountdown(mins, id) {
    let secs = 60 * mins;
    let display = document.getElementById(id);
    let timer = secs, minutes, seconds;
    let interval = setInterval(function () {
        minutes = parseInt(timer / 60, 10);
        seconds = parseInt(timer % 60, 10);
        display.textContent = padZero(minutes) + "m:" + padZero(seconds) + "s";
        if (--timer < 0 ) {
            clearInterval(interval);
        }
    }, 1000);
}

function showExamCountdownH(mins, id) {
    let secs = parseInt(mins) * 60;
    let display = document.getElementById(id);
    let interval = setInterval(function () {
        let days = Math.floor(secs/24/60/60);
        let hoursLeft = Math.floor((secs) - (days*86400));
        let hours = Math.floor(hoursLeft/3600);
        let minutesLeft = Math.floor((hoursLeft) - (hours*3600));
        let minutes = Math.floor(minutesLeft/60);
        let remainingSeconds = secs % 60;
        display.innerHTML = padZero(hours) + "h:" + padZero(minutes) + "m:" + padZero(remainingSeconds) + "s";
        if (--secs < 0) {
            clearInterval(interval);
            display.innerHTML = 'Start';
            $("#"+id).removeClass("no-cursor");
            $("#"+id).attr("href", $("#"+id).data("href"));
        }
    }, 1000);
}

function imgToBase64(url, callback) {
    let xhr = new XMLHttpRequest();
    xhr.onload = function() {
        let reader = new FileReader();
        reader.onloadend = function() {
            callback(reader.result);
        };
        reader.readAsDataURL(xhr.response);
    };
    xhr.open('GET', url);
    xhr.responseType = 'blob';
    xhr.send();
}

function imgToCanvaToBase64(src, callback, outputFormat) {
    let img = new Image();
    img.crossOrigin = 'Anonymous';
    img.onload = function() {
        let canvas = document.createElement('CANVAS');
        let ctx = canvas.getContext('2d');
        let dataURL;
        canvas.height = this.naturalHeight;
        canvas.width = this.naturalWidth;
        ctx.drawImage(this, 0, 0);
        dataURL = canvas.toDataURL(outputFormat);
        callback(dataURL);
    };
    img.src = src;
    if (img.complete || img.complete === undefined) {
        img.src = "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==";
        img.src = src;
    }
}

let calendarMonths = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov", "Dec"];
let weekDays = ["Sun", "Mon", "Tues", "Wed", "Thur", "Fri", "Sat"];
let printCounter = 0;

$(".page_reload_modal").click(function(){
    window.location.reload();
});

function isInt(x) {
    return !isNaN(x);
}

function formatAMPM(date) {
    let hours = date.getHours();
    let minutes = date.getMinutes();
    let ampm = hours >= 12 ? 'pm' : 'am';
    hours = hours % 12;
    hours = hours ? hours : 12; // the hour '0' should be '12'
    minutes = minutes < 10 ? '0'+minutes : minutes;
    let strTime = hours + ':' + minutes + ampm;
    return strTime;
}
$("#modal_select_view").on('hidden.bs.modal', function(){
    $("#form_add_results").hide();
});
