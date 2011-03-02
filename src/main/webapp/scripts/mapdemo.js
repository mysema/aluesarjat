var map = null;
var overArea = null;
var clickedArea = null;
var marker = null;
var gonzo1, gonzo2, gongo3, gonzo4;

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
var dbpediateComments = {};
var statistics = {};

$(document).ready(function(){	
    var latlng = new google.maps.LatLng(60.1662735988013, 24.93207843548);
    
    // create map
    map = new google.maps.Map(
    		document.getElementById("map"), 
    		{ zoom: 11,
    		  center: latlng,
    		  mapTypeId: google.maps.MapTypeId.ROADMAP
    		});
    
    // switch overlay on zoom
	google.maps.event.addListener(map, 'zoom_changed', function(){
		if (searchGonzo){
			searchGonzo.setMap(null);
		}
		var zoom = map.getZoom();
		if (zoom < 10){
			gonzo1.setMap(null);
			gonzo2.setMap(null);
			gonzo3.setMap(null);
			gonzo4.setMap(map);
			activeGonzo = gonzo4;
			
		}else if (zoom < 11){
			gonzo1.setMap(null);
			gonzo2.setMap(null);
			gonzo3.setMap(map);
			gonzo4.setMap(null);
			activeGonzo = gonzo3;

		}else if (zoom < 13){
			gonzo1.setMap(null);
			gonzo2.setMap(map);
			gonzo3.setMap(null);
			gonzo4.setMap(null);
			activeGonzo = gonzo2;

		}else{
			gonzo1.setMap(map);
			gonzo2.setMap(null);
			gonzo3.setMap(null);
			gonzo4.setMap(null);
			activeGonzo = gonzo1;
		}
	});
	
	// init tabs
	//$(".tab_content").hide(); //Hide all content
	//$("ul.tabs li:first").addClass("active").show(); //Activate first tab
	//$(".tab_content:first").show(); //Show first tab content

	//On Click Event
	$("ul.tabs li").live("click", function() {
		$("ul.tabs li").removeClass("active"); //Remove any "active" class
		$(this).addClass("active"); //Add "active" class to selected tab
		$(".tab_content").hide(); //Hide all tab content

		var activeTab = $(this).find("a").attr("href"); //Find the href attribute value to identify the active tab + content
		$(activeTab).fadeIn(); //Fade in the active ID content
		return false;
	});
	
	// get comments
	initNamespaces("sparql", function(){
		var query = prefixes + "SELECT ?area ?comment WHERE { ?area rdf:type dimension:Alue ; dc:description ?comment . }";
		querySparql("sparql", query, function(data){
			var bindings = data.results.bindings;
			for (var i = 0; i < bindings.length; i++){
				var binding = bindings[i];
				var area = getReadableURI(binding["area"].value); 
				comments[area.substring(area.indexOf(":")+1)] = binding["comment"].value;
			}
		});
	});
	// get DBpedia comments
	initNamespaces("sparql", function(){
		var query = prefixes + "SELECT ?area ?comment WHERE { ?area owl:sameAs ?area2 . ?area2 rdfs:comment ?comment }";
		querySparql("sparql", query, function(data){
			var bindings = data.results.bindings;
			for (var i = 0; i < bindings.length; i++){
				var binding = bindings[i];
				var area = getReadableURI(binding["area"].value); 
				dbpediateComments[area.substring(area.indexOf(":")+1)] = binding["comment"].value;
			}
		});
	});
	
	// get level 1 polygons
	$.ajax({
		url: "areas", data: {level: "1"}, datatype: "json", 
		success: function(geo){
			gonzo1 = createOverlay(geo);
		}
	});
	
	// get level 2 polygons
	$.ajax({
		url: "areas", data: {level: "2"}, datatype: "json", 
		success: function(geo){
			gonzo2 = createOverlay(geo);
		}
	});
	
	// get level 3 polygons
	$.ajax({
		url: "areas", data: {level: "3"}, datatype: "json", 
		success: function(geo){	
			gonzo3 = createOverlay(geo);
			gonzo3.setMap(map);
			activeGonzo = gonzo3;
		}
	});
	
	// get level 4 polygons
	$.ajax({
		url: "areas", data: {level: "4"}, datatype: "json", 
		success: function(geo){	
			gonzo4 = createOverlay(geo);
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
		//$("#info").html("over " + feature.properties.code);
		if (feature.properties.code != overArea){
			overArea = feature.properties.code;
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
			
			google.maps.event.addListener(marker, 'click', function(){
				clickOnFeature(null, {feature: feature});
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
		if (searchGonzo){
			searchGonzo.setMap(null);
		}	
	}
}	

function clickOnFeature(event, where) {	
	var feature = where && where.feature;
	if (feature){
		//$("#info").html("click " + feature.properties.code);
		var props = feature.properties;
		var code = props.code;
		clickedArea = code;
		if (!statistics[code]){
			// initial display
			updateInfo(props, null);
			$.ajax({
				url: "areadata", 
				data: {area: code}, 
				datatype: "json", 
				success: function(data){
					statistics[code] = data;
					if (code == clickedArea){
						updateInfo(props, data);	
					}						
				}
			});
		}else{
			updateInfo(props, statistics[code]);
		}		
	}	
}

function updateInfo(props, areaData){
	var content = [];
	content.push("<h4>"+props.name+"</h4>");
	
	// tabs 
	content.push("<ul class='tabs'>");
	content.push("<li class='active'><a href='#tab1'>Kuvaus</a></li>");
	if (dbpediateComments[props.code]){
		content.push("<li><a href='#tab2'>DBpedia</a></li>");
	}
	if (areaData){
		content.push("<li><a href='#tab3'>Tilastot</a></li>");	
	}		
	content.push("</ul>");
	
	// tab container
	content.push("<div class='tab_container'>");
	content.push("<div id='tab1' class='tab_content'>");
	if (comments[props.code]){
		content.push(comments[props.code]);
	}else{
		content.push("Ei kuvausta");
	}
	content.push("</div>");
	
	if (dbpediateComments[props.code]){
		content.push("<div id='tab2' class='tab_content' style='display:none'>");
		content.push(dbpediateComments[props.code]);
		content.push("</div>");
	}	
	
	if (areaData){
		content.push("<div id='tab3' class='tab_content' style='display:none;'>");
		content.push("<table>");
		var length = areaData.length;
		for (var i = 0; i < length; i++){
			var entry = areaData[i];
			content.push("<tr><th>"+ entry.label + "</th><td>" + entry.value + "</td></tr>");
		}
		content.push("</table>");
		content.push("</div>");
	}
	content.push("</div>");
		
	$("#info").html(content.join(""));
}
