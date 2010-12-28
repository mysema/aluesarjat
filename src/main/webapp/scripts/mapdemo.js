var map = null;
var marker = null;
var overfeature = null;

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
	
	// TODO : multiple overlays for different zoom layers
	
	$.ajax({
		url: "areas", 
		datatype: "json", 
		success: function(geo){
			var length = geo.features.length;
			for (var i = 0; i < length; i++){
				var feature = geo.features[i];
				feature.strokeColor = "#000000";
				feature.strokeOpacity = 0.5;
				feature.strokeWeight = 1;
				feature.strokeWidth = 1;
				feature.fillColor = "#FFC0C0";
				feature.fillOpacity = 0.1;
			}		
			
			var gonzo = new PolyGonzo.PgOverlay({
				map: map,
				geo: geo,
				events: {
				mousemove: function( event, where ) {
						var feature = where && where.feature;
						if( feature ) {
							if (feature != overfeature){
								overfeature = feature
								var centroid = feature.properties.center;
								var latlng = new google.maps.LatLng( centroid[1], centroid[0] );
								if (marker != null){
									marker.setMap(null);
								}
								marker = new google.maps.Marker( { 
									position: latlng, 
									map: map, 
									title: feature.properties.name } );
							}							
						}else{
							marker.setMap(null);
						}
					},
					
					click: function( event, where ) {
						var feature = where && where.feature;
						if (feature){
							$("#info").html(feature.properties.name);
						}
					}				
				}
			});
			gonzo.setMap(map);
		}
	});
});
