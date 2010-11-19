namespaces = {};

var savedQueries; 
var lastClick = null;
var limit = 200;
var offset = 0;
var queryActive = false;

String.prototype.startsWith = function(str) {return (this.match("^"+str)==str)}

$(document).ready(function(){
	
	$(".facetValue").live("click",function(event) {
		alert(event.target.id.replace('-', ':'));
	});
	
	$.ajax({
		url: "facets", 
		datatype: "json", 
		error: function(xhr, textStatus, errorThrown){
			$("#results").html(xhr.responseText);
		},
		success: function(data){
			var template = [];

			printFacet("scv:Dataset", "Tilasto", data.datasets, template)

			var dimensionTypes = data.dimensionTypes;
			for (var i=0; i < dimensionTypes.length; i++) {
				printFacet(toID(dimensionTypes[i].id), dimensionTypes[i].name, dimensionTypes[i].dimensions, template);
			}
			
			$("#facets").html(template.join(""));
		}
	});

});

function printFacet(facetId, facetName, values, template) {
	template.push("<div class='facet'><h3 class='dimensionType' id='", toID(facetId),"'>", facetName, "</h3>");
	
	for (var i=0; i < values.length; i++) {
		template.push("<div class='facetValue' id='", toID(values[i].id),"'>", values[i].name, "</div>");
	}
	template.push("</div>");
}

function toID(prefixed) {
	return prefixed.replace(':', '-');
}