$(document).ready(function () {
    $("#signin-form").submit(function (event) {
        event.preventDefault();
        fire_ajax_submit();
    });
});

function fire_ajax_submit() {
	var pw = CryptoJS.AES.encrypt($("#userPw").val(), "Secret Passphrase");
	var encPw = encodeURIComponent(pw);
	
    $.ajax({
        type: "GET",
        url: "/user/signin/" + $("#userId").val(),
        data: "pw=" + encPw,
        cache: false,
        timeout: 600000,
        success: function (data) {
        	if(data == true) {
	        	alert("로그인이 완료되었습니다.");
        		location.href = "/";
        	} else {
        		alert("로그인에 실패하였습니다.\n사용자 정보를 확인하여 주시기 바랍니다.");
        	}
        },
        error: function (e) {
        	alert("GET /user/signin/ fail");
        }
    });
}