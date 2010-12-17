var map = null;

function addPolygon(binding) {
	var area = binding["area"].value;
	var title = binding["title"].value;
	
	var coords = new Array();
	var points = binding["polygon"].value.split(" ");
	for (var i = 0; i < points.length; i++){
		var point = points[i].split(",");
		coords.push(new google.maps.LatLng(parseFloat(point[0]), parseFloat(point[1])));
	}
	
	var centerPoint = binding["center"].value.split(",");
	var marker = new MarkerWithLabel({
       position: new google.maps.LatLng(parseFloat(centerPoint[0]), parseFloat(centerPoint[1])),
       map: map,
       icon: title,
       labelAnchor: new google.maps.Point(22, 0),       
       labelContent: title,
       labelClass: "label",
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
	   fillOpacity: 0.35
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
        
    // get polygons via SPARQL    
	var query = ["SELECT * WHERE { ?area <http://www.w3.org/2003/01/geo/polygon> ?polygon " ,
				" ; <http://www.w3.org/2003/01/geo/where> ?center ",
				" ; <http://purl.org/dc/elements/1.1/title> ?title }"].join("");
	$.ajax({
		url: "sparql", 
		data: { "query": query}, 
		datatype: "json", 
		beforeSend : function (xhr) {
    		xhr.setRequestHeader('Accept', 'application/sparql-results+json');
		},
		error: function(xhr, textStatus, errorThrown){
			$("#errors").html(xhr.responseText);
		},
		success: function(data){
			var bindings = data.results.bindings;
			var length = bindings.length;
			for (var i = 0; i < length; i++){
				addPolygon(bindings[i]);				
			}			
		}
	});
});
