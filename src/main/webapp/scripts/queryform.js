$(document).ready(function(){
	
	$("#formsubmit").click(function(){
		var query = $("#query").val();
		$.ajax({
			url: "query", 
			data: { query: query}, 
			datatype: "json", 
			beforeSend : function (xhr) {
        		xhr.setRequestHeader('Accept', 'application/sparql-results+json');
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
				for (var i = 0; i < bindings.length; i++){
					var binding = bindings[i];
					html.push("<tr>");
					for (var key in binding){						
						html.push("<td class='"+binding[key].type+"'>" + binding[key].value + "</td>");
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
});			
			