$(document).ready(function () {
    $("#signup-form").submit(function (event) {
        event.preventDefault();
        fire_ajax_submit();
    });
});

function fire_ajax_submit() {
	var pw = CryptoJS.AES.encrypt($("#userPw").val(), "Secret Passphrase");
	var encPw = encodeURIComponent(pw);

    $.ajax({
        type: "POST",
        url: "/user/" + $("#userId").val(),
        data: "pw=" + encPw,
        cache: false,
        timeout: 600000,
        success: function (data) {
        	alert("사용자 생성이 완료되었습니다.");
        	
        	location.href = "/";
        },
        error: function (e) {
        	alert("POST /user fail");
        }
    });
}