String.prototype.startsWith = function(str) {return (this.match("^"+str)==str)}

var prefixes = null;
var namespaces = {};

$(document).ready(function(){	
	// get namespaces from SPARQL endpoint
	var query = "SELECT ?ns ?prefix WHERE { ?ns <http://data.mysema.com/schemas/meta#nsPrefix> ?prefix }";
	sparqlQuery(query, function(data){
		var defaultNamespaces = [];
		var bindings = data.results.bindings;
		for (var i = 0; i < bindings.length; i++){
			var binding = bindings[i];
			namespaces[binding["prefix"].value] = binding["ns"].value;
			defaultNamespaces.push("PREFIX ", binding["prefix"].value, ": <", binding["ns"].value, ">\n");
		}			
		prefixes = defaultNamespaces.join("");			
		init();
	});
		
});

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
			return "&lt;" + uri + ">";
		}
	}
}


function sparqlQuery(query, success){
	$.ajax({
		url: "sparql", 
		data: { "query": query}, 
		datatype: "json", 
		beforeSend : function (xhr) {
    		xhr.setRequestHeader('Accept', 'application/sparql-results+json');
		},
		error: function(xhr, textStatus, errorThrown){
			$("#results").html(xhr.responseText);
		},
		success: success
	});
}


function init() {
	$(".table").each(function(){
		var element = $(this);
		var content = element.html();
		if (content.length > 0){
			sparqlQuery(prefixes + content, function(data){
				var vars = data.head.vars;
				var bindings = data.results.bindings;
				
				var table = new Array();
				table.push("<table>");
				var header = null;
				for (var i = 0; i < bindings.length; i++){
					var binding = bindings[i];
					if (header == null || header != binding[vars[0]].value){
						if (header != null){
							table.push("</tr>");	
						}						
						table.push("<tr>");					
						header = binding[vars[0]].value;
						table.push("<th>" + getReadableURI(header) + "</th>");
					}					
					table.push("<td>");
					var key = vars[2]
					if ("uri" == binding[key].type){
						table.push(getReadableURI(binding[key].value));
					}else{
						table.push(binding[key].value);	
					}						
					table.push("</td>");				
				}	
				table.push("</table>");
				element.html(table.join(""));
				element.show();
				
			});
		}	
	});
}