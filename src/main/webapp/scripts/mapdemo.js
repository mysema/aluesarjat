var map = null;
var overfeature = null;
var marker = null;
var gonzo1, gonzo2, gongo3;

var activeGonzo = null;

var geoOverlay = {
		type: "FeatureCollection",
		properties: { },
		features: [ {
			strokeColor: "#000000",
			strokeOpacity: 0.8,
			strokeWeight: 2,
			strokeWidth: 2,
			fillColor: "#FFC0C0",
			fillOpacity: 0.2,
		    type: "Feature",
		    properties: {},
		    geometry: {type:"MultiPolygon", coordinates:[]}
		} ]
	};

var searchGonzo = null; 

// area data
var comments = {};
var statistics = {};

$(document).ready(function(){	
    var latlng = new google.maps.LatLng(60.1662735988013, 24.93207843548);
    
    map = new google.maps.Map(
    		document.getElementById("map"), 
    		{ zoom: 11,
    		  center: latlng,
    		  mapTypeId: google.maps.MapTypeId.ROADMAP
    		});
        
	google.maps.event.addListener(map, 'zoom_changed', function(){
		var zoom = map.getZoom();
		if (zoom < 11){
			gonzo1.setMap(null);
			gonzo2.setMap(null);
			gonzo3.setMap(map);
			activeGonzo = gonzo3;
		}else if (zoom < 13){
			gonzo1.setMap(null);
			gonzo2.setMap(map);
			gonzo3.setMap(null);
			activeGonzo = gonzo2;
		}else{
			gonzo1.setMap(map);
			gonzo2.setMap(null);
			gonzo3.setMap(null);
			activeGonzo = gonzo1;
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
			activeGonoz = gonzo3;
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
			if (marker) marker.setMap(null);
			
			/*marker = new google.maps.Marker( { 
				position: latlng, 
				map: map, 
				title: feature.properties.name } );*/
			
			marker = new MarkerWithLabel({
			    position: latlng,
			    map: map,
			    icon: null,
			    labelAnchor: new google.maps.Point(22, 0),       
			    labelContent: feature.properties.name,
			    labelClass: "label",
			});
			
			// set coordinates to that of selected feature
			geoOverlay.features[0].properties = feature.properties;
			geoOverlay.features[0].geometry = feature.geometry;
			
			if (!searchGonzo){
				searchGonzo = new PolyGonzo.PgOverlay({ 
					map: map, 
					geo: geoOverlay, 
					events: {  
						mousemove: mouseOverFeature,
						click: clickOnFeature
					} 
				});
			}
			searchGonzo.setMap(map);
		}							
	}else{
		if (marker) marker.setMap(null);
		if (searchGonzo) searchGonzo.setMap(null);
	}
}	

function clickOnFeature(event, where) {
	var feature = where && where.feature;
	if (feature){
		var code = feature.properties.code;
		if (!statistics[code]){
			$.ajax({
				url: "areadata", data: {area: code}, datatype: "json", 
				success: function(data){	
					statistics[code] = data;
					updateInfo(feature, data);
				}
			});
		}else{
			updateInfo(feature, statistics[code]);
		}
	}	
}

function updateInfo(feature, areaData){
	var content = [];
	content.push("<h4>"+feature.properties.name+"</h4>");
	if (areaData){
		var length = areaData.length;
		for (var i = 0; i < length; i++){
			var entry = areaData[i];
			content.push("<p>"+ entry.label + " : " + entry.value + "</p>");
		}
	}
	if (comments[feature.properties.code]){
		content.push("<p>"+comments[feature.properties.code]+"</p>");
	}
	$("#info").html(content.join(""));
}
