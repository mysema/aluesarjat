
String.prototype.startsWith = function(str) {return (this.match("^"+str)==str)}

jQuery.ajaxSettings.traditional = true;

$(document).ready(function(){

	var restrictions = [];

	var getFacetName = function(facetId) {
		return $("#" + toID(facetId) + " .facetTitle").text();
	}
	
	var getFacetId = function(facetValue) {
		var id = toID(facetValue);
		return $("#" + id).data("facet");
	}
	
	var printFacet = function(facet, template) {
		template.push("<div class='facet' id='", toID(facet.id), "' data-id='", facet.id, "'><h3 class='facetTitle'>", facet.name, "</h3>");
		
		var values = facet.values;
		for (var i=0; i < values.length; i++) {
			var value = values[i];
			template.push("<div class='facetValue' id='", toID(value.id), "' data-id='", value.id,"' data-facet='", facet.id, "'>", value.name, "</div>");
		}
		template.push("</div>");
	}
	
	var fromID = function(id) {
		if (id.startsWith("restriction_")) {
			id = id.substring(12);
		} else if (id.startsWith("itemValue_")) {
			id = id.substring(10);
		}
		return id.replace('-', ':');
	}
	
	var toID = function(prefixed) {
		return prefixed.replace(':', '-');
	}
	
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
			$("#results").html("");
		} else {
			$("#results").html("<img src='images/ajax-loader.gif' alt='Loading results'/>");
			$.ajax({
				url: "search", 
				datatype: "json",
				data: {"value": restrictions},
				error: function(xhr, textStatus, errorThrown) {
					$("#results").html(xhr.responseText);
				},
				success: function(data){
					var template = [];

					template.push("<table class='items'><thead>");
					
					var restrictionFacets = {};
					for (var i=0; i < restrictions.length; i++) {
						var id = toID(restrictions[i]);
						var facetValue =  $("#" + id);
						var facetId = facetValue.data("facet");
						restrictionFacets[facetId] = true;
						var facet = getFacetName(facetId);
						template.push("<tr><th>", facet, ":</th><td colspan='10'>");
						template.push("<div class='facetValue selectedValue' data-id='", restrictions[i], "'>", facetValue.text(), "</div>");
						template.push("</td></tr>");
					}
					
					template.push("<tr>");
					
					var columns = [];
					// FILTER FACETS
					if (data.facets) {
						var facets = data.facets;
						var visibleValues = {};
						for (var i=0; i < facets.length; i++) {
							var facet = facets[i];
							
							if (data.items && !restrictionFacets[facet.id]) {
								columns.push(facet.id);
								template.push("<th>", getFacetName(facet.id), "</th>");
							}
							
							var values = facet.values;
							for (var j=0; j < values.length; j++) {
								visibleValues[values[j].id] = true;
							}
						}
						
						$(".facetValue").each(function () {
							var id = $(this).data("id");
							if (restrictions.indexOf(id) >= 0) {
								$(this).addClass("selectedValue");
								$(this).addClass("visible");
								$(this).show();
							} else if (visibleValues[id] == true) {
								$(this).removeClass("selectedValue");
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
					}
					template.push("</tr></thead>");
					
					
					// SHOW ITEMS
					if (data.items) {
						var items = data.items;

						template.push("<tbody>");
						
						var previousValues = [];
						for (var i=0; i < items.length; i++) {
							var item = items[i];
							var values = item.values;
							var columnValues = [];
							template.push("<tr class='itemRow ", (i % 2 == 0 ? "odd" : "even"),"'>")
							for (var j=0; j < values.length; j++) {
								var value = values[j];
								var colIndex = columns.indexOf(getFacetId(value));
								if (colIndex >= 0) {
									if (value == previousValues[j]) {
										var id = toID(value);
										columnValues[colIndex] = "<td><div class='facetValueDuplicate' data-id='" + value + "'>" + $("#" + id).text() + "</div></td>";
									} else {
										var id = toID(value);
										columnValues[colIndex] = "<td><div class='facetValue' data-id='" + value + "'>" + $("#" + id).text() + "</div></td>";
									}
								}
							}
							template.push(columnValues.join(""));
							template.push("<td><div class='itemValue'> = ", item.value, "</div></td>");
							template.push("</div>");
							previousValues = values;
						}
						template.push("</tbody>");
					} 
					
					template.push("</table>");
					$("#results").html(template.join(""));
					
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
				printFacet(facets[i], template);
			}
			
			$("#facets").html(template.join(""));
		}
	});

});


