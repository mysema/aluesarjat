String.prototype.startsWith = function(str) {return (this.match("^"+str)==str)}

var prefixes = null;
var namespaces = {};

function getReadableURI(uri) {
	if (uri == "" || uri == " ") {
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
			return "<" + uri + ">";
		}
	}
}

function initNamespaces(endPoint, callback){	
	var query = "SELECT ?ns ?prefix WHERE { ?ns <http://data.mysema.com/schemas/meta#nsPrefix> ?prefix }";
	querySparql(endPoint, query, function(data){
		var defaultNamespaces = [];
		var bindings = data.results.bindings;
		for (var i = 0; i < bindings.length; i++){
			var binding = bindings[i];
			namespaces[binding["prefix"].value] = binding["ns"].value;
			defaultNamespaces.push("PREFIX ", binding["prefix"].value, ": <", binding["ns"].value, ">\n");
		}			
		prefixes = defaultNamespaces.join("");			
		callback();
	});
}

function querySparql(endPoint, query, success){
	$.ajax({
		url: endPoint, 
		data: { "query": query}, 
		datatype: "json", 
		beforeSend : function (xhr) {
    		xhr.setRequestHeader('Accept', 'application/sparql-results+json');
		},
		success: success
	});
}