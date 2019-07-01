$(document).ready(function () {
	$.ajax({
        type: "GET",
        url: "/user/logout",
        data: null,
        cache: false,
        timeout: 600000,
        success: function (data) {
        	if(data == false) {
        		alert("로그인 상태가 아닙니다.");
        	} else {
        		alert("로그아웃이 완료되었습니다.");
        	}
        	
        	location.href = "/";
        },
        error: function (e) {
        	alert("GET /user/logout fail");
        }
	});
});