
String.prototype.startsWith = function(str) {return (this.match("^"+str)==str)}

var restrictions = [];

jQuery.ajaxSettings.traditional = true;

$(document).ready(function(){
	
	$(".facetValue").live("click",function(event) {
		var id = $(event.target).data("id");
		
		var i = restrictions.indexOf(id);
		if (i < 0) {
			restrictions.push(id);
		} else {
			restrictions.splice(i,1);
		}

		if (restrictions.length == 0) {
			$(".facet").show();
			$(".facetValue").show();
			$(this).removeClass("selectedValue");
		} else {
			$.ajax({
				url: "search", 
				datatype: "json",
				data: {"value": restrictions},
				error: function(xhr, textStatus, errorThrown){
					$("#results").html(xhr.responseText);
				},
				success: function(data){
					// FILTER FACETS
					var values = data.values;

					var visibleValues = {};
					for (var i=0; i < values.length; i++) {
						visibleValues[values[i]] = true;
					}
					
					$(".facetValue").each(function () {
						var id = $(this).data("id");
						if (visibleValues[id] == true) {
							if (restrictions.indexOf(id) >= 0) {
								$(this).addClass("selectedValue");
							} else {
								$(this).removeClass("selectedValue");
							}
							$(this).addClass("visible");
							$(this).show();
						} else {
							$(this).removeClass("selectedValue");
							$(this).removeClass("visible");
							$(this).hide();
						}
					});
					$(".facet").each(function () {
						if ($(this).find(".visible").length > 0) {
							$(this).show();
						} else {
							$(this).hide();
						}
					});

					// SHOW ITEMS
					if (data.items) {
						var items = data.items;
						var template = [];

						template.push("<table class='items'><thead><tr><th colspan='2'>");
						
						for (var i=0; i < restrictions.length; i++) {
							var id = toID(restrictions[i]);
							template.push("<div class='facetValue selectedValue' data-id='", restrictions[i], "'>", $("#" + id).text(), "</div>");
						}
						
						template.push("</th></tr></thead><tbody>")
						for (var i=0; i < items.length; i++) {
							var item = items[i];
							var values = item.values;
							template.push("<tr class='itemRow ", (i % 2 == 0 ? "odd" : "even"),"'><td>")
							for (var j=0; j < values.length; j++) {
								var value = values[j];
								if (restrictions.indexOf(value) < 0) {
									var id = toID(value);
									template.push("<div class='facetValue' data-id='", value,"'>", $("#" + id).text(), "</div>");
								}
							}
							template.push("</td><td><div class='itemValue'> = ", item.value, "</div></td>");
							template.push("</div>");
						}
						template.push("</tbody></table>");
						$("#results").html(template.join(""));
					} else {
						$("#results").html("");
					}
				}
			});
		}
	});
	
	$.ajax({
		url: "facets", 
		datatype: "json", 
		error: function(xhr, textStatus, errorThrown){
			$("#results").html(xhr.responseText);
		},
		success: function(data){
			var template = [];

			var facets = data.facets;
			for (var i=0; i < facets.length; i++) {
				printFacet(facets[i].name, facets[i].values, template);
			}
			
			$("#facets").html(template.join(""));
		}
	});

});

function printFacet(facetName, values, template) {
	template.push("<div class='facet'><h3 class='facetTitle'>", facetName, "</h3>");
	
	for (var i=0; i < values.length; i++) {
		template.push("<div class='facetValue' id='", toID(values[i].id), "' data-id='", values[i].id,"'>", values[i].name, "</div>");
	}
	template.push("</div>");
}

function fromID(id) {
	if (id.startsWith("restriction_")) {
		id = id.substring(12);
	} else if (id.startsWith("itemValue_")) {
		id = id.substring(10);
	}
	return id.replace('-', ':');
}
function toID(prefixed) {
	return prefixed.replace(':', '-');
}