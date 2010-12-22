var map = null;

function addPolygon(binding) {
	var area = binding["area"];
	var title = binding["title"];
	var centerPoint = binding["center"].split(",");
	
	var coords = new Array();
	var points = binding["polygon"].split(" ");
	for (var i = 0; i < points.length; i++){
		var point = points[i].split(",");
		coords.push(new google.maps.LatLng(parseFloat(point[0]), parseFloat(point[1])));
	}
	
	 /*
	var marker = new MarkerWithLabel({
       position: new google.maps.LatLng(parseFloat(centerPoint[0]), parseFloat(centerPoint[1])),
       map: map,
       icon: title,
       labelAnchor: new google.maps.Point(22, 0),       
       labelContent: title,
       labelClass: "label",
     });
     */
    
     var marker = new google.maps.Marker({
       position: new google.maps.LatLng(parseFloat(centerPoint[0]), parseFloat(centerPoint[1])),
       map: map,
       title: title,
     });
     
    
    google.maps.event.addListener(marker, 'click', function(){
		$("#info").html(area);
	});
	     	
	var polygon = new google.maps.Polygon({
	   paths: coords,
	   strokeColor: "#000000",
	   strokeOpacity: 0.8,
	   strokeWeight: 1,
	   fillColor: "#FFC0C0",
	   fillOpacity: 0.1
	 });
	
	polygon.setMap(map);	
}

$(document).ready(function(){	
    var latlng = new google.maps.LatLng(60.2082651196077, 24.797472695835);
    var myOptions = {
      zoom: 10,
      center: latlng,
      mapTypeId: google.maps.MapTypeId.ROADMAP
    };
    
    map = new google.maps.Map(document.getElementById("map"), myOptions);
        
	google.maps.event.addListener(map, 'zoom_changed', function(){
		// TODO : manipulate visibility of areas
	});
        
	$.ajax({
		url: "areas", 
		datatype: "json", 
		success: function(data){
			var length = data.length;
			for (var i = 0; i < length; i++){
				addPolygon(data[i]);
			}			
		}
	});
});
