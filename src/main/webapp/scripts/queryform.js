namespaces = {};

var savedQueries; 
var lastClick = null;

String.prototype.startsWith = function(str) {return (this.match("^"+str)==str)}

$(document).ready(function(){

	$(".savedQuery").live("click",function() {
		var query = $(this).text();
		var time = new Date().getTime();
		if (lastClick != null && time - lastClick < 500) {
			lastClick = time;
			var newSavedQueries = [];
			for (var i=0; i < savedQueries.length; i++) {
				if (query == savedQueries[i]) {
					$(this).remove();
				} else if (savedQueries[i] != null && "" != savedQueries[i]) {
					newSavedQueries.push(savedQueries[i]);
				}
			}
			savedQueries = newSavedQueries;
			localStorage.savedQueries = JSON.stringify(savedQueries);
		} else {
			lastClick = time;
			$("#query").val(query);
		}
	});

	// initialize saved queries
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
	
	// get namespaces from SPARQL endpoint
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
			var defaultNamespaces = [];
			var bindings = data.results.bindings;
			for (var i = 0; i < bindings.length; i++){
				var binding = bindings[i];
				namespaces[binding["prefix"].value] = binding["ns"].value;
				defaultNamespaces.push("PREFIX ", binding["prefix"].value, ": &lt;", binding["ns"].value, "&gt;</br>");
			}
			
			$("#namespaces").html(defaultNamespaces.join(""));
			$("#query").val(
					"SELECT ?dimensionName ?dimensionURI\n" +
					"WHERE {\n" +
					"?dimensionURI rdfs:subClassOf scv:Dimension ;\n" + 
					"    dc:title ?dimensionName .\n" + 
					"}"
			);
		}
	});
	
	// SPARQL query handling
	$("#formsubmit").click(function(){
		var query = $("#namespaces").text() + $("#query").val();
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
			success: handleSPARQLResult
		});
		return false;
	});

	// Save query
	$("#saveQuery").click(function(){
		var query = $("#query").val();
		var index = savedQueries.length;
		savedQueries[localStorage.savedQueries.length] = query;
		localStorage.savedQueries = JSON.stringify(savedQueries); 
		printSavedQuery(index, query);
	});
});

function handleSPARQLResult(data){
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
	var evenRow = [];
	for (var i = 0; i < bindings.length; i++){
		var binding = bindings[i];
		html.push("<tr>");
		for (var j = 0; j < vars.length; j++){
			var key = vars[j];
			var oddRow;
			if (typeof binding[key] == "undefined") {
				binding[key] = {type: "undefined", value: "undefined"};
				oddRow = evenRow[j];
			} else if (lastColumns[j] != null && lastColumns[j] == binding[key].value) {
				binding[key].value = "&nbsp;";
				oddRow = evenRow[j];
			} else {
				lastColumns[j] = binding[key].value;
				// Clear subsequent columns
				for (var k=j+1; k < lastColumns.length && lastColumns[k] != null; k++) {
					lastColumns[k] = null;
				}
				oddRow = !evenRow[j];
				evenRow[j] = oddRow;
			}
			var cls = binding[key].type + (oddRow ? " odd" : " even");
			if ("uri" == binding[key].type) {
				html.push("<td class='"+cls+"'>" + getReadableURI(binding[key].value) + "</td>");
			} else if ("literal" == binding[key].type) {
				html.push("<td class='"+cls+"'>" + binding[key].value.replace(/\n/g, "</br>") + "</td>");
				} else {
				html.push("<td class='"+cls+"'>" + binding[key].value + "</td>");
			}
		}
		html.push("</tr>");
	}
	html.push("</tbody>");
	
	html.push("</table>");
	$("#results").html(html.join(""));
}

function printSavedQuery(index, query) {
	var div = $("#savedQueries");
	query = query.replace(/</g, "&lt;");
	div.html(div.html() + "\n<pre class='savedQuery'>" + query + "</pre>");	
}

function getReadableURI(uri) {
	if (uri == "" || uri == "&nbsp;") {
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