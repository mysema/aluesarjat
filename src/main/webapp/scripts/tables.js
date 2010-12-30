$(document).ready(function(){	
	initNamespaces("sparql", init);		
});


function init() {
	$(".table").each(function(){
		var element = $(this);
		var content = element.html();
		if (content.length > 0){
			querySparql("sparql", prefixes + content, function(data){
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