var map = null;
var overArea = null;
var clickedArea = null;
var marker = null;
var gonzo1, gonzo2, gongo3, gonzo4;

var activeGonzo = null;

var defaultNamespaces = [];

var geoOverlay = {
		type: "FeatureCollection",
		//hittest: false,
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
		    boxes: new Array(),
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
    
	// get namespaces from SPARQL endpoint
	var query = "SELECT ?ns ?prefix WHERE { ?ns <http://data.mysema.com/schemas/meta#nsPrefix> ?prefix }";
	$.ajax({
		url: "sparql", 
		data: { "query": query, "type": "json"}, 
		datatype: "json", 
		beforeSend : function (xhr) {
    		xhr.setRequestHeader('Accept', 'application/sparql-results+json');
		},
		error: function(xhr, textStatus, errorThrown){
			$("#results").html(xhr.responseText);
		},
		success: function(data){			
			var bindings = data.results.bindings;
			for (var i = 0; i < bindings.length; i++){
				var binding = bindings[i];
				defaultNamespaces.push("PREFIX ", binding["prefix"].value, ": <", binding["ns"].value, ">\n");
			}
			defaultNamespaces = defaultNamespaces.join("");
		}
	});
    
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
		feature.boxes = new Array(); 
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

function clickOnFeature(event, where) {	
	var feature = where && where.feature;
	if (feature){
		var props = feature.properties;
		var code = props.code;
		clickedArea = code;
		if (!statistics[code]){
			// initial display
			
			var qry = [];
			qry.push(defaultNamespaces)
			qry.push("SELECT ?ikl ?val WHERE {\n");
		    qry.push("  ?item dimension:vuosi vuosi:_2009 ; dimension:alue alue:",code," ; dimension:ikäryhmä ?ik ; rdf:value ?val .\n"); 
		    qry.push("  ?ik dc:title ?ikl . \n");
		    qry.push("}\n");
		    qry = qry.join("");
			
			updateInfo(props, null);
			$.ajax({
				url: "sparql", 
				data: { "query": qry, "type": "json"},  
				datatype: "json", 
				beforeSend : function (xhr) {
		    		xhr.setRequestHeader('Accept', 'application/sparql-results+json');
				},
				success: function(sparqlResults){
					var data = []; 
					var bindings = sparqlResults.results.bindings;					
					for (var i = 0; i < bindings.length; i++){
						var binding = bindings[i];
						var val = binding["val"].value;
						if (val == '..' || val == '.') {
							binding["val"].value = '0';
						}
						data.push( { label: binding["ikl"].value, value:  parseInt(binding["val"].value) } );
					}					
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
	if (areaData){
		content.push("<li class='active'><a href='#tab1'>Tilastot</a></li>");	
	}		
	if (comments[props.code]){
		content.push("<li><a href='#tab2'>Kuvaus</a></li>");
	}
	if (dbpediateComments[props.code]){
		content.push("<li><a href='#tab3'>DBpedia</a></li>");
	}	
	content.push("</ul>");
	
	// tab container
	content.push("<div class='tab_container'>");
	if (areaData){
		// calculate max 
		var length = areaData.length;
		var max = 0;
		var sum = 0;
		for (var i = 0; i < length; i++){
			var entry = areaData[i];
			if (entry.label == "Väestö yhteensä"){
				sum = entry.value;
			} else if (entry.value > max){
				max = entry.value;
			}				
		}
		
		// render table
		content.push("<div id='tab1' class='tab_content'>");		
		content.push("<h4>Väestö ikäryhmittäin (2009)</h4>");
		if (max > 0) {
			content.push("<table>");
			for (var i = 0; i < length; i++){
				var entry = areaData[i];
				if (entry.label != "Väestö yhteensä"){
					content.push("<tr>");
					content.push("<th>", entry.label, "</th>");
					content.push("<td>", entry.value, "</td>");
					content.push("<td><div class='panel' style='width: ", (100 * entry.value) / max, "px;'>&nbsp;</div></td>");
					content.push("</tr>");
				}
			}
			content.push("<tr><td>Yhteensä</td><td>", sum, "</td><td>&nbsp;</td></tr>");		
			content.push("</table>");	
		}else{
			content.push("<p>Ei tietoa</p>");
		}		
		content.push("</div>");
	}
	if (comments[props.code]){
		content.push("<div id='tab2' class='tab_content' style='display:none;'>");	
		content.push(comments[props.code]);
		content.push("</div>");
	}	
	if (dbpediateComments[props.code]){
		content.push("<div id='tab3' class='tab_content' style='display:none'>");
		content.push(dbpediateComments[props.code]);
		content.push("</div>");
	}	
	
	content.push("</div>");
		
	$("#info").html(content.join(""));
}
