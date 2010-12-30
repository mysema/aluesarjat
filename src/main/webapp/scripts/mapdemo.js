var map = null;
var overfeature = null;
var marker = null;
var gonzo1, gonzo2, gongo3;

var searchGonzo; // TODO
var comments = {};

$(document).ready(function(){	
    var latlng = new google.maps.LatLng(60.1662735988013, 24.93207843548);
    var myOptions = {
      zoom: 11,
      center: latlng,
      mapTypeId: google.maps.MapTypeId.ROADMAP
    };
    
    map = new google.maps.Map(document.getElementById("map"), myOptions);
        
	google.maps.event.addListener(map, 'zoom_changed', function(){
		var zoom = map.getZoom();
		if (zoom < 11){
			gonzo1.setMap(null);
			gonzo2.setMap(null);
			gonzo3.setMap(map);
			$("#info").html("3 " + zoom );
		}else if (zoom < 13){
			gonzo1.setMap(null);
			gonzo2.setMap(map);
			gonzo3.setMap(null);
			$("#info").html("2 "+ zoom);
		}else{
			gonzo1.setMap(map);
			gonzo2.setMap(null);
			gonzo3.setMap(null);
			$("#info").html("1 "+ zoom);
		}
	});
	
	// get DBpedia comments
	initNamespaces("sparql", function(){
		var query = prefixes + "SELECT ?area ?comment WHERE { ?area owl:sameAs ?area2 . ?area2 rdfs:comment ?comment }";
		querySparql("sparql", query, function(data){
			var bindings = data.results.bindings;
			for (var i = 0; i < bindings.length; i++){
				var binding = bindings[i];
				var area = getReadableURI(binding["area"].value); 
				comments[area.substring(area.indexOf(":")+1)] = binding["comment"].value;
			}
		});
	});
	
	$.ajax({
		url: "areas", data: {level: "1"}, datatype: "json", 
		success: function(geo){
			gonzo1 = createOverlay(geo);
		}
	});
	
	$.ajax({
		url: "areas", data: {level: "2"}, datatype: "json", 
		success: function(geo){
			gonzo2 = createOverlay(geo);
		}
	});
	
	$.ajax({
		url: "areas", data: {level: "3"}, datatype: "json", 
		success: function(geo){	
			gonzo3 = createOverlay(geo);
			gonzo3.setMap(map);
		}
	});
});

function createOverlay(geo){
	var length = geo.features.length;
	for (var i = 0; i < length; i++){
		var feature = geo.features[i];
		feature.strokeColor = "#000000";
		feature.strokeOpacity = 0.8;
		feature.strokeWeight = 1;
		feature.strokeWidth = 1;
		feature.fillColor = "#FFC0C0";
		feature.fillOpacity = 0.1;
	}					
	return new PolyGonzo.PgOverlay({ 
		map: map, 
		geo: geo, 
		events: { 
			mousemove: mouseOverFeature, 
			click: clickOnFeature 
		} 
	});
}

function mouseOverFeature(event, where) {
	var feature = where && where.feature;
	if( feature ) {
		if (feature != overfeature){
			overfeature = feature
			var centroid = feature.properties.center;
			var latlng = new google.maps.LatLng( centroid[1], centroid[0] );
			if (marker != null){
				marker.setMap(null);
			}
			/*
			marker = new google.maps.Marker( { 
				position: latlng, 
				map: map, 
				title: feature.properties.name } );
			*/
			marker = new MarkerWithLabel({
			    position: latlng,
			    map: map,
			    icon: "x",
			    labelAnchor: new google.maps.Point(22, 0),       
			    labelContent: feature.properties.name,
			    labelClass: "label",
			});
		}							
	}else{
		marker.setMap(null);
	}
}	

function clickOnFeature(event, where) {
	var feature = where && where.feature;
	if (feature){
		var content = [];
		content.push("<h4>"+feature.properties.name+"</h4>");
		if (comments[feature.properties.code]){
			content.push("<p>"+comments[feature.properties.code]+"</p>");
		}
		$("#info").html(content.join(""));
	}
	
}
