namespaces = {};

var savedQueries; 

String.prototype.startsWith = function(str) {return (this.match("^"+str)==str)}

$(document).ready(function(){

	if (localStorage.savedQueries) {
		savedQueries = JSON.parse(localStorage.savedQueries);
		for (var i=0; i < savedQueries.length; i++) {
			if (savedQueries[i] != null) {
				printSavedQuery(i, savedQueries[i]);
			}
		}
	} else {
		savedQueries = [];
		localStorage.savedQueries = JSON.stringify(savedQueries); 
	}
	
	var query = "SELECT ?ns ?prefix WHERE { ?ns <http://data.mysema.com/schemas/meta#nsPrefix> ?prefix }";
	$.ajax({
		url: "query", 
		data: { query: query}, 
		datatype: "json", 
		beforeSend : function (xhr) {
    		xhr.setRequestHeader('Accept', 'application/sparql-results+json');
		},
		error: function(xhr, textStatus, errorThrown){
			$("#results").html(xhr.responseText);
		},		
		success: function(data){
			var queryTemplate = [];
			var bindings = data.results.bindings;
			for (var i = 0; i < bindings.length; i++){
				var binding = bindings[i];
				namespaces[binding["prefix"].value] = binding["ns"].value;
				queryTemplate.push("PREFIX ", binding["prefix"].value, ": <", binding["ns"].value, ">\n");
			}
			
			queryTemplate.push("\nSELECT ?value \nWHERE { \n?item rdf:value ?value . \n}");
			
			$("#query").val(queryTemplate.join(""));
		}
	});
	
	
	
	$("#formsubmit").click(function(){
		var query = $("#query").val();
		$.ajax({
			url: "query", 
			data: { query: query}, 
			datatype: "json", 
			beforeSend : function (xhr) {
        		xhr.setRequestHeader('Accept', 'application/sparql-results+json');
    		},
			error: function(xhr, textStatus, errorThrown){
				$("#results").html(xhr.responseText);
			},			
			success: function(data){
				var vars = data.head.vars;
				var bindings = data.results.bindings;
				var html = new Array();
				html.push("<p>Results</p>");
				html.push("<table class='results'>");
				
				// head
				html.push("<thead><tr>");
				for (var i = 0; i < vars.length; i++){
				//for (var v in vars) {
					html.push("<th>" + vars[i] + "</th>");
				}				
				html.push("</tr></thead>");
				
				// body
				html.push("<tbody>");
				var lastColumns = [];
				for (var i = 0; i < bindings.length; i++){
					var binding = bindings[i];
					html.push("<tr>");
					for (var j = 0; j < vars.length; j++){
						var key = vars[j];
						if (typeof binding[key] == "undefined") {
							binding[key] = {type: "undefined", value: "undefined"};
						} else if (lastColumns[j] != null && lastColumns[j] == binding[key].value) {
							binding[key].value = "";
						} else {
							lastColumns[j] = binding[key].value;
						}
						if ("uri" == binding[key].type) {
							html.push("<td class='"+binding[key].type+"'>" + getReadableURI(binding[key].value) + "</td>");
						} else if ("literal" == binding[key].type) {
							html.push("<td class='"+binding[key].type+"'>" + binding[key].value.replace(/\n/g, "</br>") + "</td>");
 						} else {
							html.push("<td class='"+binding[key].type+"'>" + binding[key].value + "</td>");
						}
					}
					html.push("</tr>");
				}
				html.push("</tbody>");
				
				html.push("</table>");
				$("#results").html(html.join(""));
			} 
		});
		return false;
	});

	$("#saveQuery").click(function(){
		var query = $("#query").val();
		var index = savedQueries.length;
		savedQueries[localStorage.savedQueries.length] = query;
		localStorage.savedQueries = JSON.stringify(savedQueries); 
		printSavedQuery(index, query);
	});
});

function printSavedQuery(index, query) {
	var id = "savedQuery-" + index;
	var div = $("#savedQueries");
	query = query.replace(/</g, "&lt;");
	div.html(div.html() + "\n<pre id='" + id + "'>" + query + "\n</pre>");
	
	$("#" + id).click(function() {
		$("#query").val($(this).text());
	});
}

function getReadableURI(uri) {
	if (uri == "") {
		return "";
	} else {
		for (var key in namespaces){
			if (uri.startsWith(namespaces[key])) {
				return key + ":" + uri.substring(namespaces[key].length);
			}
		}
		return "<" + uri + ">";
	}
}