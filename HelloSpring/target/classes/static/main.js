var pgNo = 1;
var isEnd = false;

var placeListData = "";

$(document).ready(function () {
    $("#search-form").submit(function (event) {
        event.preventDefault();
        fire_ajax_submit();
    });
});

function fire_ajax_submit() {
	$('#feedbackDetail').html("");
	$('#map').hide();
	
    $("#btn-search").prop("disabled", true);

    $.ajax({
        type: "GET",
        contentType: "text/html",
        url: "/place",
        data: "name=" + $("#username").val() + "&page=" + pgNo,
        dataType: 'text',
        cache: false,
        timeout: 600000,
        success: function (data) {
        	placeListData = JSON.parse(data);
        	var placeNameList = "";
        	isEnd = placeListData.meta.is_end;
        	for (var i = 0; i < placeListData.documents.length; i++) {
        		placeNameList += "<button type='button' id='btn-detail' onclick='btnDetail_click(" + i + ");' class='btn'>" + placeListData.documents[i].place_name + "</button><br/>";
			}
        	
            var json = "<h4>장소 검색 결과</h4>" + placeNameList + ""
                + "<button type='button' id='btn-prev' onclick='btnPrev_click();' class='btn btn-secondary btn-sm'>< PREV</button>&nbsp;"
                + "<button type='button' id='btn-next' onclick='btnNext_click();' class='btn btn-secondary btn-sm'>NEXT ></button>";
            $('#feedback').html(json);
            $("#btn-search").prop("disabled", false);
        },
        error: function (e) {
        	alert("/place fail");
        }
    });
    
    $.ajax({
        type: "GET",
        contentType: "text/html",
        url: "/user/history",
        data: null,
        dataType: 'text',
        cache: false,
        timeout: 600000,
        success: function (data) {
        	$('#historyKeyword').html(data);
        },
        error: function (e) {
        	alert("GET /user/history fail");
        }
    });
    
    $.ajax({
        type: "GET",
        contentType: "text/html",
        url: "/rank/keyword",
        data: null,
        dataType: 'text',
        cache: false,
        timeout: 600000,
        success: function (data) {
        	$('#rankKeyword').html(data);
        },
        error: function (e) {
        	alert("/rank/keyword fail");
        }
    });
}

function btnPrev_click(){
	if(pgNo > 1) {
		pgNo = pgNo - 1;
	} else {
		alert("첫 페이지 입니다.");
		return;
	}
	
	event.preventDefault();
    fire_ajax_submit();
}

function btnNext_click(){
	if(!isEnd) {
		pgNo = pgNo + 1;
	} else {
		alert("마지막 페이지 입니다.");
		return;
	}
	
	event.preventDefault();
    fire_ajax_submit();
}

function btnDetail_click(idx){
	$('#map').show();
	
	var x = placeListData.documents[idx].x;
	var y = placeListData.documents[idx].y;
	
	var container = document.getElementById('map'); //지도를 담을 영역의 DOM 레퍼런스
	var options = { //지도를 생성할 때 필요한 기본 옵션
		center: new kakao.maps.LatLng(y, x), //지도의 중심좌표.
		level: 3 //지도의 레벨(확대, 축소 정도)
	};

	var map = new kakao.maps.Map(container, options); //지도 생성 및 객체 리턴
	
	var content = '<div class ="label"><span class="left"></span><span class="center">' + placeListData.documents[idx].place_name + '<br>(' + placeListData.documents[idx].road_address_name + ')' + '</span><span class="right"></span></div>';

	// 커스텀 오버레이가 표시될 위치입니다 
	var position = new kakao.maps.LatLng(y, x);  

	// 커스텀 오버레이를 생성합니다
	var customOverlay = new kakao.maps.CustomOverlay({
	    position: position,
	    content: content   
	});

	// 커스텀 오버레이를 지도에 표시합니다
	customOverlay.setMap(map);
	
	$('#feedbackDetail').html("<h4>" + placeListData.documents[idx].place_name + " 상세보기</h4>"
			+ "도로명주소 : " + placeListData.documents[idx].road_address_name + "<br/>"
			+ "지번주소 : " + placeListData.documents[idx].address_name + "<br/>"
			+ "전화번호 : " + placeListData.documents[idx].phone + "<br/>"
			+ "<a target='_blank' href='https://map.kakao.com/link/map/" + placeListData.documents[idx].id + "'>카카오맵 바로가기</a><br/>");
}