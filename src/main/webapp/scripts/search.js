
String.prototype.startsWith = function(str) {return (this.match("^"+str)==str)}

jQuery.ajaxSettings.traditional = true;

$(document).ready(function(){

	var restrictions = [];

	var allFacets = {};
	
	var allValues = {};
	
	var getFacetName = function(facetId) {
		return allFacets[facetId].name;
	}
	
	var getValueName = function(valueId) {
		return allValues[valueId].name;
	}
	
	var getFacetId = function(facetValue) {
		return allValues[facetValue].facet.id;
	}
	
	var printFacet = function(facet, template) {
		template.push("<div class='facet' id='", toID(facet.id), "' data-id='", facet.id, "'><h3 class='facetTitle'>", facet.name, "</h3>");
		allFacets[facet.id] = facet;
		
		var values = facet.values;
		for (var i=0; i < values.length; i++) {
			var value = values[i];
			value.facet = facet;
			template.push("<div class='facetValue' id='", toID(value.id), "' data-id='", value.id,"' data-facet='", facet.id, "'>", value.name);
			if (value.description) {
				template.push("<img src='images/info-16x16.png' alt='Click for more information' class='facetValueInfo' data-id='", value.id,"'/>");
			}
			template.push("</div>");
			allValues[value.id] = value;
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

	
	$(".facetValueInfo").live("click",function(event) {
		var id = $(event.target).data("id");
		var value = allValues[id];
		if (value.description) {
			var popup = $("#popup");
			popup.html("<h3>" + value.name + "</h3>" + value.description.replace(/\n/g, "</br>"));
			popup.show();
		}
		return false;
	});
	
	$("#popup").click(function(event){
		$(this).hide();
		return false;
	});
	
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
			$(".selectedValue").removeClass("selectedValue");
			$("#results").html("");
		} else {
			$("#results").html("<img src='images/ajax-loader.gif' alt='Loading results'/>");
			$.ajax({
				url: "search", 
				datatype: "json",
				data: {"value": restrictions, "include": ["items", "facets"]},
				error: function(xhr, textStatus, errorThrown) {
					$("#results").html(xhr.responseText);
				},
				success: function(data){
					var template = [];

					template.push("<table class='items'><thead>");
					
					var restrictionFacets = {};
					for (var i=0; i < restrictions.length; i++) {
						var restriction = allValues[restrictions[i]];
						var facet = restriction.facet;
						restrictionFacets[facet.id] = true;
						template.push("<tr><th class='restriction'>", facet.name, ":</th><td colspan='10'><div class='facetValue selectedValue' data-id='", 
								restriction.id, "'>", restriction.name);
						if (restriction.description) {
							template.push("<img src='images/info-16x16.png' alt='Click for more information' class='facetValueInfo' data-id='", restriction.id,"'/>");
						}
						template.push("</div></td></tr>");
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
					// SHOW ITEMS
					if (data.items) {
						template.push("<th>Tilastoarvo</th><th>Yksikk&ouml;</th></tr></thead>");
						var items = data.items;

						template.push("<tbody>");
						
						var previousValues = [];
						for (var i=0; i < items.length; i++) {
							var item = items[i];
							var values = item.values;
							var columnValues = [];
							var units = null;
							template.push("<tr class='itemRow ", (i % 2 == 0 ? "odd" : "even"),"'>")
							for (var j=0; j < values.length; j++) {
								var value = allValues[values[j]];
								var colIndex = columns.indexOf(value.facet.id);
								if (colIndex >= 0) {
									var columnTemplate = [];
									if (value == previousValues[j]) {
										 columnTemplate.push("<td><div class='facetValueDuplicate' data-id='", value.id, "'>", value.name);
									} else {
										columnTemplate.push("<td><div class='facetValue' data-id='", value.id, "'>", value.name);
									}
									if (restriction.description) {
										columnTemplate.push("<img src='images/info-16x16.png' alt='Click for more information' class='facetValueInfo' data-id='", restriction.id, "'/>");
									}
									columnTemplate.push("</div></td>");
									columnValues[colIndex] = columnTemplate.join("");
								}
								if (value.units) {
									units = value.units;
								}
							}
							template.push(columnValues.join(""));
							template.push("<td><div class='itemValue'> = ", item.value, "</div></td><td>", units, "</td>");
							template.push("</div>");
							previousValues = values;
						}
						template.push("</tbody>");
					} else {
						template.push("</tr></thead>");
					}
					
					template.push("</table>");
					$("#results").html(template.join(""));
					
				}
			});
		}
	});

});


