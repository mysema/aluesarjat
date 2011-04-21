String.prototype.startsWith = function(str) {return (this.match("^"+str)==str)}

var namespaces = {};
var savedQueries; 
var lastClick = null;
var limit = 200;
var offset = 0;
var queryActive = false;
var queryStartTime;

function getRequestParameters() {
	var parameters = {};
	if (window.location.search.length > 1) {
		var query = window.location.search.substring(1);
		var vars = query.split("&");
		for (var i=0; i<vars.length; i++) {
			var pair = vars[i].split("=");
			// First entry with this name
			if (typeof parameters[pair[0]] === "undefined") {
				parameters[pair[0]] = [ decodeURIComponent(pair[1]) ];
			} 
			// If second or later entry with this name
			else {
				parameters[pair[0]].push( decodeURIComponent(pair[1]) );
			}
		} 
	}
	return parameters;
}

function getBookmarkLink() {
	var href = window.location.href;
	href = href.substring(0, href.length - window.location.search.length);
	href += "?limit=" + limit + "&offset=" + offset + "&query=" + encodeURIComponent($("#query").val());
	
	return href;
}

function executeQuery () {
	$("#results").html("<img src='images/ajax-loader.gif' alt='Loading results'/>");
	var qry = 
		$("#namespaces").text() // Default namespaces 
		+ $("#query").val() // Query
		+ "\nlimit " + limit // Limit
		+ "\noffset " + offset // Offset
		;
	
	queryStartTime = new Date().getTime()
	
	$.ajax({
		url: "sparql", 
		data: { "query": qry, "type": "json"}, 
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
}

function nextPage() {
	offset += limit;
	executeQuery();
}

function prevPage() {
	if (offset > 0) {
		offset -= limit;
		if (offset < 0) {
			offset = 0;
		}
		executeQuery();
	}
}

function handleSPARQLResult(data){
	var vars = data.head.vars;
	var bindings = data.results.bindings;
	var html = new Array();
	var navigation = [];
	
	navigation.push(((new Date().getTime() - queryStartTime) / 1000), " s  - "); 
	navigation.push("<a id='openResults' href='javascript: openResults();'>Open in new window</a> - ");
	if (offset > 0) {
		navigation.push("<a href='javascript: prevPage();'>Previous page</a> - ");
	} else {
		navigation.push("Previous page - ");
	}
	if (bindings.length == limit) {
		navigation.push("<a href='javascript: nextPage();'>Next page</a>");
	} else {
		navigation.push("Next page");
	}
    navigation = navigation.join("");
    
	html.push(navigation);
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
			} 
			if (0 < i && lastColumns[j] == binding[key].value) {
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
				html.push("<td class='", cls, "'>", getReadableURI(binding[key].value));
			} else if ("literal" == binding[key].type) {
				html.push("<td class='", cls, "'>", binding[key].value.replace(/\n/g, "</br>"));
				if (binding[key]["xml:lang"]) {
					html.push(" @", binding[key]["xml:lang"]);
				}
			} else {
				html.push("<td class='", cls, "'>", binding[key].value);
			}
			html.push("</td>");
		}
		html.push("</tr>");
	}
	html.push("</tbody>");
	
	html.push("</table><p>");
	
	html.push(navigation);
	html.push("</p>");
	$("#results").html(html.join(""));
	
	queryActive = true;
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
		var prefix = null;
		for (var key in namespaces){
			if (uri.startsWith(namespaces[key]) && uri.length > namespaces[key].length) {
				if (prefix == null || namespaces[prefix].length < namespaces[key].length) {
					prefix = key;
				}  
			}
		}
		if (prefix != null) {
			return prefix + ":" + uri.substring(namespaces[prefix].length);
		} else {
			return "&lt;" + uri + "&gt;";
		}
	}
}

function openResults() {
	var win = window.open('', null, 'left=20,top=20,width=500,height=500,toolbar=no,resizable=yes,menubar=no,scrollbars=yes');
	win.document.write("<html><head><title>Results</title><link rel='stylesheet' type='text/css' href='styles/styles.css'></head><body><table class='results'>" +
			$("#results table.results").html()
			+ "</table></body></html>");
    win.document.close();
	win.focus();
}

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
			var defaultNamespaces = [];
			var bindings = data.results.bindings;
			for (var i = 0; i < bindings.length; i++){
				var binding = bindings[i];
				namespaces[binding["prefix"].value] = binding["ns"].value;
				defaultNamespaces.push("PREFIX ", binding["prefix"].value, ": &lt;", binding["ns"].value, "&gt; </br>\n");
			}
			
			$("#namespaces").html(defaultNamespaces.join(""));
			
			init();
		}
	});
	
	// SPARQL query handling
	$("#formsubmit").click(function() {
		offset = 0;
		executeQuery();
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

	limit = new Number($("#pageSize").val());
	
	// Change page size
	$("#pageSize").change(function() {
		offset = 0;
		limit = new Number($(this).val());
		if (queryActive) {
			executeQuery();
		}
	});
	
});

function init() {
	var parameters = getRequestParameters();
	if (parameters.limit) {
		limit = new Number(parameters.limit[0]);
		$("#pageSize").val(parameters.limit[0]);
	}
	if (parameters.offset) {
		offset = new Number(parameters.offset[0]);
	}
	if (parameters.query) {
		$("#query").val(parameters.query[0]);
		executeQuery();
	} else {
		// Example query
		$("#query").val(
				"SELECT ?dimensionName ?dimensionURI\n" +
				"WHERE {\n" +
				"?dimensionURI rdfs:subClassOf scv:Dimension ;\n" + 
				"    dc:title ?dimensionName .\n" + 
				"}"
		);
	}
}